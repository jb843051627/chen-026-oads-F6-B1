package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TOadsGovCaseWitness;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 验收见证意见数据层（append-only；同一名见证岗本轮一笔，唯一索引兜底）。
 *
 * @author fuce
 * @date 2026-10-06
 */
public interface TOadsGovCaseWitnessMapper extends BaseMapper<TOadsGovCaseWitness> {

    /** 本件验收步本轮在档见证，按提交时刻排（两名各一笔，同刻两笔各记各的）。 */
    @Select("SELECT * FROM t_oads_gov_case_witness "
            + "WHERE case_id = #{caseId} AND round_no = #{roundNo} AND active = 1 "
            + "ORDER BY submit_at ASC, id ASC")
    List<TOadsGovCaseWitness> listActiveRound(@Param("caseId") Long caseId, @Param("roundNo") int roundNo);

    /** 验收步后退：本轮两条见证随老轮退出（行不删，继续可查）。 */
    @Update("UPDATE t_oads_gov_case_witness SET active = 0 "
            + "WHERE case_id = #{caseId} AND round_no = #{roundNo} AND active = 1")
    int deactivateRound(@Param("caseId") Long caseId, @Param("roundNo") int roundNo);
}
