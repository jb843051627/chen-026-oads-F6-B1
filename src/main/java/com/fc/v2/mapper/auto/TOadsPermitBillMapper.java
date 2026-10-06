package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TOadsPermitBill;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 设置许可核准单数据层
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface TOadsPermitBillMapper extends BaseMapper<TOadsPermitBill> {

    /**
     * 签署处理专用：行级锁住单据那一行。
     * 两名签署人同一秒各自提交时，后一笔等前一笔提交后再读，保证逐笔串行记账、不并笔、不重判。
     */
    @Select("SELECT * FROM t_oads_permit_bill WHERE id = #{id} AND del_flag = 0 FOR UPDATE")
    TOadsPermitBill selectByIdForUpdate(@Param("id") Long id);

    /** 同一点位同一种变更在途只一张（live_token 唯一索引兜底，这里先把话讲明） */
    @Select("SELECT COUNT(1) FROM t_oads_permit_bill WHERE live_token = #{token} AND del_flag = 0")
    int countByLiveToken(@Param("token") String token);

    /** 未封存数量：建单加一、封存减一 */
    @Select("SELECT COUNT(1) FROM t_oads_permit_bill WHERE status <> 1 AND del_flag = 0")
    long countUnsealed();

    /** 对账：全部正常单据逐张取，与留痕重算同一次核对 */
    @Select("SELECT * FROM t_oads_permit_bill WHERE del_flag = 0")
    List<TOadsPermitBill> selectAllNormal();
}
