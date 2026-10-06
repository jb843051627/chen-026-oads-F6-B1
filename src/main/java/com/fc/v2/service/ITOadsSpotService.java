package com.fc.v2.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TOadsSpot;
import com.fc.v2.model.custom.SafetyGradeResult;
import com.fc.v2.model.custom.SpotDangling;
import com.fc.v2.model.custom.SpotGradeQuery;
import com.fc.v2.model.custom.SpotGradeOutcome;

/**
 * 点位底册 Service接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITOadsSpotService {

    /** 按主键查询 */
    TOadsSpot selectTOadsSpotById(Long id);

    /** 按条件查询列表（分页由调用方统一处理） */
    List<TOadsSpot> selectTOadsSpotList(Wrapper<TOadsSpot> queryWrapper);

    /** 新增 */
    int insertTOadsSpot(TOadsSpot record);

    /** 修改 */
    int updateTOadsSpot(TOadsSpot record);

    /** 批量删除 */
    int deleteTOadsSpotByIds(String ids);

    /** 按主键删除 */
    int deleteTOadsSpotById(Long id);

    /**
     * 只判不固化：一处点位在 at 时刻的分档结论。点位挂着代号走按标识取值，
     * 空着走整表取优——两入口同一套口径。
     */
    SafetyGradeResult gradeSpot(Long spotId, BigDecimal score, Date at);

    /**
     * 判了就固化（一个事务）：同一处区间比对给出档位后，把档位、判据线代号、起用那一一日
     * 写回点位行，并追加一行留痕；两步同成同回。不分档时代号与起用日一并留空。
     * 固化进点位行的那一对与留痕、与当次结论逐字相同，不取邻近规则、不取上一季度。
     */
    SafetyGradeResult gradeAndFix(Long spotId, BigDecimal score, Date at);

    /**
     * 一批点位同一次计算：一份快照判全部，逐条回档；清单/条数/逐条同一套口径。
     */
    List<SpotGradeOutcome> gradeBatch(List<SpotGradeQuery> queries, Date at);

    /**
     * 悬空清单：挂着判据线代号、回到 at 那一刻却取不到生效版本的点位，逐条列出。
     */
    List<SpotDangling> listDangling(Date at);
}
