package com.fc.v2.model.custom;

import java.math.BigDecimal;

/**
 * 牌匾更换名册里的一行入参（一行对应一处点位）。
 *
 * <p>两个数量（应更换处数、核验合格处数）以原文交来：两项须同时到达，只到一项时
 * 服务端不代填、不默认成零，该行挂待核下一轮核看。点位身份只认点位编码全等配主档，
 * 不按版面名称近似去配。报文里的结论格不作数——结论由收讫环节裁决，第四种写法不予写入。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class PlaqueRowForm {

    /** 点位编码（身份只认它） */
    private String spotNo;

    /** 报文里的版面名称（只用来与主档比对，两边有出入以主档为准） */
    private String viewName;

    /** 报文里的路段代号（只用来与主档比对） */
    private String roadCode;

    /** 应更换处数原文（缺/不是数按未到达认） */
    private String qtyText;

    /** 核验合格处数原文（两项须同时到达） */
    private String chkNumText;

    /** 现场照片附件编号；缺照片留空，行照样占位挂待核，下一轮核看补挂 */
    private String photoNo;

    /** 报文自称的结论（待核/通过/退回之外的第四种写法只扫进备注，不进结论格） */
    private String verdictRaw;

    public String getSpotNo() {
        return spotNo;
    }

    public void setSpotNo(String spotNo) {
        this.spotNo = spotNo;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public String getRoadCode() {
        return roadCode;
    }

    public void setRoadCode(String roadCode) {
        this.roadCode = roadCode;
    }

    public String getQtyText() {
        return qtyText;
    }

    public void setQtyText(String qtyText) {
        this.qtyText = qtyText;
    }

    public String getChkNumText() {
        return chkNumText;
    }

    public void setChkNumText(String chkNumText) {
        this.chkNumText = chkNumText;
    }

    public String getPhotoNo() {
        return photoNo;
    }

    public void setPhotoNo(String photoNo) {
        this.photoNo = photoNo;
    }

    public String getVerdictRaw() {
        return verdictRaw;
    }

    public void setVerdictRaw(String verdictRaw) {
        this.verdictRaw = verdictRaw;
    }

    /** 数原文解析：空/非数一律 null（未到达），不代填不默零。 */
    public static BigDecimal parseNumber(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.isEmpty()) {
            return null;
        }
        try {
            BigDecimal v = new BigDecimal(t);
            return v;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
