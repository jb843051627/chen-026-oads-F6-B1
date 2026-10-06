package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TOadsSpotSyncCursor;

/**
 * 市里增量同步游标数据层（全库唯一一行，只许同步那一处推进）
 *
 * @author fuce
 * @date 2026-10-05
 */
public interface TOadsSpotSyncCursorMapper extends BaseMapper<TOadsSpotSyncCursor> {
}
