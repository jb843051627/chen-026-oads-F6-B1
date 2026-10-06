package com.fc.v2.model.custom;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 三种视图的处数：按辖段类别、按路段代号、按建档情形。
 *
 * <p>同一季度同一批行，三种视图给出的处数必须一致——前端数一次、数据库数一次，
 * 两个数都出自裁决处同一份基础数据（一次快照分组），总数同生同成。
 * 视图为空是合法结果（本季度未建行时），不用邻段数据补齐。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class SpotViewCounts {

    /** 按辖段类别：主干道/次干道/背街巷 */
    private Map<String, Integer> byAreaKind = new LinkedHashMap<String, Integer>();

    /** 按路段代号：每个代号一处数 */
    private Map<String, Integer> byAreaNo = new LinkedHashMap<String, Integer>();

    /** 按建档情形：MANUAL/BATCH/LEGACY（纸质簿搬入的旧行） */
    private Map<String, Integer> byBuildKind = new LinkedHashMap<String, Integer>();

    /** 三视图同一总数；本层当季未建行时为 0，留空不补齐 */
    private int total;

    private String quarterCode;

    public Map<String, Integer> getByAreaKind() {
        return byAreaKind;
    }

    public void setByAreaKind(Map<String, Integer> byAreaKind) {
        this.byAreaKind = byAreaKind;
    }

    public Map<String, Integer> getByAreaNo() {
        return byAreaNo;
    }

    public void setByAreaNo(Map<String, Integer> byAreaNo) {
        this.byAreaNo = byAreaNo;
    }

    public Map<String, Integer> getByBuildKind() {
        return byBuildKind;
    }

    public void setByBuildKind(Map<String, Integer> byBuildKind) {
        this.byBuildKind = byBuildKind;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getQuarterCode() {
        return quarterCode;
    }

    public void setQuarterCode(String quarterCode) {
        this.quarterCode = quarterCode;
    }
}
