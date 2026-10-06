import com.baomidou.mybatisplus.core.conditions.Wrapper;
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
import com.fc.v2.common.exception.spot.SpotRuleException;
import com.fc.v2.service.impl.TOadsSpotRegisterServiceImpl;
import com.fc.v2.service.spot.SpotFlowStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 点位名册五检查点口径核对：固定先后与各自提示语、待补减法与三时刻重算、三档不跨档/
 * 退回新旧轮分离/压卡冻结、同码在走独占与释放、纸簿旧行兼容、三视图同一基础数据、
 * 空视图不补齐、市增量游标同次推进与冲突逐条。mapper 全 Mock，内存账台，不落真库。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TOadsSpotRegisterServiceTest {

    @Mock private TOadsSpotMapper spotMapper;
    @Mock private TOadsAreaBookMapper areaBookMapper;
    @Mock private TOadsSpotFlowLogMapper flowLogMapper;
    @Mock private TOadsBookCfgMapper bookCfgMapper;
    @Mock private TOadsPostAreaMapper postAreaMapper;
    @Mock private TOadsSpotSyncCursorMapper syncCursorMapper;

    private TOadsSpotRegisterServiceImpl svc;

    private final Map<Long, TOadsSpot> spots = new LinkedHashMap<Long, TOadsSpot>();
    private final Map<String, TOadsAreaBook> areas = new LinkedHashMap<String, TOadsAreaBook>();
    private final List<TOadsSpotFlowLog> logs = new ArrayList<TOadsSpotFlowLog>();
    private final List<TOadsPostArea> postLinks = new ArrayList<TOadsPostArea>();
    private TOadsBookCfg bookCfg;
    private TOadsSpotSyncCursor cityCursor;
    private long seq = 100;

    private static final String QUARTER = "2026Q4";
    private static final String FENCE_A = "100.000000,20.000000;101.000000,21.000000";
    private static final String FENCE_B = "110.000000,30.000000;111.000000,31.000000";

    private TOadsAreaBook area(long id, String no, String name, String kind, int status, String fence) {
        TOadsAreaBook a = new TOadsAreaBook();
        a.setId(id);
        a.setAreaNo(no);
        a.setAreaName(name);
        a.setAreaKind(kind);
        a.setStatus(status);
        a.setFenceText(fence);
        a.setDelFlag(0);
        areas.put(no, a);
        return a;
    }

    private SpotForm form(String no, String areaNo, String lng, String lat, String need, String got) {
        SpotForm f = new SpotForm();
        f.setSpotNo(no);
        f.setAreaNo(areaNo);
        f.setLongitude(lng == null ? null : new BigDecimal(lng));
        f.setLatitude(lat == null ? null : new BigDecimal(lat));
        f.setNeedText(need);
        f.setGotText(got);
        return f;
    }

    private SpotRuleException expectFail(Runnable r) {
        SpotRuleException e = assertThrows(SpotRuleException.class, r::run);
        assertNotNull(e.getMessage());
        assertFalse(e.getMessage().contains("数据有误"), "不许合并成一句数据有误：" + e.getMessage());
        return e;
    }

    @BeforeEach
    public void wire() {
        area(1, "DU00", "临江路", "主干道", 0, FENCE_A);
        area(2, "DU01", "北岸旧段", "次干道", 1, "102.000000,22.000000;103.000000,23.000000");
        area(3, "DU02", "青石巷", "背街巷", 0, FENCE_B);

        bookCfg = new TOadsBookCfg();
        bookCfg.setId(0L);
        bookCfg.setCfgKey("book");
        bookCfg.setPageSize(2);
        bookCfg.setQuarterCode(QUARTER);
        bookCfg.setDelFlag(0);

        // 本段经办岗只见 DU00；区局岗 DU00+DU02 三类（含背街巷）都可见
        postLink("P-A", PostScope.SECTION, "DU00");
        postLink("P-B", PostScope.BUREAU, "DU00");
        postLink("P-B", PostScope.BUREAU, "DU02");

        svc = new TOadsSpotRegisterServiceImpl();
        ReflectionTestUtils.setField(svc, "spotMapper", spotMapper);
        ReflectionTestUtils.setField(svc, "areaBookMapper", areaBookMapper);
        ReflectionTestUtils.setField(svc, "flowLogMapper", flowLogMapper);
        ReflectionTestUtils.setField(svc, "bookCfgMapper", bookCfgMapper);
        ReflectionTestUtils.setField(svc, "postAreaMapper", postAreaMapper);
        ReflectionTestUtils.setField(svc, "syncCursorMapper", syncCursorMapper);

        when(spotMapper.selectList(any())).thenAnswer(i -> {
            List<TOadsSpot> l = new ArrayList<TOadsSpot>();
            for (TOadsSpot s : spots.values()) {
                if (s.getDelFlag() == null || s.getDelFlag() == 0) {
                    l.add(s);
                }
            }
            return l;
        });
        when(spotMapper.selectById(any())).thenAnswer(i -> spots.get(i.getArgument(0)));
        when(spotMapper.insert(any(TOadsSpot.class))).thenAnswer(i -> {
            TOadsSpot s = i.getArgument(0);
            if (s.getId() == null) {
                s.setId(++seq);
            }
            spots.put(s.getId(), s);
            return 1;
        });
        when(spotMapper.updateById(any(TOadsSpot.class))).thenAnswer(i -> {
            TOadsSpot s = i.getArgument(0);
            spots.put(s.getId(), s);
            return 1;
        });
        when(areaBookMapper.selectList(any())).thenAnswer(i -> new ArrayList<TOadsAreaBook>(areas.values()));
        when(flowLogMapper.listBySpot(any())).thenAnswer(i -> {
            Long id = i.getArgument(0);
            List<TOadsSpotFlowLog> l = new ArrayList<TOadsSpotFlowLog>();
            for (TOadsSpotFlowLog l0 : logs) {
                if (id.equals(l0.getSpotId())) {
                    l.add(l0);
                }
            }
            return l;
        });
        when(flowLogMapper.insert(any(TOadsSpotFlowLog.class))).thenAnswer(i -> {
            TOadsSpotFlowLog l = i.getArgument(0);
            l.setId(++seq);
            logs.add(l);
            return 1;
        });
        when(flowLogMapper.deactivateRound(any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt())).thenAnswer(i -> {
            Long id = i.getArgument(0);
            int step = i.getArgument(1);
            int round = i.getArgument(2);
            int n = 0;
            for (TOadsSpotFlowLog l : logs) {
                if (id.equals(l.getSpotId()) && l.getStepNo() == step && l.getRoundNo() == round
                        && l.getActive() != null && l.getActive() == 1) {
                    l.setActive(0);
                    n++;
                }
            }
            return n;
        });
        when(bookCfgMapper.selectList(any())).thenAnswer(i -> {
            List<TOadsBookCfg> l = new ArrayList<TOadsBookCfg>();
            if (bookCfg != null) {
                l.add(bookCfg);
            }
            return l;
        });
        when(bookCfgMapper.insert(any(TOadsBookCfg.class))).thenAnswer(i -> {
            bookCfg = i.getArgument(0);
            return 1;
        });
        when(bookCfgMapper.updateById(any(TOadsBookCfg.class))).thenAnswer(i -> {
            bookCfg = i.getArgument(0);
            return 1;
        });
        when(postAreaMapper.selectList(any())).thenAnswer(i -> {
            // 按 wrapper 里出现的岗位代号过滤（内存账台模拟 eq post_no）
            com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?> w = i.getArgument(0);
            w.getSqlSegment(); // 触发参数落进 paramNameValuePairs
            Object want = null;
            for (Object v : w.getParamNameValuePairs().values()) {
                if (v instanceof String) {
                    want = v;
                }
            }
            List<TOadsPostArea> l = new ArrayList<TOadsPostArea>();
            for (TOadsPostArea p : postLinks) {
                if (want == null || want.equals(p.getPostNo())) {
                    l.add(p);
                }
            }
            return l;
        });
        when(syncCursorMapper.selectList(any())).thenAnswer(i -> {
            List<TOadsSpotSyncCursor> l = new ArrayList<TOadsSpotSyncCursor>();
            if (cityCursor != null) {
                l.add(cityCursor);
            }
            return l;
        });
        when(syncCursorMapper.selectById(any())).thenAnswer(i -> cityCursor);
        when(syncCursorMapper.insert(any(TOadsSpotSyncCursor.class))).thenAnswer(i -> {
            cityCursor = i.getArgument(0);
            return 1;
        });
        when(syncCursorMapper.updateById(any(TOadsSpotSyncCursor.class))).thenAnswer(i -> {
            cityCursor = i.getArgument(0);
            return 1;
        });
    }

    private void postLink(String postNo, String kind, String areaNo) {
        TOadsPostArea p = new TOadsPostArea();
        p.setId(++seq);
        p.setPostNo(postNo);
        p.setPostKind(kind);
        p.setAreaNo(areaNo);
        p.setDelFlag(0);
        postLinks.add(p);
    }

    private TOadsSpot findSpot(String no) {
        for (TOadsSpot s : spots.values()) {
            if (no.equals(s.getSpotNo())) {
                return s;
            }
        }
        return null;
    }

    private SpotBuildResult build(String no, String areaNo, String lng, String lat, String need, String got) {
        return svc.build(form(no, areaNo, lng, lat, need, got), SpotFlowStep.BUILD_MANUAL, "建档岗甲");
    }

    private SpotBuildResult seal(String no) {
        Long id = findSpot(no).getId();
        svc.advance(id, "核验过", "岗");
        svc.advance(id, "压卡", "岗");
        return null;
    }

    // ---------------------------------------------------------------- 检查点固定先后与各自提示语

    @Test
    public void checkpoint1IdentityMessages() {
        SpotRuleException e1 = expectFail(() ->
                svc.build(form("  ", "DU00", "100.5", "20.5", "1", "0"), SpotFlowStep.BUILD_MANUAL, "岗"));
        assertTrue(e1.getMessage().startsWith("检查点1身份"));

        build("S1", "DU00", "100.5", "20.5", "10", "2");
        SpotRuleException e2 = expectFail(() ->
                build("S1", "DU00", "100.5", "20.5", "10", "2"));
        assertTrue(e2.getMessage().startsWith("检查点1身份"), e2.getMessage());
        assertTrue(e2.getMessage().contains("第二行立不起来"));

        // 对象就是同季同码那一行本身：按更新，不挡
        SpotForm again = form("S1", "DU00", "100.6", "20.6", "10", "3");
        again.setId(findSpot("S1").getId());
        SpotBuildResult r = svc.build(again, SpotFlowStep.BUILD_MANUAL, "岗");
        assertEquals("UPDATED", r.getAction());
        assertEquals(1, spots.size());
    }

    @Test
    public void checkpoint2FenceMessages() {
        // 缺坐标：检查点2给话（即便路段代号目录里没有，顺序上也先到空间挂靠这一关报缺坐标）
        SpotRuleException e1 = expectFail(() ->
                build("S2", "NOPE", null, null, "1", "0"));
        assertTrue(e1.getMessage().startsWith("检查点2空间挂靠"), e1.getMessage());

        // 围栏之外：提示写明该点位在围栏之外
        SpotRuleException e2 = expectFail(() ->
                build("S2", "DU00", "99.0", "99.0", "1", "0"));
        assertTrue(e2.getMessage().startsWith("检查点2空间挂靠"));
        assertTrue(e2.getMessage().contains("围栏之外"));

        // 角点为闭区间：边界点收
        SpotBuildResult ok = build("S2", "DU00", "100.000000", "20.000000", "1", "0");
        assertNotNull(ok.getSpotId());
    }

    @Test
    public void checkpoint3QuantityMessagesNameFields() {
        SpotRuleException e1 = expectFail(() -> build("S3", "DU00", "100.5", "20.5", null, "1"));
        assertTrue(e1.getMessage().contains("核定应设处数缺填"), e1.getMessage());

        SpotRuleException e2 = expectFail(() -> build("S3", "DU00", "100.5", "20.5", "12.5", "1"));
        assertTrue(e2.getMessage().contains("核定应设处数不是整数"));

        SpotRuleException e3 = expectFail(() -> build("S3", "DU00", "100.5", "20.5", "-3", "1"));
        assertTrue(e3.getMessage().contains("核定应设处数为负"));

        // 已核验不是整数同样点名
        SpotRuleException e4 = expectFail(() -> build("S3", "DU00", "100.5", "20.5", "10", "2.0"));
        assertTrue(e4.getMessage().contains("已核验落地处数不是整数"), e4.getMessage());

        SpotRuleException e5 = expectFail(() -> build("S3", "DU00", "100.5", "20.5", "5", "8"));
        assertTrue(e5.getMessage().startsWith("检查点3数量"));
        assertTrue(e5.getMessage().contains("核定应设处数"));
        assertTrue(e5.getMessage().contains("已核验落地处数"));
    }

    @Test
    public void checkpoint4DirectoryMessages() {
        // 坐标在场但代号目录找不到：前三关过，第四关点名
        SpotRuleException e1 = expectFail(() -> build("S4", "GHOST", "100.5", "20.5", "1", "0"));
        assertTrue(e1.getMessage().startsWith("检查点4名录依据"), e1.getMessage());
        assertTrue(e1.getMessage().contains("在目录里找不到"));

        // 注销段不接新增（坐标在该旧段围栏内，过了空间关）
        SpotRuleException e2 = expectFail(() -> build("S4", "DU01", "102.5", "22.5", "1", "0"));
        assertTrue(e2.getMessage().startsWith("检查点4名录依据"));
        assertTrue(e2.getMessage().contains("已注销"));
    }

    @Test
    public void checkpointOrderIsStrict() {
        // 同时有身份毛病与围栏毛病：只报先一关（身份）
        SpotRuleException e = expectFail(() -> build("  ", "DU00", "1.0", "1.0", "x", "z"));
        assertTrue(e.getMessage().startsWith("检查点1身份"));
    }

    // ---------------------------------------------------------------- 检查点3：待补减法与三时刻重算

    @Test
    public void lackSubtractionExamples() {
        SpotBuildResult a = build("Q1", "DU00", "100.5", "20.5", "30", "12");
        assertEquals(18, a.getLackNum().intValue());
        assertEquals(18, findSpot("Q1").getLackNum().intValue());

        SpotBuildResult b = build("Q2", "DU00", "100.5", "20.6", "20", "7");
        assertEquals(13, b.getLackNum().intValue());

        SpotBuildResult c = build("Q3", "DU00", "100.5", "20.7", "9", "9");
        assertEquals(0, c.getLackNum().intValue());
    }

    @Test
    public void lackRecomputedOnCorrectAndRehang() {
        build("R1", "DU00", "100.5", "20.5", "30", "12");
        // 时刻二：已核验处数被更正——覆盖上一次落库的值
        SpotBuildResult c1 = svc.correctGot(findSpot("R1").getId(), 20, "岗");
        assertEquals(10, c1.getLackNum().intValue());
        assertEquals(10, findSpot("R1").getLackNum().intValue());
        // 更正到高于核定：拒
        expectFail(() -> svc.correctGot(findSpot("R1").getId(), 31, "岗"));

        // 时刻三：改挂到别的路段目录（DU02 背街巷），重算并覆盖
        SpotBuildResult c2 = svc.rehang(findSpot("R1").getId(), "DU02", new BigDecimal("110.5"),
                new BigDecimal("30.5"), "岗");
        assertEquals("DU02", c2.getAreaNo());
        assertEquals("青石巷", c2.getAreaName());
        assertEquals(10, c2.getLackNum().intValue());
        // 改挂到注销段：拒
        expectFail(() -> svc.rehang(findSpot("R1").getId(), "DU01", new BigDecimal("102.5"),
                new BigDecimal("22.5"), "岗"));
    }

    // ---------------------------------------------------------------- 三档流转：不跨档、退回留痕、压卡冻结

    @Test
    public void flowStepsNoSkipAndSystemCounts() {
        build("F1", "DU00", "100.5", "20.5", "5", "1");
        Long id = findSpot("F1").getId();
        assertEquals(SpotFlowStep.PENDING, svc.currentStep(id).intValue());

        // 从待核直接填报压卡=跨档，拒；屏上填的档不算数
        SpotRuleException e = expectFail(() -> svc.advance(id, "跳", "岗", SpotFlowStep.SEALED));
        assertTrue(e.getMessage().contains("步子不得跨"));
        assertEquals(SpotFlowStep.PENDING, svc.currentStep(id).intValue());

        SpotFlowOutcome o1 = svc.advance(id, "过", "岗");
        assertEquals(SpotFlowStep.VERIFIED, o1.getFlowStep().intValue());
        // 屏填待核（落后一档）也不作数
        expectFail(() -> svc.advance(id, "乱填", "岗", SpotFlowStep.PENDING));

        SpotFlowOutcome o2 = svc.advance(id, "到头", "岗");
        assertEquals(SpotFlowStep.SEALED, o2.getFlowStep().intValue());
        assertTrue(o2.isFrozen());

        // 压卡后：推不动、退不回、改不动、撤不下
        expectFail(() -> svc.advance(id, "x", "岗"));
        expectFail(() -> svc.returnStep(id, "x", "岗"));
        expectFail(() -> svc.hold(id, "x", "岗"));
        expectFail(() -> svc.correctGot(id, 1, "岗"));
        expectFail(() -> svc.rehang(id, "DU02", new BigDecimal("110.5"), new BigDecimal("30.5"), "岗"));
        SpotForm upd = form("F1", "DU00", "100.5", "20.5", "6", "1");
        upd.setId(id);
        expectFail(() -> svc.build(upd, SpotFlowStep.BUILD_MANUAL, "岗"));
    }

    @Test
    public void returnKeepsPriorRoundSeparate() {
        build("F2", "DU00", "100.5", "20.5", "5", "1");
        Long id = findSpot("F2").getId();
        svc.advance(id, "第一回核验记下的东西", "甲");
        assertEquals(2, svc.currentStep(id).intValue());

        SpotFlowOutcome back = svc.returnStep(id, "退回复核", "甲");
        assertEquals(SpotFlowStep.PENDING, back.getFlowStep().intValue());

        // 上一轮已核的 REACH 退出在档但不删行
        int oldReach = 0;
        int returnRows = 0;
        for (TOadsSpotFlowLog l : logs) {
            if (id.equals(l.getSpotId()) && l.getStepNo() == SpotFlowStep.VERIFIED
                    && SpotFlowStep.ACTION_REACH.equals(l.getAction())) {
                oldReach++;
                assertEquals(0, l.getActive().intValue(), "老轮文本保留但退出在档");
                assertEquals("第一回核验记下的东西", l.getNote());
            }
            if (id.equals(l.getSpotId()) && SpotFlowStep.ACTION_RETURN.equals(l.getAction())) {
                returnRows++;
            }
        }
        assertEquals(1, oldReach);
        assertEquals(1, returnRows);

        // 重走：新一轮，不与上一回排在一起（待核仍在，再推一步即重到已核，起第2轮）
        SpotFlowOutcome again = svc.advance(id, "后补的那笔", "甲");
        assertEquals(SpotFlowStep.VERIFIED, again.getFlowStep().intValue());
        assertEquals(2, again.getRoundNo().intValue());
        int activeVerifiedReach = 0;
        for (TOadsSpotFlowLog l : logs) {
            if (id.equals(l.getSpotId()) && l.getStepNo() == SpotFlowStep.VERIFIED
                    && SpotFlowStep.ACTION_REACH.equals(l.getAction()) && l.getActive() == 1) {
                activeVerifiedReach++;
                assertEquals("后补的那笔", l.getNote());
            }
        }
        assertEquals(1, activeVerifiedReach);

        // 待核档退无可退
        svc.returnStep(id, "再退", "甲");
        expectFail(() -> svc.returnStep(id, "退无可退", "甲"));
    }

    // ---------------------------------------------------------------- 同码在走独占：压卡/停住才释放

    @Test
    public void sameSpotOccupancyReleasedBySealOrHold() {
        build("O1", "DU00", "100.5", "20.5", "5", "1");
        expectFail(() -> build("O1", "DU00", "100.6", "20.6", "5", "1"));

        // 压卡后名额空出，同点位接得上新行
        seal("O1");
        SpotBuildResult second = build("O1", "DU00", "100.7", "20.7", "5", "1");
        assertEquals("CREATED", second.getAction());
        assertEquals(2, spots.size());

        // 停住同样释放
        build("O2", "DU00", "100.8", "20.8", "5", "1");
        svc.hold(findSpot("O2").getId(), "办不动先停", "岗");
        SpotBuildResult other = build("O2", "DU00", "100.9", "20.9", "5", "1");
        assertEquals("CREATED", other.getAction());
        // 停住行不再接改动
        final Long heldId = spots.values().stream()
                .filter(s -> "O2".equals(s.getSpotNo()) && !s.getId().equals(other.getSpotId()))
                .findFirst().get().getId();
        expectFail(() -> svc.correctGot(heldId, 2, "岗"));
    }

    // ---------------------------------------------------------------- 纸簿旧行兼容

    @Test
    public void legacyPaperRowsToleratedAndCountedButNotSealed() {
        TOadsSpot legacy = new TOadsSpot();
        legacy.setId(700L);
        legacy.setSpotNo(null);
        legacy.setAreaNo("DU00");
        legacy.setNeedNum(null);
        legacy.setGotNum(null);
        legacy.setLackNum(null);
        legacy.setQuarterCode(null);
        legacy.setFlowStep(null);
        legacy.setDelFlag(0);
        spots.put(700L, legacy);

        // 不碰不报错；步数空着绝不当已压卡
        assertNull(svc.currentStep(700L));

        // 旧行进流程不炸：第一步落待核
        SpotFlowOutcome o = svc.advance(700L, "补进流程", "岗");
        assertEquals(SpotFlowStep.PENDING, o.getFlowStep().intValue());

        // 旧行在三视图按 LEGACY 计得进、在名册查得到
        PostScope scope = svc.scopeOf("P-A");
        SpotViewCounts vc = svc.viewCounts(QUARTER, scope);
        assertEquals(1, vc.getTotal());
        assertEquals(1, vc.getByBuildKind().get("LEGACY").intValue());
        assertEquals(1, svc.pageBook(new SpotBookQuery(), scope, 1, 10).size());

        // 旧行（码空）不占同码名额
        SpotBuildResult fresh = build("NEW1", "DU00", "100.5", "20.5", "3", "1");
        assertEquals("CREATED", fresh.getAction());
    }

    @Test
    public void legacyHoldLandsAtPending() {
        TOadsSpot legacy = new TOadsSpot();
        legacy.setId(701L);
        legacy.setSpotNo("OLD2");
        legacy.setAreaNo("DU00");
        legacy.setQuarterCode(null);
        legacy.setFlowStep(null);
        legacy.setDelFlag(0);
        spots.put(701L, legacy);
        SpotFlowOutcome o = svc.hold(701L, "旧行停住", "岗");
        assertTrue(o.isHeld());
        assertEquals(SpotFlowStep.PENDING, o.getFlowStep().intValue());
        // 停住释放名额：同码本季可另立新行
        assertEquals("CREATED", build("OLD2", "DU00", "100.5", "20.5", "2", "0").getAction());
    }

    // ---------------------------------------------------------------- 检查点5：三视图一致、scope、查询、分页

    @Test
    public void threeViewsShareOneBaseAndEmptyIsLegal() {
        build("V1", "DU00", "100.5", "20.5", "10", "2");
        build("V2", "DU00", "100.6", "20.6", "10", "2");
        SpotBuildResult b = svc.build(form("V3", "DU02", "110.5", "30.5", "10", "2"),
                SpotFlowStep.BUILD_BATCH, "集中更换");
        assertEquals("BATCH", b.getBuildKind());

        PostScope bureau = svc.scopeOf("P-B");
        SpotViewCounts vc = svc.viewCounts(QUARTER, bureau);
        int sKind = sum(vc.getByAreaKind());
        int sNo = sum(vc.getByAreaNo());
        int sBuild = sum(vc.getByBuildKind());
        assertEquals(3, vc.getTotal());
        assertEquals(3, sKind);
        assertEquals(3, sNo);
        assertEquals(3, sBuild);
        assertEquals(2, vc.getByAreaKind().get("主干道").intValue());
        assertEquals(1, vc.getByAreaKind().get("背街巷").intValue());
        assertEquals(1, vc.getByBuildKind().get("BATCH").intValue());
        assertEquals(2, vc.getByBuildKind().get("MANUAL").intValue());
        // 数据库数一次与分页同源
        assertEquals(3, svc.countBook(new SpotBookQuery(), bureau));

        // 本段经办岗只见本段：不拿邻段补齐
        PostScope section = svc.scopeOf("P-A");
        SpotViewCounts vc2 = svc.viewCounts(QUARTER, section);
        assertEquals(2, vc2.getTotal());
        assertNull(vc2.getByAreaKind().get("背街巷"));

        // 一个辖段都没划给的岗：空是合法结果
        PostScope none = svc.scopeOf("P-NONE");
        SpotViewCounts vcEmpty = svc.viewCounts(QUARTER, none);
        assertEquals(0, vcEmpty.getTotal());
        assertTrue(vcEmpty.getByAreaKind().isEmpty());
        assertEquals(0, svc.pageBook(new SpotBookQuery(), none, 1, 10).size());
    }

    @Test
    public void queryEntriesEqAndPrefix() {
        build("AB-001", "DU00", "100.5", "20.5", "1", "0");
        build("AB-002", "DU00", "100.6", "20.6", "1", "0");
        build("XC-009", "DU00", "100.7", "20.7", "1", "0");
        PostScope scope = svc.scopeOf("P-B");

        SpotBookQuery eq = new SpotBookQuery();
        eq.setSpotNo("AB-001");
        assertEquals(1, svc.countBook(eq, scope));

        SpotBookQuery prefix = new SpotBookQuery();
        prefix.setSpotNo("AB-");
        prefix.setMatchMode("prefix");
        assertEquals(2, svc.countBook(prefix, scope));

        SpotBookQuery areaPrefix = new SpotBookQuery();
        areaPrefix.setAreaNo("DU0");
        areaPrefix.setMatchMode("prefix");
        assertEquals(3, svc.countBook(areaPrefix, scope));

        SpotBookQuery kindEq = new SpotBookQuery();
        kindEq.setAreaKind("主干道");
        assertEquals(3, svc.countBook(kindEq, scope));

        SpotBookQuery kindPrefix = new SpotBookQuery();
        kindPrefix.setAreaKind("主");
        kindPrefix.setMatchMode("prefix");
        assertEquals(3, svc.countBook(kindPrefix, scope));

        // 凡入库的行，在对应视图都可查
        assertEquals(3, svc.pageBook(new SpotBookQuery(), scope, 1, 50).size());
    }

    @Test
    public void paginationUsesConfiguredPageSize() {
        build("P1", "DU00", "100.1", "20.1", "1", "0");
        build("P2", "DU00", "100.2", "20.2", "1", "0");
        build("P3", "DU00", "100.3", "20.3", "1", "0");
        assertEquals(2, bookCfg.getPageSize().intValue());
        PostScope scope = svc.scopeOf("P-B");
        assertEquals(2, svc.pageBook(new SpotBookQuery(), scope, 1, svc.pageSize()).size());
        assertEquals(1, svc.pageBook(new SpotBookQuery(), scope, 2, svc.pageSize()).size());
        svc.configurePageSize(50, "区局");
        assertEquals(50, svc.pageSize());
        expectFail(() -> svc.configurePageSize(0, "区局"));
    }

    // ---------------------------------------------------------------- 检查点5：市增量、游标、冲突清单

    @Test
    public void citySyncCursorAndConflicts() {
        build("C1", "DU00", "100.5", "20.5", "1", "0");
        build("C2", "DU00", "100.6", "20.6", "1", "0");
        long maxLocal = Math.max(findSpot("C1").getId(), findSpot("C2").getId());

        CitySpot nameConflict = city("C1", "DU00", "市里瞎起的名", false);
        CitySpot revoked = city("C2", "DU00", "临江路", true);
        CitySyncOutcome o1 = svc.syncCity(java.util.Arrays.asList(nameConflict, revoked), "区局");
        assertEquals(0, o1.getCursorBefore());
        assertEquals(maxLocal, o1.getCursorAfter());
        assertEquals(2, o1.getConflicts().size());
        // 游标与结果同次推进
        assertEquals(maxLocal, cityCursor.getCursorId().longValue());
        // 本局行一条未动（以本局为准，不静默覆盖）
        assertEquals("临江路", findSpot("C1").getAreaName());
        assertTrue(o1.getConflicts().stream().anyMatch(c -> CitySyncOutcome.NAME_CONFLICT.equals(c.getKind())));
        assertTrue(o1.getConflicts().stream().anyMatch(c -> CitySyncOutcome.CITY_REVOKED.equals(c.getKind())));

        // 第二次同步：游标之前的不复核——旧行即便仍冲突也不再进清单
        build("C3", "DU00", "100.7", "20.7", "1", "0");
        long c3Id = findSpot("C3").getId();
        CitySyncOutcome o2 = svc.syncCity(java.util.Arrays.asList(
                city("C1", "DU00", "还是错名", false),
                city("C3", "DU00", "又一个错名", false)), "区局");
        assertEquals(maxLocal, o2.getCursorBefore());
        assertEquals(1, o2.getWindowRows());
        assertEquals(1, o2.getConflicts().size());
        assertEquals("C3", o2.getConflicts().get(0).getSpotNo());
        assertEquals(c3Id, o2.getCursorAfter());
    }

    private CitySpot city(String no, String areaNo, String name, boolean revoked) {
        CitySpot c = new CitySpot();
        c.setSpotNo(no);
        c.setAreaNo(areaNo);
        c.setAreaName(name);
        c.setRevoked(revoked);
        return c;
    }

    private int sum(Map<String, Integer> m) {
        int n = 0;
        for (Integer v : m.values()) {
            n += v == null ? 0 : v;
        }
        return n;
    }
}
