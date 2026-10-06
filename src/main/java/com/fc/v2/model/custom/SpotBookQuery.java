package com.fc.v2.model.custom;

/**
 * 名册查询：按点位编码、路段代号、路段类别三个入口；前缀匹配与全等都支持。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class SpotBookQuery {

    /** 全等 eq / 前缀 prefix */
    public static final String MATCH_EQ = "eq";
    public static final String MATCH_PREFIX = "prefix";

    private String quarterCode;

    /** 点位编码（spot_no） */
    private String spotNo;

    /** 路段代号（area_no） */
    private String areaNo;

    /** 路段类别（主干道/次干道/背街巷） */
    private String areaKind;

    /** 匹配方式，默认全等 */
    private String matchMode = MATCH_EQ;

    public String effectiveMatchMode() {
        return MATCH_PREFIX.equalsIgnoreCase(matchMode) ? MATCH_PREFIX : MATCH_EQ;
    }

    public String getQuarterCode() {
        return quarterCode;
    }

    public void setQuarterCode(String quarterCode) {
        this.quarterCode = quarterCode;
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

    public String getAreaKind() {
        return areaKind;
    }

    public void setAreaKind(String areaKind) {
        this.areaKind = areaKind;
    }

    public String getMatchMode() {
        return matchMode;
    }

    public void setMatchMode(String matchMode) {
        this.matchMode = matchMode;
    }
}
