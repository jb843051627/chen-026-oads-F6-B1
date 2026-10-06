package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TOadsGovCase;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 监管办件主档数据层。
 *
 * <p>步次以留痕重数为准，本处只做行锁、幂等重放与"未走完数量"一次查询；
 * 页首数字直接取 {@link #countRunning()}，不取前端缓存，也不在别处另算一套。
 *
 * @author fuce
 * @date 2026-10-06
 */
public interface TOadsGovCaseMapper extends BaseMapper<TOadsGovCase> {

    /** 立件/动作专用：行级锁住办件那一行，同批并发两笔逐笔串行。 */
    @Select("SELECT * FROM t_oads_gov_case WHERE id = #{id} AND del_flag = 0 FOR UPDATE")
    TOadsGovCase selectByIdForUpdate(@Param("id") Long id);

    /** 幂等重放：同键先落那一条，重放只回首条、不立第二条。 */
    @Select("SELECT * FROM t_oads_gov_case WHERE idem_key = #{idemKey} AND del_flag = 0 LIMIT 1")
    TOadsGovCase selectByIdemKey(@Param("idemKey") String idemKey);

    /** 同一处点位尚未走完（在走）的办件；撤回行不进主档、封存/划归/作废都不再占位置。 */
    @Select("SELECT * FROM t_oads_gov_case WHERE spot_no = #{spotNo} AND outcome = 'RUNNING' AND del_flag = 0")
    List<TOadsGovCase> listRunningBySpot(@Param("spotNo") String spotNo);

    /** 未走完数量：立一件加一、收尾一件减一；页首与台账同这一次查询。 */
    @Select("SELECT COUNT(1) FROM t_oads_gov_case WHERE outcome = 'RUNNING' AND del_flag = 0")
    long countRunning();

    /** 台账：封存/撤回/划归/作废照常出现，只是页面不给操作入口。 */
    @Select("SELECT * FROM t_oads_gov_case WHERE del_flag = 0 ORDER BY create_time ASC, id ASC")
    List<TOadsGovCase> selectAllNormal();
}
