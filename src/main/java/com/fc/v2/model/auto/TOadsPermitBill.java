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
 * 设置许可核准单对象 t_oads_permit_bill
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_oads_permit_bill")
@ApiModel(value = "TOadsPermitBill", description = "设置许可核准单")
public class TOadsPermitBill implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 设置许可核准单号 */
    @TableField("bill_no")
    @ApiModelProperty(value = "设置许可核准单号")
    private String billNo;

    /** 单据来源：点位主档那一行的主键（只能由主档记录生成） */
    @TableField("spot_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "点位主档行主键（单据唯一来源）")
    private Long spotId;

    /** 点位码（建单时自点位主档冗余） */
    @TableField("spot_no")
    @ApiModelProperty(value = "点位码")
    private String spotNo;

    /** 本次变更情形 HANG挂上去 MOVE改位置 REMOVE整块拆除 */
    @TableField("change_kind")
    @ApiModelProperty(value = "变更情形 HANG/MOVE/REMOVE")
    private String changeKind;

    /** 变更前挂牌情形档 */
    @TableField("from_state")
    @ApiModelProperty(value = "变更前挂牌情形档")
    private Integer fromState;

    /** 变更后挂牌情形档（封存时写回主档） */
    @TableField("to_state")
    @ApiModelProperty(value = "变更后挂牌情形档")
    private Integer toState;

    /** 当前所在段 0..2（窗口受理/区局核定/市局备案，服务端计算，不采信报文） */
    @TableField("node_no")
    @ApiModelProperty(value = "当前所在段 0..2（服务端计算）")
    private Integer nodeNo;

    /** 本段通过办法 0一名即过 1两关须同判 2先落为定（三段各用各的） */
    @TableField("sign_mode")
    @ApiModelProperty(value = "本段通过办法 0一名即过 1两关须同判 2先落为定")
    private Integer signMode;

    /** 本段要求签署人数 */
    @TableField("need_count")
    @ApiModelProperty(value = "本段要求签署人数")
    private Integer needCount;

    /** 本段本轮已落单笔数（只按留痕逐笔算，屏上仅回显） */
    @TableField("sign_count")
    @ApiModelProperty(value = "本段本轮已落单笔数（服务端累加，仅回显）")
    private Integer signCount;

    /** 本段当前轮次（本段被清一次加一轮） */
    @TableField("round_no")
    @ApiModelProperty(value = "本段当前轮次")
    private Integer roundNo;

    /** 核准情形 0在签(含待议) 1已点齐(已封存) 2已压回(本段本轮被否) */
    @TableField("status")
    @ApiModelProperty(value = "核准情形 0在签 1已点齐(封存) 2已压回")
    private Integer status;

    /** 在途去重令牌 同一点位同一种变更在途只一张；封存置空 */
    @TableField("live_token")
    @ApiModelProperty(value = "在途去重令牌")
    private String liveToken;

    /** 走满封存那一刻 */
    @TableField("sealed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    @ApiModelProperty(value = "封存时刻")
    private Date sealedAt;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
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

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public String getLiveToken() {
        return liveToken;
    }

    public void setLiveToken(String liveToken) {
        this.liveToken = liveToken;
    }

    public Date getSealedAt() {
        return sealedAt;
    }

    public void setSealedAt(Date sealedAt) {
        this.sealedAt = sealedAt;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
    }

    public Integer getSignMode() {
        return signMode;
    }

    public void setSignMode(Integer signMode) {
        this.signMode = signMode;
    }

    public Integer getNeedCount() {
        return needCount;
    }

    public void setNeedCount(Integer needCount) {
        this.needCount = needCount;
    }

    public Integer getSignCount() {
        return signCount;
    }

    public void setSignCount(Integer signCount) {
        this.signCount = signCount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
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

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
