package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TOadsGovCaseIntake;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 立件前报入暂存数据层。
 *
 * @author fuce
 * @date 2026-10-06
 */
public interface TOadsGovCaseIntakeMapper extends BaseMapper<TOadsGovCaseIntake> {

    /** 报入幂等重放：同键先落那一条（含已立件/已撤销），重放不造第二条。 */
    @Select("SELECT * FROM t_oads_gov_case_intake WHERE idem_key = #{idemKey} LIMIT 1")
    TOadsGovCaseIntake selectByIdemKey(@Param("idemKey") String idemKey);

    /** 按主键取（撤销/立件用）。 */
    @Select("SELECT * FROM t_oads_gov_case_intake WHERE id = #{id}")
    TOadsGovCaseIntake selectRowById(@Param("id") Long id);
}
