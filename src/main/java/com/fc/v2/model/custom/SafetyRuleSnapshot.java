package com.fc.v2.model.custom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fc.v2.model.auto.TOadsRuleLine;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 某一时刻整表取优的**一次计算**快照：页面可用清单、清单上方条数、逐条判定
 * 必须出自这同一个对象，不许各起一轮、各算一遍。
 *
 * <p>按标识取值与整表取优两个入口都从这一个快照里挑规则、同一套先后序，
 * 同一处点位两问必然同档。
 *
 * @author fuce
 * @date 2026-10-05
 */
@ApiModel(value = "SafetyRuleSnapshot", description = "判据线整表一次计算快照")
public class SafetyRuleSnapshot {

    /** 先后序降序、代号字面序降序（与服务端选择器同一把尺）。 */
    public static final Comparator<TOadsRuleLine> ORDER = new Comparator<TOadsRuleLine>() {
        @Override
        public int compare(TOadsRuleLine a, TOadsRuleLine b) {
            int pa = a.getPriority() == null ? Integer.MIN_VALUE : a.getPriority();
            int pb = b.getPriority() == null ? Integer.MIN_VALUE : b.getPriority();
            if (pa != pb) {
                return pb - pa;
            }
            String ca = a.getRuleCode() == null ? "" : a.getRuleCode();
            String cb = b.getRuleCode() == null ? "" : b.getRuleCode();
            int byCode = cb.compareTo(ca);
            if (byCode != 0) {
                return byCode;
            }
            long ia = a.getId() == null ? Long.MIN_VALUE : a.getId();
            long ib = b.getId() == null ? Long.MIN_VALUE : b.getId();
            return Long.compare(ib, ia);
        }
    };

    /** 一条被剔下的脏配置及其点名说法。 */
    public static class Dirty {
        private final TOadsRuleLine rule;
        private final List<String> notes;

        public Dirty(TOadsRuleLine rule, List<String> notes) {
            this.rule = rule;
            this.notes = notes;
        }

        public TOadsRuleLine getRule() {
            return rule;
        }

        public List<String> getNotes() {
            return Collections.unmodifiableList(notes);
        }
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "对齐的询问时刻")
    private Date at;

    @ApiModelProperty(value = "可用清单（已剔脏配置，先后序降序、代号字面序降序）")
    private List<TOadsRuleLine> available = new ArrayList<TOadsRuleLine>();

    @JsonIgnore
    private List<Dirty> dirty = new ArrayList<Dirty>();

    public SafetyRuleSnapshot() {
    }

    public SafetyRuleSnapshot(Date at) {
        this.at = at;
    }

    /** 条数与清单同源：清单有几条就是几条。 */
    public int count() {
        return available.size();
    }

    /** 整表取优：排在最前的那条先说话；清单为空返回 null（结论不分档）。 */
    public TOadsRuleLine top() {
        return available.isEmpty() ? null : available.get(0);
    }

    /** 按标识取值：同一代号在快照里排在最前的生效版本；没有返回 null。 */
    public TOadsRuleLine findByCode(String ruleCode) {
        if (ruleCode == null) {
            return null;
        }
        for (TOadsRuleLine r : available) {
            if (ruleCode.equals(r.getRuleCode())) {
                return r;
            }
        }
        return null;
    }

    /** 按代号找被剔下的脏配置及其点名说法。 */
    public Dirty dirtyOf(String ruleCode) {
        if (ruleCode == null) {
            return null;
        }
        for (Dirty d : dirty) {
            if (ruleCode.equals(d.getRule().getRuleCode())) {
                return d;
            }
        }
        return null;
    }

    public Date getAt() {
        return at;
    }

    public void setAt(Date at) {
        this.at = at;
    }

    public List<TOadsRuleLine> getAvailable() {
        return Collections.unmodifiableList(available);
    }

    public void setAvailable(List<TOadsRuleLine> available) {
        this.available = available;
    }

    public List<Dirty> getDirty() {
        return Collections.unmodifiableList(dirty);
    }

    public void setDirty(List<Dirty> dirty) {
        this.dirty = dirty;
    }

    /** 页面清单上方/看板用：脏配置的点名提示（点名叫格，不只是数值有问题）。 */
    public List<String> getDirtyNotes() {
        List<String> notes = new ArrayList<String>();
        for (Dirty d : dirty) {
            notes.add("判据线代号[" + d.getRule().getRuleCode() + "]本轮不予采信："
                    + String.join("；", d.getNotes()));
        }
        return Collections.unmodifiableList(notes);
    }
}
