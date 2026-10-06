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
 * 牌面安全分档固化留痕对象 t_oads_spot_grade_log（append-only，一次判定一行）
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_oads_spot_grade_log")
@ApiModel(value = "TOadsSpotGradeLog", description = "牌面安全分档固化留痕")
public class TOadsSpotGradeLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 哪一处点位 */
    @TableField("spot_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "哪一处点位")
    private Long spotId;

    /** 点位码（判定当时冗余） */
    @TableField("spot_no")
    @ApiModelProperty(value = "点位码（判定当时冗余）")
    private String spotNo;

    /** 本次询问使用的折算分（原始取值，0..100） */
    @TableField("score")
    @ApiModelProperty(value = "本次询问使用的折算分（原始取值）")
    private BigDecimal score;

    /** 两位小数四舍五入取齐后的折算分 */
    @TableField("score_aligned")
    @ApiModelProperty(value = "取齐两位小数后的折算分")
    private BigDecimal scoreAligned;

    /** 落定档位 0不分档 1..4 */
    @TableField("safe_level")
    @ApiModelProperty(value = "落定档位 0不分档 1..4")
    private Integer safeLevel;

    /** 采信那一条的判据线代号（不分档留空） */
    @TableField("rule_code")
    @ApiModelProperty(value = "采信那一条的判据线代号（不分档留空）")
    private String ruleCode;

    /** 采信那一条的主键（不分档留空） */
    @TableField("rule_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "采信那一条的主键（不分档留空）")
    private Long ruleId;

    /** 采信那一条的起用那一一日（不分档留空，逐字照抄） */
    @TableField("eff_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "采信那一条的起用那一一日（不分档留空）")
    private Date effStart;

    /** 本次询问时刻（按提交当时生效版本判，不按此刻最新版） */
    @TableField("ask_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "本次询问时刻")
    private Date askAt;

    /** 这一处给的说法（采信谁/为何不分档） */
    @TableField("grade_msg")
    @ApiModelProperty(value = "这一处给的说法")
    private String gradeMsg;

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

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public BigDecimal getScoreAligned() {
        return scoreAligned;
    }

    public void setScoreAligned(BigDecimal scoreAligned) {
        this.scoreAligned = scoreAligned;
    }

    public Integer getSafeLevel() {
        return safeLevel;
    }

    public void setSafeLevel(Integer safeLevel) {
        this.safeLevel = safeLevel;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public Date getEffStart() {
        return effStart;
    }

    public void setEffStart(Date effStart) {
        this.effStart = effStart;
    }

    public Date getAskAt() {
        return askAt;
    }

    public void setAskAt(Date askAt) {
        this.askAt = askAt;
    }

    public String getGradeMsg() {
        return gradeMsg;
    }

    public void setGradeMsg(String gradeMsg) {
        this.gradeMsg = gradeMsg;
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
