package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 点位主档变更履历对象 t_oads_spot_change
 *
 * <p>封存那一次处理里与主档改动同生的一条：由谁操作、依据哪张单、从哪一档改到哪一档、
 * 发生于哪一时刻。只许追加，支持按时间倒序回看。
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_oads_spot_change")
@ApiModel(value = "TOadsSpotChange", description = "点位主档变更履历")
public class TOadsSpotChange implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 点位主档行 */
    @TableField("spot_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "点位主档行")
    private Long spotId;

    /** 点位码 */
    @TableField("spot_no")
    @ApiModelProperty(value = "点位码")
    private String spotNo;

    /** 依据哪张核准单 */
    @TableField("bill_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "依据的核准单")
    private Long billId;

    /** 依据的核准单号 */
    @TableField("bill_no")
    @ApiModelProperty(value = "依据的核准单号")
    private String billNo;

    /** 变更情形 HANG挂上去 MOVE改位置 REMOVE整块拆除 */
    @TableField("change_kind")
    @ApiModelProperty(value = "变更情形 HANG/MOVE/REMOVE")
    private String changeKind;

    /** 从哪一档（挂牌情形档） */
    @TableField("from_state")
    @ApiModelProperty(value = "从哪一档")
    private Integer fromState;

    /** 改到哪一档（挂牌情形档） */
    @TableField("to_state")
    @ApiModelProperty(value = "改到哪一档")
    private Integer toState;

    /** 变更前挂设档位 */
    @TableField("from_ver")
    @ApiModelProperty(value = "变更前挂设档位")
    private Integer fromVer;

    /** 变更后挂设档位 */
    @TableField("to_ver")
    @ApiModelProperty(value = "变更后挂设档位")
    private Integer toVer;

    /** 由谁操作（封存处理人） */
    @TableField("operate_by")
    @ApiModelProperty(value = "由谁操作")
    private String operateBy;

    /** 发生于哪一时刻 */
    @TableField("change_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    @ApiModelProperty(value = "变更时刻")
    private Date changeTime;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSpotId() {
        return spotId;
    }

    public void setSpotId(Long spotId) {
        this.spotId = spotId;
    }

    public String getSpotNo() {
        return spotNo;
    }

    public void setSpotNo(String spotNo) {
        this.spotNo = spotNo;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getChangeKind() {
        return changeKind;
    }

    public void setChangeKind(String changeKind) {
        this.changeKind = changeKind;
    }

    public Integer getFromState() {
        return fromState;
    }

    public void setFromState(Integer fromState) {
        this.fromState = fromState;
    }

    public Integer getToState() {
        return toState;
    }

    public void setToState(Integer toState) {
        this.toState = toState;
    }

    public Integer getFromVer() {
        return fromVer;
    }

    public void setFromVer(Integer fromVer) {
        this.fromVer = fromVer;
    }

    public Integer getToVer() {
        return toVer;
    }

    public void setToVer(Integer toVer) {
        this.toVer = toVer;
    }

    public String getOperateBy() {
        return operateBy;
    }

    public void setOperateBy(String operateBy) {
        this.operateBy = operateBy;
    }

    public Date getChangeTime() {
        return changeTime;
    }

    public void setChangeTime(Date changeTime) {
        this.changeTime = changeTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
