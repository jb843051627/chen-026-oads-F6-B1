package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TOadsGovFlow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 点位监管办件流转单数据层
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface TOadsGovFlowMapper extends BaseMapper<TOadsGovFlow> {

    /** 挪关/改记/撤单专用：行级锁住那一行（只锁在册行），并发与回滚叠加也按锁内重读的关次走。 */
    @Select("SELECT * FROM t_oads_gov_flow WHERE id = #{id} AND del_flag = 0 FOR UPDATE")
    TOadsGovFlow selectRowByIdForUpdate(@Param("id") Long id);

}
