package com.fc.v2.model.custom;

/**
 * 一次在走动作的回执：步序由系统按留痕重数后给值，不回显报文自称的档。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class SpotFlowOutcome {

    private Long spotId;
    private String spotNo;
    /** 动作后系统数出的步序 1待核 2已核 3压卡；旧行未起步为 null */
    private Integer flowStep;
    /** 该步当前轮次（退回重来加一轮） */
    private Integer roundNo;
    /** 是否已就地停住 */
    private boolean held;
    /** 是否已压卡到头（冻结） */
    private boolean frozen;
    private String action;
    private String message;

    public Long getSpotId() {
        return spotId;
    }

    public void setSpotId(Long spotId) {
        this.spotId = spotId;
    }

    public String getSpotNo() {
        return spotNo;
    }

    public void setSpotNo(String spotNo) {
        this.spotNo = spotNo;
    }

    public Integer getFlowStep() {
        return flowStep;
    }

    public void setFlowStep(Integer flowStep) {
        this.flowStep = flowStep;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public boolean isHeld() {
        return held;
    }

    public void setHeld(boolean held) {
        this.held = held;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
