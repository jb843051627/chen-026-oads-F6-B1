package com.fc.v2.service.gov;

/**
 * 监管办件四步与动作、收尾形态的唯一取号处（新一套，不改旧 t_oads_gov_flow 与三档 SpotFlowStep）。
 *
 * <p>固定四步先后不可调换：{@link #DECLARE}申报 → {@link #APPROVE}核准 → {@link #SETUP}设置
 * → {@link #ACCEPT}验收。停在哪一步由服务端按逐笔留痕重数，报文里带的步次一律只作参考；
 * 写库读库都以本常量序列重数出的那个数为准。要另一种推进方式，另写一套承接，不回本类添改。
 *
 * @author fuce
 * @date 2026-10-06
 */
public final class GovCaseStep {

    private GovCaseStep() {
    }

    /** 申报（立件落的第一步） */
    public static final int DECLARE = 1;
    /** 核准（要看设置许可三段签署走完，才准进设置） */
    public static final int APPROVE = 2;
    /** 设置（核收记为同意，才准开始验收） */
    public static final int SETUP = 3;
    /** 验收（两名见证各一笔＋复验凭证编码回写，两样齐才走完封存） */
    public static final int ACCEPT = 4;

    /** 走到（一次推进；核收/见证各笔意见也记这一动作，落在各自步次上） */
    public static final String ACTION_REACH = "REACH";
    /** 退回一步（被退步当轮 active 置 0，留痕按原先后转追溯） */
    public static final String ACTION_RETURN = "RETURN";
    /** 申报之后撤回报备（收尾达成，不必再往下走） */
    public static final String ACTION_WITHDRAW = "WITHDRAW";
    /** 所属路段从目录划归邻段（收尾达成） */
    public static final String ACTION_REALIGN = "REALIGN";
    /** 作废（只许从最后一步发起） */
    public static final String ACTION_VOID = "VOID";

    /** 在走（尚未走完） */
    public static final String OUTCOME_RUNNING = "RUNNING";
    /** 验收走完封存：可查不可写 */
    public static final String OUTCOME_SEALED = "SEALED";
    /** 申报后撤回 */
    public static final String OUTCOME_WITHDRAWN = "WITHDRAWN";
    /** 路段划归邻段 */
    public static final String OUTCOME_REALIGNED = "REALIGNED";
    /** 作废（从最后一步发起） */
    public static final String OUTCOME_VOID = "VOID";

    /** 报入暂存：报入在档（正式立件之前的一小段） */
    public static final String INTAKE_OPEN = "OPEN";
    /** 报入暂存：已立件 */
    public static final String INTAKE_OPENED = "OPENED";
    /** 报入暂存：已撤销（不进主档、不占未走完位置） */
    public static final String INTAKE_REVOKED = "REVOKED";

    public static String nameOf(Integer step) {
        if (step == null) {
            return "未起步";
        }
        switch (step) {
            case DECLARE:
                return "申报";
            case APPROVE:
                return "核准";
            case SETUP:
                return "设置";
            case ACCEPT:
                return "验收";
            default:
                return "未知步";
        }
    }

    public static String outcomeName(String outcome) {
        if (outcome == null) {
            return "在走";
        }
        switch (outcome) {
            case OUTCOME_RUNNING:
                return "在走";
            case OUTCOME_SEALED:
                return "已封存";
            case OUTCOME_WITHDRAWN:
                return "已撤回";
            case OUTCOME_REALIGNED:
                return "已划归邻段";
            case OUTCOME_VOID:
                return "已作废";
            default:
                return outcome;
        }
    }
}
