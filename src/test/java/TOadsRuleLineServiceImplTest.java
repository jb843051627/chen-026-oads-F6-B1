import com.fc.v2.mapper.auto.TOadsRuleLineMapper;
import com.fc.v2.model.auto.TOadsRuleLine;
import com.fc.v2.model.custom.SafetyGradeResult;
import com.fc.v2.model.custom.SafetyRuleSnapshot;
import com.fc.v2.service.impl.TOadsRuleLineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 分档判据线服务八条口径逐条核对（mapper 全 Mock，内存账台，不落真库）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TOadsRuleLineServiceImplTest {

    @Mock
    private TOadsRuleLineMapper ruleLineMapper;

    private TOadsRuleLineServiceImpl service;

    private final Map<Long, TOadsRuleLine> rows = new HashMap<Long, TOadsRuleLine>();
    private long seq = 0;

    private static Date at(String s) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
    }

    /** 造一条在 [start,end) 内有效的规则，th 为三定值，priority 先后序，status 0可用 1撤下 */
    private TOadsRuleLine rule(String code, String th1, String th2, String th3,
                               String start, String end, int priority, int status) {
        TOadsRuleLine r = new TOadsRuleLine();
        try {
            r.setId(++seq);
            r.setRuleCode(code);
            if (th1 != null) r.setTh1Max(new BigDecimal(th1));
            if (th2 != null) r.setTh2Max(new BigDecimal(th2));
            if (th3 != null) r.setTh3Max(new BigDecimal(th3));
            r.setEffStart(start == null ? null : at(start));
            r.setEffEnd(end == null ? null : at(end));
            r.setPriority(priority);
            r.setStatus(status);
            r.setDelFlag(0);
            rows.put(r.getId(), r);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return r;
    }

    @BeforeEach
    public void wire() {
        service = new TOadsRuleLineServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "oadsRuleLineMapper", ruleLineMapper);
        when(ruleLineMapper.selectList(any())).thenAnswer(i -> {
            List<TOadsRuleLine> all = new ArrayList<TOadsRuleLine>();
            for (TOadsRuleLine r : rows.values()) {
                if (r.getDelFlag() == null || r.getDelFlag() == 0) {
                    all.add(r);
                }
            }
            return all;
        });
        when(ruleLineMapper.selectById(any())).thenAnswer(i -> rows.get(i.getArgument(0)));
    }

    // ---------- 精度与落档 ----------

    @Test
    public void roundsBeforeComparing() throws Exception {
        rule("R1", "4", "9", "18", "2020-01-01 00:00:00", null, 10, 0);
        // 8.996 进位为 9.00，不再小于够线限值 9.00 → 第3档
        assertEquals(3, service.evaluate("R1", new BigDecimal("8.996"), at("2026-10-05 00:00:00")));
        assertEquals(2, service.evaluate("R1", new BigDecimal("8.994"), at("2026-10-05 00:00:00")));
        assertEquals(1, service.evaluate("R1", new BigDecimal("3"), at("2026-10-05 00:00:00")));
        assertEquals(3, service.evaluate("R1", new BigDecimal("9.00"), at("2026-10-05 00:00:00")),
                "恰好压够线限值，落第3档");
        assertEquals(4, service.evaluate("R1", new BigDecimal("18"), at("2026-10-05 00:00:00")),
                "恰好压封顶限值，落第4档");
    }

    @Test
    public void outOfRangeIsNoLevelAndBorrowingForbidden() throws Exception {
        rule("R1", "4", "9", "18", "2020-01-01 00:00:00", null, 10, 0);
        SafetyGradeResult r1 = service.gradeByCode("R1", new BigDecimal("100.01"), at("2026-10-05 00:00:00"));
        assertEquals(0, r1.getLevel());
        assertNull(r1.getRuleCode());
        assertNull(r1.getEffStart());
        assertTrue(r1.getMessage().contains("量程"));
        assertEquals(0, service.evaluate("R1", new BigDecimal("-0.01"), at("2026-10-05 00:00:00")));
    }

    // ---------- 脏配置点名叫格 ----------

    @Test
    public void dirtyMissingCellIsNamed() throws Exception {
        rule("MISS", "4", null, "18", "2020-01-01 00:00:00", null, 10, 0);
        SafetyRuleSnapshot snap = service.snapshot(at("2026-10-05 00:00:00"));
        assertEquals(0, snap.count());
        assertEquals(1, snap.getDirtyNotes().size());
        assertTrue(snap.getDirtyNotes().get(0).contains("够线限值那一格缺值"),
                "必须点名缺的是哪一格：" + snap.getDirtyNotes());
        SafetyGradeResult g = service.gradeByCode("MISS", new BigDecimal("5"), at("2026-10-05 00:00:00"));
        assertEquals(0, g.getLevel());
        assertTrue(g.getMessage().contains("够线限值"));
    }

    @Test
    public void dirtyNotIncreasingNamesTheCellAndPriorOne() throws Exception {
        rule("EQ", "9", "9", "18", "2020-01-01 00:00:00", null, 10, 0);
        rule("BACK", "4", "20", "18", "2020-01-01 00:00:00", null, 10, 0);
        SafetyRuleSnapshot snap = service.snapshot(at("2026-10-05 00:00:00"));
        assertEquals(0, snap.count());
        String all = snap.getDirtyNotes().toString();
        assertTrue(all.contains("够线限值(9.00)没有大于它的前一格起算限值(9.00)"), all);
        assertTrue(all.contains("封顶限值(18.00)没有大于它的前一格够线限值(20.00)"), all);
    }

    @Test
    public void dirtyCeilingOverHundredExcluded() throws Exception {
        // 100.005 取齐两位为 100.01，大过 100；验证"先取齐再判脏"
        rule("HIGH", "4", "9", "100.005", "2020-01-01 00:00:00", null, 10, 0);
        SafetyRuleSnapshot snap = service.snapshot(at("2026-10-05 00:00:00"));
        assertEquals(0, snap.count());
        assertTrue(snap.getDirtyNotes().get(0).contains("封顶限值(100.01)大过100"), snap.getDirtyNotes().toString());
        // 脏值不许把分算成高档：第4档也不给
        assertEquals(0, service.evaluate("HIGH", new BigDecimal("99"), at("2026-10-05 00:00:00")));
    }

    // ---------- 时间窗：起含、止不含、止空长期有效 ----------

    @Test
    public void windowStartInclusiveEndExclusive() throws Exception {
        rule("W", "4", "9", "18", "2026-01-01 00:00:00", "2026-04-01 00:00:00", 10, 0);
        assertEquals(1, service.evaluate("W", new BigDecimal("3"), at("2026-01-01 00:00:00")),
                "起端含当口");
        assertEquals(0, service.evaluate("W", new BigDecimal("3"), at("2026-04-01 00:00:00")),
                "止端不含：交班当日 00:00:00 已不在窗");
        assertEquals(0, service.evaluate("W", new BigDecimal("3"), at("2025-12-31 23:59:59")),
                "未到起用日");
        assertEquals(2, service.evaluate("W", new BigDecimal("5"), at("2026-03-31 23:59:59")),
                "止端前一刻仍在窗");
    }

    @Test
    public void nullEndMeansForever() throws Exception {
        rule("FOREVER", "4", "9", "18", "2020-01-01 00:00:00", null, 10, 0);
        assertTrue(service.usable(ruleId("FOREVER"), at("2099-01-01 00:00:00")),
                "交班格空着的早年人工规则按长期有效认，兼容分支不能删");
    }

    // ---------- 撤下：不参与、不进清单，但按主键在册 ----------

    @Test
    public void retiredStaysRegisteredByPk() throws Exception {
        TOadsRuleLine old = rule("OLD", "4", "9", "18", "2020-01-01 00:00:00", null, 10, 1);
        assertEquals(0, service.countAvailable(at("2026-10-05 00:00:00")));
        assertEquals(0, service.evaluate("OLD", new BigDecimal("3"), at("2026-10-05 00:00:00")));
        assertNotNull(service.selectTOadsRuleLineById(old.getId()), "撤下的仍是在册一行，按主键查得到");
        assertFalse(service.usable(old.getId(), at("2026-10-05 00:00:00")));
    }

    // ---------- 并列：先后序降序，再比代号字面序（排后者为准） ----------

    @Test
    public void priorityThenCodeDesc() throws Exception {
        rule("A", "4", "9", "18", "2020-01-01 00:00:00", null, 5, 0);
        rule("B", "40", "60", "80", "2020-01-01 00:00:00", null, 8, 0);
        // B 先后序大，先说话：50 分在 A 是第4档、在 B 是第2档
        SafetyGradeResult g = service.gradeTop(new BigDecimal("50"), at("2026-10-05 00:00:00"));
        assertEquals(2, g.getLevel());
        assertEquals("B", g.getRuleCode());

        rule("C", "10", "20", "30", "2020-01-01 00:00:00", null, 8, 0);
        // B、C 先后序并列：代号字面序排在后面的 C 为准；50 在 C 是第4档
        SafetyGradeResult g2 = service.gradeTop(new BigDecimal("50"), at("2026-10-05 00:00:00"));
        assertEquals("C", g2.getRuleCode());
        assertEquals(4, g2.getLevel());
    }

    // ---------- 历史版本：按询问当时生效的版本判 ----------

    @Test
    public void historicalAskUsesVersionEffectiveThen() throws Exception {
        // 旧版 [2024-01-01, 2025-01-01)，阈值 1/2/3；新版 [2025-01-01, 止空)，阈值 4/9/18
        rule("V", "1", "2", "3", "2024-01-01 00:00:00", "2025-01-01 00:00:00", 10, 0);
        rule("V", "4", "9", "18", "2025-01-01 00:00:00", null, 10, 0);
        // 追补 2024 年那批：2.5 在旧版落第3档；拿新版判（起算限值4）会落第1档——那种错位不得再现
        assertEquals(3, service.evaluate("V", new BigDecimal("2.5"), at("2024-06-01 00:00:00")));
        assertEquals(1, service.evaluate("V", new BigDecimal("2.5"), at("2026-10-05 00:00:00")));
    }

    @Test
    public void codeWithoutEffectiveVersionIsNoLevelWithReason() throws Exception {
        rule("NOW", "4", "9", "18", "2025-01-01 00:00:00", null, 10, 0);
        SafetyGradeResult g = service.gradeByCode("GHOST", new BigDecimal("5"), at("2026-10-05 00:00:00"));
        assertEquals(0, g.getLevel());
        assertNull(g.getRuleCode());
        assertTrue(g.getMessage().contains("GHOST"));
        assertTrue(g.getMessage().contains("取不到生效版本"));
    }

    // ---------- 同源：清单/条数/逐条同一锅；两入口同档 ----------

    @Test
    public void snapshotCountMatchesList() throws Exception {
        rule("A", "4", "9", "18", "2020-01-01 00:00:00", null, 5, 0);
        rule("B", "4", "9", "18", "2020-01-01 00:00:00", null, 8, 0);
        rule("X", null, null, null, "2020-01-01 00:00:00", null, 9, 0);
        Date at = at("2026-10-05 00:00:00");
        SafetyRuleSnapshot snap = service.snapshot(at);
        assertEquals(snap.getAvailable().size(), service.countAvailable(at));
        assertEquals(2, snap.count());
        // 逐条判定就喂这同一份快照
        SafetyGradeResult byCode = service.grade(snap, "A", new BigDecimal("5"));
        SafetyGradeResult top = service.grade(snap, null, new BigDecimal("5"));
        // 两条阈值相同（4/9/18），同一处点位两入口必须同一档
        assertEquals(byCode.getLevel(), top.getLevel());
        assertEquals(2, byCode.getLevel());
    }

    @Test
    public void noAvailableRuleGivesReasonAndEmptyPair() throws Exception {
        SafetyGradeResult g = service.gradeTop(new BigDecimal("5"), at("2026-10-05 00:00:00"));
        assertEquals(0, g.getLevel());
        assertNull(g.getRuleCode());
        assertNull(g.getEffStart());
        assertTrue(g.getMessage().contains("没有可用"));
    }

    private Long ruleId(String code) {
        for (TOadsRuleLine r : rows.values()) {
            if (code.equals(r.getRuleCode())) {
                return r.getId();
            }
        }
        return null;
    }
}
