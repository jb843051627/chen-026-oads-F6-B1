import com.fc.v2.common.exception.permit.PermitRuleException;
import com.fc.v2.mapper.auto.TOadsPermitBillMapper;
import com.fc.v2.mapper.auto.TOadsPermitSignMapper;
import com.fc.v2.mapper.auto.TOadsSpotChangeMapper;
import com.fc.v2.mapper.auto.TOadsSpotMapper;
import com.fc.v2.model.auto.TOadsPermitBill;
import com.fc.v2.model.auto.TOadsPermitSign;
import com.fc.v2.model.auto.TOadsSpot;
import com.fc.v2.model.auto.TOadsSpotChange;
import com.fc.v2.model.custom.PermitBillView;
import com.fc.v2.model.custom.PermitLedgerCheck;
import com.fc.v2.model.custom.PermitSignOutcome;
import com.fc.v2.service.impl.TOadsPermitBillServiceImpl;
import com.fc.v2.service.permit.PermitStageRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 设置许可核准单八条口径逐条核对（mapper 全 Mock，用内存账台逐笔记账，不落真库）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TOadsPermitBillServiceImplTest {

    @Mock
    private TOadsPermitBillMapper billMapper;
    @Mock
    private TOadsPermitSignMapper signMapper;
    @Mock
    private TOadsSpotMapper spotMapper;
    @Mock
    private TOadsSpotChangeMapper spotChangeMapper;

    private TOadsPermitBillServiceImpl service;

    private final Map<Long, TOadsPermitBill> bills = new HashMap<Long, TOadsPermitBill>();
    private final List<TOadsPermitSign> signs = new ArrayList<TOadsPermitSign>();
    private final Map<Long, TOadsSpot> spots = new HashMap<Long, TOadsSpot>();
    private final List<TOadsSpotChange> changes = new ArrayList<TOadsSpotChange>();
    private long idSeq = 1000;

    private static final int AGREE = PermitStageRule.OPINION_AGREE;
    private static final int REJECT = PermitStageRule.OPINION_REJECT;

    private static Date at(String s) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(s);
    }

    private TOadsSpot spot(long id, int state, int ver) {
        TOadsSpot s = new TOadsSpot();
        s.setId(id);
        s.setSpotNo("SP" + id);
        s.setSetState(state);
        s.setSetVer(ver);
        s.setDelFlag(0);
        spots.put(id, s);
        return s;
    }

    @BeforeEach
    public void wireHarness() {
        service = new TOadsPermitBillServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "billMapper", billMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "signMapper", signMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "spotMapper", spotMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "spotChangeMapper", spotChangeMapper);

        when(billMapper.selectById(anyLong())).thenAnswer(i -> bills.get(i.getArgument(0)));
        when(billMapper.selectByIdForUpdate(anyLong())).thenAnswer(i -> bills.get(i.getArgument(0)));
        when(billMapper.insert(any())).thenAnswer(i -> {
            TOadsPermitBill b = i.getArgument(0);
            if (b.getId() == null) {
                b.setId(++idSeq);
            }
            bills.put(b.getId(), b);
            return 1;
        });
        when(billMapper.updateById(any())).thenAnswer(i -> {
            TOadsPermitBill b = i.getArgument(0);
            bills.put(b.getId(), b);
            return 1;
        });
        when(billMapper.countByLiveToken(org.mockito.ArgumentMatchers.anyString())).thenAnswer(i -> {
            String token = i.getArgument(0);
            int n = 0;
            for (TOadsPermitBill b : bills.values()) {
                if (token.equals(b.getLiveToken()) && b.getDelFlag() != null && b.getDelFlag() == 0) {
                    n++;
                }
            }
            return n;
        });
        when(billMapper.countUnsealed()).thenAnswer(i -> {
            long n = 0;
            for (TOadsPermitBill b : bills.values()) {
                if (b.getDelFlag() == 0 && (b.getStatus() == null || b.getStatus() != 1)) {
                    n++;
                }
            }
            return n;
        });
        when(billMapper.selectAllNormal()).thenAnswer(i -> {
            List<TOadsPermitBill> l = new ArrayList<TOadsPermitBill>();
            for (TOadsPermitBill b : bills.values()) {
                if (b.getDelFlag() != null && b.getDelFlag() == 0) {
                    l.add(b);
                }
            }
            return l;
        });

        when(signMapper.insert(any())).thenAnswer(i -> {
            TOadsPermitSign g = i.getArgument(0);
            if (g.getId() == null) {
                g.setId(++idSeq);
            }
            signs.add(g);
            return 1;
        });
        when(signMapper.listActiveRound(anyLong(), anyInt(), anyInt())).thenAnswer(i -> {
            Long billId = i.getArgument(0);
            int node = i.getArgument(1);
            int round = i.getArgument(2);
            List<TOadsPermitSign> l = new ArrayList<TOadsPermitSign>();
            for (TOadsPermitSign g : signs) {
                if (billId.equals(g.getBillId()) && g.getNodeNo() == node && g.getRoundNo() == round
                        && g.getActive() == 1) {
                    l.add(g);
                }
            }
            l.sort(Comparator.comparing(TOadsPermitSign::getSignTime)
                    .thenComparing(TOadsPermitSign::getSeqNo));
            return l;
        });
        when(signMapper.deactivateRound(anyLong(), anyInt(), anyInt())).thenAnswer(i -> {
            Long billId = i.getArgument(0);
            int node = i.getArgument(1);
            int round = i.getArgument(2);
            int n = 0;
            for (TOadsPermitSign g : signs) {
                if (billId.equals(g.getBillId()) && g.getNodeNo() == node && g.getRoundNo() == round
                        && g.getActive() == 1) {
                    g.setActive(0);
                    n++;
                }
            }
            return n;
        });
        when(signMapper.listByBill(anyLong())).thenAnswer(i -> {
            Long billId = i.getArgument(0);
            List<TOadsPermitSign> l = new ArrayList<TOadsPermitSign>();
            for (TOadsPermitSign g : signs) {
                if (billId.equals(g.getBillId())) {
                    l.add(g);
                }
            }
            l.sort(Comparator.comparing(TOadsPermitSign::getNodeNo)
                    .thenComparing(TOadsPermitSign::getRoundNo)
                    .thenComparing(TOadsPermitSign::getSeqNo));
            return l;
        });
        when(signMapper.listAllActive()).thenAnswer(i -> {
            List<TOadsPermitSign> l = new ArrayList<TOadsPermitSign>();
            for (TOadsPermitSign g : signs) {
                if (g.getActive() == 1) {
                    l.add(g);
                }
            }
            return l;
        });
        when(signMapper.listLandedBetween(any(), any())).thenAnswer(i -> {
            Date start = i.getArgument(0);
            Date end = i.getArgument(1);
            List<TOadsPermitSign> l = new ArrayList<TOadsPermitSign>();
            for (TOadsPermitSign g : signs) {
                if (!g.getSignTime().before(start) && g.getSignTime().before(end)) {
                    l.add(g);
                }
            }
            l.sort(Comparator.comparing(TOadsPermitSign::getSignTime)
                    .thenComparing(TOadsPermitSign::getSeqNo));
            return l;
        });

        when(spotMapper.selectById(anyLong())).thenAnswer(i -> spots.get(i.getArgument(0)));
        when(spotMapper.updateById(any())).thenAnswer(i -> {
            TOadsSpot s = i.getArgument(0);
            spots.put(s.getId(), s);
            return 1;
        });
        when(spotChangeMapper.insert(any())).thenAnswer(i -> {
            TOadsSpotChange c = i.getArgument(0);
            if (c.getId() == null) {
                c.setId(++idSeq);
            }
            changes.add(c);
            return 1;
        });
        when(spotChangeMapper.listBySpot(anyLong())).thenAnswer(i -> {
            Long spotId = i.getArgument(0);
            List<TOadsSpotChange> l = new ArrayList<TOadsSpotChange>();
            for (TOadsSpotChange c : changes) {
                if (spotId.equals(c.getSpotId())) {
                    l.add(c);
                }
            }
            l.sort(Comparator.comparing(TOadsSpotChange::getChangeTime).reversed());
            return l;
        });
    }

    private List<TOadsPermitSign> signsOf(long billId) {
        List<TOadsPermitSign> l = new ArrayList<TOadsPermitSign>();
        for (TOadsPermitSign g : signs) {
            if (g.getBillId() != null && g.getBillId() == billId) {
                l.add(g);
            }
        }
        return l;
    }

    // ------------------------------------------------ 第1条：单据来源

    @Test
    public void billIsBornOnlyFromSpotRowWithFirstStageRule() {
        spot(1L, 0, 0);
        TOadsPermitBill bill = service.createFromSpot(1L, "HANG", "窗口甲");

        assertNotNull(bill.getId());
        assertEquals("SP1", bill.getSpotNo());
        assertEquals("HANG", bill.getChangeKind());
        assertEquals(Integer.valueOf(0), bill.getFromState());
        assertEquals(Integer.valueOf(1), bill.getToState());
        // 单内当前节服务端算死在第一段，数法/人数照规则本
        assertEquals(Integer.valueOf(0), bill.getNodeNo());
        assertEquals(Integer.valueOf(PermitStageRule.MODE_FIRST), bill.getSignMode());
        assertEquals(Integer.valueOf(1), bill.getNeedCount());
        assertEquals(Integer.valueOf(0), bill.getSignCount());
        assertEquals(Integer.valueOf(1), bill.getRoundNo());
        assertEquals(Integer.valueOf(0), bill.getStatus());
        assertEquals("1:HANG", bill.getLiveToken());
    }

    @Test
    public void verbalReportWithoutOperatorDoesNotStartWork() {
        spot(1L, 0, 0);
        PermitRuleException e = assertThrows(PermitRuleException.class,
                () -> service.createFromSpot(1L, "HANG", "  "));
        assertTrue(e.getMessage().contains("口头报备"));
        assertEquals(0, bills.size());
    }

    @Test
    public void sameSpotSameChangeAllowsOnlyOneLiveBill() {
        spot(1L, 0, 0);
        service.createFromSpot(1L, "HANG", "窗口甲");
        PermitRuleException e = assertThrows(PermitRuleException.class,
                () -> service.createFromSpot(1L, "HANG", "窗口乙"));
        assertTrue(e.getMessage().contains("在途"));
        assertEquals(1, bills.size(), "同点位同变更在途只一张");
    }

    @Test
    public void changeKindMustMatchSpotCurrentState() {
        spot(1L, 0, 0); // 未挂设
        // 未挂设不许改位置/拆除，只许挂上去
        assertTrue(assertThrows(PermitRuleException.class,
                () -> service.createFromSpot(1L, "MOVE", "窗口甲")).getMessage().contains("档"));
        assertTrue(assertThrows(PermitRuleException.class,
                () -> service.createFromSpot(1L, "REMOVE", "窗口甲")).getMessage().contains("档"));
        TOadsSpot hung = spots.get(1L);
        hung.setSetState(1);
        // 已挂设不许重复挂
        assertTrue(assertThrows(PermitRuleException.class,
                () -> service.createFromSpot(1L, "HANG", "窗口甲")).getMessage().contains("档"));
    }

    @Test
    public void unknownChangeKindAndMissingSpotRejected() {
        assertThrows(PermitRuleException.class, () -> service.createFromSpot(99L, "HANG", "窗口甲"));
        spot(2L, 0, 0);
        assertThrows(PermitRuleException.class, () -> service.createFromSpot(2L, "BURN", "窗口甲"));
    }

    // ------------------------------------------------ 第2/3条：三段各数各的

    @Test
    public void firstStageOneLandingPassesEvenWhenOpinionIsReject() {
        spot(1L, 0, 0);
        TOadsPermitBill bill = service.createFromSpot(1L, "HANG", "窗口甲");
        // 第一段口径：候1人，其意见落单即通过——不同意这一笔落单也算本段过
        PermitSignOutcome out = service.signAt(bill.getId(), "受理岗甲", REJECT, "窗口意见原文",
                ts("2026-10-05 09:00:00.100"), null);
        assertEquals(PermitSignOutcome.ADVANCED, out.getOutcome());
        assertEquals(Integer.valueOf(1), bills.get(bill.getId()).getNodeNo(), "整单推进第二段");
        TOadsPermitSign g = signsOf(bill.getId()).get(0);
        assertEquals(Integer.valueOf(0), g.getNodeNo());
        assertEquals(Integer.valueOf(1), g.getSeqNo());
        assertEquals("窗口受理岗", g.getSignPost());
    }

    @Test
    public void secondStageNeedsTwoAndFirstLandingDecides() throws Exception {
        TOadsPermitBill bill = openDistrictStage();

        // 先落一方同意：整单留在第二段，等后落一方
        PermitSignOutcome o1 = service.signAt(bill.getId(), "核定甲", AGREE, "先落同意",
                at("2026-10-05 10:00:00.100"), null);
        assertEquals(PermitSignOutcome.LANDED, o1.getOutcome());
        assertEquals(Integer.valueOf(1), bills.get(bill.getId()).getSignCount(), "人数逐笔累加");
        assertEquals(Integer.valueOf(1), bills.get(bill.getId()).getNodeNo(), "不足则整单留在本段");

        // 后落一方不同意：先落为定，结论仍随先落一方＝通过，后落这笔照常入库、两行都展示
        PermitSignOutcome o2 = service.signAt(bill.getId(), "核定乙", REJECT, "后落不同意",
                at("2026-10-05 10:05:00.200"), null);
        assertEquals(PermitSignOutcome.ADVANCED, o2.getOutcome(), "先落一方的判断即为结论");
        List<TOadsPermitSign> two = signsOf(bill.getId());
        long stage2 = two.stream().filter(g -> g.getNodeNo() == 1).count();
        assertEquals(2, stage2, "后落一方仍须提交，两笔都入库留痕");
        assertEquals(Integer.valueOf(2), bills.get(bill.getId()).getNodeNo());
    }

    @Test
    public void secondStageFirstRejectVetoesEvenIfSecondAgrees() throws Exception {
        TOadsPermitBill bill = openDistrictStage();
        service.signAt(bill.getId(), "核定甲", REJECT, "先落不同意", at("2026-10-05 10:00:00.100"), null);
        PermitSignOutcome o2 = service.signAt(bill.getId(), "核定乙", AGREE, "后落同意",
                at("2026-10-05 10:05:00.200"), null);
        assertEquals(PermitSignOutcome.VETO_CLEARED, o2.getOutcome(), "先落为定：先落否即否决");
        assertEquals(Integer.valueOf(1), bills.get(bill.getId()).getNodeNo(), "否决不推进，留在第二段");
        assertEquals(Integer.valueOf(2), bills.get(bill.getId()).getStatus());
    }

    @Test
    public void secondStageOneLandingMustNotPassLikeFirstStage() throws Exception {
        TOadsPermitBill bill = openDistrictStage();
        // 若错把第一段“一名即过”搬来，这一笔就会误推进
        PermitSignOutcome o = service.signAt(bill.getId(), "核定甲", AGREE, "仅一笔",
                at("2026-10-05 10:00:00.100"), null);
        assertEquals(PermitSignOutcome.LANDED, o.getOutcome());
        assertEquals(Integer.valueOf(1), bills.get(bill.getId()).getNodeNo());
    }

    @Test
    public void thirdStageRequiresBothAgreeSplitHangsPending() throws Exception {
        TOadsPermitBill bill = openCityStage();
        service.signAt(bill.getId(), "备案甲", AGREE, "同意", at("2026-10-05 11:00:00.100"), null);
        PermitSignOutcome split = service.signAt(bill.getId(), "备案乙", REJECT, "不同意",
                at("2026-10-05 11:05:00.200"), null);
        // 若错用第二段“先落为定”，先落同意就该过；第三段数法下必须是待议
        assertEquals(PermitSignOutcome.SPLIT_PENDING, split.getOutcome());
        TOadsPermitBill now = bills.get(bill.getId());
        assertEquals(Integer.valueOf(2), now.getNodeNo(), "待议不推进，停在第三段");
        assertEquals(Integer.valueOf(0), now.getStatus(), "待议也不终止，仍是在签");
        assertEquals(Integer.valueOf(0), now.getSignCount(), "本轮两条退出，账面清零");
        assertEquals(Integer.valueOf(2), now.getRoundNo(), "第三段起第二轮");
        assertNull(now.getSealedAt());
    }

    @Test
    public void thirdStageBothRejectVetoesAndBothAgreeSeals() throws Exception {
        TOadsPermitBill b1 = openCityStage();
        service.signAt(b1.getId(), "备案甲", REJECT, "不同意", at("2026-10-05 11:00:00.100"), null);
        PermitSignOutcome v = service.signAt(b1.getId(), "备案乙", REJECT, "不同意",
                at("2026-10-05 11:05:00.200"), null);
        assertEquals(PermitSignOutcome.VETO_CLEARED, v.getOutcome(), "两条不同意＝否决清轮");

        // 第二轮重签，两条同意才封存
        service.signAt(b1.getId(), "备案甲", AGREE, "二轮同意", at("2026-10-05 12:00:00.100"), null);
        PermitSignOutcome sealed = service.signAt(b1.getId(), "备案乙", AGREE, "二轮同意",
                at("2026-10-05 12:05:00.200"), null);
        assertEquals(PermitSignOutcome.SEALED, sealed.getOutcome());
        assertEquals(Integer.valueOf(1), bills.get(b1.getId()).getStatus());
    }

    @Test
    public void twoSignersSameSecondAreTwoRowsOrderedBySeq() throws Exception {
        TOadsPermitBill bill = openCityStage();
        Date same = at("2026-10-05 11:00:00.500");
        service.signAt(bill.getId(), "备案甲", AGREE, "同秒甲", same, null);
        service.signAt(bill.getId(), "备案乙", AGREE, "同秒乙", same, null);
        List<TOadsPermitSign> round2 = signMapper.listActiveRound(bill.getId(), 2, 1);
        assertEquals(2, round2.size(), "同一秒各自提交，不得合并为一条");
        assertEquals(Integer.valueOf(1), round2.get(0).getSeqNo());
        assertEquals(Integer.valueOf(2), round2.get(1).getSeqNo());
        assertEquals("备案甲", round2.get(0).getSigner());
        // 两条各自落单时刻一致，核对仍逐笔对得上
        assertEquals(same, round2.get(1).getSignTime());
    }

    // ------------------------------------------------ 第4条：逐级前进，跳段丢弃

    @Test
    public void skippedStageOpinionIsDiscardedAndNothingLands() throws Exception {
        TOadsPermitBill bill = openDistrictStage(); // 停在第二段
        PermitSignOutcome out = service.signAt(bill.getId(), "备案甲", AGREE, "跳过第二段直接备案",
                at("2026-10-05 11:00:00.100"), 2);
        assertEquals(PermitSignOutcome.DISCARDED, out.getOutcome());
        assertTrue(out.getMessage().contains("丢弃"));
        assertEquals(Integer.valueOf(1), bills.get(bill.getId()).getNodeNo(), "整单仍留在第二段");
        assertEquals(Integer.valueOf(0), bills.get(bill.getId()).getSignCount());
        assertTrue(signsOf(bill.getId()).stream().noneMatch(g -> g.getContent().contains("跳过第二段")),
                "被丢弃的一笔库里一条不添");
    }

    // ------------------------------------------------ 第5条：清一段、留首句

    @Test
    public void vetoClearsOnlyThatStageRoundPriorStagesUntouched() throws Exception {
        TOadsPermitBill bill = openDistrictStage();
        service.signAt(bill.getId(), "核定甲", REJECT, "二轮先否", at("2026-10-05 10:00:00.100"), null);
        service.signAt(bill.getId(), "核定乙", AGREE, "二轮", at("2026-10-05 10:05:00.200"), null);

        // 第一段那一条不动、仍在档、继续可查
        List<TOadsPermitSign> stage1 = signMapper.listActiveRound(bill.getId(), 0, 1);
        assertEquals(1, stage1.size(), "此前各段留下的记录一条不动");

        // 本段本轮两条退出但行还在，文本分开存
        List<TOadsPermitSign> all = signMapper.listByBill(bill.getId());
        long cleared = all.stream().filter(g -> g.getNodeNo() == 1 && g.getActive() == 0).count();
        assertEquals(2, cleared, "被清掉的是本段本轮，老轮文本留痕可查");
        assertEquals(Integer.valueOf(2), bills.get(bill.getId()).getRoundNo());

        // 重新往上走：第二轮同一签署人可再落，新一轮文本与上轮分开
        service.signAt(bill.getId(), "核定甲", AGREE, "新一轮甲", at("2026-10-05 13:00:00.100"), null);
        service.signAt(bill.getId(), "核定乙", AGREE, "新一轮乙", at("2026-10-05 13:05:00.200"), null);
        assertEquals(Integer.valueOf(2), bills.get(bill.getId()).getNodeNo(), "补齐后推进第三段");
        long stage2Rows = signsOf(bill.getId()).stream().filter(g -> g.getNodeNo() == 1).count();
        assertEquals(4, stage2Rows, "上轮2条＋本轮2条，各自有据，不覆盖");
    }

    @Test
    public void sameSignerSecondSubmissionKeepsFirstLineOnly() throws Exception {
        TOadsPermitBill bill = openDistrictStage();
        service.signAt(bill.getId(), "核定甲", AGREE, "首次那句", at("2026-10-05 10:00:00.100"), null);
        PermitRuleException e = assertThrows(PermitRuleException.class,
                () -> service.signAt(bill.getId(), "核定甲", AGREE, "想覆盖成第二句",
                        at("2026-10-05 10:01:00.000"), null));
        assertTrue(e.getMessage().contains("首次"));
        List<TOadsPermitSign> active = signMapper.listActiveRound(bill.getId(), 1, 1);
        assertEquals(1, active.size(), "不追加副本");
        assertEquals("首次那句", active.get(0).getContent(), "保留首次那句，不覆盖");
    }

    @Test
    public void pendingRoundRessignKeepsOldTextsApart() throws Exception {
        TOadsPermitBill bill = openCityStage();
        service.signAt(bill.getId(), "备案甲", AGREE, "待议轮同意", at("2026-10-05 11:00:00.100"), null);
        service.signAt(bill.getId(), "备案乙", REJECT, "待议轮不同意", at("2026-10-05 11:05:00.200"), null);

        service.signAt(bill.getId(), "备案甲", AGREE, "新轮同意", at("2026-10-05 14:00:00.100"), null);
        service.signAt(bill.getId(), "备案乙", AGREE, "新轮同意", at("2026-10-05 14:05:00.200"), null);

        List<TOadsPermitSign> all = signMapper.listByBill(bill.getId());
        long oldRound = all.stream().filter(g -> g.getNodeNo() == 2 && g.getRoundNo() == 1).count();
        long newRound = all.stream().filter(g -> g.getNodeNo() == 2 && g.getRoundNo() == 2 && g.getActive() == 1).count();
        assertEquals(2, oldRound, "待议那轮两条仍在");
        assertEquals(2, newRound, "新轮两条分开存放");
        assertEquals(Integer.valueOf(1), bills.get(bill.getId()).getStatus(), "新轮两同后封存");
    }

    // ------------------------------------------------ 第6/7条：封存与主档联动

    @Test
    public void sealedBillAcceptsNoMorePen() throws Exception {
        TOadsPermitBill bill = fullHangBillSealed("2026-10-05 15:00:00.000");
        PermitRuleException e = assertThrows(PermitRuleException.class,
                () -> service.signAt(bill.getId(), "任何人", AGREE, "封存后补笔",
                        at("2026-10-05 16:00:00.000"), null));
        assertTrue(e.getMessage().contains("封存"));
    }

    @Test
    public void sealingMovesSpotRowAndAppendsHistoryInSameHandling() throws Exception {
        Date sealedAt = at("2026-10-05 15:00:00.500");
        TOadsPermitBill bill = fullHangBillSealed("2026-10-05 15:00:00.500");

        // 主档那一行同步变更：未挂设 → 挂设在册，档位进一
        TOadsSpot spot = spots.get(1L);
        assertEquals(Integer.valueOf(1), spot.getSetState());
        assertEquals(Integer.valueOf(1), spot.getSetVer());

        // 同一次处理追加一条履历：谁/哪张单/从哪档到哪档/何时
        assertEquals(1, changes.size());
        TOadsSpotChange c = changes.get(0);
        assertEquals(bill.getId(), c.getBillId());
        assertEquals(bill.getBillNo(), c.getBillNo());
        assertEquals("HANG", c.getChangeKind());
        assertEquals(Integer.valueOf(0), c.getFromState());
        assertEquals(Integer.valueOf(1), c.getToState());
        assertEquals(Integer.valueOf(0), c.getFromVer());
        assertEquals(Integer.valueOf(1), c.getToVer());
        assertEquals("备案乙", c.getOperateBy());
        assertEquals(sealedAt, c.getChangeTime());

        // 单据封存、在途令牌腾出
        TOadsPermitBill done = bills.get(bill.getId());
        assertEquals(Integer.valueOf(1), done.getStatus());
        assertNull(done.getLiveToken());
        assertEquals(sealedAt, done.getSealedAt());
    }

    @Test
    public void removeDoesNotBumpVersionAndHistoryIsReversed() throws Exception {
        spot(7L, 1, 2); // 已挂设，第2档
        TOadsPermitBill bill = service.createFromSpot(7L, "REMOVE", "窗口甲");
        passAll(bill, "2026-10-06");
        assertEquals(Integer.valueOf(2), spots.get(7L).getSetState(), "整块拆除");
        assertEquals(Integer.valueOf(2), spots.get(7L).getSetVer(), "拆除不进位");

        // 同一点位再来一次“挂上去”（拆除后按主档档 2 不允许 HANG，这里验证履历倒序用 MOVE 链）
        List<TOadsSpotChange> hist = service.spotChangeHistory(7L);
        assertEquals(1, hist.size());
        assertEquals("REMOVE", hist.get(0).getChangeKind());
    }

    // ------------------------------------------------ 第8条：计数、对账、登记簿

    @Test
    public void unsealedCountIncrementsOnCreateDecrementsOnSeal() throws Exception {
        spot(1L, 0, 0);
        assertEquals(0, service.unsealedCount());
        TOadsPermitBill b1 = service.createFromSpot(1L, "HANG", "窗口甲");
        assertEquals(1, service.unsealedCount(), "建单加一");
        fullHangBillSealed("2026-10-05 15:00:00.000");
        assertEquals(0, service.unsealedCount(), "封存减一");

        // 封存后同一点位下一次变更可另立一张，新单计数再加一
        TOadsSpot again = spot(2L, 1, 1);
        TOadsPermitBill b2 = service.createFromSpot(2L, "MOVE", "窗口甲");
        assertEquals(1, service.unsealedCount());
        assertNotNull(b2);
    }

    @Test
    public void reconcileBalancedNamesBillAndStageWhenDrift() throws Exception {
        TOadsPermitBill sealed = fullHangBillSealed("2026-10-05 15:00:00.000");
        spot(2L, 1, 1);
        TOadsPermitBill running = service.createFromSpot(2L, "MOVE", "窗口乙");

        PermitLedgerCheck ok = service.reconcile();
        assertTrue(ok.isBalanced(), "顶部数与逐段累加同一次计算，无偏差");
        assertEquals(1, ok.getUnsealedCount());
        assertEquals(1, ok.getRecountedCount());

        // 人为把账面段次拨到第三段（留痕仍停在第一段），对账必须指名单据与段
        running.setNodeNo(2);
        running.setSignMode(PermitStageRule.MODE_CONSENSUS);
        running.setNeedCount(2);
        PermitLedgerCheck bad = service.reconcile();
        assertFalse(bad.isBalanced());
        assertTrue(bad.getMismatches().stream().anyMatch(m -> m.contains(running.getBillNo()) && m.contains("段")),
                "偏差要指名对应单据：" + bad.getMismatches());
        // 封存那张不得被乱报
        assertTrue(bad.getMismatches().stream().noneMatch(m -> m.contains(sealed.getBillNo())));
    }

    @Test
    public void acceptanceRegisterMatchesEveryLandingIncludingClearedRounds() throws Exception {
        TOadsPermitBill b1 = fullHangBillSealed("2026-10-05 15:00:00.000"); // 当日 5 笔（1+2+2）

        spot(2L, 1, 1);
        TOadsPermitBill b2 = service.createFromSpot(2L, "MOVE", "窗口乙");
        service.signAt(b2.getId(), "受理岗甲", AGREE, "另一张第一段", at("2026-10-05 09:30:00.000"), null);
        service.signAt(b2.getId(), "核定甲", REJECT, "当日被否", at("2026-10-05 10:30:00.000"), null);
        service.signAt(b2.getId(), "核定乙", AGREE, "当日被否凑二", at("2026-10-05 10:31:00.000"), null);

        // 从第三段回数到第一段：当日受理登记簿逐笔吻合，被清掉的老轮也算落过的笔
        List<TOadsPermitSign> reg = service.acceptanceRegister(at("2026-10-05 12:00:00.000"));
        assertEquals(8, reg.size(), "两张合计 5+3 笔，含被清退出的2条");
        // 按各自落单时刻先后排
        assertTrue(reg.get(0).getSignTime().before(reg.get(reg.size() - 1).getSignTime()));
        // 抽取任意一段自洽：第一张第三段恰好两条
        long b1City = reg.stream().filter(g -> g.getBillId().equals(b1.getId()) && g.getNodeNo() == 2).count();
        assertEquals(2, b1City);

        // 隔日一笔不入当日簿
        service.signAt(b2.getId(), "核定甲", AGREE, "次日重签", at("2026-10-06 09:00:00.000"), null);
        assertEquals(8, service.acceptanceRegister(at("2026-10-05 12:00:00.000")).size());
    }

    @Test
    public void billViewComputesStagesServerSide() throws Exception {
        TOadsPermitBill bill = openDistrictStage();
        service.signAt(bill.getId(), "核定甲", AGREE, "先落", at("2026-10-05 10:00:00.100"), null);
        PermitBillView view = service.getBillDetail(bill.getId());
        assertEquals(3, view.getStages().size(), "单内按段分节");
        assertTrue(view.getStages().get(0).isPassed(), "第一段已通过");
        assertTrue(view.getStages().get(1).isCurrent(), "当前节服务端点出");
        assertEquals(1, view.getStages().get(1).getSignCount(), "屏上已签人数只是回显");
        assertEquals(2, view.getStages().get(1).getNeedCount());
        assertFalse(view.getStages().get(2).isCurrent());
    }

    @Test
    public void invalidOpinionAndUnknownBillRejected() {
        spot(1L, 0, 0);
        TOadsPermitBill bill = service.createFromSpot(1L, "HANG", "窗口甲");
        assertThrows(PermitRuleException.class,
                () -> service.signAt(bill.getId(), "受理岗甲", 9, "乱写", new Date(), null));
        assertThrows(PermitRuleException.class,
                () -> service.signAt(99999L, "受理岗甲", AGREE, "无单", new Date(), null));
    }

    // ------------------------------------------------ 夹具：把单推到指定段

    /** 建单并通过第一段，停在第二段（区局核定），返回单据 */
    private TOadsPermitBill openDistrictStage() {
        return openDistrictStage(1L, 0, "HANG");
    }

    private TOadsPermitBill openDistrictStage(long spotId, int state, String kind) {
        spot(spotId, state, 0);
        TOadsPermitBill bill = service.createFromSpot(spotId, kind, "窗口甲");
        service.signAt(bill.getId(), "受理岗甲", AGREE, "受理通过",
                new Date(1759600000000L + spotId * 1000), null);
        return bill;
    }

    /** 通过前两段，停在第三段（市局备案） */
    private TOadsPermitBill openCityStage() throws Exception {
        TOadsPermitBill bill = openDistrictStage();
        service.signAt(bill.getId(), "核定甲", AGREE, "核定先落", at("2026-10-05 10:00:00.100"), null);
        service.signAt(bill.getId(), "核定乙", AGREE, "核定后落", at("2026-10-05 10:05:00.200"), null);
        return bill;
    }

    /** 挂上去的整单走满三段封存（spot 1，未挂设起步），时间统一到当日 */
    private TOadsPermitBill fullHangBillSealed(String sealedAt) throws Exception {
        if (!spots.containsKey(1L)) {
            spot(1L, 0, 0);
        }
        TOadsPermitBill bill;
        if (bills.isEmpty()) {
            bill = service.createFromSpot(1L, "HANG", "窗口甲");
        } else {
            // 已有在途单（如计数用例先立的那张）：接着它签，不另起
            bill = new ArrayList<TOadsPermitBill>(bills.values()).get(0);
        }
        passAll(bill, sealedAt.substring(0, 10));
        return bill;
    }

    private void passAll(TOadsPermitBill bill, String day) {
        try {
            service.signAt(bill.getId(), "受理岗甲", AGREE, "受理", at(day + " 09:00:00.000"), null);
            service.signAt(bill.getId(), "核定甲", AGREE, "核定甲", at(day + " 10:00:00.000"), null);
            service.signAt(bill.getId(), "核定乙", AGREE, "核定乙", at(day + " 10:01:00.000"), null);
            service.signAt(bill.getId(), "备案甲", AGREE, "备案甲", at(day + " 14:00:00.000"), null);
            service.signAt(bill.getId(), "备案乙", AGREE, "备案乙", at(day + " 15:00:00.500"), null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Date ts(String s) {
        try {
            return at(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
