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
 * 点位底册对象 t_oads_spot
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_oads_spot")
@ApiModel(value = "TOadsSpot", description = "点位底册")
public class TOadsSpot implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 点位码 */
    @TableField("spot_no")
    @ApiModelProperty(value = "点位码")
    private String spotNo;

    /** 挂在哪一段册名下 */
    @TableField("area_id")
    @ApiModelProperty(value = "挂在哪一段册名下")
    private Integer areaId;

    /** 段册代号（冗余自段册） */
    @TableField("area_no")
    @ApiModelProperty(value = "段册代号（冗余自段册）")
    private String areaNo;

    /** 经度（现场采集，前端不得人工改写） */
    @TableField("longitude")
    @ApiModelProperty(value = "经度（现场采集，前端不得人工改写）")
    private BigDecimal longitude;

    /** 纬度（现场采集，前端不得人工改写） */
    @TableField("latitude")
    @ApiModelProperty(value = "纬度（现场采集，前端不得人工改写）")
    private BigDecimal latitude;

    /** 辖段全称冗余（与目录有出入时以目录为准修正） */
    @TableField("area_name")
    @ApiModelProperty(value = "辖段全称冗余（以目录为准）")
    private String areaName;

    /** 建行当季季度码 如2026Q4 */
    @TableField("quarter_code")
    @ApiModelProperty(value = "建行当季季度码")
    private String quarterCode;

    /** 在走步序 1待核 2已核 3压卡；NULL=旧行一步未走（步序以留痕重数为准） */
    @TableField("flow_step")
    @ApiModelProperty(value = "在走步序 1待核 2已核 3压卡")
    private Integer flowStep;

    /** 建行通道 MANUAL建档岗逐条 BATCH集中更换带出 */
    @TableField("build_kind")
    @ApiModelProperty(value = "建行通道 MANUAL/BATCH")
    private String buildKind;

    /** 设置主体代号或承办岗代号 */
    @TableField("set_no")
    @ApiModelProperty(value = "设置主体代号或承办岗代号")
    private String setNo;

    /** 点位挂着的判据线代号（区级配置；按标识取值入口照它问，空着的只走整表取优） */
    @TableField("rule_code")
    @ApiModelProperty(value = "点位挂着的判据线代号（区级配置）")
    private String ruleCode;

    /** 核定应设处数 */
    @TableField("need_num")
    @ApiModelProperty(value = "核定应设处数")
    private Integer needNum;

    /** 已核验落地处数 */
    @TableField("got_num")
    @ApiModelProperty(value = "已核验落地处数")
    private Integer gotNum;

    /** 还差几处（轧出来，不手填） */
    @TableField("lack_num")
    @ApiModelProperty(value = "还差几处（轧出来，不手填）")
    private Integer lackNum;

    /** 随件交来的牌面题名与要件 */
    @TableField("content")
    @ApiModelProperty(value = "随件交来的牌面题名与要件")
    private String content;

    /** 挂牌情形档 0未挂设 1挂设在册 2整块拆除（只有核准单封存才许挪） */
    @TableField("set_state")
    @ApiModelProperty(value = "挂牌情形档 0未挂设 1挂设在册 2整块拆除")
    private Integer setState;

    /** 挂设档位 挂上去或改位置封存一次加一；拆除不进位 */
    @TableField("set_ver")
    @ApiModelProperty(value = "挂设档位")
    private Integer setVer;

    /** 牌面安全分档 0不分档 1..4（服务端一处区间比对给出，页面不再算第二遍） */
    @TableField("safe_level")
    @ApiModelProperty(value = "牌面安全分档 0不分档 1..4")
    private Integer safeLevel;

    /** 本轮固化的判据线代号（不分档留空，逐字照抄当次采信那一条） */
    @TableField("safe_rule_code")
    @ApiModelProperty(value = "本轮固化的判据线代号（不分档留空）")
    private String safeRuleCode;

    /** 本轮固化的起用那一一日（不分档留空，与代号同一对，逐字相同） */
    @TableField("safe_eff_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "本轮固化的起用那一一日（不分档留空）")
    private Date safeEffStart;

    /** 册面情形 0新入册 1已核齐 2缺项 */
    @TableField("status")
    @ApiModelProperty(value = "册面情形 0新入册 1已核齐 2缺项")
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

    public String getSpotNo() {
        return spotNo;
    }

    public void setSpotNo(String spotNo) {
        this.spotNo = spotNo;
    }

    public Integer getAreaId() {
        return areaId;
    }

    public void setAreaId(Integer areaId) {
        this.areaId = areaId;
    }

    public String getAreaNo() {
        return areaNo;
    }

    public void setAreaNo(String areaNo) {
        this.areaNo = areaNo;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getQuarterCode() {
        return quarterCode;
    }

    public void setQuarterCode(String quarterCode) {
        this.quarterCode = quarterCode;
    }

    public Integer getFlowStep() {
        return flowStep;
    }

    public void setFlowStep(Integer flowStep) {
        this.flowStep = flowStep;
    }

    public String getBuildKind() {
        return buildKind;
    }

    public void setBuildKind(String buildKind) {
        this.buildKind = buildKind;
    }

    public String getSetNo() {
        return setNo;
    }

    public void setSetNo(String setNo) {
        this.setNo = setNo;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public Integer getNeedNum() {
        return needNum;
    }

    public void setNeedNum(Integer needNum) {
        this.needNum = needNum;
    }

    public Integer getGotNum() {
        return gotNum;
    }

    public void setGotNum(Integer gotNum) {
        this.gotNum = gotNum;
    }

    public Integer getLackNum() {
        return lackNum;
    }

    public void setLackNum(Integer lackNum) {
        this.lackNum = lackNum;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSetState() {
        return setState;
    }

    public void setSetState(Integer setState) {
        this.setState = setState;
    }

    public Integer getSetVer() {
        return setVer;
    }

    public void setSetVer(Integer setVer) {
        this.setVer = setVer;
    }

    public Integer getSafeLevel() {
        return safeLevel;
    }

    public void setSafeLevel(Integer safeLevel) {
        this.safeLevel = safeLevel;
    }

    public String getSafeRuleCode() {
        return safeRuleCode;
    }

    public void setSafeRuleCode(String safeRuleCode) {
        this.safeRuleCode = safeRuleCode;
    }

    public Date getSafeEffStart() {
        return safeEffStart;
    }

    public void setSafeEffStart(Date safeEffStart) {
        this.safeEffStart = safeEffStart;
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
