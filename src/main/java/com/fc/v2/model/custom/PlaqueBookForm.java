package com.fc.v2.model.custom;

import java.util.ArrayList;
import java.util.List;

/**
 * 牌匾更换名册整册报送入参。
 *
 * <p>提交形态固定为一册一次交完整：各单位先归并到区里，再按季度名册合成一册上送；
 * 不接零散逐行报送，行不许游移到别册。收自哪条通道（区级报送/市级复核）由入口点，
 * 不收报文自报。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class PlaqueBookForm {

    /** 批次码（册的身份；两条通道递同一串即同一册，第二回不重复入行） */
    private String batchNo;

    /** 名册册号（报送单位+季度合成，仅记账用） */
    private String bookNo;

    /** 归并上送的单位代号 */
    private String unitNo;

    /** 季度码 如2026Q4 */
    private String quarterCode;

    /**
     * 抬头那一格原文：单册行数上限的唯一来源。
     * 空着按 200 认；写成负数或大得没边（非数/越过硬界）按没填认，也认 200。
     */
    private String rowLimitRaw;

    /** 册内行（一次交完整，不接零行） */
    private List<PlaqueRowForm> rows = new ArrayList<PlaqueRowForm>();

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getBookNo() {
        return bookNo;
    }

    public void setBookNo(String bookNo) {
        this.bookNo = bookNo;
    }

    public String getUnitNo() {
        return unitNo;
    }

    public void setUnitNo(String unitNo) {
        this.unitNo = unitNo;
    }

    public String getQuarterCode() {
        return quarterCode;
    }

    public void setQuarterCode(String quarterCode) {
        this.quarterCode = quarterCode;
    }

    public String getRowLimitRaw() {
        return rowLimitRaw;
    }

    public void setRowLimitRaw(String rowLimitRaw) {
        this.rowLimitRaw = rowLimitRaw;
    }

    public List<PlaqueRowForm> getRows() {
        return rows;
    }

    public void setRows(List<PlaqueRowForm> rows) {
        this.rows = rows;
    }
}
