package com.fc.v2.model.custom;

import java.util.ArrayList;
import java.util.List;

/**
 * 市里每日增量同步结果：冲突逐条清单 + 游标推进。
 *
 * <p>比对起点全库唯一，记在游标字段：游标之前的行不复核，游标之后才参与本次比较。
 * 两边全称冲突、或市里已注销而本局仍挂载的，以本局为准并逐条输出冲突清单，不允许静默覆盖。
 * 游标随本次同步一并推进，且与本次结果出自同一份计算。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class CitySyncOutcome {

    public static final String NAME_CONFLICT = "NAME_CONFLICT";
    public static final String CITY_REVOKED = "CITY_REVOKED";

    /** 一条冲突 */
    public static class Conflict {
        private String spotNo;
        private String areaNo;
        private String kind;
        /** 本局一侧全称（目录为准） */
        private String localName;
        /** 市里一侧全称 */
        private String cityName;
        private String detail;

        public Conflict() {
        }

        public Conflict(String spotNo, String areaNo, String kind, String localName, String cityName, String detail) {
            this.spotNo = spotNo;
            this.areaNo = areaNo;
            this.kind = kind;
            this.localName = localName;
            this.cityName = cityName;
            this.detail = detail;
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

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String getLocalName() {
            return localName;
        }

        public void setLocalName(String localName) {
            this.localName = localName;
        }

        public String getCityName() {
            return cityName;
        }

        public void setCityName(String cityName) {
            this.cityName = cityName;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }
    }

    private long cursorBefore;
    private long cursorAfter;
    private int windowRows;
    private List<Conflict> conflicts = new ArrayList<Conflict>();
    private String note;

    public long getCursorBefore() {
        return cursorBefore;
    }

    public void setCursorBefore(long cursorBefore) {
        this.cursorBefore = cursorBefore;
    }

    public long getCursorAfter() {
        return cursorAfter;
    }

    public void setCursorAfter(long cursorAfter) {
        this.cursorAfter = cursorAfter;
    }

    public int getWindowRows() {
        return windowRows;
    }

    public void setWindowRows(int windowRows) {
        this.windowRows = windowRows;
    }

    public List<Conflict> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<Conflict> conflicts) {
        this.conflicts = conflicts;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
