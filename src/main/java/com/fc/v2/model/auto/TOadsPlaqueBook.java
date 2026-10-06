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
 * 牌匾更换名册册账对象 t_oads_plaque_book
 *
 * <p>一次报送为一册，册不拆开送；册账一行就是一册。同一批次码从区级报送与市级复核
 * 两条通道各递一回时，也只有这一行，第二回不另立册。
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_oads_plaque_book")
@ApiModel(value = "TOadsPlaqueBook", description = "牌匾更换名册册账")
public class TOadsPlaqueBook implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 批次码（册的身份；区级报送与市级复核两条通道递的是同一串） */
    @TableField("batch_no")
    @ApiModelProperty(value = "批次码")
    private String batchNo;

    /** 名册册号（报送单位+季度合成，仅记账用） */
    @TableField("book_no")
    @ApiModelProperty(value = "名册册号")
    private String bookNo;

    /** 归并上送的单位代号（各单位先归并到区里） */
    @TableField("unit_no")
    @ApiModelProperty(value = "归并上送的单位代号")
    private String unitNo;

    /** 季度码 按季合成一册 如2026Q4 */
    @TableField("quarter_code")
    @ApiModelProperty(value = "季度码")
    private String quarterCode;

    /** 收自哪条通道 AREA区级报送 CITY市级复核（只记第一回那一条） */
    @TableField("put_kind")
    @ApiModelProperty(value = "收自哪条通道 AREA/CITY")
    private String putKind;

    /** 抬头那格原文（空/负数/大得没边都照原文落，事后翻得出当初写了什么） */
    @TableField("row_limit_raw")
    @ApiModelProperty(value = "抬头那格原文")
    private String rowLimitRaw;

    /** 认下的单册行数上限（抬头唯一来源；认不得的按200） */
    @TableField("row_limit")
    @ApiModelProperty(value = "认下的单册行数上限")
    private Integer rowLimit;

    /** 进门点数所得物理行数（与上限一并落账，事后能翻出当初数了多少） */
    @TableField("counted_rows")
    @ApiModelProperty(value = "进门点数所得物理行数")
    private Integer countedRows;

    /** 入库行数（通过+待核，都在册里占位；回库逐行数，不抄页面临值） */
    @TableField("accepted_rows")
    @ApiModelProperty(value = "入库行数")
    private Integer acceptedRows;

    /** 退回行数（回库逐行数）；入库+退回=本册行数 */
    @TableField("returned_rows")
    @ApiModelProperty(value = "退回行数")
    private Integer returnedRows;

    /** 是否越过抬头上限 0否 1是（照常收讫，只在账上与回执点名，不拦册） */
    @TableField("over_limit")
    @ApiModelProperty(value = "是否越过抬头上限 0否 1是")
    private Integer overLimit;

    /** 同一批次码重复送达回数（第二回起每回加一，行不重复入） */
    @TableField("dup_count")
    @ApiModelProperty(value = "重复送达回数")
    private Integer dupCount;

    /** 收讫那一刻（四步同次做完） */
    @TableField("received_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    @ApiModelProperty(value = "收讫那一刻")
    private Date receivedAt;

    /** 册情形 0已收讫 */
    @TableField("status")
    @ApiModelProperty(value = "册情形 0已收讫")
    private Integer status;

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

    /** 备注（上限认下200等说法逐字落此） */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

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

    public String getBookNo() {
        return bookNo;
    }

    public void setBookNo(String bookNo) {
        this.bookNo = bookNo;
    }

    public String getUnitNo() {
        return unitNo;
    }

    public void setUnitNo(String unitNo) {
        this.unitNo = unitNo;
    }

    public String getQuarterCode() {
        return quarterCode;
    }

    public void setQuarterCode(String quarterCode) {
        this.quarterCode = quarterCode;
    }

    public String getPutKind() {
        return putKind;
    }

    public void setPutKind(String putKind) {
        this.putKind = putKind;
    }

    public String getRowLimitRaw() {
        return rowLimitRaw;
    }

    public void setRowLimitRaw(String rowLimitRaw) {
        this.rowLimitRaw = rowLimitRaw;
    }

    public Integer getRowLimit() {
        return rowLimit;
    }

    public void setRowLimit(Integer rowLimit) {
        this.rowLimit = rowLimit;
    }

    public Integer getCountedRows() {
        return countedRows;
    }

    public void setCountedRows(Integer countedRows) {
        this.countedRows = countedRows;
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

    public Integer getOverLimit() {
        return overLimit;
    }

    public void setOverLimit(Integer overLimit) {
        this.overLimit = overLimit;
    }

    public Integer getDupCount() {
        return dupCount;
    }

    public void setDupCount(Integer dupCount) {
        this.dupCount = dupCount;
    }

    public Date getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Date receivedAt) {
        this.receivedAt = receivedAt;
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
