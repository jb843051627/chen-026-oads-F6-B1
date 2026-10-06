package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TOadsGovCaseReceipt;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 设置步核收登记数据层。
 *
 * @author fuce
 * @date 2026-10-06
 */
public interface TOadsGovCaseReceiptMapper extends BaseMapper<TOadsGovCaseReceipt> {

    /** 本件设置步本轮在档核收（agree=1 才算记为同意，缺登记不得开始验收）。 */
    @Select("SELECT * FROM t_oads_gov_case_receipt "
            + "WHERE case_id = #{caseId} AND round_no = #{roundNo} AND active = 1 LIMIT 1")
    TOadsGovCaseReceipt findActiveRound(@Param("caseId") Long caseId, @Param("roundNo") int roundNo);

    /** 设置步后退：本轮核收随老轮退出（行不删，继续可查）。 */
    @Update("UPDATE t_oads_gov_case_receipt SET active = 0 "
            + "WHERE case_id = #{caseId} AND round_no = #{roundNo} AND active = 1")
    int deactivateRound(@Param("caseId") Long caseId, @Param("roundNo") int roundNo);
}
