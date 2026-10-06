package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TOadsSpotMapper;
import com.fc.v2.mapper.auto.TOadsAreaBookMapper;
import com.fc.v2.mapper.auto.TOadsSpotGradeLogMapper;
import com.fc.v2.model.auto.TOadsSpot;
import com.fc.v2.model.auto.TOadsAreaBook;
import com.fc.v2.model.auto.TOadsSpotGradeLog;
import com.fc.v2.model.custom.SafetyGradeResult;
import com.fc.v2.model.custom.SafetyRuleSnapshot;
import com.fc.v2.model.custom.SpotDangling;
import com.fc.v2.model.custom.SpotGradeQuery;
import com.fc.v2.model.custom.SpotGradeOutcome;
import com.fc.v2.service.ITOadsRuleLineService;
import com.fc.v2.service.ITOadsSpotService;
import com.fc.v2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 点位底册Service业务层处理
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TOadsSpotServiceImpl extends ServiceImpl<TOadsSpotMapper, TOadsSpot> implements ITOadsSpotService {

    @Autowired
    private TOadsAreaBookMapper oadsAreaBookMapper;

    @Autowired
    private ITOadsRuleLineService oadsRuleLineService;

    @Autowired
    private TOadsSpotGradeLogMapper oadsSpotGradeLogMapper;

    @Override
    public TOadsSpot selectTOadsSpotById(Long id) {
        return this.baseMapper.selectOne(new QueryWrapper<TOadsSpot>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    @Override
    public List<TOadsSpot> selectTOadsSpotList(Wrapper<TOadsSpot> queryWrapper) {
        QueryWrapper<TOadsSpot> wrapper = new QueryWrapper<TOadsSpot>();
        com.github.pagehelper.PageHelper.startPage(1, 10);
        wrapper.eq("status", 0);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public int insertTOadsSpot(TOadsSpot record) {
        if (record == null) {
            return 0;
        }

        record.setCreateBy(record.getSpotNo());
        TOadsAreaBook refArch = oadsAreaBookMapper.selectOne(new QueryWrapper<TOadsAreaBook>()
                .eq("id", record.getAreaId()).eq("del_flag", 0));
        if (refArch == null) {
            return 0;
        }
        if (refArch.getStatus() != null && refArch.getStatus() == 1) {
            return 0;
        }
        record.setAreaNo(refArch.getAreaNo());
        // 挂牌情形档/挂设档位只许由设置许可核准单封存同一次处理挪：主档新建一律未挂设、第0档
        record.setSetState(0);
        record.setSetVer(0);
        // 安全分档三列只许由分档固化那一个事务写：新建一律未分档、代号与起用日留空
        record.setSafeLevel(0);
        record.setSafeRuleCode(null);
        record.setSafeEffStart(null);
        if (StringUtils.isNotEmpty(record.getSpotNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TOadsSpot>()
                    .eq("spot_no", record.getSpotNo()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }
        // 待补处数只有一套口径：核定应设-已核验（30与12得18），存量/新增不分叉。
        // 五检查点流程的完整判定（身份/围栏/名录）在 ITOadsSpotRegisterService 那唯一裁决处；
        // 这里的老通用建行口仍按同一减法轧数，不保留 need+got 的第二条算法。
        int remIn = record.getNeedNum() == null ? 0 : record.getNeedNum();
        int remOut = record.getGotNum() == null ? 0 : record.getGotNum();
        record.setLackNum(remIn - remOut);

        record.setDelFlag(0);
        return this.baseMapper.insert(record);
    }

    @Override
    public int updateTOadsSpot(TOadsSpot record) {
        if (record == null || record.getId() == null) {
            return 0;
        }

        if (record.getId() != null && StringUtils.isNotEmpty(record.getSpotNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TOadsSpot>()
                    .eq("spot_no", record.getSpotNo()).ne("id", record.getId()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }

        // 挂牌情形档/挂设档位的唯一挪法是核准单封存；通用修改入口报来的这两列一律不采信，压回现值。
        // 安全分档三列同理：只许 gradeAndFix 那一个事务固化，旁入口一律不采信。
        // 在走步序（flow_step）只由步序留痕重数落账、待补处数（lack_num）只由 need-got 轧：
        // 通用修改口既不采信报文自报的档，也不采信手填的待补。
        TOadsSpot current = this.baseMapper.selectById(record.getId());
        if (current != null) {
            record.setSetState(current.getSetState());
            record.setSetVer(current.getSetVer());
            record.setSafeLevel(current.getSafeLevel());
            record.setSafeRuleCode(current.getSafeRuleCode());
            record.setSafeEffStart(current.getSafeEffStart());
            record.setFlowStep(current.getFlowStep());
            int need = record.getNeedNum() == null
                    ? (current.getNeedNum() == null ? 0 : current.getNeedNum()) : record.getNeedNum();
            int got = record.getGotNum() == null
                    ? (current.getGotNum() == null ? 0 : current.getGotNum()) : record.getGotNum();
            record.setLackNum(need - got);
        }

        record.setUpdateTime(new Date());
        return this.baseMapper.update(record, new UpdateWrapper<TOadsSpot>()
                .eq("id", record.getId())
                .eq("del_flag", 0));
    }

    @Override
    public int deleteTOadsSpotByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        return this.baseMapper.deleteBatchIds(Arrays.asList(idArr));
    }

    @Override
    public int deleteTOadsSpotById(Long id) {
        return this.baseMapper.deleteById(id);
    }

    // ---------------- 牌面安全分档（全市统一口径，服务端一处给出） ----------------

    @Override
    public SafetyGradeResult gradeSpot(Long spotId, BigDecimal score, Date at) {
        TOadsSpot spot = loadNormalSpot(spotId);
        if (spot == null) {
            return SafetyGradeResult.none(null, "点位不存在或已删除，不予判定");
        }
        if (at == null) {
            at = new Date();
        }
        // 点位挂着代号走按标识取值，空着走整表取优；都在同一份快照上，两问同档。
        SafetyRuleSnapshot snap = this.oadsRuleLineService.snapshot(at);
        return this.oadsRuleLineService.grade(snap, spot.getRuleCode(), score);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SafetyGradeResult gradeAndFix(Long spotId, BigDecimal score, Date at) {
        TOadsSpot spot = loadNormalSpot(spotId);
        if (spot == null) {
            return SafetyGradeResult.none(null, "点位不存在或已删除，不予判定");
        }
        if (at == null) {
            at = new Date();
        }
        // 先有依据后有结论：同一份快照、同一次比对先给出档位与采信的那一条，
        // 不许先定档位再回头挑一条规则来圆。
        SafetyRuleSnapshot snap = this.oadsRuleLineService.snapshot(at);
        SafetyGradeResult grade = this.oadsRuleLineService.grade(snap, spot.getRuleCode(), score);

        // 固化那一步与留痕那一步同成一个事务：点位行三列与留痕行一对，逐字相同。
        // 不分档（0）时判据线代号与起用那一一日一并留空——set 成 null，不借邻近、不抄上季。
        this.baseMapper.update(null, new UpdateWrapper<TOadsSpot>()
                .eq("id", spot.getId())
                .eq("del_flag", 0)
                .set("safe_level", grade.getLevel())
                .set("safe_rule_code", grade.getRuleCode())
                .set("safe_eff_start", grade.getEffStart())
                .set("update_time", at));

        TOadsSpotGradeLog log = new TOadsSpotGradeLog();
        log.setSpotId(spot.getId());
        log.setSpotNo(spot.getSpotNo());
        log.setScore(score);
        log.setScoreAligned(grade.getAlignedScore());
        log.setSafeLevel(grade.getLevel());
        log.setRuleCode(grade.getRuleCode());
        log.setRuleId(grade.getRuleId());
        log.setEffStart(grade.getEffStart());
        log.setAskAt(at);
        log.setGradeMsg(grade.getMessage());
        this.oadsSpotGradeLogMapper.insert(log);

        return grade;
    }

    @Override
    public List<SpotGradeOutcome> gradeBatch(List<SpotGradeQuery> queries, Date at) {
        List<SpotGradeOutcome> outcomes = new ArrayList<SpotGradeOutcome>();
        if (queries == null || queries.isEmpty()) {
            return outcomes;
        }
        if (at == null) {
            at = new Date();
        }
        // 一批只算一次：清单、条数、逐条判定出自同一份快照，不许各起一轮。
        SafetyRuleSnapshot snap = this.oadsRuleLineService.snapshot(at);
        for (SpotGradeQuery q : queries) {
            if (q == null || q.getSpotId() == null) {
                continue;
            }
            TOadsSpot spot = loadNormalSpot(q.getSpotId());
            if (spot == null) {
                outcomes.add(new SpotGradeOutcome(q.getSpotId(), null,
                        SafetyGradeResult.none(null, "点位不存在或已删除，不予判定")));
                continue;
            }
            SafetyGradeResult grade = this.oadsRuleLineService.grade(snap, spot.getRuleCode(), q.getScore());
            outcomes.add(new SpotGradeOutcome(spot.getId(), spot.getSpotNo(), grade));
        }
        return outcomes;
    }

    @Override
    public List<SpotDangling> listDangling(Date at) {
        if (at == null) {
            at = new Date();
        }
        SafetyRuleSnapshot snap = this.oadsRuleLineService.snapshot(at);
        List<SpotDangling> dangling = new ArrayList<SpotDangling>();
        // 只列挂着代号的；代号空着的点位走整表取优，不存在"取不到生效版本"一说。
        for (TOadsSpot spot : this.baseMapper.selectList(new QueryWrapper<TOadsSpot>()
                .eq("del_flag", 0).isNotNull("rule_code").ne("rule_code", ""))) {
            String code = spot.getRuleCode();
            if (code == null || code.trim().isEmpty()) {
                continue;
            }
            if (snap.findByCode(code) != null) {
                continue;
            }
            String reason;
            SafetyRuleSnapshot.Dirty dirty = snap.dirtyOf(code);
            if (dirty != null) {
                reason = "判据线代号[" + code + "]本轮不予采信：" + String.join("；", dirty.getNotes());
            } else {
                reason = "判据线代号[" + code + "]在" + com.fc.v2.util.OadsWorkdayUtil.ts(at)
                        + "取不到生效版本（已撤下／未到起用日／已交班／从不存在）";
            }
            dangling.add(new SpotDangling(spot.getId(), spot.getSpotNo(), code, reason));
        }
        return dangling;
    }

    private TOadsSpot loadNormalSpot(Long spotId) {
        if (spotId == null) {
            return null;
        }
        TOadsSpot spot = this.baseMapper.selectById(spotId);
        if (spot == null || (spot.getDelFlag() != null && spot.getDelFlag() != 0)) {
            return null;
        }
        return spot;
    }
}
