package com.fc.v2.model.custom;

import com.fc.v2.model.auto.TOadsGovCase;
import com.fc.v2.model.auto.TOadsGovCaseLog;
import com.fc.v2.model.auto.TOadsGovCaseReceipt;
import com.fc.v2.model.auto.TOadsGovCaseWitness;

import java.util.ArrayList;
import java.util.List;

/**
 * 一条办件的追溯回查：办件头 + 按时序压着的逐笔留痕（含后退转追溯的老轮）+ 本轮核收/见证。
 *
 * <p>头里的步次是现场按同串留痕重数的，与受理流水逐笔对同一套数法；不另取一套算法凑步数。
 *
 * @author fuce
 * @date 2026-10-06
 */
public class GovCaseTrace {

    private TOadsGovCase govCase;
    /** 系统按留痕重数的当前步次（与头列面子值可能不一致时，以此为准） */
    private Integer recountedStep;
    private String recountedStepName;
    /** 全部留痕按时序（含老轮、撤回/划归/作废） */
    private List<TOadsGovCaseLog> logs = new ArrayList<TOadsGovCaseLog>();
    /** 留痕总笔数：与当天受理流水对笔数用 */
    private int ledgerCount;
    /** 设置步本轮核收（无则 null，即未记同意） */
    private TOadsGovCaseReceipt currentReceipt;
    /** 验收步本轮在档见证 */
    private List<TOadsGovCaseWitness> currentWitnesses = new ArrayList<TOadsGovCaseWitness>();

    public TOadsGovCase getGovCase() {
        return govCase;
    }

    public void setGovCase(TOadsGovCase govCase) {
        this.govCase = govCase;
    }

    public Integer getRecountedStep() {
        return recountedStep;
    }

    public void setRecountedStep(Integer recountedStep) {
        this.recountedStep = recountedStep;
    }

    public String getRecountedStepName() {
        return recountedStepName;
    }

    public void setRecountedStepName(String recountedStepName) {
        this.recountedStepName = recountedStepName;
    }

    public List<TOadsGovCaseLog> getLogs() {
        return logs;
    }

    public void setLogs(List<TOadsGovCaseLog> logs) {
        this.logs = logs;
    }

    public int getLedgerCount() {
        return ledgerCount;
    }

    public void setLedgerCount(int ledgerCount) {
        this.ledgerCount = ledgerCount;
    }

    public TOadsGovCaseReceipt getCurrentReceipt() {
        return currentReceipt;
    }

    public void setCurrentReceipt(TOadsGovCaseReceipt currentReceipt) {
        this.currentReceipt = currentReceipt;
    }

    public List<TOadsGovCaseWitness> getCurrentWitnesses() {
        return currentWitnesses;
    }

    public void setCurrentWitnesses(List<TOadsGovCaseWitness> currentWitnesses) {
        this.currentWitnesses = currentWitnesses;
    }
}
