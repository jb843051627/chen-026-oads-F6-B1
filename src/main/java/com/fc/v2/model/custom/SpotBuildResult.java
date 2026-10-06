package com.fc.v2.model.custom;

/**
 * 建行/更新回执。
 *
 * <p>回执上的待补处数、步序、处数全部由唯一裁决处在同一次写库里给值，
 * 页面不另算第二遍；页首处数与回执处数只认这一处。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class SpotBuildResult {

    /** CREATED 新立一行 / UPDATED 就着原行更新（同码那一行本身） */
    private String action;

    private Long spotId;

    private String spotNo;

    private String areaNo;

    private String areaName;

    /** 核定应设处数 */
    private Integer needNum;

    /** 已核验落地处数 */
    private Integer gotNum;

    /** 待补处数：写库前由裁决处重算覆盖，30与12得18，两者相等得0 */
    private Integer lackNum;

    /** 建行后系统数出的步序（1待核…），屏上填的不算数 */
    private Integer flowStep;

    /** 通道 MANUAL/BATCH，两条通道同一套数量逻辑与同一套判定 */
    private String buildKind;

    /** 给报送方的交代 */
    private String message;

    public static SpotBuildResult created() {
        SpotBuildResult r = new SpotBuildResult();
        r.action = "CREATED";
        return r;
    }

    public static SpotBuildResult updated() {
        SpotBuildResult r = new SpotBuildResult();
        r.action = "UPDATED";
        return r;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

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

    public String getAreaNo() {
        return areaNo;
    }

    public void setAreaNo(String areaNo) {
        this.areaNo = areaNo;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public Integer getNeedNum() {
        return needNum;
    }

    public void setNeedNum(Integer needNum) {
        this.needNum = needNum;
    }

    public Integer getGotNum() {
        return gotNum;
    }

    public void setGotNum(Integer gotNum) {
        this.gotNum = gotNum;
    }

    public Integer getLackNum() {
        return lackNum;
    }

    public void setLackNum(Integer lackNum) {
        this.lackNum = lackNum;
    }

    public Integer getFlowStep() {
        return flowStep;
    }

    public void setFlowStep(Integer flowStep) {
        this.flowStep = flowStep;
    }

    public String getBuildKind() {
        return buildKind;
    }

    public void setBuildKind(String buildKind) {
        this.buildKind = buildKind;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
