package com.fc.v2.model.custom;

import java.util.ArrayList;
import java.util.List;

/**
 * 集中更换核验完成后"带出建行"的整批回执。
 *
 * <p>两行通道同一套数量逻辑与同一套判定：这里不做第二套校验，只把每一行交裁决处，
 * 过的收回执、不过的带回该检查点各自的提示语，逐行交代，不合并成一句"数据有误"。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class SpotBatchBringOutcome {

    /** 一行的去处 */
    public static class Item {
        private String spotNo;
        private boolean success;
        /** 成功时的建行回执 */
        private SpotBuildResult result;
        /** 失败时该检查点自己的提示语（原文） */
        private String error;

        public Item() {
        }

        public static Item ok(String spotNo, SpotBuildResult result) {
            Item i = new Item();
            i.spotNo = spotNo;
            i.success = true;
            i.result = result;
            return i;
        }

        public static Item fail(String spotNo, String error) {
            Item i = new Item();
            i.spotNo = spotNo;
            i.success = false;
            i.error = error;
            return i;
        }

        public String getSpotNo() {
            return spotNo;
        }

        public void setSpotNo(String spotNo) {
            this.spotNo = spotNo;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public SpotBuildResult getResult() {
            return result;
        }

        public void setResult(SpotBuildResult result) {
            this.result = result;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    private String batchNo;
    private int total;
    private int successCount;
    private int failureCount;
    private List<Item> items = new ArrayList<Item>();

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
