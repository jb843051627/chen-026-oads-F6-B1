package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 悬空点位：页面上挂着判据线代号，回到本次询问那一刻却取不到生效版本
 * （已撤下／未到起用日／已交班／代号从不存在／那一条是脏配置）。逐条列出。
 *
 * @author fuce
 * @date 2026-10-05
 */
@ApiModel(value = "SpotDangling", description = "挂了代号却取不到生效版本的点位")
public class SpotDangling {

    @ApiModelProperty(value = "点位主键")
    private Long spotId;

    @ApiModelProperty(value = "点位码")
    private String spotNo;

    @ApiModelProperty(value = "挂着却取不到的判据线代号")
    private String ruleCode;

    @ApiModelProperty(value = "取不到的说法")
    private String reason;

    public SpotDangling() {
    }

    public SpotDangling(Long spotId, String spotNo, String ruleCode, String reason) {
        this.spotId = spotId;
        this.spotNo = spotNo;
        this.ruleCode = ruleCode;
        this.reason = reason;
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

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
