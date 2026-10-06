package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TOadsRuleLineMapper;
import com.fc.v2.model.auto.TOadsRuleLine;
import com.fc.v2.model.custom.SafetyGradeResult;
import com.fc.v2.model.custom.SafetyRuleSnapshot;
import com.fc.v2.service.ITOadsRuleLineService;
import com.fc.v2.util.OadsSafetyGradeRule;
import com.fc.v2.util.OadsWorkdayUtil;

/**
 * 牌面安全分档判据线 Service业务层处理（rule-eval 形状：规则求值，无增删改）。
 *
 * <p>全市统一实现的服务端落点。一轮询问只算一次（{@link #snapshot}）：按标识取值、
 * 整表取优、页面可用清单、清单上方条数都从这同一个快照里出，候选集、先后序、
 * 精度取齐、贴区间处处同一把尺，两问不可能对不齐。
 *
 * <ol>
 *   <li>本轮取哪一条：时间窗 [起用, 交班) 含起不含止、止空长期有效、线情形=0可用；
 *       多条并届先比先后序（大者先说话），再比代号字面序（排后者为准）。</li>
 *   <li>没有可用规则给不给说法：给，但结论是不分档（0），代号与起用日一并留空。</li>
 *   <li>脏配置（三个定值任一格缺、后一格没严格大于前一格、封顶限值&gt;100）本轮不采信，
 *       提示点名叫格。</li>
 *   <li>压线落档：定值与折算分先两位小数四舍五入取齐，再贴左闭右开区间，
 *       等于上限落号更大的档（{@link OadsSafetyGradeRule}）。</li>
 * </ol>
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TOadsRuleLineServiceImpl implements ITOadsRuleLineService {

    /** 线的情形：0在位（可用），1已撤下（在册但不参与判定、不进可用清单） */
    private static final int STATUS_IN = 0;
    private static final int DEL_NORMAL = 0;
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    @javax.annotation.Resource
    private TOadsRuleLineMapper oadsRuleLineMapper;

    @Override
    public TOadsRuleLine selectTOadsRuleLineById(Long id) {
        // 撤下的也是库里一行在册数据：按主键照常查得到，这里不掺任何过滤口径。
        return this.oadsRuleLineMapper.selectById(id);
    }

    @Override
    public int evaluate(String ruleCode, BigDecimal input, Date at) {
        // 原有入口，形状不动：收什么回什么，只回档位。内部与完整结论同一次计算。
        return gradeByCode(ruleCode, input, at).getLevel();
    }

    @Override
    public SafetyGradeResult gradeByCode(String ruleCode, BigDecimal input, Date at) {
        if (ruleCode == null || ruleCode.trim().isEmpty()) {
            return SafetyGradeResult.none(OadsSafetyGradeRule.align(input), "判据线代号为空，不予判定");
        }
        if (at == null) {
            return SafetyGradeResult.none(OadsSafetyGradeRule.align(input), "询问时刻为空，不予判定");
        }
        return grade(snapshot(at), ruleCode, input);
    }

    @Override
    public SafetyGradeResult gradeTop(BigDecimal input, Date at) {
        if (at == null) {
            return SafetyGradeResult.none(OadsSafetyGradeRule.align(input), "询问时刻为空，不予判定");
        }
        return grade(snapshot(at), null, input);
    }

    @Override
    public SafetyGradeResult grade(SafetyRuleSnapshot snap, String ruleCode, BigDecimal input) {
        if (snap == null) {
            return SafetyGradeResult.none(OadsSafetyGradeRule.align(input), "规则快照为空，不予判定");
        }
        BigDecimal aligned = OadsSafetyGradeRule.align(input);
        if (input == null || !OadsSafetyGradeRule.inRange(aligned)) {
            // 出格的输入按不分档处理，且不许借邻近规则凑答复。
            return SafetyGradeResult.none(aligned,
                    "折算分" + (input == null ? "缺值" : aligned + "超出0..100量程") + "，按不分档处理");
        }
        TOadsRuleLine rule;
        if (ruleCode != null && !ruleCode.trim().isEmpty()) {
            // 按标识取值：只在这同一份快照的可用清单里认；找不到再看是不是被剔下的脏配置。
            rule = snap.findByCode(ruleCode);
            if (rule == null) {
                SafetyRuleSnapshot.Dirty dirty = snap.dirtyOf(ruleCode);
                if (dirty != null) {
                    return SafetyGradeResult.none(aligned, joinNotes(dirty.getRule(), dirty.getNotes()));
                }
                return SafetyGradeResult.none(aligned,
                        "判据线代号[" + ruleCode + "]在" + OadsWorkdayUtil.ts(snap.getAt())
                                + "取不到生效版本，按不分档处理");
            }
        } else {
            // 整表取优：同一快照排在最前的那条先说话。
            rule = snap.top();
            if (rule == null) {
                return SafetyGradeResult.none(aligned,
                        OadsWorkdayUtil.ts(snap.getAt()) + "没有可用的判据线规则，按不分档处理");
            }
        }
        return gradeWith(rule, aligned);
    }

    @Override
    public SafetyRuleSnapshot snapshot(Date at) {
        SafetyRuleSnapshot snap = new SafetyRuleSnapshot(at);
        if (at == null) {
            return snap;
        }
        List<TOadsRuleLine> available = new ArrayList<TOadsRuleLine>();
        List<SafetyRuleSnapshot.Dirty> dirty = new ArrayList<SafetyRuleSnapshot.Dirty>();
        for (TOadsRuleLine r : loadAllNormal()) {
            // 已撤下（status=1）：仍是在册一行（按主键可查），但不参与判定也不进可用清单。
            if (r.getStatus() == null || r.getStatus() != STATUS_IN) {
                continue;
            }
            if (!inWindow(r, at)) {
                continue;
            }
            List<String> notes = new ArrayList<String>();
            if (!configClean(r, notes)) {
                dirty.add(new SafetyRuleSnapshot.Dirty(r, notes));
                continue;
            }
            available.add(r);
        }
        Collections.sort(available, SafetyRuleSnapshot.ORDER);
        snap.setAvailable(available);
        snap.setDirty(dirty);
        return snap;
    }

    @Override
    public List<TOadsRuleLine> listAvailable(Date at) {
        return snapshot(at).getAvailable();
    }

    @Override
    public boolean usable(Long id, Date at) {
        if (id == null || at == null) {
            return false;
        }
        TOadsRuleLine r = this.oadsRuleLineMapper.selectById(id);
        return r != null
                && (r.getDelFlag() == null || r.getDelFlag() == DEL_NORMAL)
                && r.getStatus() != null && r.getStatus() == STATUS_IN
                && inWindow(r, at)
                && configClean(r, new ArrayList<String>());
    }

    @Override
    public int countAvailable(Date at) {
        // 条数与清单同一次计算：清单有几条，上方就是几。
        return snapshot(at).count();
    }

    // ---------------------------------------------------------------------

    /** 同一条规则贴同一个区间：两入口共用，杜绝各算一遍。 */
    private SafetyGradeResult gradeWith(TOadsRuleLine rule, BigDecimal aligned) {
        int level = OadsSafetyGradeRule.levelOf(aligned,
                OadsSafetyGradeRule.align(rule.getTh1Max()),
                OadsSafetyGradeRule.align(rule.getTh2Max()),
                OadsSafetyGradeRule.align(rule.getTh3Max()));
        return SafetyGradeResult.graded(rule, level, aligned,
                "采信判据线[" + rule.getRuleCode() + "]，折算分" + aligned + "落第" + level + "档");
    }

    /** 整张在册判据线只读一次，清单/条数/逐条判定都从这同一锅数据里出。 */
    private List<TOadsRuleLine> loadAllNormal() {
        return this.oadsRuleLineMapper.selectList(
                new QueryWrapper<TOadsRuleLine>().eq("del_flag", DEL_NORMAL));
    }

    /**
     * 生效窗口：起用那一一日不晚于本次询问（起端含当口），交班那一一日晚于本次询问
     * （止端不含）。交班那一格空着的早年人工规则一律按长期有效认——兼容分支，不删。
     * 起用那一格空着则无从证明其已生效，不认。
     */
    private boolean inWindow(TOadsRuleLine r, Date at) {
        LocalDateTime t = OadsWorkdayUtil.toLocal(at);
        if (r.getEffStart() == null) {
            return false;
        }
        if (OadsWorkdayUtil.toLocal(r.getEffStart()).isAfter(t)) {
            return false;
        }
        if (r.getEffEnd() == null) {
            return true;
        }
        return OadsWorkdayUtil.toLocal(r.getEffEnd()).isAfter(t);
    }

    /**
     * 三定值是否干净：任一格缺值、后一格没严格大于前一格、封顶限值&gt;100，
     * 一律不予采信；notes 里点名缺的是哪一格、又是哪一格没大于它的前一格。
     * 比对之前先按两位小数四舍五入取齐，取齐之后才比大小。
     */
    private boolean configClean(TOadsRuleLine r, List<String> notes) {
        BigDecimal th1 = OadsSafetyGradeRule.align(r.getTh1Max());
        BigDecimal th2 = OadsSafetyGradeRule.align(r.getTh2Max());
        BigDecimal th3 = OadsSafetyGradeRule.align(r.getTh3Max());
        boolean missing = false;
        if (th1 == null) {
            notes.add("起算限值那一格缺值");
            missing = true;
        }
        if (th2 == null) {
            notes.add("够线限值那一格缺值");
            missing = true;
        }
        if (th3 == null) {
            notes.add("封顶限值那一格缺值");
            missing = true;
        }
        if (missing) {
            return false;
        }
        boolean clean = true;
        if (th2.compareTo(th1) <= 0) {
            notes.add("够线限值(" + th2 + ")没有大于它的前一格起算限值(" + th1 + ")");
            clean = false;
        }
        if (th3.compareTo(th2) <= 0) {
            notes.add("封顶限值(" + th3 + ")没有大于它的前一格够线限值(" + th2 + ")");
            clean = false;
        }
        if (th3.compareTo(HUNDRED) > 0) {
            notes.add("封顶限值(" + th3 + ")大过100，不予采信，免得把脏值算成高档");
            clean = false;
        }
        return clean;
    }

    private String joinNotes(TOadsRuleLine r, List<String> notes) {
        return "判据线代号[" + r.getRuleCode() + "]本轮不予采信：" + String.join("；", notes);
    }
}
