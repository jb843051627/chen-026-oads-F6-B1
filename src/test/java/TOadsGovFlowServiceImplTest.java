import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.mapper.auto.TOadsGovFlowMapper;
import com.fc.v2.model.auto.TOadsGovFlow;
import com.fc.v2.service.impl.TOadsGovFlowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 点位监管办件流转单口径核对（照监管办件裁决处那一版，旧表结构内落地）：
 * 一步一格不跨、到顶封住不可写、回退只退一格、连点同句只认头一回、
 * 存量两列对不上以推出值为准且列表详情同源、撤单只逻辑撤历史留着。
 * mapper 全 Mock，内存账台，不落真库。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TOadsGovFlowServiceImplTest {

    @Mock
    private TOadsGovFlowMapper flowMapper;

    private TOadsGovFlowServiceImpl svc;

    private final Map<Long, TOadsGovFlow> rows = new LinkedHashMap<Long, TOadsGovFlow>();
    private long seq = 100;

    @BeforeEach
    public void wire() {
        svc = new TOadsGovFlowServiceImpl();
        ReflectionTestUtils.setField(svc, "oadsGovFlowMapper", flowMapper);
        rows.clear();

        when(flowMapper.selectById(any())).thenAnswer(i -> rows.get(i.getArgument(0)));
        when(flowMapper.selectRowByIdForUpdate(any())).thenAnswer(i -> {
            TOadsGovFlow r = rows.get(i.getArgument(0));
            return r == null || (r.getDelFlag() != null && r.getDelFlag() != 0) ? null : r;
        });
        when(flowMapper.updateById(any(TOadsGovFlow.class))).thenAnswer(i -> {
            TOadsGovFlow r = i.getArgument(0);
            rows.put(r.getId(), r);
            return 1;
        });
        when(flowMapper.selectList(any())).thenAnswer(i -> {
            Map<String, Object> p = params(i.getArgument(0));
            List<TOadsGovFlow> l = new ArrayList<TOadsGovFlow>();
            for (TOadsGovFlow r : rows.values()) {
                if (p.get("del_flag") != null && !p.get("del_flag").equals(r.getDelFlag())) {
                    continue;
                }
                l.add(r);
            }
            l.sort(Comparator.comparing(TOadsGovFlow::getId));
            return l;
        });
    }

    @SuppressWarnings("rawtypes")
    private static Map<String, Object> params(Wrapper w) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (!(w instanceof AbstractWrapper)) {
            return map;
        }
        AbstractWrapper aw = (AbstractWrapper) w;
        Map<?, ?> pairs = aw.getParamNameValuePairs();
        String seg = w.getSqlSegment();
        Matcher m = Pattern.compile("([a-z_]+)\\s*=\\s*#\\{[^}]*MPGENVAL(\\d+)}").matcher(seg == null ? "" : seg);
        while (m.find()) {
            map.put(m.group(1), pairs.get("MPGENVAL" + m.group(2)));
        }
        return map;
    }

    // ---------------------------------------------------------------- 小工具

    /** 手工造一条流转单：stage/status 可指定（含存量不一致情形），落库返回。 */
    private TOadsGovFlow sheet(Integer stage, Integer status) {
        TOadsGovFlow r = new TOadsGovFlow();
        r.setId(++seq);
        r.setBizNo("LD" + seq);
        r.setStage(stage);
        r.setStatus(status);
        r.setDelFlag(0);
        rows.put(r.getId(), r);
        return r;
    }

    // ---------------------------------------------------------------- 复现路径：造单 → 一关一记 → 走一步

    @Test
    public void 造单填记推进一步只走一格() {
        TOadsGovFlow r = sheet(0, 0);
        // 一关一记照常填
        assertTrue(svc.updateContent(r.getId(), "申报：处数2、校核88"));
        // 再走一步：只到核准，不跨格
        TOadsGovFlow after = svc.advance(r.getId(), "核准通过");
        assertNotNull(after);
        assertEquals(1, after.getStage());
        assertEquals(1, after.getStatus());
        assertEquals("申报：处数2、校核88", after.getContent());
    }

    @Test
    public void 推进一回一格四格到顶() {
        TOadsGovFlow r = sheet(0, 0);
        assertEquals(1, svc.advance(r.getId(), "核准").getStage());
        assertEquals(2, svc.advance(r.getId(), "设置").getStage());
        TOadsGovFlow top = svc.advance(r.getId(), "验收");
        assertEquals(3, top.getStage());
        // 到顶即封住
        assertEquals(2, top.getStatus());
    }

    @Test
    public void 到顶封住再推被拒且一列不动() {
        TOadsGovFlow r = sheet(0, 0);
        svc.advance(r.getId(), "核准");
        svc.advance(r.getId(), "设置");
        svc.advance(r.getId(), "验收");
        TOadsGovFlow before = rows.get(r.getId());
        String lastAction = before.getLastAction();
        // 终点再点推进：被拒，关次/状态/最近动作都不动
        assertNull(svc.advance(r.getId(), "还想再推"));
        assertEquals(3, rows.get(r.getId()).getStage());
        assertEquals(2, rows.get(r.getId()).getStatus());
        assertEquals(lastAction, rows.get(r.getId()).getLastAction());
    }

    // ---------------------------------------------------------------- 回退

    @Test
    public void 回退只退一格不到起点() {
        TOadsGovFlow r = sheet(0, 0);
        svc.advance(r.getId(), "核准");
        svc.advance(r.getId(), "设置");
        TOadsGovFlow back = svc.rollback(r.getId(), "设置退回");
        assertNotNull(back);
        // 从设置退一步到核准，不是直接回起点
        assertEquals(1, back.getStage());
        assertEquals(1, back.getStatus());
        TOadsGovFlow back2 = svc.rollback(r.getId(), "核准退回");
        assertEquals(0, back2.getStage());
        assertEquals(0, back2.getStatus());
        // 已在申报，退无可退
        assertNull(svc.rollback(r.getId(), "还想退"));
        assertEquals(0, rows.get(r.getId()).getStage());
    }

    // ---------------------------------------------------------------- 封住即锁死

    @Test
    public void 封住后推进回退改记撤单全不受理() {
        TOadsGovFlow r = sheet(0, 0);
        svc.updateContent(r.getId(), "申报记要");
        svc.advance(r.getId(), "核准");
        svc.advance(r.getId(), "设置");
        svc.advance(r.getId(), "验收");
        assertNull(svc.advance(r.getId(), "再推"));
        assertNull(svc.rollback(r.getId(), "想退"));
        assertFalse(svc.updateContent(r.getId(), "改定稿"));
        assertFalse(svc.remove(r.getId()));
        // 可查：详情还在，关次状态原样
        TOadsGovFlow view = svc.selectTOadsGovFlowById(r.getId());
        assertNotNull(view);
        assertEquals(3, view.getStage());
        assertEquals(2, view.getStatus());
        assertEquals("申报记要", view.getContent());
        assertEquals(0, rows.get(r.getId()).getDelFlag());
    }

    // ---------------------------------------------------------------- 连点去重

    @Test
    public void 连点同句批注只挪一回() {
        TOadsGovFlow r = sheet(0, 0);
        TOadsGovFlow first = svc.advance(r.getId(), "核准通过");
        assertEquals(1, first.getStage());
        // 连点第二回（同一句批注重发）：回首条结果，不再挪格
        TOadsGovFlow again = svc.advance(r.getId(), "核准通过");
        assertNotNull(again);
        assertEquals(1, again.getStage());
        // 换一句批注才算下一次动作
        TOadsGovFlow next = svc.advance(r.getId(), "设置完成");
        assertEquals(2, next.getStage());
    }

    @Test
    public void 回退连点同句也只退一回() {
        TOadsGovFlow r = sheet(0, 0);
        svc.advance(r.getId(), "核准");
        svc.advance(r.getId(), "设置");
        assertEquals(1, svc.rollback(r.getId(), "退回重办").getStage());
        TOadsGovFlow again = svc.rollback(r.getId(), "退回重办");
        assertEquals(1, again.getStage());
    }

    @Test
    public void 存量裸批注同句重发也算同一回() {
        TOadsGovFlow r = sheet(1, 1);
        // 存量旧值：没打动作前缀的裸批注
        r.setLastAction("核准通过");
        TOadsGovFlow again = svc.advance(r.getId(), "核准通过");
        assertEquals(1, again.getStage());
        // 不同句照常推进
        assertEquals(2, svc.advance(r.getId(), "设置完成").getStage());
    }

    // ---------------------------------------------------------------- 存量兼容与同源

    @Test
    public void 存量行两列对不上列表详情同一推出口径() {
        // 存量行：关次已到验收，状态还写着在办（旧版从来不落"已封住"）
        TOadsGovFlow a = sheet(3, 1);
        // 存量行：关次空着（未起）
        TOadsGovFlow b = sheet(null, null);
        List<TOadsGovFlow> list = svc.selectTOadsGovFlowList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TOadsGovFlow>());
        TOadsGovFlow listA = null;
        TOadsGovFlow listB = null;
        for (TOadsGovFlow x : list) {
            if (x.getId().equals(a.getId())) {
                listA = x;
            }
            if (x.getId().equals(b.getId())) {
                listB = x;
            }
        }
        TOadsGovFlow detailA = svc.selectTOadsGovFlowById(a.getId());
        TOadsGovFlow detailB = svc.selectTOadsGovFlowById(b.getId());
        // 列表那条和点进去看到的是同一件：同一关次、同一推出状态
        assertEquals(3, listA.getStage());
        assertEquals(2, listA.getStatus());
        assertEquals(listA.getStage(), detailA.getStage());
        assertEquals(listA.getStatus(), detailA.getStatus());
        // 关次空着的存量行按 0（未起）计，两边一致
        assertEquals(0, listB.getStage());
        assertEquals(0, listB.getStatus());
        assertEquals(listB.getStatus(), detailB.getStatus());
        // 推出口径同样管住写动作：关次到顶的存量行推不动、退不动、改不动、撤不动
        assertNull(svc.advance(a.getId(), "再推"));
        assertNull(svc.rollback(a.getId(), "想退"));
        assertFalse(svc.updateContent(a.getId(), "改"));
        assertFalse(svc.remove(a.getId()));
    }

    // ---------------------------------------------------------------- 撤单只逻辑撤

    @Test
    public void 撤单逻辑撤历史留着() {
        TOadsGovFlow r = sheet(0, 0);
        svc.updateContent(r.getId(), "申报记要");
        svc.advance(r.getId(), "核准");
        assertTrue(svc.remove(r.getId()));
        // 历史没丢：行还在库，只是撤下标记
        TOadsGovFlow kept = rows.get(r.getId());
        assertNotNull(kept);
        assertEquals(1, kept.getDelFlag());
        assertEquals("申报记要", kept.getContent());
        // 台账与详情同一份过滤：两边同时不见
        assertNull(svc.selectTOadsGovFlowById(r.getId()));
        assertTrue(svc.selectTOadsGovFlowList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TOadsGovFlow>()).isEmpty());
        // 撤下的行写动作不受理
        assertNull(svc.advance(r.getId(), "再推"));
        assertFalse(svc.updateContent(r.getId(), "改"));
        assertFalse(svc.remove(r.getId()));
    }

    // ---------------------------------------------------------------- 并发口径：写动作都走行锁

    @Test
    public void 写动作一律先锁行锁内重读() {
        TOadsGovFlow r = sheet(0, 0);
        svc.advance(r.getId(), "核准");
        svc.rollback(r.getId(), "退回");
        svc.updateContent(r.getId(), "记要");
        svc.remove(r.getId());
        org.mockito.Mockito.verify(flowMapper, org.mockito.Mockito.times(4))
                .selectRowByIdForUpdate(r.getId());
    }
}
