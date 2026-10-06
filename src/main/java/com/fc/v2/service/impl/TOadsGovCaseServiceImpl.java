package com.fc.v2.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.exception.gov.GovCaseRuleException;
import com.fc.v2.mapper.auto.TOadsAreaBookMapper;
import com.fc.v2.mapper.auto.TOadsGovCaseIntakeMapper;
import com.fc.v2.mapper.auto.TOadsGovCaseLogMapper;
import com.fc.v2.mapper.auto.TOadsGovCaseMapper;
import com.fc.v2.mapper.auto.TOadsGovCaseReceiptMapper;
import com.fc.v2.mapper.auto.TOadsGovCaseWitnessMapper;
import com.fc.v2.mapper.auto.TOadsPermitBillMapper;
import com.fc.v2.mapper.auto.TOadsSpotMapper;
import com.fc.v2.model.auto.TOadsAreaBook;
import com.fc.v2.model.auto.TOadsGovCase;
import com.fc.v2.model.auto.TOadsGovCaseIntake;
import com.fc.v2.model.auto.TOadsGovCaseLog;
import com.fc.v2.model.auto.TOadsGovCaseReceipt;
import com.fc.v2.model.auto.TOadsGovCaseWitness;
import com.fc.v2.model.auto.TOadsPermitBill;
import com.fc.v2.model.auto.TOadsSpot;
import com.fc.v2.model.custom.GovCaseForm;
import com.fc.v2.model.custom.GovCaseOutcome;
import com.fc.v2.model.custom.GovCaseTrace;
import com.fc.v2.service.ITOadsGovCaseService;
import com.fc.v2.service.gov.GovCaseStep;
import com.fc.v2.util.SnowflakeIdWorker;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 监管办件四步流程唯一裁决处实现。
 *
 * <p>四个问题只在本类给答案，旁处不做判定、不预设结论：当下第几步（按逐笔留痕重数）、
 * 下一步准入齐不齐（许可三段/核收同意/两名见证＋凭证）、封存后接不接新意见（不接）、
 * 同一步第二次批语怎么放（留首次，不覆盖不追加）。办件主档与留痕处联动同一件事，
 * 回看步数不另取算法；不借邻步旧结论，不拿别的办件步数来套。
 *
 * <p>并发：每个改动先按主键行锁（FOR UPDATE）串行，再叠加幂等键唯一索引：
 * 同批并发两回同键，后一笔等前一笔提交后只回首条结果，不添第二行、不并成一条、不丢一条。
 *
 * @author fuce
 * @date 2026-10-06
 */
@Service
public class TOadsGovCaseServiceImpl implements ITOadsGovCaseService {

    private static final int DEL_OK = 0;
    /** 设置许可三段签署走完：核准单封存状态 */
    private static final int PERMIT_SEALED = 1;
    /** 验收要两名见证岗各一笔 */
    private static final int WITNESS_NEED = 2;

    @javax.annotation.Resource
    private TOadsGovCaseMapper govCaseMapper;
    @javax.annotation.Resource
    private TOadsGovCaseLogMapper logMapper;
    @javax.annotation.Resource
    private TOadsGovCaseWitnessMapper witnessMapper;
    @javax.annotation.Resource
    private TOadsGovCaseReceiptMapper receiptMapper;
    @javax.annotation.Resource
    private TOadsGovCaseIntakeMapper intakeMapper;
    @javax.annotation.Resource
    private TOadsPermitBillMapper permitBillMapper;
    @javax.annotation.Resource
    private TOadsSpotMapper spotMapper;
    @javax.annotation.Resource
    private TOadsAreaBookMapper areaBookMapper;

    // ================================================================ 立件前报入与撤销

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TOadsGovCaseIntake intake(GovCaseForm form, String operator) {
        if (form == null) {
            throw new GovCaseRuleException("立件四项输入一项都没收到：点位编码、所属路段代号、处数、拟判定的校核分值，本件不收");
        }
        // 幂等重放先于一切：同键只认先落那一条。
        if (present(form.getIdemKey())) {
            TOadsGovCaseIntake exist = intakeMapper.selectByIdemKey(form.getIdemKey().trim());
            if (exist != null) {
                return exist;
            }
        }
        String spotNo = requireText(form.getSpotNo(), "点位编码");
        String areaNo = requireText(form.getAreaNo(), "所属路段代号");
        int placeCount = requireInt(form.getPlaceCountText(), "处数");
        int checkScore = requireInt(form.getCheckScoreText(), "拟判定的校核分值");
        if (placeCount <= 0) {
            throw new GovCaseRuleException("处数须为正整数，报来的 " + form.getPlaceCountText().trim() + " 不作数");
        }
        if (checkScore < 0) {
            throw new GovCaseRuleException("拟判定的校核分值不得为负，报来的 " + form.getCheckScoreText().trim() + " 不作数");
        }
        // 正式立件前那一小段也不许同点位排两条：有报入在档、或已有在走办件，都立不起来。
        TOadsGovCase running = findRunning(spotNo);
        if (running != null) {
            throw new GovCaseRuleException("点位 " + spotNo + " 已有在走办件 " + running.getCaseNo()
                    + "（停在「" + GovCaseStep.nameOf(currentStep(running.getId())) + "」），第二件立不起来");
        }
        TOadsGovCaseIntake open = findOpenIntake(spotNo);
        if (open != null) {
            throw new GovCaseRuleException("点位 " + spotNo + " 已有报入在档 " + open.getIntakeNo()
                    + " 尚未正式立件，先立件或撤销后才许再报");
        }

        Date now = new Date();
        TOadsGovCaseIntake row = new TOadsGovCaseIntake();
        row.setIntakeNo("BR" + SnowflakeIdWorker.getUUID());
        row.setSpotNo(spotNo);
        row.setAreaNo(areaNo);
        row.setPlaceCount(placeCount);
        row.setCheckScore(checkScore);
        row.setState(GovCaseStep.INTAKE_OPEN);
        row.setIdemKey(present(form.getIdemKey()) ? form.getIdemKey().trim() : null);
        row.setCreateBy(operator);
        row.setCreateTime(now);
        try {
            intakeMapper.insert(row);
        } catch (DuplicateKeyException e) {
            // 同批并发同键：只认先落那一条。
            if (present(form.getIdemKey())) {
                return intakeMapper.selectByIdemKey(form.getIdemKey().trim());
            }
            throw e;
        }
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TOadsGovCaseIntake revokeIntake(Long intakeId, String operator) {
        if (intakeId == null) {
            throw new GovCaseRuleException("撤销须点出哪一条报入暂存");
        }
        TOadsGovCaseIntake row = intakeMapper.selectRowById(intakeId);
        if (row == null) {
            throw new GovCaseRuleException("报入暂存查无此条：" + intakeId);
        }
        if (GovCaseStep.INTAKE_OPENED.equals(row.getState())) {
            throw new GovCaseRuleException("报入 " + row.getIntakeNo() + " 已正式立件（办件 "
                    + row.getOpenedCaseId() + "），撤销只受理立件之前那一小段，此条不撤");
        }
        if (GovCaseStep.INTAKE_REVOKED.equals(row.getState())) {
            // 撤销重放：本就已撤，原条返回，不另造一笔。
            return row;
        }
        row.setState(GovCaseStep.INTAKE_REVOKED);
        row.setUpdateBy(operator);
        row.setUpdateTime(new Date());
        intakeMapper.updateById(row);
        // 被撤记录不进主档、不占未走完位置——本行以外什么都不动。
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GovCaseOutcome open(Long intakeId, String operator) {
        if (intakeId == null) {
            throw new GovCaseRuleException("正式立件须点出哪一条报入暂存");
        }
        TOadsGovCaseIntake row = intakeMapper.selectRowById(intakeId);
        if (row == null) {
            throw new GovCaseRuleException("报入暂存查无此条：" + intakeId);
        }
        if (GovCaseStep.INTAKE_REVOKED.equals(row.getState())) {
            throw new GovCaseRuleException("报入 " + row.getIntakeNo() + " 已撤销，不进主档，不能再立件");
        }
        if (GovCaseStep.INTAKE_OPENED.equals(row.getState()) && row.getOpenedCaseId() != null) {
            // 立件并发重放：只回首条立起来的那件。
            return outcomeOf(row.getOpenedCaseId(), null, null, GovCaseStep.DECLARE, true);
        }

        // 同点位在走独占（库内 uk_gov_case_spot_running 再兜一道）。
        TOadsGovCase running = findRunning(row.getSpotNo());
        if (running != null) {
            throw new GovCaseRuleException("点位 " + row.getSpotNo() + " 已有在走办件 " + running.getCaseNo()
                    + "，报入 " + row.getIntakeNo() + " 立不起来");
        }
        // 办件跟着一处在册点位走：点位与所属路段都须在册，才立得起来。
        Long spotId = resolveSpotId(row.getSpotNo());
        if (spotId == null) {
            throw new GovCaseRuleException("点位编码 " + row.getSpotNo() + " 在点位底册里查无在册行，"
                    + "办件跟着在册点位走，报入 " + row.getIntakeNo() + " 立不起来");
        }
        TOadsAreaBook area = findArea(row.getAreaNo());
        if (area == null) {
            throw new GovCaseRuleException("所属路段代号 " + row.getAreaNo() + " 在路段目录里找不到，"
                    + "报入 " + row.getIntakeNo() + " 立不起来");
        }
        if (area.getStatus() != null && area.getStatus() != 0) {
            throw new GovCaseRuleException("所属路段 " + row.getAreaNo() + "（" + area.getAreaName()
                    + "）已从目录撤出，不是在册路段，报入 " + row.getIntakeNo() + " 立不起来");
        }

        Date now = new Date();
        TOadsGovCase c = new TOadsGovCase();
        c.setCaseNo("BG" + SnowflakeIdWorker.getUUID());
        c.setSpotId(spotId);
        c.setSpotNo(row.getSpotNo());
        c.setAreaNo(row.getAreaNo());
        c.setPlaceCount(row.getPlaceCount());
        c.setCheckScore(row.getCheckScore());
        c.setStepNo(GovCaseStep.DECLARE);
        c.setOutcome(GovCaseStep.OUTCOME_RUNNING);
        c.setIdemKey("OPEN:" + row.getId());
        c.setDelFlag(DEL_OK);
        c.setCreateBy(operator);
        c.setCreateTime(now);
        try {
            govCaseMapper.insert(c);
        } catch (DuplicateKeyException e) {
            TOadsGovCase d = findRunning(row.getSpotNo());
            if (d != null) {
                throw new GovCaseRuleException("点位 " + row.getSpotNo() + " 已有在走办件 " + d.getCaseNo()
                        + "（并发立件只认先落那一件），本件不立第二条");
            }
            throw e;
        }
        // 落第一步：申报。步次由系统落，报文里带了也只作参考。
        appendLog(c, GovCaseStep.DECLARE, 1, GovCaseStep.ACTION_REACH,
                "申报立件：处数" + c.getPlaceCount() + "、校核分值" + c.getCheckScore(),
                operator, null, 1, now, null);

        row.setState(GovCaseStep.INTAKE_OPENED);
        row.setOpenedCaseId(c.getId());
        row.setUpdateBy(operator);
        row.setUpdateTime(now);
        intakeMapper.updateById(row);

        GovCaseOutcome o = outcomeOf(c.getId(), GovCaseStep.ACTION_REACH, null, GovCaseStep.DECLARE, false);
        o.setMessage("办件 " + c.getCaseNo() + " 已立，落第一步「申报」；步次以服务端留痕为准");
        return o;
    }

    // ================================================================ 四步之间移动

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GovCaseOutcome advance(Long caseId, String note, String operator, Integer claimStep,
                                  String channel, String idemKey) {
        String effKey = actionKey(caseId, idemKey);
        TOadsGovCase c = lockRow(caseId);
        GovCaseOutcome replay = replayIfSeen(caseId, effKey);
        if (replay != null) {
            return replay;
        }
        requireRunning(c);
        Recount rc = recount(caseId);
        int current = rc.step == null ? GovCaseStep.DECLARE : rc.step;
        // 同一步第二次送意见（报文步次点的就是当下这一步）：本行保留第一次那条，
        // 第二次既不覆盖也不追加，整条仍留在原地——不能当成再推一步。
        if (claimStep != null && claimStep.intValue() == current) {
            GovCaseOutcome kept = outcomeOf(caseId, GovCaseStep.ACTION_REACH, null, current, false);
            kept.setRoundNo(rc.roundOf(current));
            kept.setMessage("「" + GovCaseStep.nameOf(current) + "」这一步已有首次意见在档，"
                    + "第二次送意见保留第一次那条，不覆盖也不追加，整条仍留在原地");
            return kept;
        }
        int target = current + 1;
        if (target > GovCaseStep.ACCEPT) {
            throw new GovCaseRuleException("办件 " + c.getCaseNo() + " 已到最后一步「验收」，没有紧邻下一步可推");
        }
        // 报文自称步次只作核对：与服务端数出的下一步对不上，就是跨步，不生效、整条留在原地。
        if (claimStep != null && claimStep.intValue() != target) {
            throw new GovCaseRuleException("办件 " + c.getCaseNo() + " 当下停在「" + GovCaseStep.nameOf(current)
                    + "」，紧邻下一步只能是「" + GovCaseStep.nameOf(target) + "」；报文送的是「"
                    + GovCaseStep.nameOf(claimStep) + "」，跨过中间步不生效，整条仍留在「"
                    + GovCaseStep.nameOf(current) + "」");
        }
        // 下一步准入，逐道点名。
        if (target == GovCaseStep.SETUP && !permitThreeNodesSigned(c)) {
            throw new GovCaseRuleException("办件 " + c.getCaseNo() + " 进「设置」的准入不齐：设置许可三段签署"
                    + "（窗口受理/区局核准/市局备案）尚未走完，未走完不得进入设置，整条仍留在「核准」");
        }
        if (target == GovCaseStep.ACCEPT) {
            TOadsGovCaseReceipt receipt = activeReceipt(caseId, rc.roundOf(GovCaseStep.SETUP));
            if (receipt == null || receipt.getAgree() == null || receipt.getAgree() != 1) {
                throw new GovCaseRuleException("办件 " + c.getCaseNo() + " 进「验收」的准入不齐：设置一步的核收"
                        + (receipt == null ? "还没登记" : "未记为同意") + "，验收不得开始，整条仍留在「设置」");
            }
        }

        int round = maxRound(caseId, target) + 1;
        Date now = new Date();
        appendLog(c, target, round, GovCaseStep.ACTION_REACH, note, operator, channel, 1, now, effKey);
        c.setStepNo(target);
        c.setUpdateBy(operator);
        c.setUpdateTime(now);
        govCaseMapper.updateById(c);

        GovCaseOutcome o = outcomeOf(caseId, GovCaseStep.ACTION_REACH, effKey, target, false);
        o.setRoundNo(round);
        o.setMessage("已走到「" + GovCaseStep.nameOf(target) + "」（第" + round + "轮），系统按逐笔留痕数步次");
        return o;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GovCaseOutcome returnStep(Long caseId, String note, String operator, String idemKey) {
        String effKey = actionKey(caseId, idemKey);
        TOadsGovCase c = lockRow(caseId);
        GovCaseOutcome replay = replayIfSeen(caseId, effKey);
        if (replay != null) {
            return replay;
        }
        requireRunning(c);
        Recount rc = recount(caseId);
        int current = rc.step == null ? 0 : rc.step;
        if (current <= GovCaseStep.DECLARE) {
            throw new GovCaseRuleException("办件 " + c.getCaseNo() + " 已在第一步「申报」，前面没有步可退");
        }
        int round = rc.roundOf(current);
        Date now = new Date();
        // 只清被退那一步当轮：REACH 批语按原先后转追溯（active 置 0 行不删）；该步的核收/见证老轮随同退出。
        logMapper.deactivateStepRound(caseId, current, round);
        if (current == GovCaseStep.SETUP) {
            receiptMapper.deactivateRound(caseId, round);
        }
        if (current == GovCaseStep.ACCEPT) {
            witnessMapper.deactivateRound(caseId, round);
        }
        appendLog(c, current, round, GovCaseStep.ACTION_RETURN, note, operator, null, 0, now, effKey);
        int backTo = current - 1;
        c.setStepNo(backTo);
        c.setUpdateBy(operator);
        c.setUpdateTime(now);
        govCaseMapper.updateById(c);

        GovCaseOutcome o = outcomeOf(caseId, GovCaseStep.ACTION_RETURN, effKey, backTo, false);
        o.setRoundNo(round);
        o.setMessage("已由「" + GovCaseStep.nameOf(current) + "」退回一步；该步第" + round
                + "轮批语按原先后转追溯，重新走到这一步时从新一轮重新累计");
        return o;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GovCaseOutcome recordReceipt(Long caseId, boolean agree, String note, String operator, String idemKey) {
        String effKey = actionKey(caseId, idemKey);
        TOadsGovCase c = lockRow(caseId);
        GovCaseOutcome replay = replayIfSeen(caseId, effKey);
        if (replay != null) {
            return replay;
        }
        requireRunning(c);
        Recount rc = recount(caseId);
        if (rc.step == null || rc.step != GovCaseStep.SETUP) {
            throw new GovCaseRuleException("办件 " + c.getCaseNo() + " 当下停在「" + GovCaseStep.nameOf(rc.step)
                    + "」，核收是「设置」这一步的事，步次没走到这里，本笔不生效");
        }
        int round = rc.roundOf(GovCaseStep.SETUP);
        TOadsGovCaseReceipt first = receiptMapper.findActiveRound(caseId, round);
        if (first != null) {
            // 同一步第二次送：保留首次那条，不覆盖不追加。
            throw new GovCaseRuleException("办件 " + c.getCaseNo() + " 设置步第" + round
                    + "轮核收已记为「" + (first.getAgree() != null && first.getAgree() == 1 ? "同意" : "未同意")
                    + "」，同一步第二次送意见保留首次那条，不覆盖也不追加");
        }
        Date now = new Date();
        TOadsGovCaseReceipt r = new TOadsGovCaseReceipt();
        r.setCaseId(caseId);
        r.setRoundNo(round);
        r.setAgree(agree ? 1 : 0);
        r.setNote(note);
        r.setOperator(operator);
        r.setReceivedAt(now);
        r.setActive(1);
        r.setCreateBy(operator);
        r.setCreateTime(now);
        receiptMapper.insert(r);
        appendLog(c, GovCaseStep.SETUP, round, GovCaseStep.ACTION_REACH,
                "设置步核收登记：" + (agree ? "记为同意" : "未记同意") + (note == null ? "" : "（" + note + "）"),
                operator, null, 1, now, effKey);
        c.setUpdateBy(operator);
        c.setUpdateTime(now);
        govCaseMapper.updateById(c);

        GovCaseOutcome o = outcomeOf(caseId, GovCaseStep.ACTION_REACH, effKey, GovCaseStep.SETUP, false);
        o.setRoundNo(round);
        o.setMessage("设置步核收已" + (agree ? "记为同意，下一步「验收」准入齐备" : "记为未同意，验收仍不得开始"));
        return o;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GovCaseOutcome submitWitness(Long caseId, String witnessPost, String opinion, String certNo,
                                        String operator, String idemKey) {
        String effKey = actionKey(caseId, idemKey);
        if (witnessPost == null || witnessPost.trim().isEmpty()) {
            throw new GovCaseRuleException("见证意见须点出是哪一个见证岗提交的，无名一笔不收");
        }
        if (opinion == null || opinion.trim().isEmpty()) {
            throw new GovCaseRuleException("见证意见不能空交，见证岗 " + witnessPost + " 本笔不收");
        }
        TOadsGovCase c = lockRow(caseId);
        GovCaseOutcome replay = replayIfSeen(caseId, effKey);
        if (replay != null) {
            return replay;
        }
        requireRunning(c);
        Recount rc = recount(caseId);
        if (rc.step == null || rc.step != GovCaseStep.ACCEPT) {
            throw new GovCaseRuleException("办件 " + c.getCaseNo() + " 当下停在「" + GovCaseStep.nameOf(rc.step)
                    + "」，见证是「验收」这一步的事，步次没走到这里，本笔不生效");
        }
        int round = rc.roundOf(GovCaseStep.ACCEPT);
        List<TOadsGovCaseWitness> existed = witnessMapper.listActiveRound(caseId, round);
        String post = witnessPost.trim();
        for (TOadsGovCaseWitness w : existed) {
            if (post.equals(w.getWitnessPost())) {
                // 同一见证岗第二次：保留首次，不覆盖不追加。
                throw new GovCaseRuleException("见证岗 " + post + " 在本件验收第" + round
                        + "轮已提交过意见，保留首次那条，第二次不覆盖也不追加");
            }
        }
        if (existed.size() >= WITNESS_NEED) {
            throw new GovCaseRuleException("本件验收第" + round + "轮两名见证已齐，不该再有见证意见进来");
        }

        Date now = new Date();
        TOadsGovCaseWitness w = new TOadsGovCaseWitness();
        w.setCaseId(caseId);
        w.setRoundNo(round);
        w.setWitnessPost(post);
        w.setOpinion(opinion.trim());
        w.setCertNo(present(certNo) ? certNo.trim() : null);
        w.setSubmitAt(now);
        w.setActive(1);
        w.setCreateBy(operator);
        w.setCreateTime(now);
        witnessMapper.insert(w);
        appendLog(c, GovCaseStep.ACCEPT, round, GovCaseStep.ACTION_REACH,
                "见证岗 " + post + " 意见：" + opinion.trim(), operator, null, 1, now, effKey);

        // 两名各一笔 + 复验凭证编码回写，两样齐才同次封存；缺一样留在验收继续等。
        List<TOadsGovCaseWitness> landed = witnessMapper.listActiveRound(caseId, round);
        String cert = certFrom(landed, c.getRecheckCertNo());
        if (landed.size() >= WITNESS_NEED && present(cert)) {
            return seal(c, round, cert, operator, now);
        }
        c.setUpdateBy(operator);
        c.setUpdateTime(now);
        govCaseMapper.updateById(c);

        GovCaseOutcome o = outcomeOf(caseId, GovCaseStep.ACTION_REACH, effKey, GovCaseStep.ACCEPT, false);
        o.setRoundNo(round);
        o.setMessage("验收第" + round + "轮见证已落 " + landed.size() + "/" + WITNESS_NEED
                + (present(cert) ? "，复验凭证已具" : "，复验凭证编码尚未回写") + "，两样齐才封存");
        return o;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GovCaseOutcome withdraw(Long caseId, String note, String operator, String idemKey) {
        String effKey = actionKey(caseId, idemKey);
        TOadsGovCase c = lockRow(caseId);
        GovCaseOutcome replay = replayIfSeen(caseId, effKey);
        if (replay != null) {
            return replay;
        }
        requireRunning(c);
        Recount rc = recount(caseId);
        if (rc.step == null || rc.step != GovCaseStep.DECLARE) {
            throw new GovCaseRuleException("办件 " + c.getCaseNo() + " 撤回报备只受理停在第一步「申报」的件；"
                    + "当下在「" + GovCaseStep.nameOf(rc.step) + "」，要往回走请逐次后退");
        }
        return finish(c, GovCaseStep.OUTCOME_WITHDRAWN, GovCaseStep.ACTION_WITHDRAW,
                GovCaseStep.DECLARE, rc.roundOf(GovCaseStep.DECLARE), note, operator, effKey,
                "申报之后撤回报备，本件收尾，不再往下走");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GovCaseOutcome realign(Long caseId, String newAreaNo, String note, String operator, String idemKey) {
        String effKey = actionKey(caseId, idemKey);
        if (!present(newAreaNo)) {
            throw new GovCaseRuleException("路段划归邻段须点出邻段代号，空着的不收");
        }
        TOadsGovCase c = lockRow(caseId);
        GovCaseOutcome replay = replayIfSeen(caseId, effKey);
        if (replay != null) {
            return replay;
        }
        requireRunning(c);
        String target = newAreaNo.trim();
        TOadsAreaBook area = findArea(target);
        if (area == null) {
            throw new GovCaseRuleException("邻段代号 " + target + " 在路段目录里找不到，划归落不下去");
        }
        if (area.getStatus() != null && area.getStatus() != 0) {
            throw new GovCaseRuleException("邻段 " + target + "（" + area.getAreaName()
                    + "）已从目录撤出，划不过去");
        }
        if (target.equals(c.getAreaNo())) {
            throw new GovCaseRuleException("办件 " + c.getCaseNo() + " 本就挂在 " + target
                    + "，不存在划归这一说");
        }
        Recount rc = recount(caseId);
        int step = rc.step == null ? GovCaseStep.DECLARE : rc.step;
        Date now = new Date();
        c.setAreaNo(target);
        c.setUpdateBy(operator);
        c.setUpdateTime(now);
        govCaseMapper.updateById(c);
        return finish(c, GovCaseStep.OUTCOME_REALIGNED, GovCaseStep.ACTION_REALIGN, step,
                Math.max(rc.roundOf(step), 1), note, operator, effKey,
                "所属路段由目录划归邻段 " + target + "，本件收尾，不再往下走");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GovCaseOutcome voidCase(Long caseId, String note, String operator, String idemKey) {
        String effKey = actionKey(caseId, idemKey);
        TOadsGovCase c = lockRow(caseId);
        GovCaseOutcome replay = replayIfSeen(caseId, effKey);
        if (replay != null) {
            return replay;
        }
        requireRunning(c);
        Recount rc = recount(caseId);
        if (rc.step == null || rc.step != GovCaseStep.ACCEPT) {
            throw new GovCaseRuleException("作废只许从最后一步「验收」发起；办件 " + c.getCaseNo()
                    + " 当下停在「" + GovCaseStep.nameOf(rc.step) + "」，不受理作废");
        }
        return finish(c, GovCaseStep.OUTCOME_VOID, GovCaseStep.ACTION_VOID,
                GovCaseStep.ACCEPT, rc.roundOf(GovCaseStep.ACCEPT), note, operator, effKey,
                "已从最后一步「验收」发起作废，本件收尾");
    }

    // ================================================================ 查询、追溯、计数

    @Override
    public Integer currentStep(Long caseId) {
        if (caseId == null) {
            return null;
        }
        return recount(caseId).step;
    }

    @Override
    public GovCaseTrace trace(Long caseId) {
        TOadsGovCase c = getCase(caseId);
        if (c == null) {
            throw new GovCaseRuleException("办件查无此条：" + caseId);
        }
        List<TOadsGovCaseLog> logs = logMapper.listByCase(caseId);
        Recount rc = recount(caseId);
        GovCaseTrace t = new GovCaseTrace();
        t.setGovCase(c);
        t.setRecountedStep(rc.step);
        t.setRecountedStepName(GovCaseStep.nameOf(rc.step));
        t.setLogs(logs);
        // 捋出的笔数就是这一串本身，截取任一步单独核也取同一串，不另算一套。
        t.setLedgerCount(logs.size());
        Integer setupRound = rc.rounds.get(GovCaseStep.SETUP);
        if (setupRound != null) {
            t.setCurrentReceipt(receiptMapper.findActiveRound(caseId, setupRound));
        }
        Integer acceptRound = rc.rounds.get(GovCaseStep.ACCEPT);
        if (acceptRound != null) {
            t.setCurrentWitnesses(witnessMapper.listActiveRound(caseId, acceptRound));
        }
        return t;
    }

    @Override
    public List<TOadsGovCase> listCases() {
        return govCaseMapper.selectAllNormal();
    }

    @Override
    public TOadsGovCase getCase(Long caseId) {
        if (caseId == null) {
            return null;
        }
        TOadsGovCase c = govCaseMapper.selectById(caseId);
        if (c == null || (c.getDelFlag() != null && c.getDelFlag() != DEL_OK)) {
            return null;
        }
        return c;
    }

    @Override
    public long runningCount() {
        return govCaseMapper.countRunning();
    }

    @Override
    public List<TOadsGovCaseLog> acceptanceRegister(Date day) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(day == null ? new Date() : day);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date start = cal.getTime();
        cal.add(Calendar.DATE, 1);
        Date end = cal.getTime();
        return logMapper.listLandedBetween(start, end);
    }

    // ================================================================ 内部：封存与收尾

    /** 验收两样齐：封存。可查不可写，新增意见/更正/抽回都在 requireRunning 被拦。 */
    private GovCaseOutcome seal(TOadsGovCase c, int round, String certNo, String operator, Date at) {
        c.setRecheckCertNo(certNo);
        c.setOutcome(GovCaseStep.OUTCOME_SEALED);
        c.setSealedAt(at);
        c.setStepNo(GovCaseStep.ACCEPT);
        c.setUpdateBy(operator);
        c.setUpdateTime(at);
        govCaseMapper.updateById(c);

        GovCaseOutcome o = outcomeOf(c.getId(), GovCaseStep.ACTION_REACH, null, GovCaseStep.ACCEPT, false);
        o.setRoundNo(round);
        o.setSealed(true);
        o.setWritable(false);
        o.setMessage("两名见证各提交一次、复验凭证编码 " + certNo + " 已回写本行，两样齐备，验收走完即封存；"
                + "此后可查不可写，新增意见、更正内容、抽回整条都不受理");
        return o;
    }

    /** 撤回/划归/作废三种收尾同一条路：落收尾留痕、改收尾形态、腾出同点位在走位置。 */
    private GovCaseOutcome finish(TOadsGovCase c, String outcome, String action, int step, int round,
                                  String note, String operator, String effKey, String message) {
        Date now = new Date();
        appendLog(c, step, round, action, note, operator, null, 1, now, effKey);
        c.setOutcome(outcome);
        c.setSealedAt(now);
        c.setUpdateBy(operator);
        c.setUpdateTime(now);
        govCaseMapper.updateById(c);

        GovCaseOutcome o = outcomeOf(c.getId(), action, effKey, step, false);
        o.setRoundNo(round);
        o.setMessage(message);
        return o;
    }

    // ================================================================ 内部：步次留痕重数（唯一算法）

    /**
     * 停在哪一步只按 append-only 留痕重数：取连续到达的最高一步在档 REACH。
     * 头列面子值、报文值都不算数；封存/收尾不改变这套数法。
     */
    private Recount recount(Long caseId) {
        List<TOadsGovCaseLog> logs = logMapper.listByCase(caseId);
        Recount rc = new Recount();
        for (int step = GovCaseStep.DECLARE; step <= GovCaseStep.ACCEPT; step++) {
            int round = 0;
            boolean reached = false;
            for (TOadsGovCaseLog l : logs) {
                if (l.getStepNo() == null || l.getStepNo() != step) {
                    continue;
                }
                if (l.getActive() != null && l.getActive() == 1
                        && GovCaseStep.ACTION_REACH.equals(l.getAction())) {
                    reached = true;
                    if (l.getRoundNo() != null && l.getRoundNo() > round) {
                        round = l.getRoundNo();
                    }
                }
            }
            if (!reached) {
                break;
            }
            rc.step = step;
            rc.rounds.put(step, round);
        }
        return rc;
    }

    private int maxRound(Long caseId, int step) {
        int max = 0;
        for (TOadsGovCaseLog l : logMapper.listByCase(caseId)) {
            if (l.getStepNo() != null && l.getStepNo() == step && l.getRoundNo() != null
                    && l.getRoundNo() > max) {
                max = l.getRoundNo();
            }
        }
        return max;
    }

    private void appendLog(TOadsGovCase c, int step, int round, String action, String note,
                           String operator, String channel, int active, Date at, String idemKey) {
        List<TOadsGovCaseLog> all = logMapper.listByCase(c.getId());
        int seq = 1;
        for (TOadsGovCaseLog l : all) {
            if (l.getStepNo() != null && l.getStepNo() == step && l.getRoundNo() != null
                    && l.getRoundNo() == round) {
                seq++;
            }
        }
        TOadsGovCaseLog l = new TOadsGovCaseLog();
        l.setCaseId(c.getId());
        l.setSpotNo(c.getSpotNo());
        l.setStepNo(step);
        l.setRoundNo(round);
        l.setSeqNo(seq);
        l.setAction(action);
        l.setNote(note);
        l.setOperator(operator);
        l.setChannel(channel);
        l.setActive(active);
        l.setActedAt(at);
        l.setIdemKey(idemKey);
        l.setCreateBy(operator);
        l.setCreateTime(at);
        logMapper.insert(l);
    }

    /** 步次重数结果：当前步及各步当前轮。 */
    private static class Recount {
        private Integer step;
        private final Map<Integer, Integer> rounds = new HashMap<Integer, Integer>();

        private int roundOf(int step) {
            Integer r = rounds.get(step);
            return r == null ? 0 : r;
        }
    }

    // ================================================================ 内部：准入取数

    /** 设置许可三段签署走完 = 该点位有一张已封存（三段点齐）的核准单。只认库，不凭印象。 */
    private boolean permitThreeNodesSigned(TOadsGovCase c) {
        List<TOadsPermitBill> bills = permitBillMapper.selectList(new QueryWrapper<TOadsPermitBill>()
                .eq("spot_no", c.getSpotNo())
                .eq("status", PERMIT_SEALED)
                .eq("del_flag", DEL_OK));
        return bills != null && !bills.isEmpty();
    }

    private TOadsGovCaseReceipt activeReceipt(Long caseId, int round) {
        if (round <= 0) {
            return null;
        }
        return receiptMapper.findActiveRound(caseId, round);
    }

    /** 两名见证随附的凭证编码，或主档已回写的那串；两名齐且有一串才回写。 */
    private String certFrom(List<TOadsGovCaseWitness> witnesses, String already) {
        if (present(already)) {
            return already.trim();
        }
        for (TOadsGovCaseWitness w : witnesses) {
            if (present(w.getCertNo())) {
                return w.getCertNo().trim();
            }
        }
        return null;
    }

    // ================================================================ 内部：并发/幂等/小工具

    /** 行锁后确认仍在走：封存/撤回/划归/作废后不出版写窗口。 */
    private TOadsGovCase requireRunning(TOadsGovCase c) {
        if (!GovCaseStep.OUTCOME_RUNNING.equals(c.getOutcome())) {
            Integer step = currentStep(c.getId());
            throw new GovCaseRuleException("办件 " + c.getCaseNo() + " 已" + GovCaseStep.outcomeName(c.getOutcome())
                    + "（停在「" + GovCaseStep.nameOf(step) + "」）：封存后的记录可查不可写，"
                    + "新增意见、更正内容、抽回整条三种动作都不受理");
        }
        return c;
    }

    /** 行级锁：封存/收尾后也锁得到行，锁内先处理幂等重放，再判可写。 */
    private TOadsGovCase lockRow(Long caseId) {
        if (caseId == null) {
            throw new GovCaseRuleException("须点出哪一条办件");
        }
        TOadsGovCase c = govCaseMapper.selectByIdForUpdate(caseId);
        if (c == null) {
            throw new GovCaseRuleException("办件查无此条：" + caseId);
        }
        return c;
    }

    /** 幂等重放：同键那行已落，就按当下状态回首条结果，不添第二行。 */
    private GovCaseOutcome replayIfSeen(Long caseId, String effKey) {
        if (effKey == null) {
            return null;
        }
        TOadsGovCaseLog seen = logMapper.findByIdemKey(effKey);
        if (seen == null) {
            return null;
        }
        return outcomeOf(caseId, seen.getAction(), effKey, seen.getStepNo(), true);
    }

    private String actionKey(Long caseId, String idemKey) {
        return present(idemKey) ? "CASE:" + caseId + ":" + idemKey.trim() : null;
    }

    private GovCaseOutcome outcomeOf(Long caseId, String action, String effKey, Integer stepHint,
                                     boolean replayed) {
        TOadsGovCase c = getCase(caseId);
        Recount rc = recount(caseId);
        GovCaseOutcome o = new GovCaseOutcome();
        o.setCaseId(caseId);
        o.setCaseNo(c == null ? null : c.getCaseNo());
        o.setSpotNo(c == null ? null : c.getSpotNo());
        o.setStepNo(rc.step);
        o.setStepName(GovCaseStep.nameOf(rc.step));
        o.setOutcome(c == null ? null : c.getOutcome());
        o.setOutcomeName(GovCaseStep.outcomeName(c == null ? null : c.getOutcome()));
        o.setAction(action);
        o.setSealed(c != null && GovCaseStep.OUTCOME_SEALED.equals(c.getOutcome()));
        o.setWritable(c != null && GovCaseStep.OUTCOME_RUNNING.equals(c.getOutcome()));
        o.setRunningCount(runningCount());
        o.setReplayed(replayed);
        if (replayed) {
            o.setMessage("同键请求已办过，按先落那一条回首条结果，不添第二行、不并成一条、不丢一条");
        }
        return o;
    }

    private TOadsGovCase findRunning(String spotNo) {
        List<TOadsGovCase> list = govCaseMapper.listRunningBySpot(spotNo);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    private TOadsGovCaseIntake findOpenIntake(String spotNo) {
        List<TOadsGovCaseIntake> rows = intakeMapper.selectList(new QueryWrapper<TOadsGovCaseIntake>()
                .eq("spot_no", spotNo)
                .eq("state", GovCaseStep.INTAKE_OPEN));
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    private TOadsAreaBook findArea(String areaNo) {
        List<TOadsAreaBook> rows = areaBookMapper.selectList(new QueryWrapper<TOadsAreaBook>()
                .eq("area_no", areaNo)
                .eq("del_flag", DEL_OK));
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    private Long resolveSpotId(String spotNo) {
        List<TOadsSpot> spots = spotMapper.selectList(new QueryWrapper<TOadsSpot>()
                .eq("spot_no", spotNo)
                .eq("del_flag", DEL_OK)
                .orderByDesc("id"));
        return spots == null || spots.isEmpty() ? null : spots.get(0).getId();
    }

    private static boolean present(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String requireText(String value, String field) {
        if (!present(value)) {
            throw new GovCaseRuleException("立件四项输入缺一：" + field + " 没报，本件立不起来");
        }
        return value.trim();
    }

    private static int requireInt(String text, String field) {
        if (!present(text)) {
            throw new GovCaseRuleException("立件四项输入缺一：" + field + " 没报，本件立不起来");
        }
        String t = text.trim();
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (ch == '-' && i == 0) {
                continue;
            }
            if (ch < '0' || ch > '9') {
                throw new GovCaseRuleException(field + " 须为整数，报来的「" + t + "」不作数，本件立不起来");
            }
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            throw new GovCaseRuleException(field + " 须为整数，报来的「" + t + "」不作数，本件立不起来");
        }
    }
}
