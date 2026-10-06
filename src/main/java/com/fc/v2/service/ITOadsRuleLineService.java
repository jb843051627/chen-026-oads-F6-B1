package com.fc.v2.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TOadsRuleLine;
import com.fc.v2.model.custom.SafetyGradeResult;
import com.fc.v2.model.custom.SafetyRuleSnapshot;

/**
 * 牌面安全分档判据线 Service接口（rule-eval 形状：规则求值，无增删改）。
 *
 * <p>本接口是"本轮取哪一条规则、没有可用规则时给不给说法、多条并列采信谁、
 * 折算分压在上限落哪一档"四问的**唯一出处**。区级只做库表配置，不改写逻辑。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITOadsRuleLineService {

    /** 按主键查询规则：撤下的也是在册一行，按主键仍查得到。 */
    TOadsRuleLine selectTOadsRuleLineById(Long id);

    /**
     * 【原有入口，形状不动】按判据线代号取值：按 ruleCode 取 at 时刻生效的那一版贴区间，
     * 收什么回什么。返回档位 1..4；无生效版本 / 输入越界 / 参数缺失 / 脏配置一律 0。
     * 今后要按路段、按季度取规则，另起新方法承接，不改本方法模样。
     */
    int evaluate(String ruleCode, BigDecimal input, Date at);

    /**
     * 按判据线代号取值的完整结论（与 {@link #evaluate} 同一次计算）：
     * 不分档时判据线代号与起用那一一日一并留空。
     */
    SafetyGradeResult gradeByCode(String ruleCode, BigDecimal input, Date at);

    /**
     * 整表取优：at 时刻可用规则中先后序最大者说话（并列再比代号字面序）。
     * 与 {@link #gradeByCode} 同一套口径，同一处点位两问必须同一档。
     */
    SafetyGradeResult gradeTop(BigDecimal input, Date at);

    /**
     * 整表一次计算快照：可用清单、清单上方条数、逐条判定都出自这一个对象，
     * 不许各起一轮。脏配置剔除时点名叫格（缺的是哪一格、哪一格没大于前一格）。
     */
    SafetyRuleSnapshot snapshot(Date at);

    /**
     * 在**已算出的同一份快照**上判一处：ruleCode 非空走按标识取值（取不到生效版本/脏配置
     * 有说法），空着走整表取优。看板逐条判定与清单、条数必须用这同一个快照对象。
     */
    SafetyGradeResult grade(SafetyRuleSnapshot snap, String ruleCode, BigDecimal input);

    /** at 时刻可用规则（先后序降序、代号字面序降序）；与快照同源。 */
    List<TOadsRuleLine> listAvailable(Date at);

    /** 单条规则在 at 时刻是否可用（旁路复用，口径与定位/列表一致）。 */
    boolean usable(Long id, Date at);

    /** at 时刻可用规则条数（= 可用清单长度，同一次计算）。 */
    int countAvailable(Date at);
}
