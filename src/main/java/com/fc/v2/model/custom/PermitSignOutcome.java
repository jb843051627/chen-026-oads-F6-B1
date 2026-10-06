package com.fc.v2.model.custom;

import com.fc.v2.model.auto.TOadsPermitBill;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * 一次签署落库后的交代。
 *
 * <p>落不落库、留在本段还是推进、是否清轮重走、是否封存，都由服务端按段次规则本点出。
 *
 * @author fuce
 * @date 2026-10-05
 */
@ApiModel(value = "PermitSignOutcome", description = "核准单签署结果")
public class PermitSignOutcome implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 落单但本段尚未凑齐（含第二段先落一方等后落一方） */
    public static final int LANDED = 0;
    /** 本段通过，整单推进下一段 */
    public static final int ADVANCED = 1;
    /** 本段给出不同意结论：清本段本轮，本段起新一轮重走 */
    public static final int VETO_CLEARED = 2;
    /** 第三段一同一异挂待议：不推进不终止，本轮另存上轮，本段起新一轮 */
    public static final int SPLIT_PENDING = 3;
    /** 第三段两条同意齐备：走满封存，主档与履历同一次处理已改 */
    public static final int SEALED = 4;
    /** 跳段来的意见不予采信：库里一条不添，整单留在原段 */
    public static final int DISCARDED = 5;

    @ApiModelProperty(value = "结果 0落单 1推进 2否决清轮 3待议清轮 4封存")
    private int outcome;

    @ApiModelProperty(value = "服务端交代原话")
    private String message;

    @ApiModelProperty(value = "处理后的单据（段次、人数均为服务端重算值）")
    private TOadsPermitBill bill;

    public static PermitSignOutcome of(int outcome, String message, TOadsPermitBill bill) {
        PermitSignOutcome r = new PermitSignOutcome();
        r.outcome = outcome;
        r.message = message;
        r.bill = bill;
        return r;
    }

    public int getOutcome() {
        return outcome;
    }

    public void setOutcome(int outcome) {
        this.outcome = outcome;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public TOadsPermitBill getBill() {
        return bill;
    }

    public void setBill(TOadsPermitBill bill) {
        this.bill = bill;
    }
}
