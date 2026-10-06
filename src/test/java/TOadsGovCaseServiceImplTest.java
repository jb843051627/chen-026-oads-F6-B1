import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.common.exception.gov.GovCaseRuleException;
import com.fc.v2.mapper.auto.TOadsAreaBookMapper;
import com.fc.v2.mapper.auto.TOadsGovCaseIntakeMapper;
import com.fc.v2.mapper.auto.TOadsGovCaseLogMapper;
import com.fc.v2.mapper.auto.TOadsGovCaseMapper;
import com.fc.v2.mapper.auto.TOadsGovCaseReceiptMapper;
import com.fc.v2.mapper.auto.TOadsGovCaseWitnessMapper;
import com.fc.v2.mapper.auto.TOadsPermitBillMapper;
import com.fc.v2.mapper.auto.TOadsSpotMapper;
import com.fc.v2.model.auto.TOadsAreaBook;
import com.fc.v2.model.auto.TOadsGovCase;
import com.fc.v2.model.auto.TOadsGovCaseIntake;
import com.fc.v2.model.auto.TOadsGovCaseLog;
import com.fc.v2.model.auto.TOadsGovCaseReceipt;
import com.fc.v2.model.auto.TOadsGovCaseWitness;
import com.fc.v2.model.auto.TOadsPermitBill;
import com.fc.v2.model.auto.TOadsSpot;
import com.fc.v2.model.custom.GovCaseForm;
import com.fc.v2.model.custom.GovCaseOutcome;
import com.fc.v2.model.custom.GovCaseTrace;
import com.fc.v2.service.gov.GovCaseStep;
import com.fc.v2.service.impl.TOadsGovCaseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;
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
 * 监管办件四步流程口径核对：四项输入逐项点名、步次服务端按留痕重数（报文步次只作参考）、
 * 紧邻推进/跨步不生效、三步准入（三段签署/核收同意/两名见证＋凭证回写）、
 * 后退老轮转追溯重新累计、同步二次不覆盖不追加、封存可查不可写、同点位在走唯一、
 * 撤销（立件前）与撤回（申报后）/划归/作废分路径、同批幂等、未走完计数与追溯笔数。
 * mapper 全 Mock，内存账台，不落真库。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TOadsGovCaseServiceImplTest {

    @Mock private TOadsGovCaseMapper govCaseMapper;
    @Mock private TOadsGovCaseLogMapper logMapper;
    @Mock private TOadsGovCaseWitnessMapper witnessMapper;
    @Mock private TOadsGovCaseReceiptMapper receiptMapper;
    @Mock private TOadsGovCaseIntakeMapper intakeMapper;
    @Mock private TOadsPermitBillMapper permitBillMapper;
    @Mock private TOadsSpotMapper spotMapper;
    @Mock private TOadsAreaBookMapper areaBookMapper;

    private TOadsGovCaseServiceImpl svc;

    private final Map<Long, TOadsGovCase> cases = new LinkedHashMap<Long, TOadsGovCase>();
    private final List<TOadsGovCaseLog> logs = new ArrayList<TOadsGovCaseLog>();
    private final List<TOadsGovCaseWitness> witnesses = new ArrayList<TOadsGovCaseWitness>();
    private final List<TOadsGovCaseReceipt> receipts = new ArrayList<TOadsGovCaseReceipt>();
    private final Map<Long, TOadsGovCaseIntake> intakes = new LinkedHashMap<Long, TOadsGovCaseIntake>();
    private final Map<String, TOadsSpot> spots = new LinkedHashMap<String, TOadsSpot>();
    private final Map<String, TOadsAreaBook> areas = new LinkedHashMap<String, TOadsAreaBook>();
    private final List<TOadsPermitBill> sealedBills = new ArrayList<TOadsPermitBill>();
    private long seq = 1000;

    private static final String OP = "P-DUTY";

    // ---------------------------------------------------------------- 账台接线

    @BeforeEach
    public void wire() {
        svc = new TOadsGovCaseServiceImpl();
        ReflectionTestUtils.setField(svc, "govCaseMapper", govCaseMapper);
        ReflectionTestUtils.setField(svc, "logMapper", logMapper);
        ReflectionTestUtils.setField(svc, "witnessMapper", witnessMapper);
        ReflectionTestUtils.setField(svc, "receiptMapper", receiptMapper);
        ReflectionTestUtils.setField(svc, "intakeMapper", intakeMapper);
        ReflectionTestUtils.setField(svc, "permitBillMapper", permitBillMapper);
        ReflectionTestUtils.setField(svc, "spotMapper", spotMapper);
        ReflectionTestUtils.setField(svc, "areaBookMapper", areaBookMapper);

        spots.clear();
        spots.put("S1", spot("S1", "DU00"));
        spots.put("S2", spot("S2", "DU00"));
        areas.clear();
        area("DU00", "临江路", 0);
        area("DU01", "北岸旧段", 1);
        area("DU02", "青石巷", 0);

        when(govCaseMapper.selectByIdForUpdate(any())).thenAnswer(i -> cases.get(i.getArgument(0)));
        when(govCaseMapper.selectById(any())).thenAnswer(i -> cases.get(i.getArgument(0)));
        when(govCaseMapper.insert(any(TOadsGovCase.class))).thenAnswer(i -> {
            TOadsGovCase c = i.getArgument(0);
            c.setId(++seq);
            cases.put(c.getId(), c);
            return 1;
        });
        when(govCaseMapper.updateById(any(TOadsGovCase.class))).thenAnswer(i -> {
            TOadsGovCase c = i.getArgument(0);
            cases.put(c.getId(), c);
            return 1;
        });
        when(govCaseMapper.listRunningBySpot(any())).thenAnswer(i -> {
            String no = i.getArgument(0);
            List<TOadsGovCase> l = new ArrayList<TOadsGovCase>();
            for (TOadsGovCase c : cases.values()) {
                if (no.equals(c.getSpotNo()) && GovCaseStep.OUTCOME_RUNNING.equals(c.getOutcome())
                        && (c.getDelFlag() == null || c.getDelFlag() == 0)) {
                    l.add(c);
                }
            }
            return l;
        });
        when(govCaseMapper.countRunning()).thenAnswer(i -> {
            long n = 0;
            for (TOadsGovCase c : cases.values()) {
                if (GovCaseStep.OUTCOME_RUNNING.equals(c.getOutcome())
                        && (c.getDelFlag() == null || c.getDelFlag() == 0)) {
                    n++;
                }
            }
            return n;
        });
        when(govCaseMapper.selectAllNormal()).thenAnswer(i -> new ArrayList<TOadsGovCase>(cases.values()));

        when(logMapper.insert(any(TOadsGovCaseLog.class))).thenAnswer(i -> {
            TOadsGovCaseLog l = i.getArgument(0);
            l.setId(++seq);
            logs.add(l);
            return 1;
        });
        when(logMapper.listByCase(any())).thenAnswer(i -> {
            Long id = i.getArgument(0);
            List<TOadsGovCaseLog> l = new ArrayList<TOadsGovCaseLog>();
            for (TOadsGovCaseLog x : logs) {
                if (id.equals(x.getCaseId())) {
                    l.add(x);
                }
            }
            l.sort(Comparator.comparing(TOadsGovCaseLog::getActedAt)
                    .thenComparing(TOadsGovCaseLog::getSeqNo).thenComparing(TOadsGovCaseLog::getId));
            return l;
        });
        when(logMapper.findByIdemKey(any())).thenAnswer(i -> {
            String k = i.getArgument(0);
            for (TOadsGovCaseLog l : logs) {
                if (k.equals(l.getIdemKey())) {
                    return l;
                }
            }
            return null;
        });
        when(logMapper.deactivateStepRound(any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt())).thenAnswer(i -> {
            Long id = i.getArgument(0);
            int step = i.getArgument(1);
            int round = i.getArgument(2);
            int n = 0;
            for (TOadsGovCaseLog l : logs) {
                if (id.equals(l.getCaseId()) && l.getStepNo() == step && l.getRoundNo() == round
                        && l.getActive() != null && l.getActive() == 1) {
                    l.setActive(0);
                    n++;
                }
            }
            return n;
        });
        when(logMapper.listLandedBetween(any(), any())).thenAnswer(i -> {
            java.util.Date start = i.getArgument(0);
            java.util.Date end = i.getArgument(1);
            List<TOadsGovCaseLog> l = new ArrayList<TOadsGovCaseLog>();
            for (TOadsGovCaseLog x : logs) {
                if (!x.getActedAt().before(start) && x.getActedAt().before(end)) {
                    l.add(x);
                }
            }
            l.sort(Comparator.comparing(TOadsGovCaseLog::getActedAt)
                    .thenComparing(TOadsGovCaseLog::getSeqNo).thenComparing(TOadsGovCaseLog::getId));
            return l;
        });

        when(witnessMapper.insert(any(TOadsGovCaseWitness.class))).thenAnswer(i -> {
            TOadsGovCaseWitness w = i.getArgument(0);
            w.setId(++seq);
            witnesses.add(w);
            return 1;
        });
        when(witnessMapper.listActiveRound(any(), org.mockito.ArgumentMatchers.anyInt())).thenAnswer(i -> {
            Long id = i.getArgument(0);
            int round = i.getArgument(1);
            List<TOadsGovCaseWitness> l = new ArrayList<TOadsGovCaseWitness>();
            for (TOadsGovCaseWitness w : witnesses) {
                if (id.equals(w.getCaseId()) && w.getRoundNo() == round
                        && w.getActive() != null && w.getActive() == 1) {
                    l.add(w);
                }
            }
            l.sort(Comparator.comparing(TOadsGovCaseWitness::getSubmitAt)
                    .thenComparing(TOadsGovCaseWitness::getId));
            return l;
        });
        when(witnessMapper.deactivateRound(any(), org.mockito.ArgumentMatchers.anyInt())).thenAnswer(i -> {
            Long id = i.getArgument(0);
            int round = i.getArgument(1);
            int n = 0;
            for (TOadsGovCaseWitness w : witnesses) {
                if (id.equals(w.getCaseId()) && w.getRoundNo() == round
                        && w.getActive() != null && w.getActive() == 1) {
                    w.setActive(0);
                    n++;
                }
            }
            return n;
        });

        when(receiptMapper.insert(any(TOadsGovCaseReceipt.class))).thenAnswer(i -> {
            TOadsGovCaseReceipt r = i.getArgument(0);
            r.setId(++seq);
            receipts.add(r);
            return 1;
        });
        when(receiptMapper.findActiveRound(any(), org.mockito.ArgumentMatchers.anyInt())).thenAnswer(i -> {
            Long id = i.getArgument(0);
            int round = i.getArgument(1);
            for (TOadsGovCaseReceipt r : receipts) {
                if (id.equals(r.getCaseId()) && r.getRoundNo() == round
                        && r.getActive() != null && r.getActive() == 1) {
                    return r;
                }
            }
            return null;
        });
        when(receiptMapper.deactivateRound(any(), org.mockito.ArgumentMatchers.anyInt())).thenAnswer(i -> {
            Long id = i.getArgument(0);
            int round = i.getArgument(1);
            int n = 0;
            for (TOadsGovCaseReceipt r : receipts) {
                if (id.equals(r.getCaseId()) && r.getRoundNo() == round
                        && r.getActive() != null && r.getActive() == 1) {
                    r.setActive(0);
                    n++;
                }
            }
            return n;
        });

        when(intakeMapper.insert(any(TOadsGovCaseIntake.class))).thenAnswer(i -> {
            TOadsGovCaseIntake x = i.getArgument(0);
            x.setId(++seq);
            intakes.put(x.getId(), x);
            return 1;
        });
        when(intakeMapper.updateById(any(TOadsGovCaseIntake.class))).thenAnswer(i -> {
            TOadsGovCaseIntake x = i.getArgument(0);
            intakes.put(x.getId(), x);
            return 1;
        });
        when(intakeMapper.selectRowById(any())).thenAnswer(i -> intakes.get(i.getArgument(0)));
        when(intakeMapper.selectByIdemKey(any())).thenAnswer(i -> {
            String k = i.getArgument(0);
            for (TOadsGovCaseIntake x : intakes.values()) {
                if (k.equals(x.getIdemKey())) {
                    return x;
                }
            }
            return null;
        });
        when(intakeMapper.selectList(any())).thenAnswer(i -> {
            Map<String, Object> p = params(i.getArgument(0));
            List<TOadsGovCaseIntake> l = new ArrayList<TOadsGovCaseIntake>();
            for (TOadsGovCaseIntake x : intakes.values()) {
                if (p.get("spot_no") != null && !p.get("spot_no").equals(x.getSpotNo())) {
                    continue;
                }
                if (p.get("state") != null && !p.get("state").equals(x.getState())) {
                    continue;
                }
                l.add(x);
            }
            return l;
        });

        when(spotMapper.selectList(any())).thenAnswer(i -> {
            Map<String, Object> p = params(i.getArgument(0));
            List<TOadsSpot> l = new ArrayList<TOadsSpot>();
            for (TOadsSpot s : spots.values()) {
                if (p.get("spot_no") != null && !p.get("spot_no").equals(s.getSpotNo())) {
                    continue;
                }
                l.add(s);
            }
            return l;
        });
        when(areaBookMapper.selectList(any())).thenAnswer(i -> {
            Map<String, Object> p = params(i.getArgument(0));
            List<TOadsAreaBook> l = new ArrayList<TOadsAreaBook>();
            for (TOadsAreaBook a : areas.values()) {
                if (p.get("area_no") != null && !p.get("area_no").equals(a.getAreaNo())) {
                    continue;
                }
                if (p.get("del_flag") != null && !p.get("del_flag").equals(a.getDelFlag())) {
                    continue;
                }
                l.add(a);
            }
            return l;
        });
        when(permitBillMapper.selectList(any())).thenAnswer(i -> {
            Map<String, Object> p = params(i.getArgument(0));
            List<TOadsPermitBill> l = new ArrayList<TOadsPermitBill>();
            for (TOadsPermitBill b : sealedBills) {
                if (p.get("spot_no") != null && !p.get("spot_no").equals(b.getSpotNo())) {
                    continue;
                }
                l.add(b);
            }
            return l;
        });
    }

    private TOadsSpot spot(String no, String areaNo) {
        TOadsSpot s = new TOadsSpot();
        s.setId(++seq);
        s.setSpotNo(no);
        s.setAreaNo(areaNo);
        s.setDelFlag(0);
        return s;
    }

    private void area(String no, String name, int status) {
        TOadsAreaBook a = new TOadsAreaBook();
        a.setId((long) (no.hashCode() & 0x7fffffff));
        a.setAreaNo(no);
        a.setAreaName(name);
        a.setStatus(status);
        a.setDelFlag(0);
        areas.put(no, a);
    }

    private void markPermitSealed(String spotNo) {
        TOadsPermitBill b = new TOadsPermitBill();
        b.setId(++seq);
        b.setBillNo("XK" + seq);
        b.setSpotNo(spotNo);
        b.setStatus(1);
        b.setDelFlag(0);
        sealedBills.add(b);
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
        // 形如：spot_no = #{ew.paramNameValuePairs.MPGENVAL1}
        Matcher m = Pattern.compile("([a-z_]+)\\s*=\\s*#\\{[^}]*MPGENVAL(\\d+)}").matcher(seg == null ? "" : seg);
        while (m.find()) {
            map.put(m.group(1), pairs.get("MPGENVAL" + m.group(2)));
        }
        return map;
    }

    // ---------------------------------------------------------------- 小工具

    private GovCaseForm form(String spot, String area, String qty, String score) {
        GovCaseForm f = new GovCaseForm();
        f.setSpotNo(spot);
        f.setAreaNo(area);
        f.setPlaceCountText(qty);
        f.setCheckScoreText(score);
        return f;
    }

    /** 报入 + 正式立件，返回在走办件。 */
    private TOadsGovCase openCase(String spot, String area) {
        TOadsGovCaseIntake in = svc.intake(form(spot, area, "2", "88"), OP);
        GovCaseOutcome o = svc.open(in.getId(), OP);
        assertEquals(GovCaseStep.DECLARE, o.getStepNo());
        return cases.get(o.getCaseId());
    }

    private GovCaseRuleException expectFail(Runnable r) {
        GovCaseRuleException e = assertThrows(GovCaseRuleException.class, r::run);
        assertNotNull(e.getMessage());
        assertFalse(e.getMessage().contains("信息不全"), "不许笼统一句信息不全：" + e.getMessage());
        return e;
    }

    private int countActiveReach(long caseId, int step) {
        int n = 0;
        for (TOadsGovCaseLog l : logs) {
            if (l.getCaseId() == caseId && l.getStepNo() == step
                    && GovCaseStep.ACTION_REACH.equals(l.getAction()) && l.getActive() == 1) {
                n++;
            }
        }
        return n;
    }

    // ---------------------------------------------------------------- 第一块：构成与建立

    @Test
    public void 四项输入缺一逐项点名() {
        GovCaseRuleException e1 = expectFail(() -> svc.intake(form(null, "DU00", "2", "88"), OP));
        assertTrue(e1.getMessage().contains("点位编码"));
        GovCaseRuleException e2 = expectFail(() -> svc.intake(form("S1", " ", "2", "88"), OP));
        assertTrue(e2.getMessage().contains("所属路段代号"));
        GovCaseRuleException e3 = expectFail(() -> svc.intake(form("S1", "DU00", null, "88"), OP));
        assertTrue(e3.getMessage().contains("处数"));
        GovCaseRuleException e4 = expectFail(() -> svc.intake(form("S1", "DU00", "2", ""), OP));
        assertTrue(e4.getMessage().contains("校核分值"));
        GovCaseRuleException e5 = expectFail(() -> svc.intake(form("S1", "DU00", "两处", "88"), OP));
        assertTrue(e5.getMessage().contains("处数"));
        GovCaseRuleException e6 = expectFail(() -> svc.intake(form("S1", "DU00", "0", "88"), OP));
        assertTrue(e6.getMessage().contains("处数"));
        assertEquals(0, intakes.size());
    }

    @Test
    public void 立件落第一步申报且计入未走完() {
        TOadsGovCase c = openCase("S1", "DU00");
        assertEquals(1, svc.currentStep(c.getId()));
        assertEquals(1, svc.runningCount());
        assertEquals("申报", GovCaseStep.nameOf(svc.currentStep(c.getId())));
        assertEquals(GovCaseStep.OUTCOME_RUNNING, c.getOutcome());
    }

    @Test
    public void 报文步次只作参考写库读库按服务端() {
        TOadsGovCase c = openCase("S1", "DU00");
        // 头列面子值也不能让它多走：当前步只按留痕重数。
        c.setStepNo(4);
        assertEquals(1, svc.currentStep(c.getId()));
    }

    @Test
    public void 同点位第二件立不起来封存后可另立() {
        TOadsGovCase first = openCase("S1", "DU00");
        expectFail(() -> svc.intake(form("S1", "DU00", "1", "60"), OP));
        // 直接立第二条报入也挡：先造一条报入在档
        TOadsGovCase c = walkToAcceptTwoWitnessNoCert(first);
        // 两名见证齐但没凭证：仍在验收、仍在走
        assertEquals(4, svc.currentStep(c.getId()));
        expectFail(() -> svc.intake(form("S1", "DU00", "1", "60"), OP));
        // 作废收尾后名额空出
        svc.voidCase(c.getId(), "验收步作废", OP, null);
        TOadsGovCase second = openCase("S1", "DU00");
        assertNotEquals(first.getId(), second.getId());
        assertEquals(1, svc.runningCount());
    }

    @Test
    public void 撤销只在立件前且不进主档不占位() {
        TOadsGovCaseIntake in = svc.intake(form("S1", "DU00", "2", "88"), OP);
        svc.revokeIntake(in.getId(), OP);
        assertEquals(0, cases.size());
        expectFail(() -> svc.open(in.getId(), OP));
        assertEquals(0, svc.runningCount());
        // 撤掉后同点位可再报、可立
        TOadsGovCase c = openCase("S1", "DU00");
        assertNotNull(c);
        // 已立件的不能撤销
        TOadsGovCaseIntake opened = null;
        for (TOadsGovCaseIntake x : intakes.values()) {
            if (GovCaseStep.INTAKE_OPENED.equals(x.getState())) {
                opened = x;
            }
        }
        assertNotNull(opened);
        final TOadsGovCaseIntake openedFinal = opened;
        expectFail(() -> svc.revokeIntake(openedFinal.getId(), OP));
    }

    // ---------------------------------------------------------------- 第二块：移动与封存

    @Test
    public void 跨步送进来不生效整条留在原地() {
        TOadsGovCase c = openCase("S1", "DU00");
        expectFail(() -> svc.advance(c.getId(), "跳着报", OP, 3, null, null));
        assertEquals(1, svc.currentStep(c.getId()));
        // 不带自称步次也只能走紧邻：此时许可未签，进不了设置，先到核准可以
        GovCaseOutcome o = svc.advance(c.getId(), "核准意见", OP, null, null, null);
        assertEquals(2, o.getStepNo());
        expectFail(() -> svc.advance(c.getId(), "再跳", OP, 4, null, null));
        assertEquals(2, svc.currentStep(c.getId()));
    }

    @Test
    public void 进设置须三段签署走完() {
        TOadsGovCase c = openCase("S1", "DU00");
        svc.advance(c.getId(), "核准", OP, null, null, null);
        GovCaseRuleException e = expectFail(() -> svc.advance(c.getId(), "无许可硬进设置", OP, 3, null, null));
        assertTrue(e.getMessage().contains("三段签署"));
        assertEquals(2, svc.currentStep(c.getId()));
        markPermitSealed("S1");
        assertEquals(3, svc.advance(c.getId(), "三段齐进设置", OP, 3, null, null).getStepNo());
    }

    @Test
    public void 进验收须核收记同意且同轮第二笔不覆盖() {
        TOadsGovCase c = readyAtSetup("S1");
        // 未登记核收
        expectFail(() -> svc.advance(c.getId(), "直奔验收", OP, 4, null, null));
        // 先记未同意
        svc.recordReceipt(c.getId(), false, "现场未清", OP, null);
        expectFail(() -> svc.advance(c.getId(), "还是想验收", OP, 4, null, null));
        // 同轮第二次送：保留首次，不覆盖不追加
        GovCaseRuleException e = expectFail(() -> svc.recordReceipt(c.getId(), true, "改口同意", OP, null));
        assertTrue(e.getMessage().contains("保留首次"));
        TOadsGovCaseReceipt first = null;
        for (TOadsGovCaseReceipt r : receipts) {
            if (r.getCaseId().equals(c.getId())) {
                first = r;
            }
        }
        assertNotNull(first);
        assertEquals(0, first.getAgree().intValue());
        // 退回核准、核收老轮退出；再走到设置，重新记同意才准验收
        svc.returnStep(c.getId(), "设置退回", OP, null);
        svc.advance(c.getId(), "再核准", OP, 3, null, null);
        svc.recordReceipt(c.getId(), true, "重走后同意", OP, null);
        assertEquals(4, svc.advance(c.getId(), "进验收", OP, 4, null, null).getStepNo());
    }

    @Test
    public void 核收与见证错步提交不生效() {
        TOadsGovCase c = openCase("S1", "DU00");
        expectFail(() -> svc.recordReceipt(c.getId(), true, "早到", OP, null));
        expectFail(() -> svc.submitWitness(c.getId(), "W1", "早到", null, OP, null));
        expectFail(() -> svc.submitWitness(c.getId(), " ", "x", null, OP, null));
        expectFail(() -> svc.submitWitness(c.getId(), "W1", " ", null, OP, null));
    }

    @Test
    public void 两名见证加凭证回写才封存() {
        TOadsGovCase c = readyAtAccept("S1");
        GovCaseOutcome a = svc.submitWitness(c.getId(), "W-A", "合格", "CERT-1", OP, null);
        assertFalse(a.isSealed());
        // 同岗第二次：保留首次，不覆盖不追加
        expectFail(() -> svc.submitWitness(c.getId(), "W-A", "改口", "CERT-9", OP, null));
        assertEquals(4, svc.currentStep(c.getId()));
        // 第二岗不带凭证：第一岗那串已具，两样齐封存
        GovCaseOutcome b = svc.submitWitness(c.getId(), "W-B", "也合格", null, OP, null);
        assertTrue(b.isSealed());
        assertEquals("CERT-1", cases.get(c.getId()).getRecheckCertNo());
        assertEquals(GovCaseStep.OUTCOME_SEALED, cases.get(c.getId()).getOutcome());
        assertEquals(0, svc.runningCount());
    }

    @Test
    public void 两见证齐但缺凭证留在验收继续等() {
        TOadsGovCase c = readyAtAccept("S1");
        svc.submitWitness(c.getId(), "W-A", "意见A", null, OP, null);
        GovCaseOutcome b = svc.submitWitness(c.getId(), "W-B", "意见B", null, OP, null);
        assertFalse(b.isSealed());
        assertEquals(4, svc.currentStep(c.getId()));
        assertEquals(GovCaseStep.OUTCOME_RUNNING, cases.get(c.getId()).getOutcome());
        // 第三名进不来
        expectFail(() -> svc.submitWitness(c.getId(), "W-C", "意见C", "C", OP, null));
    }

    @Test
    public void 封存后可查不可写() {
        TOadsGovCase c = readyAtAccept("S1");
        sealWith(c);
        for (String action : new String[]{"advance", "return", "receipt", "witness", "withdraw", "void", "realign"}) {
            GovCaseRuleException e = expectFail(() -> mutate(action, c.getId()));
            assertTrue(e.getMessage().contains("可查不可写") || e.getMessage().contains("封存"),
                    action + " 应被封存挡下：" + e.getMessage());
        }
        // 可查：台账照常出现、追溯取得到、当前步仍是验收
        assertTrue(svc.listCases().contains(cases.get(c.getId())));
        GovCaseTrace t = svc.trace(c.getId());
        assertEquals(4, t.getRecountedStep().intValue());
        assertNotNull(t.getGovCase());
    }

    private void mutate(String action, Long id) {
        switch (action) {
            case "advance":
                svc.advance(id, "x", OP, null, null, null);
                break;
            case "return":
                svc.returnStep(id, "x", OP, null);
                break;
            case "receipt":
                svc.recordReceipt(id, true, "x", OP, null);
                break;
            case "witness":
                svc.submitWitness(id, "W-X", "x", null, OP, null);
                break;
            case "withdraw":
                svc.withdraw(id, "x", OP, null);
                break;
            case "void":
                svc.voidCase(id, "x", OP, null);
                break;
            case "realign":
                svc.realign(id, "DU02", "x", OP, null);
                break;
            default:
                throw new IllegalStateException(action);
        }
    }

    @Test
    public void 后退只退一步老轮转追溯重走重新累计() {
        TOadsGovCase c = readyAtSetup("S1");
        svc.recordReceipt(c.getId(), true, "首次同意", OP, null);
        svc.advance(c.getId(), "进验收", OP, 4, null, null);
        assertEquals(4, svc.currentStep(c.getId()));
        // 连续退回申报：每步只退一步
        svc.returnStep(c.getId(), "验收退", OP, null);
        assertEquals(3, svc.currentStep(c.getId()));
        svc.returnStep(c.getId(), "设置退", OP, null);
        assertEquals(2, svc.currentStep(c.getId()));
        svc.returnStep(c.getId(), "核准退", OP, null);
        assertEquals(1, svc.currentStep(c.getId()));
        // 第一步退无可退
        expectFail(() -> svc.returnStep(c.getId(), "还想退", OP, null));
        // 被退各步当轮 REACH 已转追溯（active=0），行没删
        assertEquals(0, countActiveReach(c.getId(), 2));
        assertEquals(0, countActiveReach(c.getId(), 3));
        assertEquals(0, countActiveReach(c.getId(), 4));
        int oldLogCount = logsOf(c.getId()).size();
        assertTrue(oldLogCount >= 7);
        // 重走：许可仍在，到设置后核收要重新记
        markPermitSealed("S1");
        svc.advance(c.getId(), "重走核准", OP, 2, null, null);
        svc.advance(c.getId(), "重走进设置", OP, 3, null, null);
        expectFail(() -> svc.advance(c.getId(), "没核收想验收", OP, 4, null, null));
        svc.recordReceipt(c.getId(), true, "第二轮同意", OP, null);
        svc.advance(c.getId(), "重走进验收", OP, 4, null, null);
        assertEquals(4, svc.currentStep(c.getId()));
    }

    @Test
    public void 同一步第二次送意见留首次不覆盖不追加() {
        TOadsGovCase c = openCase("S1", "DU00");
        // 第一次进核准，落首次意见
        GovCaseOutcome first = svc.advance(c.getId(), "首次核准意见", OP, 2, null, null);
        assertEquals(2, first.getStepNo());
        int logsAfterFirst = logsOf(c.getId()).size();
        // 第二次还点当下这一步（同一步重复送）：不推进、不添行
        GovCaseOutcome again = svc.advance(c.getId(), "第二次核准意见", OP, 2, null, null);
        assertFalse(again.isReplayed());
        assertEquals(2, again.getStepNo());
        assertEquals(logsAfterFirst, logsOf(c.getId()).size());
        assertEquals(1, countActiveReach(c.getId(), 2));
        // 不带自称步次的重复送：走到准入处被挡（许可三段未签），同样不推进
        expectFail(() -> svc.advance(c.getId(), "无步次第二次", OP, null, null, null));
        assertEquals(2, svc.currentStep(c.getId()));
        assertEquals(logsAfterFirst, logsOf(c.getId()).size());
    }

    @Test
    public void 撤回只受理申报步() {
        TOadsGovCase c = openCase("S1", "DU00");
        GovCaseOutcome o = svc.withdraw(c.getId(), "申报后撤回", OP, null);
        assertEquals(GovCaseStep.OUTCOME_WITHDRAWN, o.getOutcome());
        assertEquals(0, svc.runningCount());
        assertTrue(svc.listCases().contains(cases.get(c.getId())));

        TOadsGovCase c2 = openCase("S2", "DU00");
        svc.advance(c2.getId(), "已核准", OP, null, null, null);
        expectFail(() -> svc.withdraw(c2.getId(), "晚了", OP, null));
        assertEquals(1, svc.runningCount());
    }

    @Test
    public void 路段划归邻段即收尾() {
        TOadsGovCase c = openCase("S1", "DU00");
        expectFail(() -> svc.realign(c.getId(), "DU99", null, OP, null));
        expectFail(() -> svc.realign(c.getId(), "DU00", null, OP, null));
        expectFail(() -> svc.realign(c.getId(), "DU01", null, OP, null));
        GovCaseOutcome o = svc.realign(c.getId(), "DU02", "目录划归", OP, null);
        assertEquals(GovCaseStep.OUTCOME_REALIGNED, o.getOutcome());
        assertEquals("DU02", cases.get(c.getId()).getAreaNo());
        assertEquals(0, svc.runningCount());
        // 划归后同点位可另立
        assertNotNull(openCase("S1", "DU02"));
    }

    @Test
    public void 作废只从最后一步发起() {
        TOadsGovCase c = openCase("S1", "DU00");
        expectFail(() -> svc.voidCase(c.getId(), "申报就作废不许", OP, null));
        TOadsGovCase atAccept = walkToAcceptTwoWitnessNoCert(c);
        GovCaseOutcome o = svc.voidCase(atAccept.getId(), "验收步作废", OP, null);
        assertEquals(GovCaseStep.OUTCOME_VOID, o.getOutcome());
        assertEquals(0, svc.runningCount());
    }

    // ---------------------------------------------------------------- 第三块：追溯、计数、幂等

    @Test
    public void 追溯笔数与当天受理流水逐笔吻合() {
        TOadsGovCase c = readyAtAccept("S1");
        sealWith(c);
        GovCaseTrace t = svc.trace(c.getId());
        // 立件1 + 核准1 + 设置1 + 核收1 + 验收1 + 见证2 = 7（封存不另起一行，第二名见证即走完）
        assertEquals(7, t.getLedgerCount());
        List<TOadsGovCaseLog> today = svc.acceptanceRegister(new java.util.Date());
        int forCase = 0;
        for (TOadsGovCaseLog l : today) {
            if (l.getCaseId().equals(c.getId())) {
                forCase++;
            }
        }
        assertEquals(7, forCase);
        // 时序严格不逆
        for (int i = 1; i < t.getLogs().size(); i++) {
            assertFalse(t.getLogs().get(i).getActedAt().before(t.getLogs().get(i - 1).getActedAt()));
        }
    }

    @Test
    public void 同批并办幂等同键不重放第二笔() {
        // 报入幂等
        GovCaseForm f = form("S1", "DU00", "2", "88");
        f.setIdemKey("INTAKE-1");
        TOadsGovCaseIntake in1 = svc.intake(f, OP);
        TOadsGovCaseIntake in2 = svc.intake(f, OP);
        assertEquals(in1.getId(), in2.getId());
        GovCaseOutcome o1 = svc.open(in1.getId(), OP);
        // 推进幂等：同键两回只落一笔
        GovCaseOutcome a1 = svc.advance(o1.getCaseId(), "核准", OP, 2, "AREA", "ADV-2");
        GovCaseOutcome a2 = svc.advance(o1.getCaseId(), "核准重发", OP, 2, "CITY", "ADV-2");
        assertTrue(a2.isReplayed());
        assertEquals(2, svc.currentStep(o1.getCaseId()));
        int advanceCount = 0;
        for (TOadsGovCaseLog l : logs) {
            if (l.getCaseId().equals(o1.getCaseId()) && l.getStepNo() == 2
                    && GovCaseStep.ACTION_REACH.equals(l.getAction())) {
                advanceCount++;
            }
        }
        assertEquals(1, advanceCount);
        // 不同键各自落，不并不丢；进设置前把三段签署补齐
        markPermitSealed("S1");
        GovCaseOutcome a3 = svc.advance(o1.getCaseId(), "另一通道另案键", OP, null, "CITY", "ADV-OTHER");
        assertFalse(a3.isReplayed());
    }

    @Test
    public void 报入幂等键并发撞唯一索引只认先落一条() {
        // 模拟同键并发：第二次 insert 被库唯一索引顶回，服务端回首条
        when(intakeMapper.insert(any(TOadsGovCaseIntake.class))).thenAnswer(i -> {
            TOadsGovCaseIntake x = i.getArgument(0);
            for (TOadsGovCaseIntake old : intakes.values()) {
                if ("RACE".equals(old.getIdemKey())) {
                    throw new DuplicateKeyException("uk_gov_intake_idem");
                }
            }
            x.setId(++seq);
            intakes.put(x.getId(), x);
            return 1;
        });
        GovCaseForm f1 = form("S1", "DU00", "2", "88");
        f1.setIdemKey("RACE");
        TOadsGovCaseIntake first = svc.intake(f1, OP);
        GovCaseForm f2 = form("S1", "DU00", "2", "88");
        f2.setIdemKey("RACE");
        TOadsGovCaseIntake again = svc.intake(f2, OP);
        assertEquals(first.getId(), again.getId());
    }

    @Test
    public void 页首数量取后端一次查询() {
        assertEquals(0, svc.runningCount());
        TOadsGovCase c1 = openCase("S1", "DU00");
        TOadsGovCase c2 = openCase("S2", "DU00");
        assertEquals(2, svc.runningCount());
        svc.withdraw(c1.getId(), null, OP, null);
        assertEquals(1, svc.runningCount());
        TOadsGovCase c3 = walkToAcceptTwoWitnessNoCert(c2);
        svc.voidCase(c3.getId(), null, OP, null);
        assertEquals(0, svc.runningCount());
    }

    // ---------------------------------------------------------------- 整条演练辅助

    /** 立件 → 核准 →（三段签署）→ 设置。 */
    private TOadsGovCase readyAtSetup(String spot) {
        TOadsGovCase c = openCase(spot, "DU00");
        svc.advance(c.getId(), "核准", OP, null, null, null);
        markPermitSealed(spot);
        svc.advance(c.getId(), "进设置", OP, 3, null, null);
        return cases.get(c.getId());
    }

    /** 一路走到验收（核收记同意）。 */
    private TOadsGovCase readyAtAccept(String spot) {
        TOadsGovCase c = readyAtSetup(spot);
        svc.recordReceipt(c.getId(), true, "同意验收", OP, null);
        svc.advance(c.getId(), "进验收", OP, 4, null, null);
        return cases.get(c.getId());
    }

    private void sealWith(TOadsGovCase c) {
        svc.submitWitness(c.getId(), "W-A", "意见A", "CERT-X", OP, null);
        svc.submitWitness(c.getId(), "W-B", "意见B", null, OP, null);
    }

    /** 走到验收并落两名无凭证见证（停在最后一步、仍在走）。 */
    private TOadsGovCase walkToAcceptTwoWitnessNoCert(TOadsGovCase c) {
        long id = c.getId();
        if (svc.currentStep(id) < 2) {
            svc.advance(id, "核准", OP, null, null, null);
        }
        if (svc.currentStep(id) < 3) {
            markPermitSealed(c.getSpotNo());
            svc.advance(id, "进设置", OP, 3, null, null);
        }
        if (svc.currentStep(id) < 4) {
            svc.recordReceipt(id, true, "同意", OP, null);
            svc.advance(id, "进验收", OP, 4, null, null);
        }
        svc.submitWitness(id, "W-A", "意见A", null, OP, null);
        svc.submitWitness(id, "W-B", "意见B", null, OP, null);
        return cases.get(id);
    }

    private List<TOadsGovCaseLog> logsOf(Long caseId) {
        List<TOadsGovCaseLog> l = new ArrayList<TOadsGovCaseLog>();
        for (TOadsGovCaseLog x : logs) {
            if (caseId.equals(x.getCaseId())) {
                l.add(x);
            }
        }
        return l;
    }
}
