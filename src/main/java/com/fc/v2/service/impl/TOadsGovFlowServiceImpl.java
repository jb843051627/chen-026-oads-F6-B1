package com.fc.v2.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TOadsGovFlowMapper;
import com.fc.v2.model.auto.TOadsGovFlow;
import com.fc.v2.service.ITOadsGovFlowService;

/**
 * 点位监管办件流转单 Service业务层处理（state-machine 形状：单据流转）。
 *
 * <p><b>口径定死（2026-10-06 起以此为准，不再两版并存）</b>：流转单照监管办件裁决处
 * {@link TOadsGovCaseServiceImpl} 那一版的口径走，在旧表 t_oads_gov_flow 结构内落地
 * ——不添列、不改名、不动存量数据。六条取舍如下，审计问哪一条都指回本类：
 *
 * <p>一、关次是唯一事实：stage 0申报→1核准→2设置→3验收，一步只走一格，不许跨格；
 * status 不由屏上填、不各自算，只由 {@link #statusOf(int)} 一处推出（0未起/1在办/2已封住）。
 * 存量行两列对不上时以推出值为准，随下一次写动作落库改正——兼容存量，两边同一个口径。
 *
 * <p>二、到顶即封住：stage 到 3（验收）即 status=2 已封住，此后推进/回退/改记/撤单一律不受理，
 * 可查不可写。归档与回退同认这一个"封住"，不各算各的。
 *
 * <p>三、回退只退一格：退到 0（申报）为止，退无可退；不存在"一步回起点"。
 *
 * <p>四、连点去重：旧表没有幂等键列，以"动作+批注"当键——同一动作同一句批注连着来，
 * 只认头一回（重放回首条结果，不再挪格、不添第二笔）；要再动作须换一句批注。
 * 新办件（t_oads_gov_case）有正式幂等键列，那边不靠批注去重；
 * 两处说的是同一句话：同键只认先落那一条。
 *
 * <p>五、并发与回滚叠加：每个写动作先 FOR UPDATE 锁住本行，锁内重读关次再判定，
 * 两笔并发按锁内看到的数逐笔串行，结果与排队执行一致。
 *
 * <p>六、撤单只逻辑撤（del_flag=1），历史留着；列表与详情同走 del_flag=0 这一份过滤、
 * 同一份 {@link #statusOf(int)} 推算，台账上那条和点进去看到的是同一件。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TOadsGovFlowServiceImpl implements ITOadsGovFlowService {

    /** 关次：申报（起点） */
    private static final int STAGE_DECLARE = 0;
    /** 关次：验收（终点，到顶即封住） */
    private static final int STAGE_ACCEPT = 3;
    /** 未起 */
    private static final int STATUS_NOT_STARTED = 0;
    /** 在办 */
    private static final int STATUS_ACTIVE = 1;
    /** 已封住 */
    private static final int STATUS_SEALED = 2;
    private static final int DEL_OK = 0;
    private static final int DEL_GONE = 1;
    /** 挪关动作前缀：推进（去重键的一半，另一半是批注） */
    private static final String TAG_ADV = "ADV:";
    /** 挪关动作前缀：回退 */
    private static final String TAG_RET = "RET:";

    @javax.annotation.Resource
    private TOadsGovFlowMapper oadsGovFlowMapper;

    @Override
    public TOadsGovFlow selectTOadsGovFlowById(Long id) {
        if (id == null) {
            return null;
        }
        TOadsGovFlow r = this.oadsGovFlowMapper.selectById(id);
        if (r == null || (r.getDelFlag() != null && r.getDelFlag() != DEL_OK)) {
            return null;
        }
        // 详情与列表同一份推算：存量行两列对不上时，屏上看到的都是推出值。
        return normalize(r);
    }

    @Override
    public List<TOadsGovFlow> selectTOadsGovFlowList(QueryWrapper<TOadsGovFlow> queryWrapper) {
        QueryWrapper<TOadsGovFlow> qw = queryWrapper == null ? new QueryWrapper<TOadsGovFlow>() : queryWrapper;
        // 台账只出在册行，按主键定序——与详情同一份过滤，页码翻起来行不挪窝。
        qw.eq("del_flag", DEL_OK).orderByAsc("id");
        List<TOadsGovFlow> list = this.oadsGovFlowMapper.selectList(qw);
        List<TOadsGovFlow> out = new ArrayList<TOadsGovFlow>();
        if (list != null) {
            for (TOadsGovFlow r : list) {
                out.add(normalize(r));
            }
        }
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TOadsGovFlow advance(Long id, String remark) {
        TOadsGovFlow r = lockRow(id);
        if (r == null) {
            return null;
        }
        // 连点重放先于一切：同一动作同一句批注，只认头一回。
        if (isReplay(r, TAG_ADV, remark)) {
            return r;
        }
        int st = stageOf(r);
        if (st >= STAGE_ACCEPT) {
            // 到顶即封住：没有紧邻下一格可推，再点不挪、也不改写任何一列。
            return null;
        }
        r.setStage(st + 1);
        r.setStatus(statusOf(st + 1));
        r.setLastAction(TAG_ADV + norm(remark));
        this.oadsGovFlowMapper.updateById(r);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TOadsGovFlow rollback(Long id, String remark) {
        TOadsGovFlow r = lockRow(id);
        if (r == null) {
            return null;
        }
        if (isReplay(r, TAG_RET, remark)) {
            return r;
        }
        int st = stageOf(r);
        if (st >= STAGE_ACCEPT) {
            // 封住的单子退不得：归档与回退同认这一个"封住"。
            return null;
        }
        if (st <= STAGE_DECLARE) {
            // 已在申报，前面没有格可退；不存在"一步回起点"之外的退法。
            return null;
        }
        r.setStage(st - 1);
        r.setStatus(statusOf(st - 1));
        r.setLastAction(TAG_RET + norm(remark));
        this.oadsGovFlowMapper.updateById(r);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateContent(Long id, String remark) {
        TOadsGovFlow r = lockRow(id);
        if (r == null) {
            return false;
        }
        if (stageOf(r) >= STAGE_ACCEPT) {
            // 定稿之后字改不动；要改只能在封住之前。
            return false;
        }
        r.setContent(remark);
        return this.oadsGovFlowMapper.updateById(r) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean remove(Long id) {
        TOadsGovFlow r = lockRow(id);
        if (r == null) {
            return false;
        }
        if (stageOf(r) >= STAGE_ACCEPT) {
            // 锁住的单子移不得。
            return false;
        }
        // 只逻辑撤下，历史留着；台账与详情同走 del_flag=0，撤下即两边同时不见。
        r.setDelFlag(DEL_GONE);
        return this.oadsGovFlowMapper.updateById(r) > 0;
    }

    // ================================================================ 内部：唯一口径出处

    /** 写动作专用：行级锁住本行（在册行才锁得到），锁内重读的关次才算数。 */
    private TOadsGovFlow lockRow(Long id) {
        if (id == null) {
            return null;
        }
        return this.oadsGovFlowMapper.selectRowByIdForUpdate(id);
    }

    /** 关次唯一事实：存量行 stage 空着按 0（申报、未起）计，列表/详情/写动作同认这一个数。 */
    private static int stageOf(TOadsGovFlow r) {
        return r.getStage() == null ? STAGE_DECLARE : r.getStage();
    }

    /** 状态只由关次一处推出：0未起 / 中间在办 / 到顶（验收）已封住。旁处不得另算一套。 */
    private static int statusOf(int stage) {
        if (stage <= STAGE_DECLARE) {
            return STATUS_NOT_STARTED;
        }
        if (stage >= STAGE_ACCEPT) {
            return STATUS_SEALED;
        }
        return STATUS_ACTIVE;
    }

    /** 读路径统一回正：状态列与推出值不符的存量行，屏上先按推出值走，落库随下一次写动作改正。 */
    private static TOadsGovFlow normalize(TOadsGovFlow r) {
        r.setStage(stageOf(r));
        r.setStatus(statusOf(stageOf(r)));
        return r;
    }

    /**
     * 连点去重（旧表没有幂等键列的取舍）：同一动作同一句批注连着来，只认头一回。
     * 新值带动作前缀比对；存量旧值没前缀，裸批注相等也算同一回——只挡一次，写库后即带前缀。
     */
    private static boolean isReplay(TOadsGovFlow r, String tag, String remark) {
        String last = r.getLastAction();
        if (last == null) {
            return false;
        }
        if ((tag + norm(remark)).equals(last)) {
            return true;
        }
        return !last.startsWith(TAG_ADV) && !last.startsWith(TAG_RET) && last.equals(norm(remark));
    }

    private static String norm(String remark) {
        return remark == null ? "" : remark.trim();
    }

}
