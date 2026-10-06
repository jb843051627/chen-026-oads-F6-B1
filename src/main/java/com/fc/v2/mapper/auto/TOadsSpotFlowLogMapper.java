package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TOadsSpotFlowLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 点位在走步序留痕数据层（append-only：只 insert，不 update 内容，退回只抹 active）
 *
 * @author fuce
 * @date 2026-10-05
 */
public interface TOadsSpotFlowLogMapper extends BaseMapper<TOadsSpotFlowLog> {

    /** 这一行全部留痕，按步序、轮次、时刻排，供系统重数停在哪一步。 */
    @Select("SELECT * FROM t_oads_spot_flow_log WHERE spot_id = #{spotId} ORDER BY step_no, round_no, acted_at, id")
    List<TOadsSpotFlowLog> listBySpot(@Param("spotId") Long spotId);

    /** 退回：只把这一行这一步当轮在档记录退出（active 置 0，行不删，老轮继续可查）。 */
    @Update("UPDATE t_oads_spot_flow_log SET active = 0 WHERE spot_id = #{spotId} AND step_no = #{stepNo} AND round_no = #{roundNo} AND active = 1")
    int deactivateRound(@Param("spotId") Long spotId, @Param("stepNo") int stepNo, @Param("roundNo") int roundNo);
}
