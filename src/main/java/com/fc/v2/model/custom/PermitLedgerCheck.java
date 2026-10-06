package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 核准单台账对账结果。
 *
 * <p>页面顶部那个未封存数与逐段累加结果出自同一次计算；两边一旦对不上，这里把对应的
 * 单据号逐一指名报出，不许只报一个差数。
 *
 * @author fuce
 * @date 2026-10-05
 */
@ApiModel(value = "PermitLedgerCheck", description = "核准单台账对账结果")
public class PermitLedgerCheck implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 未封存单据数（建单加一、封存减一） */
    @ApiModelProperty(value = "未封存单据数")
    private long unsealedCount;

    /** 按逐段留痕重算应在途的单据数 */
    @ApiModelProperty(value = "逐段留痕重算的在途数")
    private long recountedCount;

    /** 两边是否一致 */
    @ApiModelProperty(value = "是否账账相符")
    private boolean balanced;

    /** 对不上的单据：单据号加毛病说明 */
    @ApiModelProperty(value = "对不上的单据指名")
    private List<String> mismatches = new ArrayList<String>();

    public long getUnsealedCount() {
        return unsealedCount;
    }

    public void setUnsealedCount(long unsealedCount) {
        this.unsealedCount = unsealedCount;
    }

    public long getRecountedCount() {
        return recountedCount;
    }

    public void setRecountedCount(long recountedCount) {
        this.recountedCount = recountedCount;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public void setBalanced(boolean balanced) {
        this.balanced = balanced;
    }

    public List<String> getMismatches() {
        return mismatches;
    }

    public void setMismatches(List<String> mismatches) {
        this.mismatches = mismatches;
    }
}
