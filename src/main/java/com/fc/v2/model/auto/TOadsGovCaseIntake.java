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
 * 监管办件立件前报入暂存对象 t_oads_gov_case_intake
 *
 * <p>撤销只可能出现在第一条办件正式立起来之前那一小段：报入落本行，立件后标 OPENED 并指向办件，
 * 撤销标 REVOKED。被撤记录不进主档（t_oads_gov_case），也不占用未走完位置。
 *
 * @author fuce
 * @date 2026-10-06
 */
@TableName("t_oads_gov_case_intake")
@ApiModel(value = "TOadsGovCaseIntake", description = "监管办件立件前报入暂存")
public class TOadsGovCaseIntake implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 报入流水号（服务端发） */
    @TableField("intake_no")
    @ApiModelProperty(value = "报入流水号")
    private String intakeNo;

    /** 点位编码 */
    @TableField("spot_no")
    @ApiModelProperty(value = "点位编码")
    private String spotNo;

    /** 所属路段代号 */
    @TableField("area_no")
    @ApiModelProperty(value = "所属路段代号")
    private String areaNo;

    /** 处数 */
    @TableField("place_count")
    @ApiModelProperty(value = "处数")
    private Integer placeCount;

    /** 拟判定的校核分值 */
    @TableField("check_score")
    @ApiModelProperty(value = "拟判定的校核分值")
    private Integer checkScore;

    /** 暂存形态 OPEN报入在档 OPENED已立件 REVOKED已撤销 */
    @TableField("state")
    @ApiModelProperty(value = "暂存形态 OPEN/OPENED/REVOKED")
    private String state;

    /** 立件后指向的办件 */
    @TableField("opened_case_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "立件后指向的办件")
    private Long openedCaseId;

    /** 报入幂等键（同键重放取首条） */
    @TableField("idem_key")
    @ApiModelProperty(value = "幂等键")
    private String idemKey;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIntakeNo() {
        return intakeNo;
    }

    public void setIntakeNo(String intakeNo) {
        this.intakeNo = intakeNo;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getOpenedCaseId() {
        return openedCaseId;
    }

    public void setOpenedCaseId(Long openedCaseId) {
        this.openedCaseId = openedCaseId;
    }

    public String getIdemKey() {
        return idemKey;
    }

    public void setIdemKey(String idemKey) {
        this.idemKey = idemKey;
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
}
