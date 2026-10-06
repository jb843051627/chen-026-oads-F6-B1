import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.exception.gov.GovCaseRuleException;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 旧版点位监管办件流转单（t_oads_gov_flow，stage 0..3）停写口径核对：
 * 只读方法照常（存量数据继续可查）；推进/回退/改内容/移除四个写动作一律拒绝，
 * 且一个库写调用都不许发生——旧表不动数据，流转只走新版 t_oads_gov_case。
 * mapper 全 Mock，不落真库。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TOadsGovFlowServiceImplTest {

    @Mock
    private TOadsGovFlowMapper mapper;

    private TOadsGovFlowServiceImpl svc;

    private TOadsGovFlow legacy;

    @BeforeEach
    public void wire() {
        svc = new TOadsGovFlowServiceImpl();
        ReflectionTestUtils.setField(svc, "oadsGovFlowMapper", mapper);

        legacy = new TOadsGovFlow();
        legacy.setId(7L);
        legacy.setBizNo("OLD-7");
        legacy.setStage(3);
        legacy.setStatus(2);
        legacy.setContent("旧套一关一记");

        when(mapper.selectById(7L)).thenReturn(legacy);
        List<TOadsGovFlow> rows = new ArrayList<TOadsGovFlow>();
        rows.add(legacy);
        when(mapper.selectList(any())).thenReturn(rows);
    }

    @Test
    public void 旧单只读照常可查() {
        assertSame(legacy, svc.selectTOadsGovFlowById(7L));
        List<TOadsGovFlow> list = svc.selectTOadsGovFlowList(new QueryWrapper<TOadsGovFlow>());
        assertEquals(1, list.size());
        assertSame(legacy, list.get(0));
    }

    @Test
    public void 旧单到终点也不许推进且不写库() {
        GovCaseRuleException e = assertThrows(GovCaseRuleException.class,
                () -> svc.advance(7L, "再点一次推进"));
        assertTrue(e.getMessage().contains("t_oads_gov_case"));
        assertTrue(e.getMessage().contains("逐笔留痕重数"));
        verify(mapper, never()).updateById(any());
        verify(mapper, never()).insert(any());
        verify(mapper, never()).deleteById(any());
        // 存量原样未动
        assertEquals(3, legacy.getStage().intValue());
        assertEquals(2, legacy.getStatus().intValue());
    }

    @Test
    public void 旧单不许回退且不写库() {
        assertThrows(GovCaseRuleException.class, () -> svc.rollback(7L, "想退回"));
        verify(mapper, never()).updateById(any());
        assertEquals(3, legacy.getStage().intValue());
    }

    @Test
    public void 定稿后不许改内容且不写库() {
        assertThrows(GovCaseRuleException.class, () -> svc.updateContent(7L, "偷改一关一记"));
        verify(mapper, never()).updateById(any());
        assertEquals("旧套一关一记", legacy.getContent());
    }

    @Test
    public void 锁住的旧单不许移除历史不删() {
        assertThrows(GovCaseRuleException.class, () -> svc.remove(7L));
        verify(mapper, never()).deleteById(any());
        verify(mapper, never()).deleteById(7L);
        // 仍查得到，历史还在
        assertNotNull(svc.selectTOadsGovFlowById(7L));
    }
}
