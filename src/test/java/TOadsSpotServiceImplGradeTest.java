import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.mapper.auto.TOadsRuleLineMapper;
import com.fc.v2.mapper.auto.TOadsSpotGradeLogMapper;
import com.fc.v2.mapper.auto.TOadsSpotMapper;
import com.fc.v2.model.auto.TOadsRuleLine;
import com.fc.v2.model.auto.TOadsSpot;
import com.fc.v2.model.auto.TOadsSpotGradeLog;
import com.fc.v2.model.custom.SafetyGradeResult;
import com.fc.v2.model.custom.SpotDangling;
import com.fc.v2.model.custom.SpotGradeQuery;
import com.fc.v2.model.custom.SpotGradeOutcome;
import com.fc.v2.service.impl.TOadsRuleLineServiceImpl;
import com.fc.v2.service.impl.TOadsSpotServiceImpl;
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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 点位分档固化口径核对：先依据后结论、固化+留痕同一趟且逐字同对、
 * 不分档留空、悬空清单、两入口同档、通用修改入口抹不动固化三列。
 * mapper 全 Mock，内存账台，不落真库（事务边界由 Spring @Transactional 保证，此处验调用与取值）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TOadsSpotServiceImplGradeTest {

    @Mock
    private TOadsSpotMapper spotMapper;
    @Mock
    private TOadsRuleLineMapper ruleLineMapper;
    @Mock
    private TOadsSpotGradeLogMapper gradeLogMapper;

    private TOadsSpotServiceImpl spotService;
    private TOadsRuleLineServiceImpl ruleService;

    private final Map<Long, TOadsSpot> spots = new HashMap<Long, TOadsSpot>();
    private final List<TOadsRuleLine> rules = new ArrayList<TOadsRuleLine>();
    private final List<TOadsSpotGradeLog> logs = new ArrayList<TOadsSpotGradeLog>();
    private Map<String, Object> lastSet;
    private long ruleSeq = 0;

    private static Date at(String s) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
    }

    private TOadsSpot spot(long id, String no, String ruleCode) {
        TOadsSpot s = new TOadsSpot();
        s.setId(id);
        s.setSpotNo(no);
        s.setRuleCode(ruleCode);
        s.setDelFlag(0);
        spots.put(id, s);
        return s;
    }

    private TOadsRuleLine rule(String code, String t1, String t2, String t3,
                               String start, String end, int priority, int status) throws Exception {
        TOadsRuleLine r = new TOadsRuleLine();
        r.setId(++ruleSeq);
        r.setRuleCode(code);
        r.setTh1Max(new BigDecimal(t1));
        r.setTh2Max(new BigDecimal(t2));
        r.setTh3Max(new BigDecimal(t3));
        r.setEffStart(start == null ? null : at(start));
        r.setEffEnd(end == null ? null : at(end));
        r.setPriority(priority);
        r.setStatus(status);
        r.setDelFlag(0);
        rules.add(r);
        return r;
    }

    /** 从 UpdateWrapper 的 SET 子句解出列->值（MP 3.4 参数占位写法）。 */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractSet(UpdateWrapper<?> w) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        Map<String, Object> params = w.getParamNameValuePairs();
        Pattern p = Pattern.compile("([\\w_]+)=#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}");
        for (String part : w.getSqlSet().split(",")) {
            Matcher m = p.matcher(part.trim());
            if (m.matches()) {
                out.put(m.group(1), params.get(m.group(2)));
            }
        }
        return out;
    }

    @BeforeEach
    public void wire() {
        ruleService = new TOadsRuleLineServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(ruleService, "oadsRuleLineMapper", ruleLineMapper);

        spotService = new TOadsSpotServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(spotService, "baseMapper", spotMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(spotService, "oadsRuleLineService", ruleService);
        org.springframework.test.util.ReflectionTestUtils.setField(spotService, "oadsSpotGradeLogMapper", gradeLogMapper);

        when(ruleLineMapper.selectList(any())).thenAnswer(i -> new ArrayList<TOadsRuleLine>(rules));
        when(spotMapper.selectById(any())).thenAnswer(i -> spots.get(i.getArgument(0)));
        when(spotMapper.selectList(any())).thenAnswer(i -> new ArrayList<TOadsSpot>(spots.values()));
        when(spotMapper.update(any(), any(UpdateWrapper.class))).thenAnswer(i -> {
            lastSet = extractSet(i.getArgument(1));
            return 1;
        });
        when(gradeLogMapper.insert(any())).thenAnswer(i -> {
            logs.add(i.getArgument(0));
            return 1;
        });
    }

    @Test
    public void fixWritesRowAndLogWithSamePair() throws Exception {
        TOadsSpot s = spot(1L, "SP-001", "A");
        TOadsRuleLine a = rule("A", "4", "9", "18", "2020-01-01 00:00:00", null, 10, 0);
        Date ask = at("2026-10-05 10:00:00");

        SafetyGradeResult g = spotService.gradeAndFix(1L, new BigDecimal("9.00"), ask);

        assertEquals(3, g.getLevel(), "压够线限值落第3档");
        assertEquals("A", g.getRuleCode());
        // 点位行固化值
        assertEquals(3, ((Number) lastSet.get("safe_level")).intValue());
        assertEquals("A", lastSet.get("safe_rule_code"));
        assertEquals(a.getEffStart(), lastSet.get("safe_eff_start"));
        // 留痕一行
        assertEquals(1, logs.size());
        TOadsSpotGradeLog log = logs.get(0);
        assertEquals(3, log.getSafeLevel().intValue());
        assertEquals("A", log.getRuleCode());
        // 固化进行的那一对与留痕逐字相同
        assertEquals(lastSet.get("safe_rule_code"), log.getRuleCode());
        assertEquals(lastSet.get("safe_eff_start"), log.getEffStart());
        assertEquals(g.getEffStart(), log.getEffStart());
        assertEquals(ask, log.getAskAt());
        assertEquals(new BigDecimal("9.00"), log.getScoreAligned());
    }

    @Test
    public void fixRoundingIncidentScore() throws Exception {
        spot(2L, "SP-002", "A");
        rule("A", "4", "9", "18", "2020-01-01 00:00:00", null, 10, 0);
        // 上季度事故值：取数进位、比对没进位
        SafetyGradeResult g = spotService.gradeAndFix(2L, new BigDecimal("8.996"), at("2026-10-05 10:00:00"));
        assertEquals(3, g.getLevel());
        assertEquals(3, ((Number) lastSet.get("safe_level")).intValue());
    }

    @Test
    public void noAvailableRuleFixesEmptyPair() throws Exception {
        spot(3L, "SP-003", null);
        // 库里一条可用规则也没有
        SafetyGradeResult g = spotService.gradeAndFix(3L, new BigDecimal("5"), at("2026-10-05 10:00:00"));
        assertEquals(0, g.getLevel());
        // 点位行：档位0，代号与起用日一并留空（SET 里显式置 null，不许借邻近规则）
        assertEquals(0, ((Number) lastSet.get("safe_level")).intValue());
        assertTrue(lastSet.containsKey("safe_rule_code"));
        assertNull(lastSet.get("safe_rule_code"));
        assertTrue(lastSet.containsKey("safe_eff_start"));
        assertNull(lastSet.get("safe_eff_start"));
        // 留痕同样空对
        assertEquals(1, logs.size());
        assertNull(logs.get(0).getRuleCode());
        assertNull(logs.get(0).getEffStart());
        assertEquals(0, logs.get(0).getSafeLevel().intValue());
    }

    @Test
    public void twoEntriesGiveSameLevel() throws Exception {
        spot(4L, "SP-004", "A");
        rule("A", "4", "9", "18", "2020-01-01 00:00:00", null, 10, 0);
        Date ask = at("2026-10-05 10:00:00");
        // 同一时刻从按标识取值与整表取优两入口问同一处点位
        SafetyGradeResult byCode = ruleService.gradeByCode("A", new BigDecimal("9.10"), ask);
        SafetyGradeResult top = ruleService.gradeTop(new BigDecimal("9.10"), ask);
        assertEquals(byCode.getLevel(), top.getLevel());
        assertEquals(3, byCode.getLevel());
        // 点位入口与之一致
        assertEquals(3, spotService.gradeSpot(4L, new BigDecimal("9.10"), ask).getLevel());
    }

    @Test
    public void danglingSpotsListed() throws Exception {
        spot(10L, "SP-010", "GONE");   // 已撤下
        spot(11L, "SP-011", "GHOST"); // 从不存在
        spot(12L, "SP-012", "A");     // 正常
        rule("GONE", "4", "9", "18", "2020-01-01 00:00:00", null, 10, 1);
        rule("A", "4", "9", "18", "2020-01-01 00:00:00", null, 10, 0);

        List<SpotDangling> dangling = spotService.listDangling(at("2026-10-05 10:00:00"));
        assertEquals(2, dangling.size());
        SpotDangling first = dangling.get(0);
        assertTrue(Arrays.asList("GONE", "GHOST").contains(first.getRuleCode()));
        assertNotNull(first.getReason());
        assertTrue(first.getReason().contains("取不到生效版本"));
        // 正常点位不在悬空清单
        assertFalse(dangling.stream().anyMatch(d -> "SP-012".equals(d.getSpotNo())));
    }

    @Test
    public void batchGradesOnOneSnapshot() throws Exception {
        spot(20L, "SP-020", "A");
        spot(21L, "SP-021", null);
        rule("A", "4", "9", "18", "2020-01-01 00:00:00", null, 10, 0);

        List<SpotGradeQuery> qs = Arrays.asList(
                new SpotGradeQuery(20L, new BigDecimal("3")),
                new SpotGradeQuery(21L, new BigDecimal("20")));
        List<SpotGradeOutcome> outcomes = spotService.gradeBatch(qs, at("2026-10-05 10:00:00"));
        assertEquals(2, outcomes.size());
        assertEquals(1, outcomes.get(0).getGrade().getLevel());
        assertEquals(4, outcomes.get(1).getGrade().getLevel());
    }

    @Test
    public void genericEditCannotTamperFixedColumns() throws Exception {
        TOadsSpot current = spot(30L, "SP-030", null);
        current.setSafeLevel(3);
        current.setSafeRuleCode("A");
        current.setSafeEffStart(at("2020-01-01 00:00:00"));

        TOadsSpot tamper = new TOadsSpot();
        tamper.setId(30L);
        tamper.setSafeLevel(4);
        tamper.setSafeRuleCode("FAKE-NEAR-RULE");
        tamper.setSafeEffStart(at("2026-01-01 00:00:00"));
        when(spotMapper.selectById(30L)).thenReturn(current);
        when(spotMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);

        spotService.updateTOadsSpot(tamper);

        // 通用修改入口报来的固化三列被压回现值，旁处改不动
        assertEquals(3, tamper.getSafeLevel().intValue());
        assertEquals("A", tamper.getSafeRuleCode());
        assertEquals(current.getSafeEffStart(), tamper.getSafeEffStart());
    }
}
