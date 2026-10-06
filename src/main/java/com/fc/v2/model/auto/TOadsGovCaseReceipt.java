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
 * 监管办件设置步核收登记对象 t_oads_gov_case_receipt
 *
 * <p>设置一步的核收未记为同意，验收不得开始：agree=1 才算这一项准入齐备。
 * 设置步退回重走时老轮行 active 置 0 保留，重走须重新记同意。
 *
 * @author fuce
 * @date 2026-10-06
 */
@TableName("t_oads_gov_case_receipt")
@ApiModel(value = "TOadsGovCaseReceipt", description = "监管办件设置步核收登记")
public class TOadsGovCaseReceipt implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 哪条办件 */
    @TableField("case_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "办件主键")
    private Long caseId;

    /** 设置步轮次（退回重走加一轮，老轮行保留） */
    @TableField("round_no")
    @ApiModelProperty(value = "设置步轮次")
    private Integer roundNo;

    /** 核收是否记为同意 1同意 0未同意 */
    @TableField("agree")
    @ApiModelProperty(value = "核收是否记为同意 1同意 0未同意")
    private Integer agree;

    /** 核收交代原文 */
    @TableField("note")
    @ApiModelProperty(value = "核收交代原文")
    private String note;

    /** 经办岗 */
    @TableField("operator")
    @ApiModelProperty(value = "经办岗")
    private String operator;

    /** 核收落账时刻（毫秒） */
    @TableField("received_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    @ApiModelProperty(value = "核收落账时刻")
    private Date receivedAt;

    /** 本轮是否在档 1当前轮 0设置步退回后随老轮退出 */
    @TableField("active")
    @ApiModelProperty(value = "本轮是否在档")
    private Integer active;

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

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public Integer getAgree() {
        return agree;
    }

    public void setAgree(Integer agree) {
        this.agree = agree;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Date getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Date receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Integer getActive() {
        return active;
    }

    public void setActive(Integer active) {
        this.active = active;
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
