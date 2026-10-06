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
import java.math.BigDecimal;
import java.util.Date;

/**
 * 牌匾更换名册行账对象 t_oads_plaque_row
 *
 * <p>进门点行数只认物理行：报文里几行就落几条，同一处点位写两行的两条都落，
 * 不按点位编码去重、不并笔——三个回执数都以这一地物理行为准，依据只此一个。
 * 行只许挂在收讫那一册上，不许游移到别册；老化归档只挪卷宗层 {@link #layer}，不换册不删行。
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_oads_plaque_row")
@ApiModel(value = "TOadsPlaqueRow", description = "牌匾更换名册行账")
public class TOadsPlaqueRow implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 落在哪一册（行不许游移到别册；归档只挪卷宗层不换册） */
    @TableField("book_id")
    @ApiModelProperty(value = "落在哪一册")
    private Long bookId;

    /** 批次码（冗余自册账） */
    @TableField("batch_no")
    @ApiModelProperty(value = "批次码")
    private String batchNo;

    /** 原报物理行次（进门点数的次序，同码两行各占各的行次） */
    @TableField("row_no")
    @ApiModelProperty(value = "原报物理行次")
    private Integer rowNo;

    /** 点位编码（身份只认它；缺填退回，不按版面名近似去配） */
    @TableField("spot_no")
    @ApiModelProperty(value = "点位编码")
    private String spotNo;

    /** 报文里的版面名称那串字符（只用来与主档比对，不作数） */
    @TableField("view_name")
    @ApiModelProperty(value = "报文里的版面名称（只用来比对）")
    private String viewName;

    /** 报文里的路段代号那串字符（只用来与主档比对，不作数） */
    @TableField("road_code")
    @ApiModelProperty(value = "报文里的路段代号（只用来比对）")
    private String roadCode;

    /** 本行应更换处数（须与核验合格处数同时到达；不代填不默零） */
    @TableField("qty")
    @ApiModelProperty(value = "本行应更换处数")
    private BigDecimal qty;

    /** 本行核验合格处数（两项只到一项即验收不过） */
    @TableField("chk_num")
    @ApiModelProperty(value = "本行核验合格处数")
    private BigDecimal chkNum;

    /** 现场照片附件编号（随册一起递；缺则空着占位待核，下轮补挂） */
    @TableField("photo_no")
    @ApiModelProperty(value = "现场照片附件编号")
    private String photoNo;

    /** 行结论 PENDING待核 PASS通过 REJECT退回（只此三值，服务端裁决） */
    @TableField("verdict")
    @ApiModelProperty(value = "行结论 PENDING/PASS/REJECT")
    private String verdict;

    /** 退回理由逐句原文（多条分号隔，哪行哪条一句句回查） */
    @TableField("reject_reason")
    @ApiModelProperty(value = "退回理由逐句原文")
    private String rejectReason;

    /** 主档点中的点位编码（全等配上才带出；配不上留空） */
    @TableField("master_spot_no")
    @ApiModelProperty(value = "主档点中的点位编码")
    private String masterSpotNo;

    /** 主档带出的版面名称（牌面题名） */
    @TableField("master_view_name")
    @ApiModelProperty(value = "主档带出的版面名称")
    private String masterViewName;

    /** 主档带出的路段代号 */
    @TableField("master_road_code")
    @ApiModelProperty(value = "主档带出的路段代号")
    private String masterRoadCode;

    /** 报文与主档有无出入 0无 1有（以主档为准，回执列出本行） */
    @TableField("master_diff")
    @ApiModelProperty(value = "报文与主档有无出入 0无 1有")
    private Integer masterDiff;

    /** 照片补挂那一刻（补挂前为空） */
    @TableField("photo_attached_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    @ApiModelProperty(value = "照片补挂那一刻")
    private Date photoAttachedAt;

    /** 卷宗层 0现行层 1历史卷宗层（老化归档逐行挪，行一条不减） */
    @TableField("layer")
    @ApiModelProperty(value = "卷宗层 0现行层 1历史卷宗层")
    private Integer layer;

    /** 本行转入历史卷宗层那一刻 */
    @TableField("archived_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    @ApiModelProperty(value = "本行转入历史卷宗层那一刻")
    private Date archivedAt;

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

    /** 备注（第四种结论写法原文等扫在此处） */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
    }

    public String getSpotNo() {
        return spotNo;
    }

    public void setSpotNo(String spotNo) {
        this.spotNo = spotNo;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public String getRoadCode() {
        return roadCode;
    }

    public void setRoadCode(String roadCode) {
        this.roadCode = roadCode;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public BigDecimal getChkNum() {
        return chkNum;
    }

    public void setChkNum(BigDecimal chkNum) {
        this.chkNum = chkNum;
    }

    public String getPhotoNo() {
        return photoNo;
    }

    public void setPhotoNo(String photoNo) {
        this.photoNo = photoNo;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getMasterSpotNo() {
        return masterSpotNo;
    }

    public void setMasterSpotNo(String masterSpotNo) {
        this.masterSpotNo = masterSpotNo;
    }

    public String getMasterViewName() {
        return masterViewName;
    }

    public void setMasterViewName(String masterViewName) {
        this.masterViewName = masterViewName;
    }

    public String getMasterRoadCode() {
        return masterRoadCode;
    }

    public void setMasterRoadCode(String masterRoadCode) {
        this.masterRoadCode = masterRoadCode;
    }

    public Integer getMasterDiff() {
        return masterDiff;
    }

    public void setMasterDiff(Integer masterDiff) {
        this.masterDiff = masterDiff;
    }

    public Date getPhotoAttachedAt() {
        return photoAttachedAt;
    }

    public void setPhotoAttachedAt(Date photoAttachedAt) {
        this.photoAttachedAt = photoAttachedAt;
    }

    public Integer getLayer() {
        return layer;
    }

    public void setLayer(Integer layer) {
        this.layer = layer;
    }

    public Date getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Date archivedAt) {
        this.archivedAt = archivedAt;
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
