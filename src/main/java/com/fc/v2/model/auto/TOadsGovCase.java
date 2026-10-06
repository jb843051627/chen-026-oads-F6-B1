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
 * 监管办件主档对象 t_oads_gov_case
 *
 * <p>一条办件跟着一处在册点位走，固定四步 1申报 2核准 3设置 4验收。
 * 当前步次以 t_oads_gov_case_log 留痕重数为准，本列只作落账与回显；报文里带步次只作参考。
 *
 * @author fuce
 * @date 2026-10-06
 */
@TableName("t_oads_gov_case")
@ApiModel(value = "TOadsGovCase", description = "监管办件主档")
public class TOadsGovCase implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 办件编号（服务端发，报送不自带） */
    @TableField("case_no")
    @ApiModelProperty(value = "办件编号")
    private String caseNo;

    /** 跟着的那一处在册点位行 */
    @TableField("spot_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "点位主档行主键")
    private Long spotId;

    /** 点位编码（建立四项输入之一） */
    @TableField("spot_no")
    @ApiModelProperty(value = "点位编码")
    private String spotNo;

    /** 所属路段代号（建立四项输入之一） */
    @TableField("area_no")
    @ApiModelProperty(value = "所属路段代号")
    private String areaNo;

    /** 处数（建立四项输入之一） */
    @TableField("place_count")
    @ApiModelProperty(value = "处数")
    private Integer placeCount;

    /** 拟判定的校核分值（建立四项输入之一） */
    @TableField("check_score")
    @ApiModelProperty(value = "拟判定的校核分值")
    private Integer checkScore;

    /** 当前步次 1申报 2核准 3设置 4验收（服务端按留痕重数落账，报文值不作数） */
    @TableField("step_no")
    @ApiModelProperty(value = "当前步次（服务端重数）")
    private Integer stepNo;

    /** 收尾形态 RUNNING在走 SEALED已封存 WITHDRAWN已撤回 REALIGNED已划归邻段 VOID已作废 */
    @TableField("outcome")
    @ApiModelProperty(value = "收尾形态")
    private String outcome;

    /** 封存/收尾达成那一刻 */
    @TableField("sealed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    @ApiModelProperty(value = "封存/收尾时刻")
    private Date sealedAt;

    /** 复验凭证编码（验收步两名见证齐备同次回写本行） */
    @TableField("recheck_cert_no")
    @ApiModelProperty(value = "复验凭证编码")
    private String recheckCertNo;

    /** 同批并办幂等键（同键重放只回首条结果，不立第二条） */
    @TableField("idem_key")
    @ApiModelProperty(value = "幂等键")
    private String idemKey;

    /** 删除标记 0正常 1删除（撤销行不进主档） */
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

    public String getCaseNo() {
        return caseNo;
    }

    public void setCaseNo(String caseNo) {
        this.caseNo = caseNo;
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

    public String getAreaNo() {
        return areaNo;
    }

    public void setAreaNo(String areaNo) {
        this.areaNo = areaNo;
    }

    public Integer getPlaceCount() {
        return placeCount;
    }

    public void setPlaceCount(Integer placeCount) {
        this.placeCount = placeCount;
    }

    public Integer getCheckScore() {
        return checkScore;
    }

    public void setCheckScore(Integer checkScore) {
        this.checkScore = checkScore;
    }

    public Integer getStepNo() {
        return stepNo;
    }

    public void setStepNo(Integer stepNo) {
        this.stepNo = stepNo;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public Date getSealedAt() {
        return sealedAt;
    }

    public void setSealedAt(Date sealedAt) {
        this.sealedAt = sealedAt;
    }

    public String getRecheckCertNo() {
        return recheckCertNo;
    }

    public void setRecheckCertNo(String recheckCertNo) {
        this.recheckCertNo = recheckCertNo;
    }

    public String getIdemKey() {
        return idemKey;
    }

    public void setIdemKey(String idemKey) {
        this.idemKey = idemKey;
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
