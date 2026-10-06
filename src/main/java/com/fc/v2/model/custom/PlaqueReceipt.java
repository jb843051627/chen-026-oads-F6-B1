package com.fc.v2.model.custom;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 牌匾更换名册收讫回执。
 *
 * <p>回执只有三个数字：本册行数、入库行数、退回行数，后两者之和须等于第一个。
 * 三个数一律回库逐行数出来——每次出回执都重数一遍行账，册账汇总与页面那份都只是转述，
 * 两边出自同一次计算才谈得上口径一致。回执另附：
 * <ul>
 *   <li>逐行结论——任取一行都要能与三个数互相印证；对不齐的一行一行点名，不接受"基本相符"；</li>
 *   <li>退回逐句——哪一行因哪一条退回；</li>
 *   <li>主档出入行——版面名称、路段代号报文与点位主档不一致时，以主档为准并把行列出；</li>
 *   <li>重复送达的交代——同一批次码第二回落在账上是什么样子。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-10-05
 */
public class PlaqueReceipt {

    /** 逐行复核条目 */
    public static class RowItem {
        private int rowNo;
        private String spotNo;
        /** 落账结论（待核/通过/退回） */
        private String verdict;
        /** 入库还是退回（与结论同源：通过、待核计入入库） */
        private boolean accepted;
        /** 应更换处数（null=未到达，不代填不默零） */
        private java.math.BigDecimal qty;
        /** 核验合格处数（null=未到达） */
        private java.math.BigDecimal chkNum;
        /** 退回理由原文（通过/待核为 null） */
        private String rejectReason;
        /** 是否缺照片占位待核 */
        private boolean photoPending;
        /** 报文与主档有无出入（以主档为准） */
        private boolean masterDiff;
        private String reportViewName;
        private String masterViewName;
        private String reportRoadCode;
        private String masterRoadCode;
        /** 卷宗层 0现行 1历史卷宗 */
        private int layer;

        public int getRowNo() {
            return rowNo;
        }

        public void setRowNo(int rowNo) {
            this.rowNo = rowNo;
        }

        public String getSpotNo() {
            return spotNo;
        }

        public void setSpotNo(String spotNo) {
            this.spotNo = spotNo;
        }

        public String getVerdict() {
            return verdict;
        }

        public void setVerdict(String verdict) {
            this.verdict = verdict;
        }

        public boolean isAccepted() {
            return accepted;
        }

        public void setAccepted(boolean accepted) {
            this.accepted = accepted;
        }

        public String getRejectReason() {
            return rejectReason;
        }

        public void setRejectReason(String rejectReason) {
            this.rejectReason = rejectReason;
        }

        public java.math.BigDecimal getQty() {
            return qty;
        }

        public void setQty(java.math.BigDecimal qty) {
            this.qty = qty;
        }

        public java.math.BigDecimal getChkNum() {
            return chkNum;
        }

        public void setChkNum(java.math.BigDecimal chkNum) {
            this.chkNum = chkNum;
        }

        public boolean isPhotoPending() {
            return photoPending;
        }

        public void setPhotoPending(boolean photoPending) {
            this.photoPending = photoPending;
        }

        public boolean isMasterDiff() {
            return masterDiff;
        }

        public void setMasterDiff(boolean masterDiff) {
            this.masterDiff = masterDiff;
        }

        public String getReportViewName() {
            return reportViewName;
        }

        public void setReportViewName(String reportViewName) {
            this.reportViewName = reportViewName;
        }

        public String getMasterViewName() {
            return masterViewName;
        }

        public void setMasterViewName(String masterViewName) {
            this.masterViewName = masterViewName;
        }

        public String getReportRoadCode() {
            return reportRoadCode;
        }

        public void setReportRoadCode(String reportRoadCode) {
            this.reportRoadCode = reportRoadCode;
        }

        public String getMasterRoadCode() {
            return masterRoadCode;
        }

        public void setMasterRoadCode(String masterRoadCode) {
            this.masterRoadCode = masterRoadCode;
        }

        public int getLayer() {
            return layer;
        }

        public void setLayer(int layer) {
            this.layer = layer;
        }
    }

    /** 对不齐时一行一行点名的条目（结论与三个数印证不上、或销账与流水对不上的那笔） */
    public static class Mismatch {
        private int rowNo;
        private String spotNo;
        private String detail;

        public Mismatch() {
        }

        public Mismatch(int rowNo, String spotNo, String detail) {
            this.rowNo = rowNo;
            this.spotNo = spotNo;
            this.detail = detail;
        }

        public int getRowNo() {
            return rowNo;
        }

        public void setRowNo(int rowNo) {
            this.rowNo = rowNo;
        }

        public String getSpotNo() {
            return spotNo;
        }

        public void setSpotNo(String spotNo) {
            this.spotNo = spotNo;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }
    }

    private String batchNo;
    private String putKind;
    private String quarterCode;
    private Date receivedAt;

    /** 进门点数所得、与抬头认下上限一并落账的那两个数 */
    private int rowLimit;
    private String rowLimitRaw;
    private boolean overLimit;

    /** 回执三数（回库逐行数） */
    private int totalRows;
    private int acceptedRows;
    private int returnedRows;
    /** 入库+退回是否等于本册行数（false 时 mismatches 逐行点名） */
    private boolean balanced;

    /** 同一批次码此前已收过几回（0=头一回） */
    private int dupCount;
    /** 本次是否重复送达（第二回起不重复入行，回执照同一份行账重出） */
    private boolean duplicate;
    /** 第二回走的通道（与头一回不同也不并成第二册） */
    private String duplicatePutKind;

    private List<RowItem> rows = new ArrayList<RowItem>();
    /** 主档有出入的行（以主档为准，列出来） */
    private List<RowItem> masterDiffRows = new ArrayList<RowItem>();
    /** 一行一行对不齐的点名（没有即空，不许写"基本相符"） */
    private List<Mismatch> mismatches = new ArrayList<Mismatch>();
    /** 收讫口径一句话交代（上限怎么认、重送怎么算等，原文） */
    private String note;

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getPutKind() {
        return putKind;
    }

    public void setPutKind(String putKind) {
        this.putKind = putKind;
    }

    public String getQuarterCode() {
        return quarterCode;
    }

    public void setQuarterCode(String quarterCode) {
        this.quarterCode = quarterCode;
    }

    public Date getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Date receivedAt) {
        this.receivedAt = receivedAt;
    }

    public int getRowLimit() {
        return rowLimit;
    }

    public void setRowLimit(int rowLimit) {
        this.rowLimit = rowLimit;
    }

    public String getRowLimitRaw() {
        return rowLimitRaw;
    }

    public void setRowLimitRaw(String rowLimitRaw) {
        this.rowLimitRaw = rowLimitRaw;
    }

    public boolean isOverLimit() {
        return overLimit;
    }

    public void setOverLimit(boolean overLimit) {
        this.overLimit = overLimit;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getAcceptedRows() {
        return acceptedRows;
    }

    public void setAcceptedRows(int acceptedRows) {
        this.acceptedRows = acceptedRows;
    }

    public int getReturnedRows() {
        return returnedRows;
    }

    public void setReturnedRows(int returnedRows) {
        this.returnedRows = returnedRows;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public void setBalanced(boolean balanced) {
        this.balanced = balanced;
    }

    public int getDupCount() {
        return dupCount;
    }

    public void setDupCount(int dupCount) {
        this.dupCount = dupCount;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public String getDuplicatePutKind() {
        return duplicatePutKind;
    }

    public void setDuplicatePutKind(String duplicatePutKind) {
        this.duplicatePutKind = duplicatePutKind;
    }

    public List<RowItem> getRows() {
        return rows;
    }

    public void setRows(List<RowItem> rows) {
        this.rows = rows;
    }

    public List<RowItem> getMasterDiffRows() {
        return masterDiffRows;
    }

    public void setMasterDiffRows(List<RowItem> masterDiffRows) {
        this.masterDiffRows = masterDiffRows;
    }

    public List<Mismatch> getMismatches() {
        return mismatches;
    }

    public void setMismatches(List<Mismatch> mismatches) {
        this.mismatches = mismatches;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
