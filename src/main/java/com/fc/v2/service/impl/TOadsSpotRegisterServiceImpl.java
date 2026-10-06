package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.exception.spot.SpotRuleException;
import com.fc.v2.mapper.auto.TOadsAreaBookMapper;
import com.fc.v2.mapper.auto.TOadsBookCfgMapper;
import com.fc.v2.mapper.auto.TOadsPostAreaMapper;
import com.fc.v2.mapper.auto.TOadsSpotFlowLogMapper;
import com.fc.v2.mapper.auto.TOadsSpotMapper;
import com.fc.v2.mapper.auto.TOadsSpotSyncCursorMapper;
import com.fc.v2.model.auto.TOadsAreaBook;
import com.fc.v2.model.auto.TOadsBookCfg;
import com.fc.v2.model.auto.TOadsPostArea;
import com.fc.v2.model.auto.TOadsSpot;
import com.fc.v2.model.auto.TOadsSpotFlowLog;
import com.fc.v2.model.auto.TOadsSpotSyncCursor;
import com.fc.v2.model.custom.CitySpot;
import com.fc.v2.model.custom.CitySyncOutcome;
import com.fc.v2.model.custom.PostScope;
import com.fc.v2.model.custom.SpotBookQuery;
import com.fc.v2.model.custom.SpotBuildResult;
import com.fc.v2.model.custom.SpotFlowOutcome;
import com.fc.v2.model.custom.SpotForm;
import com.fc.v2.model.custom.SpotViewCounts;
import com.fc.v2.service.ITOadsSpotRegisterService;
import com.fc.v2.service.spot.AreaFence;
import com.fc.v2.service.spot.SpotFlowStep;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 点位名册五检查点唯一裁决处实现。
 *
 * <p>五个检查点严格按 1身份 → 2空间挂靠 → 3数量 → 4名录依据 → 5同步与展示 的先后走，
 * 前一个不过不进下一个，每个检查点有各自的提示语，从不合并成一句"数据有误"。
 *
 * <p>一行能否落库、待补处数多少、辖段视图出现哪几行、页首与回执那几个数，四样只在本类给答案：
 * 分页/计数/三视图同走 {@link #scopeRows} 取数加 {@link #matches} 谓词这一份基础数据，
 * 旁处没有第二条判定路径。
 *
 * @author fuce
 * @date 2026-10-05
 */
@Service
public class TOadsSpotRegisterServiceImpl implements ITOadsSpotRegisterService {

    private static final String CFG_BOOK = "book";
    private static final String CFG_CITY = "city";
    private static final int DEL_OK = 0;
    /** 段册情形：已撤出（注销） */
    private static final int AREA_REVOKED = 1;
    /** 旧行建档情形键 */
    private static final String BUILD_LEGACY = "LEGACY";
    private static final String KIND_UNKNOWN = "未知类别";

    @javax.annotation.Resource
    private TOadsSpotMapper spotMapper;
    @javax.annotation.Resource
    private TOadsAreaBookMapper areaBookMapper;
    @javax.annotation.Resource
    private TOadsSpotFlowLogMapper flowLogMapper;
    @javax.annotation.Resource
    private TOadsBookCfgMapper bookCfgMapper;
    @javax.annotation.Resource
    private TOadsPostAreaMapper postAreaMapper;
    @javax.annotation.Resource
    private TOadsSpotSyncCursorMapper syncCursorMapper;

    // ================================================================ 建行 / 更新（检查点 1-4）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpotBuildResult build(SpotForm form, String buildKind, String operator) {
        if (form == null) {
            throw new SpotRuleException("检查点1身份：未收到报送材料，无从建行");
        }
        String kind = SpotFlowStep.BUILD_BATCH.equals(buildKind)
                ? SpotFlowStep.BUILD_BATCH : SpotFlowStep.BUILD_MANUAL;
        String quarter = form.getQuarterCode() == null || form.getQuarterCode().trim().isEmpty()
                ? currentQuarter() : form.getQuarterCode().trim();

        // -------- 检查点1：身份（点位码报送自带，不另发、不校构成规则） --------
        String spotNo = form.getSpotNo() == null ? null : form.getSpotNo().trim();
        if (spotNo == null || spotNo.isEmpty()) {
            throw new SpotRuleException("检查点1身份：点位编码须由报送材料自带，系统不另发第二串，本行不收");
        }
        TOadsSpot target = null;
        if (form.getId() != null) {
            target = loadSpot(form.getId());
            // 例外只有一种：本次操作的对象就是本季同码那一行本身——按更新，不得当成多出来的一行挡回去。
            if (target.getSpotNo() != null && !spotNo.equals(target.getSpotNo().trim())) {
                throw new SpotRuleException(
                        "检查点1身份：点位编码随报送材料自带、不得更换；本行现码为" + target.getSpotNo());
            }
        }
        // 本季度同一编码已有"仍然有效（在走）"的行时拒绝；压卡/就地停住后名额才空出。
        List<TOadsSpot> sameNoQuarter = new ArrayList<TOadsSpot>();
        for (TOadsSpot s : allSpots()) {
            if (sameQuarter(s, quarter) && spotNo.equals(s.getSpotNo()) && !sameId(s, form.getId())
                    && isAlive(s)) {
                sameNoQuarter.add(s);
            }
        }
        if (!sameNoQuarter.isEmpty()) {
            throw new SpotRuleException("检查点1身份：本季度点位编码 " + spotNo
                    + " 已有仍在册在走的行，第二行立不起来；待其压卡或就地停住后，同点位才可另立新行");
        }
        if (target != null) {
            Recount targetRc = recount(target.getId());
            if (targetRc.step != null && targetRc.step == SpotFlowStep.SEALED) {
                throw new SpotRuleException("检查点1身份：点位编码 " + spotNo
                        + " 那一行已走到压卡，加不进去、字改不动、也撤不下来；非动不可只能另开一行");
            }
            if (targetRc.held) {
                throw new SpotRuleException("检查点1身份：点位编码 " + spotNo
                        + " 那一行已就地停住，不再接改动；要动只能另开一行");
            }
        }

        // 先把目录这一行取到手供空间挂靠取围栏；目录里找没找到、注销没注销，是检查点4才给的交代。
        String areaNo = form.getAreaNo() == null ? null : form.getAreaNo().trim();
        TOadsAreaBook area = areaNo == null || areaNo.isEmpty() ? null : findArea(areaNo);

        // -------- 检查点2：空间挂靠（坐标现场采集，落围栏之外一律不收） --------
        BigDecimal lng = form.getLongitude();
        BigDecimal lat = form.getLatitude();
        if (lng == null || lat == null) {
            throw new SpotRuleException("检查点2空间挂靠：点位 " + spotNo
                    + " 的经纬度来自现场采集，缺坐标即按落在辖段围栏之外处理，不予收录");
        }
        // 目录行存在才判得动围栏；目录里找不到的情形不在此处下结论，留给检查点4点名。
        if (area != null && !AreaFence.contains(area.getFenceText(), lng, lat)) {
            throw new SpotRuleException("检查点2空间挂靠：点位 " + spotNo + " 的坐标（"
                    + lng.stripTrailingZeros().toPlainString() + "," + lat.stripTrailingZeros().toPlainString()
                    + "）在辖段 " + areaNo + "（" + area.getAreaName() + "）的围栏之外，不予收录");
        }

        // -------- 检查点3：数量（待补=核定应设-已核验，写库前重算，覆盖旧值） --------
        Integer need = parseNeed(form.getNeedText());
        Integer got = parseGot(form.getGotText());
        if (need < got) {
            throw new SpotRuleException("检查点3数量：核定应设处数(" + need + ")小于已核验落地处数(" + got
                    + ")（同时即已核验落地处数高于核定应设处数），本行 " + spotNo + " 进不了库");
        }
        int lack = need - got;

        // -------- 检查点4：名录依据（找不到/已注销在此点名；冗余全称以目录为准） --------
        if (area == null) {
            throw new SpotRuleException("检查点4名录依据：路段代号 "
                    + (areaNo == null || areaNo.isEmpty() ? "（空）" : areaNo) + " 在目录里找不到，建行失败");
        }
        boolean newToArea = target == null || target.getAreaNo() == null
                || !area.getAreaNo().equals(target.getAreaNo().trim());
        if (newToArea && area.getStatus() != null && area.getStatus() == AREA_REVOKED) {
            // 注销段（整段划归邻段、或改隶别的辖段）不再接受新增行（改挂进来视同新增）；
            // 注销前已存在的行仍可列出、仍计入在册处数，不做连带清除。
            throw new SpotRuleException("检查点4名录依据：路段 " + areaNo + "（" + area.getAreaName()
                    + "）已注销，不再接受新增行；注销前已存在的行仍列出、仍计入在册处数");
        }

        // -------- 落库：页首与回执的数都以这里写回的为准 --------
        boolean isNew = target == null;
        SpotBuildResult result = isNew ? SpotBuildResult.created() : SpotBuildResult.updated();
        Date now = new Date();
        if (isNew) {
            TOadsSpot row = new TOadsSpot();
            row.setSpotNo(spotNo);
            row.setAreaNo(area.getAreaNo());
            row.setAreaName(area.getAreaName());
            row.setLongitude(lng);
            row.setLatitude(lat);
            row.setNeedNum(need);
            row.setGotNum(got);
            row.setLackNum(lack);
            row.setContent(form.getContent());
            row.setQuarterCode(quarter);
            row.setBuildKind(kind);
            // 建好落第一步：待核。档由系统按留痕数，不由屏上填。
            row.setFlowStep(SpotFlowStep.PENDING);
            // 既有不变量：挂牌/分档列不由建行入口写
            row.setSetState(0);
            row.setSetVer(0);
            row.setSafeLevel(0);
            row.setStatus(0);
            row.setDelFlag(DEL_OK);
            row.setCreateBy(operator);
            row.setCreateTime(now);
            spotMapper.insert(row);
            appendLog(row.getId(), spotNo, SpotFlowStep.PENDING, 1, SpotFlowStep.ACTION_REACH,
                    "建行落第一步：待核（" + (kind.equals(SpotFlowStep.BUILD_BATCH) ? "集中更换带出" : "建档岗逐条建立")
                            + "）", operator, now);
            result.setSpotId(row.getId());
        } else {
            // 更新就着原行：步序不重置（退不退回由留痕说话）；坐标/挂靠/数量照同三关重判，待补重算覆盖。
            if (target.getSpotNo() == null || target.getSpotNo().trim().isEmpty()) {
                // 旧纸簿搬入、码空着的旧行：本次补录编码予以兼容，不当报错。
                target.setSpotNo(spotNo);
            }
            target.setAreaNo(area.getAreaNo());
            // 冗余全称与目录有出入：以目录为准修正，两边并存视为缺陷。
            target.setAreaName(area.getAreaName());
            target.setLongitude(lng);
            target.setLatitude(lat);
            target.setNeedNum(need);
            target.setGotNum(got);
            target.setLackNum(lack);
            target.setContent(form.getContent());
            // 旧行借本次更新进入当季名册；步序列仍不动，停在哪一步按留痕重数。
            if (target.getQuarterCode() == null || target.getQuarterCode().trim().isEmpty()) {
                target.setQuarterCode(quarter);
            }
            target.setUpdateBy(operator);
            target.setUpdateTime(now);
            spotMapper.updateById(target);
            result.setSpotId(target.getId());
        }

        result.setSpotNo(spotNo);
        result.setAreaNo(area.getAreaNo());
        result.setAreaName(area.getAreaName());
        result.setNeedNum(need);
        result.setGotNum(got);
        result.setLackNum(lack);
        result.setFlowStep(currentStep(result.getSpotId()));
        result.setBuildKind(isNew ? kind : target.getBuildKind());
        result.setMessage((isNew ? "已建行并落第一步「待核」" : "已就原行更新")
                + "；核定应设 " + need + "、已核验落地 " + got + "、待补 " + lack
                + "（写库前由裁决处统一重算，回执以此为准）");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpotBuildResult correctGot(Long spotId, Integer newGot, String operator) {
        TOadsSpot row = requireMutable(spotId);
        int got = newGot == null ? 0 : newGot.intValue();
        if (newGot != null && newGot < 0) {
            throw new SpotRuleException("检查点3数量：已核验落地处数为负(" + newGot + ")，点位 "
                    + row.getSpotNo() + " 进不了库");
        }
        int need = row.getNeedNum() == null ? 0 : row.getNeedNum();
        if (got > need) {
            throw new SpotRuleException("检查点3数量：已核验落地处数(" + got + ")高于核定应设处数(" + need
                    + ")，点位 " + row.getSpotNo() + " 的更正进不了库");
        }
        // 三个重算时刻之一：已核验处数被更正。重算结果覆盖上一次落库的值。
        int lack = need - got;
        row.setGotNum(got);
        row.setLackNum(lack);
        row.setUpdateBy(operator);
        row.setUpdateTime(new Date());
        spotMapper.updateById(row);
        return resultOf(row, "已核验落地处数已更正为 " + got + "，待补处数重算为 " + lack + "（覆盖旧值）");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpotBuildResult rehang(Long spotId, String newAreaNo, BigDecimal longitude, BigDecimal latitude,
                                  String operator) {
        TOadsSpot row = requireMutable(spotId);
        SpotForm form = new SpotForm();
        form.setId(spotId);
        form.setSpotNo(row.getSpotNo());
        form.setAreaNo(newAreaNo);
        form.setLongitude(longitude);
        form.setLatitude(latitude);
        form.setNeedText(row.getNeedNum() == null ? null : row.getNeedNum().toString());
        form.setGotText(row.getGotNum() == null ? null : row.getGotNum().toString());
        form.setContent(row.getContent());
        // 三个重算时刻之一：改挂到别的路段目录（重走空间挂靠、名录依据，数量重算）。
        return build(form, row.getBuildKind() == null ? SpotFlowStep.BUILD_MANUAL : row.getBuildKind(), operator);
    }

    // ================================================================ 在走三档（步子不得跨）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpotFlowOutcome advance(Long spotId, String note, String operator) {
        return advance(spotId, note, operator, null);
    }

    /**
     * 屏上填的档 {@code claimStep} 只用来核对：与"下一步"对不上就是跨档，这一档没过之后的路不给走。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpotFlowOutcome advance(Long spotId, String note, String operator, Integer claimStep) {
        TOadsSpot row = requireSpot(spotId);
        Recount rc = recount(spotId);
        if (rc.step != null && rc.step == SpotFlowStep.SEALED) {
            throw new SpotRuleException("点位 " + row.getSpotNo() + " 已压卡到头：往后加不进去、字改不动、也撤不下来");
        }
        if (rc.held) {
            throw new SpotRuleException("点位 " + row.getSpotNo() + " 已就地停住，不再往上推；要走只能另开一行");
        }
        int target = (rc.step == null ? SpotFlowStep.PENDING : rc.step + 1);
        if (target > SpotFlowStep.SEALED) {
            throw new SpotRuleException("点位 " + row.getSpotNo() + " 已到头，无步可推");
        }
        if (claimStep != null && claimStep.intValue() != target) {
            throw new SpotRuleException("步子不得跨：点位 " + row.getSpotNo() + " 目前停在"
                    + SpotFlowStep.nameOf(rc.step) + "，下一档只能是「" + SpotFlowStep.nameOf(target)
                    + "」，屏上填的「" + SpotFlowStep.nameOf(claimStep) + "」不作数");
        }
        int round = maxRound(spotId, target) + 1;
        Date now = new Date();
        appendLog(spotId, row.getSpotNo(), target, round, SpotFlowStep.ACTION_REACH, note, operator, now);
        row.setFlowStep(target);
        row.setUpdateBy(operator);
        row.setUpdateTime(now);
        spotMapper.updateById(row);

        SpotFlowOutcome o = outcome(row, SpotFlowStep.ACTION_REACH, target, round, false,
                target == SpotFlowStep.SEALED);
        o.setMessage("已走到「" + SpotFlowStep.nameOf(target) + "」（第" + round + "轮）"
                + (target == SpotFlowStep.SEALED ? "：压卡到头，本行冻结" : "，系统按步序留痕数档"));
        return o;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpotFlowOutcome returnStep(Long spotId, String note, String operator) {
        TOadsSpot row = requireSpot(spotId);
        Recount rc = recount(spotId);
        if (rc.step != null && rc.step == SpotFlowStep.SEALED) {
            throw new SpotRuleException("点位 " + row.getSpotNo() + " 已压卡，撤不下来；非动不可只能另开一行");
        }
        if (rc.held) {
            throw new SpotRuleException("点位 " + row.getSpotNo() + " 已就地停住，没有退回可走");
        }
        // 行还没到压卡，停在「已核」才退得回「待核」；停在待核则退无可退。
        if (rc.step == null || rc.step < SpotFlowStep.VERIFIED) {
            throw new SpotRuleException("点位 " + row.getSpotNo() + " 目前停在" + SpotFlowStep.nameOf(rc.step)
                    + "，前面没有「已核」可退回");
        }
        int fromStep = rc.step;
        int round = activeRound(spotId, fromStep);
        Date now = new Date();
        // 只清在走那一步的当轮：active 置 0，行不删；上一回记下的东西保留，不与后补那笔排在一起。
        flowLogMapper.deactivateRound(spotId, fromStep, round);
        appendLog(spotId, row.getSpotNo(), fromStep, round, SpotFlowStep.ACTION_RETURN, note, operator, now);
        int backTo = fromStep - 1;
        row.setFlowStep(backTo == 0 ? SpotFlowStep.PENDING : backTo);
        row.setUpdateBy(operator);
        row.setUpdateTime(now);
        spotMapper.updateById(row);
        SpotFlowOutcome o = outcome(row, SpotFlowStep.ACTION_RETURN, row.getFlowStep(),
                activeRound(spotId, row.getFlowStep()), false, false);
        o.setMessage("已由「" + SpotFlowStep.nameOf(fromStep) + "」退回一步重来；上一轮(" + round
                + ")的留痕保留在档、不与后补的那笔排在一起");
        return o;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpotFlowOutcome hold(Long spotId, String note, String operator) {
        TOadsSpot row = requireSpot(spotId);
        Recount rc = recount(spotId);
        if (rc.step != null && rc.step == SpotFlowStep.SEALED) {
            throw new SpotRuleException("点位 " + row.getSpotNo() + " 已压卡到头，无需再停");
        }
        if (rc.held) {
            throw new SpotRuleException("点位 " + row.getSpotNo() + " 已就地停住，不重复记停");
        }
        int step = rc.step == null ? SpotFlowStep.PENDING : rc.step;
        Date now = new Date();
        if (rc.step == null) {
            // 纸簿旧行一步未走：就地停住先把它落在「待核」，不允许一碰就报错。
            appendLog(spotId, row.getSpotNo(), SpotFlowStep.PENDING, 1, SpotFlowStep.ACTION_REACH,
                    "旧行进流程，先落第一步：待核", operator, now);
        }
        int round = activeRound(spotId, step);
        appendLog(spotId, row.getSpotNo(), step, Math.max(round, 1), SpotFlowStep.ACTION_HOLD, note, operator, now);
        row.setFlowStep(step);
        row.setUpdateBy(operator);
        row.setUpdateTime(now);
        spotMapper.updateById(row);
        SpotFlowOutcome o = outcome(row, SpotFlowStep.ACTION_HOLD, step, Math.max(round, 1), true, false);
        o.setMessage("点位 " + row.getSpotNo() + " 已就地停在「" + SpotFlowStep.nameOf(step)
                + "」；同点位名额空出，要继续只能另开一行");
        return o;
    }

    @Override
    public Integer currentStep(Long spotId) {
        if (spotId == null) {
            return null;
        }
        return recount(spotId).step;
    }

    // ================================================================ 检查点5：名册分页 / 查询 / 三视图

    @Override
    public List<TOadsSpot> pageBook(SpotBookQuery query, PostScope scope, int pageNum, int pageSize) {
        List<TOadsSpot> rows = filteredRows(query, scope);
        int from = Math.max(0, (pageNum - 1) * pageSize);
        if (from >= rows.size() || pageSize <= 0) {
            return new ArrayList<TOadsSpot>();
        }
        int to = Math.min(rows.size(), from + pageSize);
        return new ArrayList<TOadsSpot>(rows.subList(from, to));
    }

    @Override
    public long countBook(SpotBookQuery query, PostScope scope) {
        // 页首那一个数与分页、三视图同源同算，前端不数第二遍。
        return filteredRows(query, scope).size();
    }

    @Override
    public SpotViewCounts viewCounts(String quarterCode, PostScope scope) {
        String quarter = quarterCode == null || quarterCode.trim().isEmpty()
                ? currentQuarter() : quarterCode.trim();
        SpotBookQuery q = new SpotBookQuery();
        q.setQuarterCode(quarter);
        List<TOadsSpot> rows = filteredRows(q, scope);
        Map<String, TOadsAreaBook> areaMap = areaMap();

        SpotViewCounts counts = new SpotViewCounts();
        counts.setQuarterCode(quarter);
        Map<String, Integer> byKind = new LinkedHashMap<String, Integer>();
        // 三类各自排序：先把三类按固定次序摆好，未知类别附后；有行才有值，空类不补零。
        for (String k : new String[]{"主干道", "次干道", "背街巷"}) {
            byKind.put(k, 0);
        }
        Map<String, Integer> byNo = new TreeMap<String, Integer>();
        Map<String, Integer> byBuild = new LinkedHashMap<String, Integer>();
        // 同一份基础数据分三摞，总数只可能是同一个。
        for (TOadsSpot r : rows) {
            TOadsAreaBook a = r.getAreaNo() == null ? null : areaMap.get(r.getAreaNo());
            String kind = a == null || a.getAreaKind() == null ? KIND_UNKNOWN : a.getAreaKind();
            bump(byKind, kind);
            bump(byNo, r.getAreaNo() == null ? "未挂靠" : r.getAreaNo());
            bump(byBuild, r.getBuildKind() == null || r.getBuildKind().trim().isEmpty()
                    ? BUILD_LEGACY : r.getBuildKind().trim());
        }
        // 空类不留键（本层当季未建行留空，不以零值凑数）
        byKind.values().removeIf(v -> v == 0);
        counts.setByAreaKind(byKind);
        counts.setByAreaNo(byNo);
        counts.setByBuildKind(byBuild);
        counts.setTotal(rows.size());
        return counts;
    }

    @Override
    public int pageSize() {
        TOadsBookCfg cfg = bookCfg();
        if (cfg != null && cfg.getPageSize() != null && cfg.getPageSize() > 0) {
            return cfg.getPageSize();
        }
        return 10;
    }

    @Override
    public String currentQuarter() {
        TOadsBookCfg cfg = bookCfg();
        if (cfg != null && cfg.getQuarterCode() != null && !cfg.getQuarterCode().trim().isEmpty()) {
            return cfg.getQuarterCode().trim();
        }
        // 配置缺位时的兜底取季：不影响"只一处给值"——正常库由 t_oads_book_cfg 单行给。
        Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int q = c.get(Calendar.MONTH) / 3 + 1;
        return y + "Q" + q;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void configurePageSize(int pageSize, String operator) {
        if (pageSize <= 0) {
            throw new SpotRuleException("名册每页条数须为正整数，区局配置 " + pageSize + " 不合规");
        }
        TOadsBookCfg cfg = bookCfg();
        Date now = new Date();
        if (cfg == null) {
            cfg = new TOadsBookCfg();
            cfg.setId(0L);
            cfg.setCfgKey(CFG_BOOK);
            cfg.setQuarterCode(quarterByDate(now));
            cfg.setPageSize(pageSize);
            cfg.setDelFlag(DEL_OK);
            cfg.setCreateBy(operator);
            cfg.setCreateTime(now);
            bookCfgMapper.insert(cfg);
            return;
        }
        cfg.setPageSize(pageSize);
        cfg.setUpdateBy(operator);
        cfg.setUpdateTime(now);
        bookCfgMapper.updateById(cfg);
    }

    @Override
    public PostScope scopeOf(String postNo) {
        if (postNo == null || postNo.trim().isEmpty()) {
            return new PostScope("ANON", PostScope.SECTION);
        }
        List<TOadsPostArea> links = postAreaMapper.selectList(new QueryWrapper<TOadsPostArea>()
                .eq("post_no", postNo.trim()).eq("del_flag", DEL_OK));
        if (links.isEmpty()) {
            // 一个辖段都没划给它：什么都看不见，合法的空，不拿邻段补齐。
            return new PostScope(postNo.trim(), PostScope.SECTION);
        }
        String kind = links.get(0).getPostKind();
        PostScope scope = new PostScope(postNo.trim(),
                PostScope.BUREAU.equals(kind) ? PostScope.BUREAU : PostScope.SECTION);
        for (TOadsPostArea l : links) {
            if (l.getAreaNo() != null && !l.getAreaNo().trim().isEmpty()) {
                scope.getAreaNos().add(l.getAreaNo().trim());
            }
        }
        return scope;
    }

    // ================================================================ 检查点5：市里每日增量 + 游标

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CitySyncOutcome syncCity(List<CitySpot> cityRows, String operator) {
        TOadsSpotSyncCursor cursor = loadCityCursor();
        boolean cursorExisted = cursor != null;
        Date now = new Date();
        if (cursor == null) {
            cursor = new TOadsSpotSyncCursor();
            cursor.setId(0L);
            cursor.setCfgKey(CFG_CITY);
            cursor.setCursorId(0L);
            cursor.setDelFlag(DEL_OK);
        }
        final long before = cursor.getCursorId() == null ? 0L : cursor.getCursorId();

        // 唯一窗口：游标之后的本局行才参与本次比较；游标之前一律不复核。
        List<TOadsSpot> window = new ArrayList<TOadsSpot>();
        long maxId = before;
        for (TOadsSpot s : allSpots()) {
            long id = s.getId() == null ? 0L : s.getId();
            if (id > before) {
                window.add(s);
                if (id > maxId) {
                    maxId = id;
                }
            }
        }
        Map<String, TOadsSpot> localByNo = new HashMap<String, TOadsSpot>();
        for (TOadsSpot s : window) {
            if (s.getSpotNo() != null && !s.getSpotNo().trim().isEmpty()) {
                localByNo.put(s.getSpotNo().trim(), s);
            }
        }

        CitySyncOutcome outcome = new CitySyncOutcome();
        outcome.setCursorBefore(before);
        outcome.setWindowRows(window.size());
        List<CitySpot> city = cityRows == null ? new ArrayList<CitySpot>() : cityRows;
        for (CitySpot c : city) {
            if (c == null || c.getSpotNo() == null) {
                continue;
            }
            TOadsSpot local = localByNo.get(c.getSpotNo().trim());
            if (local == null) {
                // 市里有、本局窗口没有：以本局为准，不代为新增，也不算冲突。
                continue;
            }
            String localName = local.getAreaName();
            if (Boolean.TRUE.equals(c.getRevoked())) {
                // 市里已注销而本局仍挂载：逐条输出，不静默撤行。
                outcome.getConflicts().add(new CitySyncOutcome.Conflict(local.getSpotNo(), local.getAreaNo(),
                        CitySyncOutcome.CITY_REVOKED, localName, c.getAreaName(),
                        "市里已注销该点位，本局仍挂载；以本局为准，本行不撤，逐条登记"));
            } else if (c.getAreaName() != null && !c.getAreaName().trim().isEmpty()
                    && localName != null && !localName.equals(c.getAreaName().trim())) {
                // 两边全称冲突：以本局目录为准，逐条列出，不允许静默覆盖。
                outcome.getConflicts().add(new CitySyncOutcome.Conflict(local.getSpotNo(), local.getAreaNo(),
                        CitySyncOutcome.NAME_CONFLICT, localName, c.getAreaName().trim(),
                        "两边路段全称冲突，以本局目录为准，市里那一套不予覆盖"));
            }
        }

        // 游标随本次同步一并推进，且推进值与窗口出自同一份计算；别处不得自行调整。
        cursor.setCursorId(maxId);
        cursor.setSyncedAt(now);
        cursor.setConflictNote("窗口" + window.size() + "行，冲突" + outcome.getConflicts().size() + "条，均以本局为准");
        cursor.setUpdateBy(operator);
        cursor.setUpdateTime(now);
        if (cursorExisted) {
            syncCursorMapper.updateById(cursor);
        } else {
            if (cursor.getId() == null) {
                cursor.setId(0L);
            }
            syncCursorMapper.insert(cursor);
        }

        outcome.setCursorAfter(maxId);
        outcome.setNote("游标 " + before + " → " + maxId + "；冲突 " + outcome.getConflicts().size()
                + " 条逐条列出，本局行一条未动");
        return outcome;
    }

    // ================================================================ 内部：取数与谓词（唯一一份）

    /** 在册基础行：未删除；当季行加上纸簿旧存量行（quarter 为空），再按岗位辖段裁范围。 */
    private List<TOadsSpot> scopeRows(String quarter, PostScope scope) {
        List<TOadsSpot> rows = new ArrayList<TOadsSpot>();
        if (scope == null || scope.isEmpty()) {
            return rows;
        }
        for (TOadsSpot s : allSpots()) {
            String q = s.getQuarterCode() == null ? null : s.getQuarterCode().trim();
            boolean inQuarter = quarter.equals(q) || q == null;
            boolean inScope = s.getAreaNo() != null && scope.getAreaNos().contains(s.getAreaNo().trim());
            if (inQuarter && inScope) {
                rows.add(s);
            }
        }
        sortRows(rows);
        return rows;
    }

    private List<TOadsSpot> filteredRows(SpotBookQuery query, PostScope scope) {
        String quarter = query == null || query.getQuarterCode() == null || query.getQuarterCode().trim().isEmpty()
                ? currentQuarter() : query.getQuarterCode().trim();
        List<TOadsSpot> rows = scopeRows(quarter, scope);
        if (query == null) {
            return rows;
        }
        Map<String, TOadsAreaBook> areaMap = areaMap();
        List<TOadsSpot> out = new ArrayList<TOadsSpot>();
        for (TOadsSpot r : rows) {
            if (matches(r, query, areaMap)) {
                out.add(r);
            }
        }
        return out;
    }

    /** 三个查询入口（点位编码/路段代号/路段类别），全等与前缀同一谓词；分页、计数、三视图都走它。 */
    private boolean matches(TOadsSpot r, SpotBookQuery q, Map<String, TOadsAreaBook> areaMap) {
        if (!textMatch(r.getSpotNo(), q.getSpotNo(), q.effectiveMatchMode())) {
            return false;
        }
        if (!textMatch(r.getAreaNo(), q.getAreaNo(), q.effectiveMatchMode())) {
            return false;
        }
        if (q.getAreaKind() != null && !q.getAreaKind().trim().isEmpty()) {
            TOadsAreaBook a = r.getAreaNo() == null ? null : areaMap.get(r.getAreaNo());
            String kind = a == null ? null : a.getAreaKind();
            if (kind == null || !textMatch(kind, q.getAreaKind(), q.effectiveMatchMode())) {
                return false;
            }
        }
        return true;
    }

    private boolean textMatch(String value, String input, String mode) {
        if (input == null || input.trim().isEmpty()) {
            return true;
        }
        if (value == null) {
            return false;
        }
        String v = value.trim();
        String k = input.trim();
        return SpotBookQuery.MATCH_PREFIX.equals(mode) ? v.startsWith(k) : v.equals(k);
    }

    private void sortRows(List<TOadsSpot> rows) {
        Collections.sort(rows, new Comparator<TOadsSpot>() {
            @Override
            public int compare(TOadsSpot a, TOadsSpot b) {
                long ai = a.getId() == null ? 0L : a.getId();
                long bi = b.getId() == null ? 0L : b.getId();
                return Long.compare(ai, bi);
            }
        });
    }

    private List<TOadsSpot> allSpots() {
        List<TOadsSpot> list = spotMapper.selectList(new QueryWrapper<TOadsSpot>().eq("del_flag", DEL_OK));
        return list == null ? new ArrayList<TOadsSpot>() : list;
    }

    private Map<String, TOadsAreaBook> areaMap() {
        List<TOadsAreaBook> all = areaBookMapper.selectList(new QueryWrapper<TOadsAreaBook>()
                .eq("del_flag", DEL_OK));
        Map<String, TOadsAreaBook> map = new HashMap<String, TOadsAreaBook>();
        for (TOadsAreaBook a : all) {
            map.put(a.getAreaNo(), a);
        }
        return map;
    }

    private TOadsAreaBook findArea(String areaNo) {
        for (TOadsAreaBook a : areaBookMapper.selectList(new QueryWrapper<TOadsAreaBook>()
                .eq("del_flag", DEL_OK))) {
            if (areaNo.equals(a.getAreaNo())) {
                return a;
            }
        }
        return null;
    }

    private void bump(Map<String, Integer> map, String key) {
        Integer v = map.get(key);
        map.put(key, v == null ? 1 : v + 1);
    }

    // ================================================================ 内部：步序留痕重数

    /**
     * 这一行停在哪一步，只按 append-only 留痕重数：取连续到达的最高一档 REACH。
     * 旧行一条留痕都没有时 step=null（未起步），绝不当成已经压卡。
     */
    private Recount recount(Long spotId) {
        List<TOadsSpotFlowLog> logs = flowLogMapper.listBySpot(spotId);
        Recount rc = new Recount();
        if (logs == null || logs.isEmpty()) {
            return rc;
        }
        boolean reachSeen = false;
        for (int step = SpotFlowStep.PENDING; step <= SpotFlowStep.SEALED; step++) {
            int activeRound = 0;
            boolean reached = false;
            for (TOadsSpotFlowLog l : logs) {
                if (l.getStepNo() == null || l.getStepNo() != step) {
                    continue;
                }
                if (l.getActive() != null && l.getActive() == 1
                        && SpotFlowStep.ACTION_REACH.equals(l.getAction())) {
                    reached = true;
                    reachSeen = true;
                    if (l.getRoundNo() != null && l.getRoundNo() > activeRound) {
                        activeRound = l.getRoundNo();
                    }
                }
                if (l.getActive() != null && l.getActive() == 1
                        && SpotFlowStep.ACTION_HOLD.equals(l.getAction())) {
                    rc.held = true;
                    rc.holdStep = step;
                }
            }
            if (!reached) {
                break;
            }
            rc.step = step;
            rc.rounds.put(step, activeRound);
        }
        if (!reachSeen) {
            rc.step = null;
        }
        return rc;
    }

    private int maxRound(Long spotId, int step) {
        int max = 0;
        for (TOadsSpotFlowLog l : flowLogMapper.listBySpot(spotId)) {
            if (l.getStepNo() != null && l.getStepNo() == step && l.getRoundNo() != null
                    && l.getRoundNo() > max) {
                max = l.getRoundNo();
            }
        }
        return max;
    }

    private int activeRound(Long spotId, int step) {
        Integer r = recount(spotId).rounds.get(step);
        return r == null ? 0 : r;
    }

    private void appendLog(Long spotId, String spotNo, int step, int round, String action,
                           String note, String operator, Date at) {
        TOadsSpotFlowLog l = new TOadsSpotFlowLog();
        l.setSpotId(spotId);
        l.setSpotNo(spotNo);
        l.setStepNo(step);
        l.setRoundNo(round);
        l.setAction(action);
        l.setNote(note);
        l.setOperator(operator);
        l.setActive(SpotFlowStep.ACTION_RETURN.equals(action) ? 0 : 1);
        l.setActedAt(at);
        l.setCreateBy(operator);
        l.setCreateTime(at);
        flowLogMapper.insert(l);
    }

    private static class Recount {
        private Integer step;
        private final Map<Integer, Integer> rounds = new HashMap<Integer, Integer>();
        private boolean held;
        private int holdStep;
    }

    // ================================================================ 内部：行/配置/数量小工具

    private TOadsSpot loadSpot(Long id) {
        TOadsSpot s = spotMapper.selectById(id);
        if (s == null || (s.getDelFlag() != null && s.getDelFlag() != DEL_OK)) {
            throw new SpotRuleException("点位底册上查无此行：" + id);
        }
        return s;
    }

    private TOadsSpot requireSpot(Long id) {
        if (id == null) {
            throw new SpotRuleException("须点出哪一处点位行");
        }
        return loadSpot(id);
    }

    /** 更正/改挂只接"还在走、没停住、没压卡"的行；停在哪一步以留痕重数为准。 */
    private TOadsSpot requireMutable(Long id) {
        TOadsSpot row = requireSpot(id);
        Recount rc = recount(id);
        if (rc.step != null && rc.step == SpotFlowStep.SEALED) {
            throw new SpotRuleException("点位 " + row.getSpotNo() + " 已压卡到头，数量改不动；非动不可只能另开一行");
        }
        if (rc.held) {
            throw new SpotRuleException("点位 " + row.getSpotNo() + " 已就地停住，数量不再改；要动只能另开一行");
        }
        return row;
    }

    /** 仍然有效的在走行：未压卡、未就地停住。旧行（无步序留痕）不算在走，不占同码名额。 */
    private boolean isAlive(TOadsSpot s) {
        Recount rc = recount(s.getId());
        return !(rc.step != null && rc.step == SpotFlowStep.SEALED) && !rc.held;
    }

    private boolean sameQuarter(TOadsSpot s, String quarter) {
        return quarter.equals(s.getQuarterCode() == null ? null : s.getQuarterCode().trim());
    }

    private boolean sameId(TOadsSpot s, Long id) {
        return id != null && id.equals(s.getId());
    }

    private Integer parseNeed(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new SpotRuleException("检查点3数量：核定应设处数缺填，本行进不了库");
        }
        Integer v = SpotForm.parseStrict(text);
        if (v == null) {
            throw new SpotRuleException("检查点3数量：核定应设处数不是整数（" + text.trim() + "），本行进不了库");
        }
        if (v < 0) {
            throw new SpotRuleException("检查点3数量：核定应设处数为负(" + v + ")，本行进不了库");
        }
        return v;
    }

    private Integer parseGot(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        Integer v = SpotForm.parseStrict(text);
        if (v == null) {
            throw new SpotRuleException("检查点3数量：已核验落地处数不是整数（" + text.trim() + "），本行进不了库");
        }
        if (v < 0) {
            throw new SpotRuleException("检查点3数量：已核验落地处数为负(" + v + ")，本行进不了库");
        }
        return v;
    }

    private TOadsBookCfg bookCfg() {
        for (TOadsBookCfg c : bookCfgMapper.selectList(new QueryWrapper<TOadsBookCfg>()
                .eq("del_flag", DEL_OK))) {
            if (CFG_BOOK.equals(c.getCfgKey())) {
                return c;
            }
        }
        return null;
    }

    private TOadsSpotSyncCursor loadCityCursor() {
        for (TOadsSpotSyncCursor c : syncCursorMapper.selectList(new QueryWrapper<TOadsSpotSyncCursor>())) {
            if (CFG_CITY.equals(c.getCfgKey()) && (c.getDelFlag() == null || c.getDelFlag() == DEL_OK)) {
                return c;
            }
        }
        return null;
    }

    private String quarterByDate(Date at) {
        Calendar c = Calendar.getInstance();
        c.setTime(at);
        return c.get(Calendar.YEAR) + "Q" + (c.get(Calendar.MONTH) / 3 + 1);
    }

    private SpotBuildResult resultOf(TOadsSpot row, String message) {
        SpotBuildResult r = SpotBuildResult.updated();
        r.setSpotId(row.getId());
        r.setSpotNo(row.getSpotNo());
        r.setAreaNo(row.getAreaNo());
        r.setAreaName(row.getAreaName());
        r.setNeedNum(row.getNeedNum());
        r.setGotNum(row.getGotNum());
        r.setLackNum(row.getLackNum());
        r.setFlowStep(currentStep(row.getId()));
        r.setBuildKind(row.getBuildKind());
        r.setMessage(message);
        return r;
    }

    private SpotFlowOutcome outcome(TOadsSpot row, String action, int step, int round, boolean held,
                                    boolean frozen) {
        SpotFlowOutcome o = new SpotFlowOutcome();
        o.setSpotId(row.getId());
        o.setSpotNo(row.getSpotNo());
        o.setAction(action);
        o.setFlowStep(step);
        o.setRoundNo(round);
        o.setHeld(held);
        o.setFrozen(frozen);
        return o;
    }
}
