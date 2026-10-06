package com.fc.v2.model.custom;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fc.v2.model.auto.TOadsRuleLine;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 一次分档询问的结论。取值规则、给不给说法、并列采信谁、压线落哪档，
 * 全部由服务端那一处区间比对给出，页面不再算第二遍。
 *
 * <p>不分档时 level=0，ruleCode 与 effStart 一并留空（回写也留空），
 * 不许借邻近规则的数值凑答复。
 *
 * @author fuce
 * @date 2026-10-05
 */
@ApiModel(value = "SafetyGradeResult", description = "牌面安全分档结论")
public class SafetyGradeResult {

    @ApiModelProperty(value = "档位 0不分档 1..4")
    private int level;

    @ApiModelProperty(value = "本轮说话的判据线代号（不分档留空）")
    private String ruleCode;

    @ApiModelProperty(value = "本轮说话那一条的起用那一一日（不分档留空，固化时逐字照抄这一个）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date effStart;

    @ApiModelProperty(value = "规则主键")
    private Long ruleId;

    @ApiModelProperty(value = "线名（仅回显，不参与固化）")
    private String ruleName;

    @ApiModelProperty(value = "取齐两位小数后的折算分")
    private BigDecimal alignedScore;

    @ApiModelProperty(value = "结论说法：没有可用规则/越界/采信哪一条，都由这一处给")
    private String message;

    public static SafetyGradeResult none(BigDecimal alignedScore, String message) {
        SafetyGradeResult r = new SafetyGradeResult();
        r.level = 0;
        r.alignedScore = alignedScore;
        r.message = message;
        return r;
    }

    public static SafetyGradeResult graded(TOadsRuleLine rule, int level,
                                           BigDecimal alignedScore, String message) {
        SafetyGradeResult r = new SafetyGradeResult();
        r.level = level;
        r.ruleId = rule.getId();
        r.ruleCode = rule.getRuleCode();
        r.effStart = rule.getEffStart();
        r.ruleName = rule.getRuleName();
        r.alignedScore = alignedScore;
        r.message = message;
        return r;
    }

    public boolean isGraded() {
        return level > 0;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public Date getEffStart() {
        return effStart;
    }

    public void setEffStart(Date effStart) {
        this.effStart = effStart;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public BigDecimal getAlignedScore() {
        return alignedScore;
    }

    public void setAlignedScore(BigDecimal alignedScore) {
        this.alignedScore = alignedScore;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
