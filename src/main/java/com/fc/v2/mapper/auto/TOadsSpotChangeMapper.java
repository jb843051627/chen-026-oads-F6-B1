package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TOadsSpotChange;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 点位主档变更履历数据层（append-only）
 *
 * @author fuce
 * @date 2026-10-05
 */
public interface TOadsSpotChangeMapper extends BaseMapper<TOadsSpotChange> {

    /** 某点位的变更履历，按发生时刻倒序，一段往回看 */
    @Select("SELECT * FROM t_oads_spot_change WHERE spot_id = #{spotId} "
            + "ORDER BY change_time DESC, id DESC")
    List<TOadsSpotChange> listBySpot(@Param("spotId") Long spotId);
}
