package com.fc.v2.model.custom;

import java.math.BigDecimal;

/**
 * 建行/更正/改挂共用入参。
 *
 * <p>两条建行通道（建档岗逐条 MANUAL、集中更换带出 BATCH）复用同一份字段与同一套判定，
 * 界面上不长第二种样式。坐标与辖段挂靠来自现场采集，服务端照收照判，不提供人工改写坐标的口子。
 * 待补处数没有录入口：lack 由 need-got 在写库前重算，报文即便带了也不采信。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class SpotForm {

    /** 更新/更正/改挂时点出既有那一行（此时同码按更新处理，不当成多出来的一行） */
    private Long id;

    /** 点位码：报送材料自带，系统不另发第二串，也不校对构成规则 */
    private String spotNo;

    /** 辖段挂靠代号（现场采集） */
    private String areaNo;

    /** 经度（现场采集） */
    private BigDecimal longitude;

    /** 纬度（现场采集） */
    private BigDecimal latitude;

    /**
     * 核定应设处数（文本原文交来，由裁决处严格解析）。
     * 缺填／不是整数／为负／小于已核验，一律拒，提示点名字段名。
     */
    private String needText;

    /**
     * 已核验落地处数（文本原文交来，由裁决处严格解析）。
     * 高于核定应设，拒。待补处数没有录入口，由前两个轧出。
     */
    private String gotText;

    /** 随件交来的牌面题名与要件 */
    private String content;

    /** 季度码；不传时由区局名册配置给当前季度 */
    private String quarterCode;

    /** 建行通道 MANUAL/BATCH，由调用入口点，不收页面自报 */
    private String buildKind;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getNeedNum() {
        return parseStrict(needText);
    }

    /** 原始文本，供裁决处区分"缺填"与"不是整数"并点名提示。 */
    public String getNeedText() {
        return needText;
    }

    public void setNeedText(String needText) {
        this.needText = needText;
    }

    public Integer getGotNum() {
        return parseStrict(gotText);
    }

    /** 原始文本，已核验落地处数非整数同样点名字段拒绝。 */
    public String getGotText() {
        return gotText;
    }

    public void setGotText(String gotText) {
        this.gotText = gotText;
    }

    /** 严格整数解析：空、带小数、带杂符一律 null（不做截断、不做四舍五入）。 */
    public static Integer parseStrict(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.isEmpty()) {
            return null;
        }
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '-' && i == 0 && t.length() > 1) {
                continue;
            }
            if (c < '0' || c > '9') {
                return null;
            }
        }
        try {
            long v = Long.parseLong(t);
            if (v > Integer.MAX_VALUE || v < Integer.MIN_VALUE) {
                return null;
            }
            return (int) v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getQuarterCode() {
        return quarterCode;
    }

    public void setQuarterCode(String quarterCode) {
        this.quarterCode = quarterCode;
    }

    public String getBuildKind() {
        return buildKind;
    }

    public void setBuildKind(String buildKind) {
        this.buildKind = buildKind;
    }
}
