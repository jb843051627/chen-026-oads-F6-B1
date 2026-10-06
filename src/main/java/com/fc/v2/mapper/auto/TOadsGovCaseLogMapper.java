package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TOadsGovCaseLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/**
 * 监管办件逐笔留痕数据层（append-only：只 insert，不改正文，后退只抹 active）。
 *
 * @author fuce
 * @date 2026-10-06
 */
public interface TOadsGovCaseLogMapper extends BaseMapper<TOadsGovCaseLog> {

    /**
     * 这一件全部留痕（含后退转追溯的老轮、撤回/划归/作废），按提交时刻、笔序排。
     * 追溯与步次重数都取这一串，不另取一套算法。
     */
    @Select("SELECT * FROM t_oads_gov_case_log WHERE case_id = #{caseId} "
            + "ORDER BY acted_at ASC, seq_no ASC, id ASC")
    List<TOadsGovCaseLog> listByCase(@Param("caseId") Long caseId);

    /** 本件本步本轮仍在档的走到笔（同一步二次送意见由裁决处先拦，不覆盖不追加）。 */
    @Select("SELECT * FROM t_oads_gov_case_log "
            + "WHERE case_id = #{caseId} AND step_no = #{stepNo} AND round_no = #{roundNo} AND active = 1 "
            + "ORDER BY acted_at ASC, seq_no ASC")
    List<TOadsGovCaseLog> listActiveStepRound(@Param("caseId") Long caseId,
                                              @Param("stepNo") int stepNo,
                                              @Param("roundNo") int roundNo);

    /**
     * 后退一步：只把这一件这一步当轮在档行退出（active 置 0，行不删，按原先后留作追溯）；
     * 其余各步与这一步老轮一条不动。
     */
    @Update("UPDATE t_oads_gov_case_log SET active = 0 "
            + "WHERE case_id = #{caseId} AND step_no = #{stepNo} AND round_no = #{roundNo} AND active = 1")
    int deactivateStepRound(@Param("caseId") Long caseId,
                            @Param("stepNo") int stepNo,
                            @Param("roundNo") int roundNo);

    /** 动作幂等重放：同键先落那一行，重放不添第二行。 */
    @Select("SELECT * FROM t_oads_gov_case_log WHERE idem_key = #{idemKey} LIMIT 1")
    TOadsGovCaseLog findByIdemKey(@Param("idemKey") String idemKey);

    /** 当日受理流水：当日全部落笔（含后退转追溯的老轮），按提交时刻、笔序排，逐笔对得上。 */
    @Select("SELECT * FROM t_oads_gov_case_log "
            + "WHERE acted_at >= #{start} AND acted_at < #{end} "
            + "ORDER BY acted_at ASC, seq_no ASC, id ASC")
    List<TOadsGovCaseLog> listLandedBetween(@Param("start") Date start, @Param("end") Date end);
}
