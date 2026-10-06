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
 * 监管办件逐笔留痕对象 t_oads_gov_case_log（append-only）
 *
 * <p>每一步那句意见连同提交时刻，按时序压在办件后面，日后追溯只取这一串。
 * 停在哪一步由系统按本表逐行重数，报文里的步次不作数。
 * 后退只把被退那一步当轮 active 置 0（行不删，按原先后留作追溯）；重走另起一轮重新累计。
 *
 * @author fuce
 * @date 2026-10-06
 */
@TableName("t_oads_gov_case_log")
@ApiModel(value = "TOadsGovCaseLog", description = "监管办件逐笔留痕")
public class TOadsGovCaseLog implements Serializable {
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

    /** 点位码（落账当时冗余） */
    @TableField("spot_no")
    @ApiModelProperty(value = "点位码")
    private String spotNo;

    /** 步次 1申报 2核准 3设置 4验收；撤回/划归/作废记发生时所在步 */
    @TableField("step_no")
    @ApiModelProperty(value = "步次")
    private Integer stepNo;

    /** 该步轮次 退一次加一轮 老轮文本留本表 active=0 */
    @TableField("round_no")
    @ApiModelProperty(value = "该步轮次")
    private Integer roundNo;

    /** 本件本步本轮第几笔（逐笔累加，同刻两通道各一笔也不并） */
    @TableField("seq_no")
    @ApiModelProperty(value = "本轮笔序")
    private Integer seqNo;

    /** 动作 REACH走到 RETURN退回 WITHDRAW撤回 REALIGN划归邻段 VOID作废 */
    @TableField("action")
    @ApiModelProperty(value = "动作 REACH/RETURN/WITHDRAW/REALIGN/VOID")
    private String action;

    /** 这一步当回那句意见（原文，退回不抹） */
    @TableField("note")
    @ApiModelProperty(value = "意见原文")
    private String note;

    /** 值守岗（只记是谁，步次不由他点） */
    @TableField("operator")
    @ApiModelProperty(value = "值守岗")
    private String operator;

    /** 送意见的通道（两通道同刻各送各的，逐笔对流水） */
    @TableField("channel")
    @ApiModelProperty(value = "通道")
    private String channel;

    /** 动作幂等键：同键重放不添第二行（同批并发两回只落一笔） */
    @TableField("idem_key")
    @ApiModelProperty(value = "动作幂等键")
    private String idemKey;

    /** 本轮是否在档 1当前轮 0随退回转入追溯（不删行，继续可查） */
    @TableField("active")
    @ApiModelProperty(value = "本轮是否在档")
    private Integer active;

    /** 提交时刻（毫秒） */
    @TableField("acted_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    @ApiModelProperty(value = "提交时刻")
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

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
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

    public Integer getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(Integer seqNo) {
        this.seqNo = seqNo;
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

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getIdemKey() {
        return idemKey;
    }

    public void setIdemKey(String idemKey) {
        this.idemKey = idemKey;
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
