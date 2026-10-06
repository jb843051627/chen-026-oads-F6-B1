package com.fc.v2.model.custom;

import com.fc.v2.model.auto.TOadsPermitBill;
import com.fc.v2.model.auto.TOadsPermitSign;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 核准单整单视图：单头加按段分节。
 *
 * <p>当前处于哪一节、每节应到几人、已到几笔，全部服务端算好回显；被清掉的老轮文本
 * 一并带出（active=0），本轮与上轮分开摆。
 *
 * @author fuce
 * @date 2026-10-05
 */
@ApiModel(value = "PermitBillView", description = "核准单整单视图")
public class PermitBillView implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 单头（段次、人数为服务端值） */
    @ApiModelProperty(value = "单头")
    private TOadsPermitBill bill;

    /** 三段分节，顺序即窗口受理→区局核定→市局备案 */
    @ApiModelProperty(value = "三段分节")
    private List<Stage> stages = new ArrayList<Stage>();

    public TOadsPermitBill getBill() {
        return bill;
    }

    public void setBill(TOadsPermitBill bill) {
        this.bill = bill;
    }

    public List<Stage> getStages() {
        return stages;
    }

    public void setStages(List<Stage> stages) {
        this.stages = stages;
    }

    /** 一节：四项数据（要求人数、通过办法、已签笔数、整单当前段）外加留痕 */
    @ApiModel(value = "PermitBillView.Stage", description = "核准单一节")
    public static class Stage implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "段次 0..2")
        private int nodeNo;
        @ApiModelProperty(value = "段名")
        private String stageName;
        @ApiModelProperty(value = "岗位名")
        private String postName;
        @ApiModelProperty(value = "本段要求签署人数")
        private int needCount;
        @ApiModelProperty(value = "本段通过办法码 0一名即过 1两关须同判 2先落为定")
        private int signMode;
        @ApiModelProperty(value = "整单当前是否停在本段")
        private boolean current;
        @ApiModelProperty(value = "本段是否已通过（当前轮 active 签署按规则本判过）")
        private boolean passed;
        @ApiModelProperty(value = "本段当前轮次")
        private int roundNo;
        @ApiModelProperty(value = "本段当前轮已落笔数（回显值，与留痕同一次算出）")
        private int signCount;
        @ApiModelProperty(value = "本段全部留痕，含被清退出的老轮，按轮次、笔序排")
        private List<TOadsPermitSign> signs = new ArrayList<TOadsPermitSign>();

        public int getNodeNo() {
            return nodeNo;
        }

        public void setNodeNo(int nodeNo) {
            this.nodeNo = nodeNo;
        }

        public String getStageName() {
            return stageName;
        }

        public void setStageName(String stageName) {
            this.stageName = stageName;
        }

        public String getPostName() {
            return postName;
        }

        public void setPostName(String postName) {
            this.postName = postName;
        }

        public int getNeedCount() {
            return needCount;
        }

        public void setNeedCount(int needCount) {
            this.needCount = needCount;
        }

        public int getSignMode() {
            return signMode;
        }

        public void setSignMode(int signMode) {
            this.signMode = signMode;
        }

        public boolean isCurrent() {
            return current;
        }

        public void setCurrent(boolean current) {
            this.current = current;
        }

        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public int getRoundNo() {
            return roundNo;
        }

        public void setRoundNo(int roundNo) {
            this.roundNo = roundNo;
        }

        public int getSignCount() {
            return signCount;
        }

        public void setSignCount(int signCount) {
            this.signCount = signCount;
        }

        public List<TOadsPermitSign> getSigns() {
            return signs;
        }

        public void setSigns(List<TOadsPermitSign> signs) {
            this.signs = signs;
        }
    }
}
