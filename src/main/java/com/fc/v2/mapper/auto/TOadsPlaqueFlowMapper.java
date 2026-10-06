package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TOadsPlaqueFlow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 牌匾更换名册收讫流水数据层（append-only：只 insert）。
 *
 * @author fuce
 * @date 2026-10-05
 */
public interface TOadsPlaqueFlowMapper extends BaseMapper<TOadsPlaqueFlow> {

    /** 这一册全部流水，按落账时刻、主键排；对账以最早收讫那笔 RECEIVE 的快照为准逐笔核对。 */
    @Select("SELECT * FROM t_oads_plaque_flow WHERE batch_no = #{batchNo} ORDER BY acted_at, id")
    List<TOadsPlaqueFlow> listByBatch(@Param("batchNo") String batchNo);
}
