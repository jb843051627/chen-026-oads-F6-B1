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
 * 监管办件验收见证意见对象 t_oads_gov_case_witness
 *
 * <p>验收需两名见证岗各提交一次：同一名见证岗第二笔保留首次、不覆盖不追加
 * （uk_gov_witness_once 兜底，裁决处先把话讲明）；复验凭证编码两名齐备后同次回写主档。
 *
 * @author fuce
 * @date 2026-10-06
 */
@TableName("t_oads_gov_case_witness")
@ApiModel(value = "TOadsGovCaseWitness", description = "监管办件验收见证意见")
public class TOadsGovCaseWitness implements Serializable {
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

    /** 验收步轮次（退回重走加一轮，老轮行保留） */
    @TableField("round_no")
    @ApiModelProperty(value = "验收步轮次")
    private Integer roundNo;

    /** 见证岗代号 */
    @TableField("witness_post")
    @ApiModelProperty(value = "见证岗代号")
    private String witnessPost;

    /** 见证意见原文 */
    @TableField("opinion")
    @ApiModelProperty(value = "见证意见原文")
    private String opinion;

    /** 该笔随附复验凭证编码（两名齐备后取一回写主档） */
    @TableField("cert_no")
    @ApiModelProperty(value = "随附复验凭证编码")
    private String certNo;

    /** 提交时刻（毫秒，同刻两笔各记各的） */
    @TableField("submit_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    @ApiModelProperty(value = "提交时刻")
    private Date submitAt;

    /** 本轮是否在档 1当前轮 0验收步退回后随老轮退出 */
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

    public String getWitnessPost() {
        return witnessPost;
    }

    public void setWitnessPost(String witnessPost) {
        this.witnessPost = witnessPost;
    }

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }

    public String getCertNo() {
        return certNo;
    }

    public void setCertNo(String certNo) {
        this.certNo = certNo;
    }

    public Date getSubmitAt() {
        return submitAt;
    }

    public void setSubmitAt(Date submitAt) {
        this.submitAt = submitAt;
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
