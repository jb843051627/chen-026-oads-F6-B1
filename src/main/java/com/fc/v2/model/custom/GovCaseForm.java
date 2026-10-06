package com.fc.v2.model.custom;

/**
 * 监管办件立件四项输入：点位编码、所属路段代号、处数、拟判定的校核分值。
 * 缺任意一项都立不起来，报错逐项点名；idemKey 为同批并办幂等键，可空（空则无重放保障）。
 *
 * @author fuce
 * @date 2026-10-06
 */
public class GovCaseForm {

    /** 点位编码 */
    private String spotNo;
    /** 所属路段代号 */
    private String areaNo;
    /** 处数（报文字符串，裁决处按整数严判） */
    private String placeCountText;
    /** 拟判定的校核分值（报文字符串，裁决处按整数严判） */
    private String checkScoreText;
    /** 同批并办幂等键 */
    private String idemKey;

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

    public String getPlaceCountText() {
        return placeCountText;
    }

    public void setPlaceCountText(String placeCountText) {
        this.placeCountText = placeCountText;
    }

    public String getCheckScoreText() {
        return checkScoreText;
    }

    public void setCheckScoreText(String checkScoreText) {
        this.checkScoreText = checkScoreText;
    }

    public String getIdemKey() {
        return idemKey;
    }

    public void setIdemKey(String idemKey) {
        this.idemKey = idemKey;
    }
}
