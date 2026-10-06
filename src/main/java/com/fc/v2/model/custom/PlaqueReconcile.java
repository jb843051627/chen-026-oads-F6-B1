package com.fc.v2.model.custom;

import java.util.ArrayList;
import java.util.List;

/**
 * 牌匾更换名册"销账与流水"对账结果。
 *
 * <p>口径定死：销账（册账三数与行账汇总）与收讫流水（append-only）联动，两边对不上的那一笔，
 * <b>账实差以流水为准</b>——由收讫环节按流水把销账抹平；这个取舍由本环节在收讫规矩里定，
 * 不由页面挑、不由经办人挑。每次抹平本身再落一笔 ADJUST 流水，归"收讫环节对账"认。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class PlaqueReconcile {

    /** 一笔对不上的账（销账那侧 vs 流水那侧） */
    public static class Diff {
        private String batchNo;
        private String field;
        private int ledgerValue;
        private int flowValue;
        private String detail;

        public Diff() {
        }

        public Diff(String batchNo, String field, int ledgerValue, int flowValue, String detail) {
            this.batchNo = batchNo;
            this.field = field;
            this.ledgerValue = ledgerValue;
            this.flowValue = flowValue;
            this.detail = detail;
        }

        public String getBatchNo() {
            return batchNo;
        }

        public void setBatchNo(String batchNo) {
            this.batchNo = batchNo;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public int getLedgerValue() {
            return ledgerValue;
        }

        public void setLedgerValue(int ledgerValue) {
            this.ledgerValue = ledgerValue;
        }

        public int getFlowValue() {
            return flowValue;
        }

        public void setFlowValue(int flowValue) {
            this.flowValue = flowValue;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }
    }

    /** 参与对账的册数 */
    private int bookCount;
    /** 对不上的笔数（抹平前） */
    private int diffCount;
    /** 是否已经按流水抹平销账 */
    private boolean adjusted;
    private List<Diff> diffs = new ArrayList<Diff>();
    /** 口径一句话：账实差以流水为准，校正归收讫环节认 */
    private String note;

    public int getBookCount() {
        return bookCount;
    }

    public void setBookCount(int bookCount) {
        this.bookCount = bookCount;
    }

    public int getDiffCount() {
        return diffCount;
    }

    public void setDiffCount(int diffCount) {
        this.diffCount = diffCount;
    }

    public boolean isAdjusted() {
        return adjusted;
    }

    public void setAdjusted(boolean adjusted) {
        this.adjusted = adjusted;
    }

    public List<Diff> getDiffs() {
        return diffs;
    }

    public void setDiffs(List<Diff> diffs) {
        this.diffs = diffs;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
