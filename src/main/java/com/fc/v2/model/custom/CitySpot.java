package com.fc.v2.model.custom;

import java.math.BigDecimal;

/**
 * 市里一侧的点位行（每日增量比对的对侧数据）。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class CitySpot {

    /** 市里侧行主键/版本序，用来与本局游标比先后 */
    private Long cityId;

    /** 点位码 */
    private String spotNo;

    /** 路段代号 */
    private String areaNo;

    /** 市里一侧记的路段全称（与本局目录冗余全称冲突时逐条列出） */
    private String areaName;

    /** 市里是否已注销；市里已注销而本局仍挂载的，逐条列出，不静默清除 */
    private Boolean revoked;

    /** 经纬度（现场采集同源，仅备查） */
    private BigDecimal longitude;

    private BigDecimal latitude;

    public Long getCityId() {
        return cityId;
    }

    public void setCityId(Long cityId) {
        this.cityId = cityId;
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

    public Boolean getRevoked() {
        return revoked;
    }

    public void setRevoked(Boolean revoked) {
        this.revoked = revoked;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }
}
