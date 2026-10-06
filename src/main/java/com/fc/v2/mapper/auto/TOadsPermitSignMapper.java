package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TOadsPermitSign;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/**
 * 核准单逐段签署留痕数据层（append-only，没有物理删除）
 *
 * @author fuce
 * @date 2026-10-05
 */
public interface TOadsPermitSignMapper extends BaseMapper<TOadsPermitSign> {

    /** 本单本段本轮仍在档的签署，按各自落单时刻、笔序排（同秒各记各的，不并笔） */
    @Select("SELECT * FROM t_oads_permit_sign "
            + "WHERE bill_id = #{billId} AND node_no = #{nodeNo} AND round_no = #{roundNo} AND active = 1 "
            + "ORDER BY sign_time ASC, seq_no ASC")
    List<TOadsPermitSign> listActiveRound(@Param("billId") Long billId,
                                          @Param("nodeNo") int nodeNo,
                                          @Param("roundNo") int roundNo);

    /**
     * 清一段：只把本单本段本轮在档签署退出（active 置 0，行不删）；
     * 此前各段与本段老轮一条不动。
     */
    @Update("UPDATE t_oads_permit_sign SET active = 0 "
            + "WHERE bill_id = #{billId} AND node_no = #{nodeNo} AND round_no = #{roundNo} AND active = 1")
    int deactivateRound(@Param("billId") Long billId,
                        @Param("nodeNo") int nodeNo,
                        @Param("roundNo") int roundNo);

    /** 整单全部留痕（含被清退出的老轮），按段、轮、笔序排 */
    @Select("SELECT * FROM t_oads_permit_sign WHERE bill_id = #{billId} "
            + "ORDER BY node_no ASC, round_no ASC, seq_no ASC")
    List<TOadsPermitSign> listByBill(@Param("billId") Long billId);

    /** 全部在档签署，供只凭留痕的逐段重算 */
    @Select("SELECT * FROM t_oads_permit_sign WHERE active = 1")
    List<TOadsPermitSign> listAllActive();

    /** 当日受理登记簿：当日全部落笔（含被清退出的老轮），按落单时刻、笔序排 */
    @Select("SELECT * FROM t_oads_permit_sign "
            + "WHERE sign_time >= #{start} AND sign_time < #{end} "
            + "ORDER BY sign_time ASC, seq_no ASC")
    List<TOadsPermitSign> listLandedBetween(@Param("start") Date start, @Param("end") Date end);
}
