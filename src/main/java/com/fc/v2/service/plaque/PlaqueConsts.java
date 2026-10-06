package com.fc.v2.service.plaque;

/**
 * 牌匾更换名册收讫链路的唯一取号处（新名目自有的一套，不与旧集中更换那套串用）。
 *
 * <p>行结论只认三个取值：{@link #PENDING}待核、{@link #PASS}通过、{@link #REJECT}退回，
 * 由收讫环节服务端裁决，报文自称的那格不作数，第四种写法不予写入。
 *
 * @author fuce
 * @date 2026-10-05
 */
public final class PlaqueConsts {

    private PlaqueConsts() {
    }

    /** 待核（照片缺挂、数据只到一项、编码配不上主档等，下一轮核看） */
    public static final String PENDING = "PENDING";
    /** 通过 */
    public static final String PASS = "PASS";
    /** 退回（第3步五条退回情形之一） */
    public static final String REJECT = "REJECT";

    /** 通道：区级报送 */
    public static final String PUT_AREA = "AREA";
    /** 通道：市级复核 */
    public static final String PUT_CITY = "CITY";

    /** 卷宗层：现行层（现行名单只列这一层） */
    public static final int LAYER_CURRENT = 0;
    /** 卷宗层：历史卷宗层（老化归档逐行挪入，存量一条不减，照常可查） */
    public static final int LAYER_HISTORY = 1;

    /** 流水动作：收讫（四步同次做完那一笔） */
    public static final String FLOW_RECEIVE = "RECEIVE";
    /** 流水动作：同一批次码重复送达（第二回起每回一笔，行不重复入） */
    public static final String FLOW_RESEND = "RESEND";
    /** 流水动作：出回执（每次出回执都把回库逐行数得的三数快照留一笔） */
    public static final String FLOW_RECEIPT = "RECEIPT";
    /** 流水动作：对账校正（销账按流水抹平，这笔归收讫环节认） */
    public static final String FLOW_ADJUST = "ADJUST";
    /** 流水动作：老化归档 */
    public static final String FLOW_ARCHIVE = "ARCHIVE";
    /** 流水动作：缺照片补挂（行去处若因此由待核变通过，销账三数同次回库重数回写） */
    public static final String FLOW_ATTACH = "ATTACH";

    /** 抬头那格空着、或认不得时认下的单册行数上限（别处不设第二条来源） */
    public static final int DEFAULT_ROW_LIMIT = 200;
    /** 抬头写成"大得没边"的硬界：超过此值一律按没填认（认 200） */
    public static final int HARD_ROW_LIMIT = 100000;

    /** 点位编码规范位数下限：不足此位算"位数不够"，只判配不上主档，不在第3步退回之列 */
    public static final int SPOT_CODE_MIN_LEN = 6;
    /** 点位编码规范位数上限：超出此长即第3步"编码长度超出规范"，判退回 */
    public static final int SPOT_CODE_MAX_LEN = 32;

    /** 当地核验上限缺省键：辖段未单列时取这条 */
    public static final String LIMIT_DEFAULT_AREA = "DEFAULT";

    /** 对账校正这笔的认主：账实差以流水为准，抹平销账归收讫环节认，不由页面挑 */
    public static final String RECONCILE_OWNER = "收讫环节对账";

    public static String verdictName(String verdict) {
        if (PENDING.equals(verdict)) {
            return "待核";
        }
        if (PASS.equals(verdict)) {
            return "通过";
        }
        if (REJECT.equals(verdict)) {
            return "退回";
        }
        return "第四种写法";
    }
}
