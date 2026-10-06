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
 * 牌匾更换名册收讫流水对象 t_oads_plaque_flow
 *
 * <p>append-only：一次收讫、重复送达、出回执、对账校正、老化归档各落一行，只许追加，
 * 不改不删。销账（册账/行账汇总）与流水同事务写；账实有差以本流水为准，
 * 对账时按流水抹平销账，校正那一笔归收讫环节认。
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_oads_plaque_flow")
@ApiModel(value = "TOadsPlaqueFlow", description = "牌匾更换名册收讫流水")
public class TOadsPlaqueFlow implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 哪一册（批次码） */
    @TableField("batch_no")
    @ApiModelProperty(value = "批次码")
    private String batchNo;

    /** 动作 RECEIVE收讫 RESEND重复送达 RECEIPT出回执 ADJUST对账校正 ARCHIVE老化归档 */
    @TableField("action")
    @ApiModelProperty(value = "动作 RECEIVE/RESEND/RECEIPT/ADJUST/ARCHIVE")
    private String action;

    /** 本笔走的通道 AREA/CITY（RESEND 记第二回走的那条） */
    @TableField("put_kind")
    @ApiModelProperty(value = "本笔走的通道 AREA/CITY")
    private String putKind;

    /** 本笔记的本册行数（快照，回库逐行数出来的那个） */
    @TableField("total_rows")
    @ApiModelProperty(value = "本笔记的本册行数")
    private Integer totalRows;

    /** 本笔记的入库行数 */
    @TableField("accepted_rows")
    @ApiModelProperty(value = "本笔记的入库行数")
    private Integer acceptedRows;

    /** 本笔记的退回行数 */
    @TableField("returned_rows")
    @ApiModelProperty(value = "本笔记的退回行数")
    private Integer returnedRows;

    /** 这一笔的交代（逐行点名/上限认值/校正归谁认，原文） */
    @TableField("content")
    @ApiModelProperty(value = "这一笔的交代")
    private String content;

    /** 经办岗（系统对账时记收讫环节） */
    @TableField("operator")
    @ApiModelProperty(value = "经办岗")
    private String operator;

    /** 落账时刻（毫秒） */
    @TableField("acted_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    @ApiModelProperty(value = "落账时刻")
    private Date actedAt;

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

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPutKind() {
        return putKind;
    }

    public void setPutKind(String putKind) {
        this.putKind = putKind;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getAcceptedRows() {
        return acceptedRows;
    }

    public void setAcceptedRows(Integer acceptedRows) {
        this.acceptedRows = acceptedRows;
    }

    public Integer getReturnedRows() {
        return returnedRows;
    }

    public void setReturnedRows(Integer returnedRows) {
        this.returnedRows = returnedRows;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Date getActedAt() {
        return actedAt;
    }

    public void setActedAt(Date actedAt) {
        this.actedAt = actedAt;
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
