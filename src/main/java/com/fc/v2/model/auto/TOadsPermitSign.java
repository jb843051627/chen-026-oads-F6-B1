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
 * 核准单逐段签署留痕对象 t_oads_permit_sign
 *
 * <p>一次签署一行，只许追加不许改。清段只把 active 抹成 0，老轮文本继续可查。
 * 段次(node_no)是服务端按单据当前段点出后落库的，报文自称第几段不作数。
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_oads_permit_sign")
@ApiModel(value = "TOadsPermitSign", description = "核准单逐段签署留痕")
public class TOadsPermitSign implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 哪张核准单 */
    @TableField("bill_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "核准单主键")
    private Long billId;

    /** 落单时服务端点出的段 0窗口受理 1区局核准 2市局备案 */
    @TableField("node_no")
    @ApiModelProperty(value = "落单段（服务端点出）")
    private Integer nodeNo;

    /** 本段轮次 清一段加一轮 本轮文本与上轮分开存放 */
    @TableField("round_no")
    @ApiModelProperty(value = "本段轮次")
    private Integer roundNo;

    /** 本单本段本轮第几笔（逐笔累加，同秒各记各的也不并笔） */
    @TableField("seq_no")
    @ApiModelProperty(value = "本轮笔序")
    private Integer seqNo;

    /** 所在岗（服务端按段点出） */
    @TableField("sign_post")
    @ApiModelProperty(value = "所在岗")
    private String signPost;

    /** 签署人 */
    @TableField("signer")
    @ApiModelProperty(value = "签署人")
    private String signer;

    /** 判断 1同意 2不同意 */
    @TableField("opinion")
    @ApiModelProperty(value = "判断 1同意 2不同意")
    private Integer opinion;

    /** 签署文本，原文入库留痕 */
    @TableField("content")
    @ApiModelProperty(value = "签署文本")
    private String content;

    /** 各自落单时刻（毫秒计时，同一秒两笔仍为两条） */
    @TableField("sign_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    @ApiModelProperty(value = "落单时刻")
    private Date signTime;

    /** 本轮是否在档 1当前轮 0随本段清轮退出（不删行，继续可查） */
    @TableField("active")
    @ApiModelProperty(value = "是否当前轮在档")
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

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
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

    public String getSignPost() {
        return signPost;
    }

    public void setSignPost(String signPost) {
        this.signPost = signPost;
    }

    public String getSigner() {
        return signer;
    }

    public void setSigner(String signer) {
        this.signer = signer;
    }

    public Integer getOpinion() {
        return opinion;
    }

    public void setOpinion(Integer opinion) {
        this.opinion = opinion;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getSignTime() {
        return signTime;
    }

    public void setSignTime(Date signTime) {
        this.signTime = signTime;
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
