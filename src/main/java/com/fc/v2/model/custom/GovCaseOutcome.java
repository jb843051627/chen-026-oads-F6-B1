package com.fc.v2.model.custom;

/**
 * 一次监管办件动作的回执：步次由服务端按逐笔留痕重数后给值，不回显报文自称的步次。
 *
 * @author fuce
 * @date 2026-10-06
 */
public class GovCaseOutcome {

    private Long caseId;
    private String caseNo;
    private String spotNo;
    /** 动作后系统重数出的步次 1申报 2核准 3设置 4验收 */
    private Integer stepNo;
    private String stepName;
    /** 收尾形态 RUNNING/SEALED/WITHDRAWN/REALIGNED/VOID */
    private String outcome;
    private String outcomeName;
    /** 本次落账动作 */
    private String action;
    /** 落账步次轮次 */
    private Integer roundNo;
    /** 是否已封存（可查不可写） */
    private boolean sealed;
    /** 是否仍可写（在走且未封存） */
    private boolean writable;
    /** 回执同次后端一次查询的未走完数量（页首与它对同一个数） */
    private long runningCount;
    /** 重放：幂等键命中先落那条，本次未添新行 */
    private boolean replayed;
    private String message;

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getCaseNo() {
        return caseNo;
    }

    public void setCaseNo(String caseNo) {
        this.caseNo = caseNo;
    }

    public String getSpotNo() {
        return spotNo;
    }

    public void setSpotNo(String spotNo) {
        this.spotNo = spotNo;
    }

    public Integer getStepNo() {
        return stepNo;
    }

    public void setStepNo(Integer stepNo) {
        this.stepNo = stepNo;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getOutcomeName() {
        return outcomeName;
    }

    public void setOutcomeName(String outcomeName) {
        this.outcomeName = outcomeName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public boolean isSealed() {
        return sealed;
    }

    public void setSealed(boolean sealed) {
        this.sealed = sealed;
    }

    public boolean isWritable() {
        return writable;
    }

    public void setWritable(boolean writable) {
        this.writable = writable;
    }

    public long getRunningCount() {
        return runningCount;
    }

    public void setRunningCount(long runningCount) {
        this.runningCount = runningCount;
    }

    public boolean isReplayed() {
        return replayed;
    }

    public void setReplayed(boolean replayed) {
        this.replayed = replayed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
