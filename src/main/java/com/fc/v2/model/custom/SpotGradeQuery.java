package com.fc.v2.model.custom;

import java.math.BigDecimal;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 一处点位的分档询问：点位 + 该点位按市局公式折算出来的那枚折算分。
 *
 * @author fuce
 * @date 2026-10-05
 */
@ApiModel(value = "SpotGradeQuery", description = "点位分档询问")
public class SpotGradeQuery {

    @ApiModelProperty(value = "点位主键")
    private Long spotId;

    @ApiModelProperty(value = "折算分（版面尺寸/材质/安装高度按市局公式折算，量程0..100）")
    private BigDecimal score;

    public SpotGradeQuery() {
    }

    public SpotGradeQuery(Long spotId, BigDecimal score) {
        this.spotId = spotId;
        this.score = score;
    }

    public Long getSpotId() {
        return spotId;
    }

    public void setSpotId(Long spotId) {
        this.spotId = spotId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }
}
