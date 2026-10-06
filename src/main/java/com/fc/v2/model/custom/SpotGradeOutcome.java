package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 一处点位这一趟的判定结果（批量逐条回显）。
 *
 * @author fuce
 * @date 2026-10-05
 */
@ApiModel(value = "SpotGradeOutcome", description = "点位逐条分档结果")
public class SpotGradeOutcome {

    @ApiModelProperty(value = "点位主键")
    private Long spotId;

    @ApiModelProperty(value = "点位码")
    private String spotNo;

    @ApiModelProperty(value = "分档结论（档位/采信谁/说法）")
    private SafetyGradeResult grade;

    public SpotGradeOutcome() {
    }

    public SpotGradeOutcome(Long spotId, String spotNo, SafetyGradeResult grade) {
        this.spotId = spotId;
        this.spotNo = spotNo;
        this.grade = grade;
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

    public void setSpotNo(String spotNo)
    {
        this.spotNo = spotNo;
    }

    public SafetyGradeResult getGrade() {
        return grade;
    }

    public void setGrade(SafetyGradeResult grade) {
        this.grade = grade;
    }
}
