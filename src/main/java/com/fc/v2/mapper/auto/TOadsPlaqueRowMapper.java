package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TOadsPlaqueRow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 牌匾更换名册行账数据层。
 *
 * <p>回执三个数一律回库逐行数，不采信册账汇总、更不采信页面那份；本接口给出的就是逐行清单，
 * 计数在唯一裁决处按这份清单算。
 *
 * @author fuce
 * @date 2026-10-05
 */
public interface TOadsPlaqueRowMapper extends BaseMapper<TOadsPlaqueRow> {

    /** 这一册全部行（含历史卷宗层），按原报行次排；三个回执数都从这份清单逐行数。 */
    @Select("SELECT * FROM t_oads_plaque_row WHERE book_id = #{bookId} AND del_flag = 0 ORDER BY row_no, id")
    List<TOadsPlaqueRow> listByBook(@Param("bookId") Long bookId);

    /** 这一册指定卷宗层的行（现行名单只数 layer=0；历史层名单看 layer=1）。 */
    @Select("SELECT * FROM t_oads_plaque_row WHERE book_id = #{bookId} AND layer = #{layer} AND del_flag = 0 ORDER BY row_no, id")
    List<TOadsPlaqueRow> listByBookLayer(@Param("bookId") Long bookId, @Param("layer") int layer);
}
