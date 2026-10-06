package com.fc.v2.model.auto;

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
 * 市里增量同步游标对象 t_oads_spot_sync_cursor（全库唯一一行 cfg_key='city'）
 *
 * <p>比对起点全库唯一：游标之前的行不复核，游标之后才参与本次比较。
 * 游标随本次同步一并推进，且与本次结果出自同一份计算；别处不得自行调整。
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_oads_spot_sync_cursor")
@ApiModel(value = "TOadsSpotSyncCursor", description = "市里增量同步游标")
public class TOadsSpotSyncCursor implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 配置键（city=市里增量，单键单行） */
    @TableField("cfg_key")
    @ApiModelProperty(value = "配置键")
    private String cfgKey;

    /** 本次起点：点位主键小于等于此值的行不复核 */
    @TableField("cursor_id")
    @ApiModelProperty(value = "游标点位主键")
    private Long cursorId;

    /** 最近一次同步时刻 */
    @TableField("synced_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    @ApiModelProperty(value = "最近一次同步时刻")
    private Date syncedAt;

    /** 最近一次同步交代 */
    @TableField("conflict_note")
    @ApiModelProperty(value = "最近一次同步交代")
    private String conflictNote;

    @TableField("del_flag")
    private Integer delFlag;

    @TableField("update_by")
    private String updateBy;

    @TableField("update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCfgKey() {
        return cfgKey;
    }

    public void setCfgKey(String cfgKey) {
        this.cfgKey = cfgKey;
    }

    public Long getCursorId() {
        return cursorId;
    }

    public void setCursorId(Long cursorId) {
        this.cursorId = cursorId;
    }

    public Date getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(Date syncedAt) {
        this.syncedAt = syncedAt;
    }

    public String getConflictNote() {
        return conflictNote;
    }

    public void setConflictNote(String conflictNote) {
        this.conflictNote = conflictNote;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
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
}
