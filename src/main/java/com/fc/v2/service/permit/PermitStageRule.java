package com.fc.v2.service.permit;

import com.fc.v2.model.auto.TOadsPermitSign;

import java.util.Comparator;
import java.util.List;

/**
 * 核准单三段规则本——段次、人数、数法只认这一处给出的答案。
 *
 * <p>单据表上的 sign_mode/need_count、屏上的已签署人数都只是这一处点出结果的回显；
 * 报文自称第几段、该按哪种数法，一律不作数。要新增一种数法，另立一套承接，不回头改本枚举。
 *
 * <ul>
 *   第1条段（窗口受理）：候 1 人，其意见落单即过；
 *   第2条段（区局核定）：候 2 人，先落单一方的判断即为结论，后落一方照常提交、两笔都留痕；
 *   第3条段（市局备案）：候 2 人且须同时认可，两条同意才过，一同一异挂待议。
 * </ul>
 *
 * @author fuce
 * @date 2026-10-05
 */
public enum PermitStageRule {

    /** 第一段：区政务窗口受理，1 名受理岗，一名即过 */
    WINDOW(0, "窗口受理", "窗口受理岗", 1, SignMode.FIRST),
    /** 第二段：区局核定，2 名核定岗，先落为定（后落一方仍须提交） */
    DISTRICT(1, "区局核定", "区局核定岗", 2, SignMode.LEADER),
    /** 第三段：市局备案，2 名备案岗，两关须同判 */
    CITY(2, "市局备案", "市局备案岗", 2, SignMode.CONSENSUS);

    /** 数法：0一名即过 1两关须同判 2先落为定（落库 sign_mode 仅回显，判定仍回本枚举） */
    public static final int MODE_FIRST = 0;
    public static final int MODE_CONSENSUS = 1;
    public static final int MODE_LEADER = 2;

    /** 同意 */
    public static final int OPINION_AGREE = 1;
    /** 不同意 */
    public static final int OPINION_REJECT = 2;

    private final int node;
    private final String stageName;
    private final String postName;
    private final int needCount;
    private final SignMode mode;

    PermitStageRule(int node, String stageName, String postName, int needCount, SignMode mode) {
        this.node = node;
        this.stageName = stageName;
        this.postName = postName;
        this.needCount = needCount;
        this.mode = mode;
    }

    /** 按服务端当前段次取规则；段次越界按错误处理，绝不向相邻段借数法 */
    public static PermitStageRule ofNode(int node) {
        for (PermitStageRule rule : values()) {
            if (rule.node == node) {
                return rule;
            }
        }
        throw new IllegalArgumentException("不存在的段次：" + node + "（只认 0窗口受理/1区局核定/2市局备案）");
    }

    /** 下一段；第三段没有下一段 */
    public PermitStageRule next() {
        return node >= CITY.node ? null : ofNode(node + 1);
    }

    /**
     * 按本处数法点本轮已在档签署的结论。行数不足一律 WAIT，留在本段。
     * 排序以各自落单时刻为准（同秒看笔序），两名签署人同秒提交也不并成一条。
     */
    public Verdict judge(List<TOadsPermitSign> activeSigns) {
        int landed = activeSigns == null ? 0 : activeSigns.size();
        if (landed < needCount) {
            return Verdict.WAIT;
        }
        List<TOadsPermitSign> ordered = byLandingOrder(activeSigns);
        switch (mode) {
            case FIRST:
                // 第一段：意见落单即过，同意与否都不另立说法
                return Verdict.PASS;
            case LEADER:
                // 第二段：先落单一方的判断即为结论，后落一方只凑人数、不改结论
                return ordered.get(0).getOpinion() == OPINION_AGREE ? Verdict.PASS : Verdict.VETO;
            case CONSENSUS:
                // 第三段：两条同意才过；两条不同意即否决；一同一异挂待议，不推进不终止
                boolean firstAgree = ordered.get(0).getOpinion() == OPINION_AGREE;
                boolean secondAgree = ordered.get(1).getOpinion() == OPINION_AGREE;
                if (firstAgree && secondAgree) {
                    return Verdict.PASS;
                }
                if (!firstAgree && !secondAgree) {
                    return Verdict.VETO;
                }
                return Verdict.SPLIT;
            default:
                throw new IllegalStateException("未承接的数法：" + mode);
        }
    }

    /** 落单先后：各自时刻为先，同秒以笔序为准——逐笔累加，不并笔 */
    public static List<TOadsPermitSign> byLandingOrder(List<TOadsPermitSign> signs) {
        signs.sort(Comparator
                .comparing(TOadsPermitSign::getSignTime)
                .thenComparing(TOadsPermitSign::getSeqNo));
        return signs;
    }

    public int getNode() {
        return node;
    }

    public String getStageName() {
        return stageName;
    }

    public String getPostName() {
        return postName;
    }

    public int getNeedCount() {
        return needCount;
    }

    public int getModeCode() {
        return mode.code;
    }

    /** 本轮落笔未齐，整单留在本段 */
    public enum Verdict { WAIT, PASS, VETO, SPLIT }

    private enum SignMode {
        FIRST(MODE_FIRST), LEADER(MODE_LEADER), CONSENSUS(MODE_CONSENSUS);
        private final int code;

        SignMode(int code) {
            this.code = code;
        }
    }
}
