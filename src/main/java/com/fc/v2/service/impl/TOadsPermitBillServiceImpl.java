package com.fc.v2.service.impl;

import com.fc.v2.common.exception.permit.PermitRuleException;
import com.fc.v2.mapper.auto.TOadsPermitBillMapper;
import com.fc.v2.mapper.auto.TOadsPermitSignMapper;
import com.fc.v2.mapper.auto.TOadsSpotChangeMapper;
import com.fc.v2.mapper.auto.TOadsSpotMapper;
import com.fc.v2.model.auto.TOadsPermitBill;
import com.fc.v2.model.auto.TOadsPermitSign;
import com.fc.v2.model.auto.TOadsSpot;
import com.fc.v2.model.auto.TOadsSpotChange;
import com.fc.v2.model.custom.PermitBillView;
import com.fc.v2.model.custom.PermitLedgerCheck;
import com.fc.v2.model.custom.PermitSignOutcome;
import com.fc.v2.service.ITOadsPermitBillService;
import com.fc.v2.service.permit.PermitChangeKind;
import com.fc.v2.service.permit.PermitStageRule;
import com.fc.v2.service.permit.PermitStageRule.Verdict;
import com.fc.v2.util.SnowflakeIdWorker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设置许可核准单 Service 实现——八条口径的唯一裁决处。
 *
 * <p>段次怎么数、下一段何时开、清轮抹去哪一段、封存后接不接补笔，答案只从
 * {@link PermitStageRule} 与本类的签署流程点出。单据表上的 mode/need、报文自称的段次、
 * 页面回显的人数都不作数。
 *
 * @author fuce
 * @date 2026-10-05
 */
@Service
public class TOadsPermitBillServiceImpl implements ITOadsPermitBillService {

    /** 在签（含第三段一同一异的待议） */
    private static final int STATUS_RUNNING = 0;
    /** 已点齐（走满封存） */
    private static final int STATUS_SEALED = 1;
    /** 本段本轮被否、已压回重走 */
    private static final int STATUS_PUSHED_BACK = 2;

    @javax.annotation.Resource
    private TOadsPermitBillMapper billMapper;
    @javax.annotation.Resource
    private TOadsPermitSignMapper signMapper;
    @javax.annotation.Resource
    private TOadsSpotMapper spotMapper;
    @javax.annotation.Resource
    private TOadsSpotChangeMapper spotChangeMapper;

    // ---------------------------------------------------------------- 建单

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TOadsPermitBill createFromSpot(Long spotId, String changeKind, String operator) {
        if (spotId == null) {
            throw new PermitRuleException("核准单只能由点位主档那一行立来，须点出点位主档行");
        }
        if (operator == null || operator.trim().isEmpty()) {
            throw new PermitRuleException("口头报备不算开工：建单须落建单人");
        }
        TOadsSpot spot = spotMapper.selectById(spotId);
        if (spot == null || (spot.getDelFlag() != null && spot.getDelFlag() != 0)) {
            throw new PermitRuleException("点位主档上查无此行，无从立单：" + spotId);
        }
        PermitChangeKind kind = PermitChangeKind.of(changeKind);
        kind.requireCurrentState(currentState(spot));

        // 同一点位、同一次变更在途只对应一张；live_token 唯一索引兜底
        String token = spotId + ":" + kind.getCode();
        if (billMapper.countByLiveToken(token) > 0) {
            throw new PermitRuleException("同一点位同一种变更已有在途核准单，不许另起一张");
        }

        TOadsPermitBill bill = new TOadsPermitBill();
        bill.setBillNo("XK" + SnowflakeIdWorker.getUUID());
        bill.setSpotId(spot.getId());
        bill.setSpotNo(spot.getSpotNo());
        bill.setChangeKind(kind.getCode());
        bill.setFromState(kind.getFromState());
        bill.setToState(kind.getToState());
        // 起步永远落在第一段，由规则本点出首段的数法
        PermitStageRule first = PermitStageRule.WINDOW;
        bill.setNodeNo(first.getNode());
        bill.setSignMode(first.getModeCode());
        bill.setNeedCount(first.getNeedCount());
        bill.setSignCount(0);
        bill.setRoundNo(1);
        bill.setStatus(STATUS_RUNNING);
        bill.setLiveToken(token);
        bill.setDelFlag(0);
        bill.setCreateBy(operator);
        billMapper.insert(bill);
        return bill;
    }

    // ---------------------------------------------------------------- 签署（唯一入口）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermitSignOutcome sign(Long billId, String signer, Integer opinion, String content, Integer claimNode) {
        return signAt(billId, signer, opinion, content, new Date(), claimNode);
    }

    /**
     * 落单时刻显式入参的口子，供同一处理里取时与事后核对；控制器仍只走五参的 {@link #sign}。
     * 事务由 {@link #sign} 从代理入口开启，本方法内部不再自开。
     */
    public PermitSignOutcome signAt(Long billId, String signer, Integer opinion, String content, Date landingAt,
                                    Integer claimNode) {
        if (billId == null) {
            throw new PermitRuleException("须点出哪一张核准单");
        }
        if (signer == null || signer.trim().isEmpty()) {
            throw new PermitRuleException("须落签署人，不接受无名一笔");
        }
        if (opinion == null
                || (opinion != PermitStageRule.OPINION_AGREE && opinion != PermitStageRule.OPINION_REJECT)) {
            throw new PermitRuleException("判断只认 1同意 / 2不同意");
        }
        // 行锁：同秒两笔也逐笔串行，不并笔
        TOadsPermitBill bill = billMapper.selectByIdForUpdate(billId);
        if (bill == null) {
            throw new PermitRuleException("核准单不存在：" + billId);
        }
        // 第6条：走满即封存，封存后一笔都不接
        if (bill.getStatus() != null && bill.getStatus() == STATUS_SEALED) {
            throw new PermitRuleException("本单已封存，不得再进一笔、不得改一字、不得整张撤回");
        }

        // 段次只从单据当前值取，再交规则本；报文自称第几段只用于核对、不作记账依据
        int node = bill.getNodeNo() == null ? 0 : bill.getNodeNo();
        if (claimNode != null && claimNode.intValue() != node) {
            // 第4条：上一段未通过，跳段落进来的意见整笔丢弃，库里一条不添
            return PermitSignOutcome.of(PermitSignOutcome.DISCARDED,
                    "报文称第" + (claimNode + 1) + "段，本单实际停在第" + (node + 1) + "段；"
                            + "上一段未通过，这一笔不予采信、整笔丢弃，整单仍留在第" + (node + 1) + "段",
                    bill);
        }
        PermitStageRule rule;
        try {
            rule = PermitStageRule.ofNode(node);
        } catch (IllegalArgumentException e) {
            throw new PermitRuleException(e.getMessage());
        }
        int round = bill.getRoundNo() == null ? 1 : bill.getRoundNo();

        List<TOadsPermitSign> active = signMapper.listActiveRound(billId, node, round);

        // 第5条：同一段第二次提交，保留首次那句，不覆盖也不追加副本
        for (TOadsPermitSign exist : active) {
            if (signer.equals(exist.getSigner())) {
                throw new PermitRuleException("同一签署人在本段本轮已有落单，保留首次那句，不许再落副本");
            }
        }
        if (active.size() >= rule.getNeedCount()) {
            throw new PermitRuleException("本段本轮人数已点齐，不该再有笔进来");
        }

        Date at = landingAt == null ? new Date() : landingAt;
        TOadsPermitSign row = new TOadsPermitSign();
        row.setBillId(billId);
        row.setNodeNo(node);
        row.setRoundNo(round);
        row.setSeqNo(active.size() + 1);
        row.setSignPost(rule.getPostName());
        row.setSigner(signer);
        row.setOpinion(opinion);
        row.setContent(content);
        row.setSignTime(at);
        row.setActive(1);
        row.setCreateBy(signer);
        signMapper.insert(row);

        // 结论只让规则本按本段数法点；同秒两笔也按各自时刻排先后，不并成一条
        List<TOadsPermitSign> landed = new ArrayList<TOadsPermitSign>(active);
        landed.add(row);
        Verdict verdict = rule.judge(landed);

        switch (verdict) {
            case WAIT:
                bill.setSignCount(landed.size());
                bill.setStatus(STATUS_RUNNING);
                billMapper.updateById(bill);
                return PermitSignOutcome.of(PermitSignOutcome.LANDED,
                        rule.getStageName() + "已落" + landed.size() + "笔，还差"
                                + (rule.getNeedCount() - landed.size()) + "笔，整单留在本段",
                        bill);
            case VETO:
                clearCurrentRound(bill, rule, round);
                bill.setStatus(STATUS_PUSHED_BACK);
                billMapper.updateById(bill);
                return PermitSignOutcome.of(PermitSignOutcome.VETO_CLEARED,
                        rule.getStageName() + "给出不同意结论：只清本段本轮" + landed.size()
                                + "笔，前段记录一条不动；本段起第" + bill.getRoundNo() + "轮重走",
                        bill);
            case SPLIT:
                // 第三段一同一异：既不推进也不终止；本轮另存、本段重走，整单挂在待议
                clearCurrentRound(bill, rule, round);
                bill.setStatus(STATUS_RUNNING);
                billMapper.updateById(bill);
                return PermitSignOutcome.of(PermitSignOutcome.SPLIT_PENDING,
                        "市局备案一条同意一条不同意，整单挂在待议环节，不推进不终止；"
                                + "本轮两条留痕另存，市局备案段起第" + bill.getRoundNo() + "轮重新签",
                        bill);
            case PASS:
            default:
                if (rule.next() != null) {
                    // 第4条：本段记为通过，下一段才开放；新段从头轮起点
                    PermitStageRule next = rule.next();
                    bill.setNodeNo(next.getNode());
                    bill.setSignMode(next.getModeCode());
                    bill.setNeedCount(next.getNeedCount());
                    bill.setSignCount(0);
                    bill.setRoundNo(1);
                    bill.setStatus(STATUS_RUNNING);
                    billMapper.updateById(bill);
                    return PermitSignOutcome.of(PermitSignOutcome.ADVANCED,
                            rule.getStageName() + "通过，整单推进至" + next.getStageName(), bill);
                }
                // 第三段通过＝走满封存：主档与履历出自同一次处理
                seal(bill, rule, landed.size(), signer, at);
                return PermitSignOutcome.of(PermitSignOutcome.SEALED,
                        "三段签署齐备，本单封存；点位主档已同步变更并追加履历，此后不再接补笔", bill);
        }
    }

    /**
     * 第5条：清除粒度是一段——只把本单本段本轮的在档签署退出（active 置 0，行不删），
     * 此前各段与本段老轮的留痕一条不动。
     */
    private void clearCurrentRound(TOadsPermitBill bill, PermitStageRule rule, int round) {
        signMapper.deactivateRound(bill.getId(), rule.getNode(), round);
        bill.setSignCount(0);
        bill.setRoundNo(round + 1);
    }

    /**
     * 第6/7条：封存单据、同步点位主档、追加变更履历，同一事务同生共死。
     */
    private void seal(TOadsPermitBill bill, PermitStageRule lastRule, int landedCount, String operator, Date at) {
        TOadsSpot spot = spotMapper.selectById(bill.getSpotId());
        if (spot == null) {
            throw new PermitRuleException("封存时点位主档行不见了，主档与履历都不许动：" + bill.getSpotId());
        }
        PermitChangeKind kind = PermitChangeKind.of(bill.getChangeKind());
        int fromState = currentState(spot);
        int fromVer = spot.getSetVer() == null ? 0 : spot.getSetVer();
        int toState = kind.getToState();
        int toVer = fromVer + (kind.isBumpVer() ? 1 : 0);

        // 主档那一行：封存这一刻才许挪
        spot.setSetState(toState);
        spot.setSetVer(toVer);
        spot.setUpdateBy(operator);
        spot.setUpdateTime(at);
        spotMapper.updateById(spot);

        // 履历：由谁操作、依据哪张单、从哪档改到哪档、什么时刻，逐条记下
        TOadsSpotChange change = new TOadsSpotChange();
        change.setSpotId(spot.getId());
        change.setSpotNo(spot.getSpotNo());
        change.setBillId(bill.getId());
        change.setBillNo(bill.getBillNo());
        change.setChangeKind(kind.getCode());
        change.setFromState(fromState);
        change.setToState(toState);
        change.setFromVer(fromVer);
        change.setToVer(toVer);
        change.setOperateBy(operator);
        change.setChangeTime(at);
        change.setCreateBy(operator);
        spotChangeMapper.insert(change);

        // 单据自身封存
        bill.setSignCount(landedCount);
        bill.setStatus(STATUS_SEALED);
        bill.setLiveToken(null);
        bill.setSealedAt(at);
        bill.setUpdateBy(operator);
        bill.setUpdateTime(at);
        billMapper.updateById(bill);
    }

    // ---------------------------------------------------------------- 回显

    @Override
    public PermitBillView getBillDetail(Long id) {
        TOadsPermitBill bill = billMapper.selectById(id);
        if (bill == null || (bill.getDelFlag() != null && bill.getDelFlag() != 0)) {
            throw new PermitRuleException("核准单不存在：" + id);
        }
        List<TOadsPermitSign> all = signMapper.listByBill(id);

        PermitBillView view = new PermitBillView();
        view.setBill(bill);
        int currentNode = bill.getNodeNo() == null ? 0 : bill.getNodeNo();
        boolean sealed = bill.getStatus() != null && bill.getStatus() == STATUS_SEALED;
        int billRound = bill.getRoundNo() == null ? 1 : bill.getRoundNo();

        for (PermitStageRule rule : PermitStageRule.values()) {
            PermitBillView.Stage stage = new PermitBillView.Stage();
            stage.setNodeNo(rule.getNode());
            stage.setStageName(rule.getStageName());
            stage.setPostName(rule.getPostName());
            stage.setNeedCount(rule.getNeedCount());
            stage.setSignMode(rule.getModeCode());
            stage.setCurrent(!sealed && rule.getNode() == currentNode);

            List<TOadsPermitSign> nodeSigns = new ArrayList<TOadsPermitSign>();
            int lastRound = 1;
            for (TOadsPermitSign s : all) {
                if (s.getNodeNo() != null && s.getNodeNo() == rule.getNode()) {
                    nodeSigns.add(s);
                    if (s.getRoundNo() != null && s.getRoundNo() > lastRound) {
                        lastRound = s.getRoundNo();
                    }
                }
            }
            // 当前段展示服务端单据轮次；走过的段没有更新轮次，按其最后一轮展示
            int showRound = rule.getNode() == currentNode && !sealed ? billRound : lastRound;
            stage.setRoundNo(showRound);
            stage.setSigns(nodeSigns);

            int activeCount = 0;
            for (TOadsPermitSign s : nodeSigns) {
                if (s.getActive() != null && s.getActive() == 1 && s.getRoundNo() != null
                        && s.getRoundNo() == showRound) {
                    activeCount++;
                }
            }
            stage.setSignCount(activeCount);
            // 封存则三段齐过；未封存时停在当前段，走过的段一律 node 更小
            stage.setPassed(sealed || rule.getNode() < currentNode);
            view.getStages().add(stage);
        }
        return view;
    }

    @Override
    public List<TOadsPermitBill> selectBillList(com.baomidou.mybatisplus.core.conditions.Wrapper<TOadsPermitBill> queryWrapper) {
        return billMapper.selectList(queryWrapper);
    }

    // ---------------------------------------------------------------- 计数与对账

    @Override
    public long unsealedCount() {
        return billMapper.countUnsealed();
    }

    @Override
    public PermitLedgerCheck reconcile() {
        PermitLedgerCheck check = new PermitLedgerCheck();
        List<TOadsPermitBill> all = billMapper.selectAllNormal();
        List<TOadsPermitSign> activeSigns = signMapper.listAllActive();

        long topNumber = 0;
        long recountedUnsealed = 0;
        for (TOadsPermitBill bill : all) {
            boolean tableSealed = bill.getStatus() != null && bill.getStatus() == STATUS_SEALED;
            if (!tableSealed) {
                topNumber++;
            }
            // 只凭 append-only 留痕重算这张单走到哪、当前段几笔
            Recounted rc = recountOne(bill, activeSigns);
            if (!rc.sealed) {
                recountedUnsealed++;
            }
            String name = bill.getBillNo() == null ? String.valueOf(bill.getId()) : bill.getBillNo();
            if (tableSealed != rc.sealed) {
                check.getMismatches().add("单据 " + name + "：账面" + (tableSealed ? "已封存" : "未封存")
                        + "，留痕重算为" + (rc.sealed ? "已封存" : "未封存"));
            }
            if (!tableSealed) {
                int tableNode = bill.getNodeNo() == null ? 0 : bill.getNodeNo();
                int tableCount = bill.getSignCount() == null ? 0 : bill.getSignCount();
                if (tableNode != rc.node) {
                    check.getMismatches().add("单据 " + name + "：账面停在第" + (tableNode + 1) + "段，"
                            + "留痕重算停在第" + (rc.node + 1) + "段");
                }
                if (tableCount != rc.signCount) {
                    check.getMismatches().add("单据 " + name + "：账面本段已签" + tableCount + "笔，"
                            + "逐笔累加为" + rc.signCount + "笔");
                }
                try {
                    PermitStageRule rule = PermitStageRule.ofNode(rc.node);
                    if (bill.getNeedCount() == null || bill.getNeedCount() != rule.getNeedCount()
                            || bill.getSignMode() == null || bill.getSignMode() != rule.getModeCode()) {
                        check.getMismatches().add("单据 " + name + "：本段人数/数法与规则本不一致");
                    }
                } catch (IllegalArgumentException ignore) {
                    check.getMismatches().add("单据 " + name + "：段次越界");
                }
            }
        }
        check.setUnsealedCount(topNumber);
        check.setRecountedCount(recountedUnsealed);
        check.setBalanced(topNumber == recountedUnsealed && check.getMismatches().isEmpty());
        return check;
    }

    /** 只按这张单的逐笔留痕重算：走到哪段、当前轮几笔、封存没有。 */
    private Recounted recountOne(TOadsPermitBill bill, List<TOadsPermitSign> allActive) {
        Map<Integer, List<TOadsPermitSign>> grouped = new HashMap<Integer, List<TOadsPermitSign>>();
        for (TOadsPermitSign s : allActive) {
            if (bill.getId().equals(s.getBillId())) {
                int node = s.getNodeNo() == null ? 0 : s.getNodeNo();
                int round = s.getRoundNo() == null ? 1 : s.getRoundNo();
                int key = node * 1000 + round;
                List<TOadsPermitSign> list = grouped.get(key);
                if (list == null) {
                    list = new ArrayList<TOadsPermitSign>();
                    grouped.put(key, list);
                }
                list.add(s);
            }
        }
        Recounted rc = new Recounted();
        int node = 0;
        int guard = 0;
        while (guard++ < 4) {
            PermitStageRule rule;
            try {
                rule = PermitStageRule.ofNode(node);
            } catch (IllegalArgumentException e) {
                break;
            }
            int round = latestRound(grouped, node);
            List<TOadsPermitSign> active = roundKey(grouped, node, round);
            PermitStageRule.byLandingOrder(active);
            rc.node = node;
            rc.round = round;
            rc.signCount = active.size();
            if (active.size() < rule.getNeedCount()) {
                rc.sealed = false;
                return rc;
            }
            Verdict verdict = rule.judge(new ArrayList<TOadsPermitSign>(active));
            if (verdict != Verdict.PASS) {
                // 被否或待议：整单留在本段
                rc.sealed = false;
                return rc;
            }
            if (rule.next() == null) {
                rc.sealed = true;
                return rc;
            }
            node++;
        }
        rc.sealed = false;
        return rc;
    }

    private int latestRound(Map<Integer, List<TOadsPermitSign>> map, int node) {
        int max = 1;
        for (Integer key : map.keySet()) {
            if (key / 1000 == node && key % 1000 > max) {
                max = key % 1000;
            }
        }
        return max;
    }

    private List<TOadsPermitSign> roundKey(Map<Integer, List<TOadsPermitSign>> map, int node, int round) {
        List<TOadsPermitSign> list = map.get(node * 1000 + round);
        return list == null ? new ArrayList<TOadsPermitSign>() : list;
    }

    // ---------------------------------------------------------------- 登记簿与履历

    @Override
    public List<TOadsSpotChange> spotChangeHistory(Long spotId) {
        if (spotId == null) {
            throw new PermitRuleException("须点出点位主档行");
        }
        return spotChangeMapper.listBySpot(spotId);
    }

    @Override
    public List<TOadsPermitSign> acceptanceRegister(Date day) {
        if (day == null) {
            throw new PermitRuleException("须点出哪一日的受理登记簿");
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(day);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date start = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date end = cal.getTime();
        // 逐笔吻合：被清退出的老轮也算当日落过的笔，一并入簿
        return signMapper.listLandedBetween(start, end);
    }

    // ---------------------------------------------------------------- 杂项

    private int currentState(TOadsSpot spot) {
        return spot.getSetState() == null ? PermitChangeKind.STATE_UNSET : spot.getSetState();
    }

    /** 留痕重算结果。 */
    private static class Recounted {
        private int node;
        private int round = 1;
        private int signCount;
        private boolean sealed;
    }
}
