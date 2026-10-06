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
 * 点位在走步序留痕对象 t_oads_spot_flow_log（append-only）
 *
 * <p>这一行停在哪一步由系统按本表逐行重数，屏上填的档、经办人记的档都不算数。
 * 退回只把在走那一步当轮的 active 置 0，行不删；重走另起一轮，上一轮文本保留、不与后补并在一起。
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_oads_spot_flow_log")
@ApiModel(value = "TOadsSpotFlowLog", description = "点位在走步序留痕")
public class TOadsSpotFlowLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 哪一处点位行 */
    @TableField("spot_id")
    @ApiModelProperty(value = "哪一处点位行")
    private Long spotId;

    /** 点位码（落账当时冗余） */
    @TableField("spot_no")
    @ApiModelProperty(value = "点位码")
    private String spotNo;

    /** 步序 1待核 2已核 3压卡 */
    @TableField("step_no")
    @ApiModelProperty(value = "步序 1待核 2已核 3压卡")
    private Integer stepNo;

    /** 该步轮次 退一步加一轮 老轮留在本表 active=0 */
    @TableField("round_no")
    @ApiModelProperty(value = "该步轮次")
    private Integer roundNo;

    /** 动作 REACH走到 RETURN退回 HOLD就地停住 */
    @TableField("action")
    @ApiModelProperty(value = "动作 REACH/RETURN/HOLD")
    private String action;

    /** 这一步当回记下的东西（原文，退回不抹） */
    @TableField("note")
    @ApiModelProperty(value = "这一步当回记下的东西")
    private String note;

    /** 经办岗（只记是谁，档不由他点） */
    @TableField("operator")
    @ApiModelProperty(value = "经办岗")
    private String operator;

    /** 本轮是否在档 1当前轮 0随退回退出（不删行，继续可查） */
    @TableField("active")
    @ApiModelProperty(value = "本轮是否在档")
    private Integer active;

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

    public Integer getStepNo() {
        return stepNo;
    }

    public void setStepNo(Integer stepNo) {
        this.stepNo = stepNo;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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

    public Integer getActive() {
        return active;
    }

    public void setActive(Integer active) {
        this.active = active;
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
