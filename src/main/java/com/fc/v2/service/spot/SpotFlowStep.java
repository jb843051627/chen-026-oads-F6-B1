package com.fc.v2.service.spot;

/**
 * 点位在走步序与动作的唯一取号处。
 *
 * <p>只认三档，步子不得跨：{@link #PENDING}待核 → {@link #VERIFIED}已核 → {@link #SEALED}压卡。
 * 这一档没过，之后的路都不给走；这一行停在哪一步由系统按步序留痕自己数，
 * 屏上填的档、经办人记的档都不算数。
 *
 * @author fuce
 * @date 2026-10-05
 */
public final class SpotFlowStep {

    private SpotFlowStep() {
    }

    /** 待核（建行后落的第一步） */
    public static final int PENDING = 1;
    /** 已核 */
    public static final int VERIFIED = 2;
    /** 压卡（到头：冻结，不加不改不撤） */
    public static final int SEALED = 3;

    /** 走到（一次推进） */
    public static final String ACTION_REACH = "REACH";
    /** 退回一步重来（只清在走那一步当轮，老轮留痕保留） */
    public static final String ACTION_RETURN = "RETURN";
    /** 就地停住（不再往上推，但也没走到压卡） */
    public static final String ACTION_HOLD = "HOLD";

    /** 建档岗逐条建立 */
    public static final String BUILD_MANUAL = "MANUAL";
    /** 集中更换业务在核验完成后带出 */
    public static final String BUILD_BATCH = "BATCH";

    public static String nameOf(Integer step) {
        if (step == null) {
            return "未起步";
        }
        switch (step) {
            case PENDING:
                return "待核";
            case VERIFIED:
                return "已核";
            case SEALED:
                return "压卡";
            default:
                return "未知档";
        }
    }
}
