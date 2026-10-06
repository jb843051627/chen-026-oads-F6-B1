import com.fc.v2.common.exception.spot.SpotRuleException;
import com.fc.v2.mapper.auto.TOadsCheckRowMapper;
import com.fc.v2.model.custom.SpotBatchBringOutcome;
import com.fc.v2.model.custom.SpotBuildResult;
import com.fc.v2.model.custom.SpotForm;
import com.fc.v2.service.ITOadsSpotRegisterService;
import com.fc.v2.service.impl.TOadsCheckRowServiceImpl;
import com.fc.v2.service.spot.SpotFlowStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 集中更换带出（第二建行通道）：submitBatch 本体不动，bringOutSpots 逐行交名册裁决处，
 * 两通道同一套数量逻辑与判定；某行不过只带回该行检查点自己的提示语，不并句、不拖累别行。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TOadsCheckRowBringOutTest {

    @Mock private TOadsCheckRowMapper checkRowMapper;
    @Mock private ITOadsSpotRegisterService registerService;

    private TOadsCheckRowServiceImpl svc;

    @BeforeEach
    public void wire() {
        svc = new TOadsCheckRowServiceImpl();
        ReflectionTestUtils.setField(svc, "oadsCheckRowMapper", checkRowMapper);
        ReflectionTestUtils.setField(svc, "spotRegisterService", registerService);
    }

    private SpotForm form(String no, String need, String got) {
        SpotForm f = new SpotForm();
        f.setSpotNo(no);
        f.setAreaNo("DU00");
        f.setNeedText(need);
        f.setGotText(got);
        return f;
    }

    @Test
    public void bringOutDelegatesSameJudgeAndKeepsPerRowMessages() {
        SpotBuildResult ok1 = SpotBuildResult.created();
        ok1.setSpotNo("B1");
        ok1.setLackNum(18);
        when(registerService.build(any(), eq(SpotFlowStep.BUILD_BATCH), eq("核验岗")))
                .thenReturn(ok1)
                .thenThrow(new SpotRuleException("检查点2空间挂靠：点位 B2 的坐标在辖段 DU00 的围栏之外，不予收录"))
                .thenThrow(new SpotRuleException("检查点3数量：核定应设处数缺填，本行进不了库"));

        List<SpotForm> forms = new ArrayList<SpotForm>(Arrays.asList(
                form("B1", "30", "12"),
                form("B2", "5", "1"),
                form("B3", null, "1")));

        SpotBatchBringOutcome o = svc.bringOutSpots("BN-1", forms, "核验岗");
        assertEquals("BN-1", o.getBatchNo());
        assertEquals(3, o.getTotal());
        assertEquals(1, o.getSuccessCount());
        assertEquals(2, o.getFailureCount());

        assertTrue(o.getItems().get(0).isSuccess());
        assertEquals(18, o.getItems().get(0).getResult().getLackNum().intValue());
        assertFalse(o.getItems().get(1).isSuccess());
        assertTrue(o.getItems().get(1).getError().startsWith("检查点2空间挂靠"));
        assertFalse(o.getItems().get(1).getError().contains("数据有误"));
        assertFalse(o.getItems().get(2).isSuccess());
        assertTrue(o.getItems().get(2).getError().contains("核定应设处数缺填"));
    }

    @Test
    public void emptyBatchBringsNothing() {
        SpotBatchBringOutcome o = svc.bringOutSpots("BN-2", new ArrayList<SpotForm>(), "岗");
        assertEquals(0, o.getTotal());
        assertTrue(o.getItems().isEmpty());
    }

    @Test
    public void oversizeBatchRejectedWhole() {
        List<SpotForm> big = new ArrayList<SpotForm>();
        for (int i = 0; i < 501; i++) {
            big.add(form("X" + i, "1", "0"));
        }
        SpotRuleException e = assertThrows(SpotRuleException.class,
                () -> svc.bringOutSpots("BN-3", big, "岗"));
        assertTrue(e.getMessage().contains("超过单批上限"));
    }
}
