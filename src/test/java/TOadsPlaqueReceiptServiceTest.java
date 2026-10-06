import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.common.exception.plaque.PlaqueRuleException;
import com.fc.v2.mapper.auto.TOadsPlaqueBookMapper;
import com.fc.v2.mapper.auto.TOadsPlaqueFlowMapper;
import com.fc.v2.mapper.auto.TOadsPlaqueLimitMapper;
import com.fc.v2.mapper.auto.TOadsPlaqueRowMapper;
import com.fc.v2.mapper.auto.TOadsSpotMapper;
import com.fc.v2.model.auto.TOadsPlaqueBook;
import com.fc.v2.model.auto.TOadsPlaqueFlow;
import com.fc.v2.model.auto.TOadsPlaqueLimit;
import com.fc.v2.model.auto.TOadsPlaqueRow;
import com.fc.v2.model.auto.TOadsSpot;
import com.fc.v2.model.custom.PlaqueArchiveResult;
import com.fc.v2.model.custom.PlaqueBookForm;
import com.fc.v2.model.custom.PlaqueReceipt;
import com.fc.v2.model.custom.PlaqueReconcile;
import com.fc.v2.model.custom.PlaqueRowForm;
import com.fc.v2.service.impl.TOadsPlaqueReceiptServiceImpl;
import com.fc.v2.service.plaque.PlaqueConsts;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 牌匾更换名册收讫四步口径核对（mapper 全 Mock，内存账台，不落真库）：
 * 点行数只数物理行（同码两行各一条）、抬头唯一来源与认 200 的三种情形、
 * 数量同到/编码配主档/缺照片占位/第四种结论不写入、第3步五条退回逐句、
 * 回执三数回库逐行与互印点名、重复送达不重复入行、补挂同次再裁、
 * 对账以流水为准抹平销账、满两年度待核行转历史卷宗层、主档出入以主档为准。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TOadsPlaqueReceiptServiceTest {

    @Mock private TOadsPlaqueBookMapper bookMapper;
    @Mock private TOadsPlaqueRowMapper rowMapper;
    @Mock private TOadsPlaqueFlowMapper flowMapper;
    @Mock private TOadsPlaqueLimitMapper limitMapper;
    @Mock private TOadsSpotMapper spotMapper;

    private TOadsPlaqueReceiptServiceImpl svc;

    private final Map<Long, TOadsPlaqueBook> books = new LinkedHashMap<Long, TOadsPlaqueBook>();
    private final Map<Long, TOadsPlaqueRow> rows = new LinkedHashMap<Long, TOadsPlaqueRow>();
    private final List<TOadsPlaqueFlow> flows = new ArrayList<TOadsPlaqueFlow>();
    private final Map<String, TOadsSpot> spots = new LinkedHashMap<String, TOadsSpot>();
    private final List<TOadsPlaqueLimit> limits = new ArrayList<TOadsPlaqueLimit>();
    private long seq = 1000;

    private static String stringParam(Wrapper<?> w) {
        com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?> aw =
                (com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?>) w;
        aw.getSqlSegment();
        for (Object v : aw.getParamNameValuePairs().values()) {
            if (v instanceof String) {
                return (String) v;
            }
        }
        return null;
    }

    @BeforeEach
    public void wire() {
        // 点位主档：SP0001 在 DU00（当地线 50），SP0002 在 DU02（走全区缺省线 100）
        masterSpot("SP0001", "DU00", "临江路一号牌匾");
        masterSpot("SP0002", "DU02", "青石巷二号牌匾");
        limit("DEFAULT", new BigDecimal("100"));
        limit("DU00", new BigDecimal("50"));

        svc = new TOadsPlaqueReceiptServiceImpl();
        ReflectionTestUtils.setField(svc, "bookMapper", bookMapper);
        ReflectionTestUtils.setField(svc, "rowMapper", rowMapper);
        ReflectionTestUtils.setField(svc, "flowMapper", flowMapper);
        ReflectionTestUtils.setField(svc, "limitMapper", limitMapper);
        ReflectionTestUtils.setField(svc, "spotMapper", spotMapper);

        when(bookMapper.selectList(any())).thenAnswer(i -> {
            String no = stringParam(i.getArgument(0));
            List<TOadsPlaqueBook> l = new ArrayList<TOadsPlaqueBook>();
            for (TOadsPlaqueBook b : books.values()) {
                if (no == null || no.equals(b.getBatchNo())) {
                    l.add(b);
                }
            }
            return l;
        });
        when(bookMapper.insert(any(TOadsPlaqueBook.class))).thenAnswer(i -> {
            TOadsPlaqueBook b = i.getArgument(0);
            b.setId(++seq);
            books.put(b.getId(), b);
            return 1;
        });
        when(bookMapper.updateById(any(TOadsPlaqueBook.class))).thenAnswer(i -> {
            TOadsPlaqueBook b = i.getArgument(0);
            books.put(b.getId(), b);
            return 1;
        });

        when(rowMapper.insert(any(TOadsPlaqueRow.class))).thenAnswer(i -> {
            TOadsPlaqueRow r = i.getArgument(0);
            r.setId(++seq);
            rows.put(r.getId(), r);
            return 1;
        });
        when(rowMapper.updateById(any(TOadsPlaqueRow.class))).thenAnswer(i -> {
            TOadsPlaqueRow r = i.getArgument(0);
            rows.put(r.getId(), r);
            return 1;
        });
        when(rowMapper.listByBook(anyLong())).thenAnswer(i -> {
            Long bookId = i.getArgument(0);
            List<TOadsPlaqueRow> l = new ArrayList<TOadsPlaqueRow>();
            for (TOadsPlaqueRow r : rows.values()) {
                if (bookId.equals(r.getBookId())) {
                    l.add(r);
                }
            }
            l.sort((a, b) -> Integer.compare(a.getRowNo(), b.getRowNo()));
            return l;
        });
        when(rowMapper.listByBookLayer(anyLong(), anyInt())).thenAnswer(i -> {
            Long bookId = i.getArgument(0);
            int layer = i.getArgument(1);
            List<TOadsPlaqueRow> l = new ArrayList<TOadsPlaqueRow>();
            for (TOadsPlaqueRow r : rows.values()) {
                if (bookId.equals(r.getBookId()) && r.getLayer() != null && r.getLayer() == layer) {
                    l.add(r);
                }
            }
            l.sort((a, b) -> Integer.compare(a.getRowNo(), b.getRowNo()));
            return l;
        });

        when(flowMapper.insert(any(TOadsPlaqueFlow.class))).thenAnswer(i -> {
            TOadsPlaqueFlow f = i.getArgument(0);
            f.setId(++seq);
            flows.add(f);
            return 1;
        });
        when(flowMapper.listByBatch(any())).thenAnswer(i -> {
            String no = i.getArgument(0);
            List<TOadsPlaqueFlow> l = new ArrayList<TOadsPlaqueFlow>();
            for (TOadsPlaqueFlow f : flows) {
                if (no.equals(f.getBatchNo())) {
                    l.add(f);
                }
            }
            l.sort((a, b) -> {
                int t = a.getActedAt().compareTo(b.getActedAt());
                return t != 0 ? t : Long.compare(a.getId(), b.getId());
            });
            return l;
        });

        when(limitMapper.selectList(any())).thenAnswer(i -> new ArrayList<TOadsPlaqueLimit>(limits));
        when(spotMapper.selectList(any())).thenAnswer(i -> {
            String no = stringParam(i.getArgument(0));
            List<TOadsSpot> l = new ArrayList<TOadsSpot>();
            for (TOadsSpot s : spots.values()) {
                if (no == null || no.equals(s.getSpotNo())) {
                    l.add(s);
                }
            }
            return l;
        });
    }

    private void masterSpot(String no, String areaNo, String content) {
        TOadsSpot s = new TOadsSpot();
        s.setId((long) (spots.size() + 1));
        s.setSpotNo(no);
        s.setAreaNo(areaNo);
        s.setContent(content);
        s.setDelFlag(0);
        spots.put(no, s);
    }

    private void limit(String areaNo, BigDecimal v) {
        TOadsPlaqueLimit l = new TOadsPlaqueLimit();
        l.setId((long) (limits.size() + 1));
        l.setAreaNo(areaNo);
        l.setQtyLimit(v);
        l.setStatus(0);
        l.setDelFlag(0);
        limits.add(l);
    }

    private PlaqueRowForm row(String code, String viewName, String road, String qty, String chk, String photo) {
        PlaqueRowForm r = new PlaqueRowForm();
        r.setSpotNo(code);
        r.setViewName(viewName);
        r.setRoadCode(road);
        r.setQtyText(qty);
        r.setChkNumText(chk);
        r.setPhotoNo(photo);
        return r;
    }

    private PlaqueBookForm book(String batchNo, String rawLimit, PlaqueRowForm... rs) {
        PlaqueBookForm f = new PlaqueBookForm();
        f.setBatchNo(batchNo);
        f.setBookNo(batchNo + "-BOOK");
        f.setUnitNo("U1");
        f.setQuarterCode("2026Q4");
        f.setRowLimitRaw(rawLimit);
        for (PlaqueRowForm r : rs) {
            f.getRows().add(r);
        }
        return f;
    }

    private List<TOadsPlaqueRow> rowsOf(String batchNo) {
        TOadsPlaqueBook b = svc.requireBook(batchNo);
        return rowMapper.listByBook(b.getId());
    }

    private long countFlow(String batchNo, String action) {
        return flowMapper.listByBatch(batchNo).stream().filter(f -> action.equals(f.getAction())).count();
    }

    // ================================================================ 第1步：点行数

    @Test
    public void countPhysicalRows_duplicateSpotCodeLandsTwice_defaultLimit200() {
        PlaqueReceipt rc = svc.receive(book("B-COUNT", null,
                row("SP0001", null, null, "10", "8", "P1"),
                row("SP0001", null, null, "10", "8", "P2")), PlaqueConsts.PUT_AREA, "tester");

        // 物理行各落一条：同码不去重不并笔，三个数都按物理行
        assertEquals(2, rc.getTotalRows());
        assertEquals(2, rc.getAcceptedRows());
        assertEquals(0, rc.getReturnedRows());
        assertTrue(rc.isBalanced());
        assertEquals(2, rowsOf("B-COUNT").size());
        assertEquals(200, rc.getRowLimit());
        assertEquals(1, countFlow("B-COUNT", PlaqueConsts.FLOW_RECEIVE));
        // 出回执又落一笔 RECEIPT，三数仍是同口径
        PlaqueReceipt rc2 = svc.receipt("B-COUNT", "tester");
        assertEquals(2, rc2.getTotalRows());
        assertEquals(1, countFlow("B-COUNT", PlaqueConsts.FLOW_RECEIPT));
    }

    @Test
    public void rowLimitOnlyFromHeader_blankNegativeHugeAllFallBackTo200() {
        for (String raw : new String[]{"", "  ", "abc", "-5", "100001", "99999999999999999999"}) {
            String batch = "B-LIMIT-" + Math.abs(raw.hashCode());
            svc.receive(book(batch, raw, row("SP0001", null, null, "1", "1", "P")),
                    PlaqueConsts.PUT_AREA, "tester");
            assertEquals(200, svc.requireBook(batch).getRowLimit().intValue(), "抬头「" + raw + "」应按没填认200");
        }
    }

    @Test
    public void overHeaderLimitStillReceivedButFlagged() {
        PlaqueReceipt rc = svc.receive(book("B-OVER", "1",
                row("SP0001", null, null, "1", "1", "P1"),
                row("SP0002", null, null, "1", "1", "P2")), PlaqueConsts.PUT_AREA, "tester");
        assertEquals(1, rc.getRowLimit());
        assertTrue(rc.isOverLimit());
        assertTrue(rc.getNote().contains("越过抬头"));
        assertEquals(2, rc.getTotalRows()); // 册照收不拦
    }

    @Test
    public void emptyOrCodelessBookRejectedAsABook() {
        PlaqueBookForm empty = book("B-EMPTY", null);
        assertThrows(PlaqueRuleException.class,
                () -> svc.receive(empty, PlaqueConsts.PUT_AREA, "tester"));
        PlaqueBookForm noCode = book(null, null, row("SP0001", null, null, "1", "1", "P"));
        assertThrows(PlaqueRuleException.class,
                () -> svc.receive(noCode, PlaqueConsts.PUT_AREA, "tester"));
    }

    // ================================================================ 第2步：验数据

    @Test
    public void quantitiesMustArriveTogether_noFillNoZero_pending() {
        // 只到应更换：不默认核验合格为零，挂待核，仍算入库占位
        PlaqueReceipt rc = svc.receive(book("B-Q1", null,
                row("SP0001", null, null, "10", null, "P")), PlaqueConsts.PUT_AREA, "tester");
        TOadsPlaqueRow r = rowsOf("B-Q1").get(0);
        assertEquals(PlaqueConsts.PENDING, r.getVerdict());
        assertNull(r.getChkNum());
        assertTrue(r.getRejectReason().contains("未同时到达"));
        assertEquals(1, rc.getAcceptedRows());
        assertEquals(0, rc.getReturnedRows());

        // 只到核验合格同理
        svc.receive(book("B-Q2", null, row("SP0001", null, null, null, "3", "P")),
                PlaqueConsts.PUT_AREA, "tester");
        assertEquals(PlaqueConsts.PENDING, rowsOf("B-Q2").get(0).getVerdict());
    }

    @Test
    public void codeOnlyExactMatched_shortCodeAndUnmatchedStayPending() {
        svc.receive(book("B-CODE", null,
                row("SP1", null, null, "2", "2", "P"),          // 位数不够
                row("SP9999", null, null, "2", "2", "P")),       // 全等配不上
                PlaqueConsts.PUT_AREA, "tester");
        List<TOadsPlaqueRow> rs = rowsOf("B-CODE");
        assertEquals(PlaqueConsts.PENDING, rs.get(0).getVerdict());
        assertTrue(rs.get(0).getRejectReason().contains("位数不够"));
        assertEquals(PlaqueConsts.PENDING, rs.get(1).getVerdict());
        assertTrue(rs.get(1).getRejectReason().contains("全等配不上"));
    }

    @Test
    public void missingPhotoKeepsRowInBookAsPending() {
        PlaqueReceipt rc = svc.receive(book("B-PHOTO", null,
                row("SP0001", null, null, "2", "2", null)), PlaqueConsts.PUT_AREA, "tester");
        TOadsPlaqueRow r = rowsOf("B-PHOTO").get(0);
        assertNull(r.getPhotoNo());
        assertEquals(PlaqueConsts.PENDING, r.getVerdict());
        assertTrue(rc.getRows().get(0).isPhotoPending());
        assertEquals(1, rc.getTotalRows()); // 照样占位
    }

    @Test
    public void fourthVerdictSpellingIsNotWritten() {
        PlaqueRowForm rf = row("SP0001", null, null, "2", "2", "P");
        rf.setVerdictRaw("通过个屁");
        svc.receive(book("B-V4", null, rf), PlaqueConsts.PUT_AREA, "tester");
        TOadsPlaqueRow r = rowsOf("B-V4").get(0);
        assertEquals(PlaqueConsts.PASS, r.getVerdict()); // 结论服务端裁
        assertTrue(r.getRemark().contains("第四种写法不予写入"));
    }

    // ================================================================ 第3步：定去向（五条逐句退回）

    @Test
    public void fiveRejectReasonsEachTraceable() {
        String longCode = "SP" + repeat("0", 40); // 42 位，超规范 32
        svc.receive(book("B-REJ", null,
                row("SP0001", null, null, "60", "60", "P"),   // 高于 DU00 当地线 50
                row("SP0002", null, null, "0", "0", "P"),     // 为零
                row("SP0002", null, null, "-3", "1", "P"),    // 为负
                row(null, null, null, "3", "3", "P"),         // 编码缺填
                row(longCode, null, null, "3", "3", "P")),    // 编码超长
                PlaqueConsts.PUT_AREA, "tester");
        List<TOadsPlaqueRow> rs = rowsOf("B-REJ");
        assertEquals(5, rs.size());
        for (TOadsPlaqueRow r : rs) {
            assertEquals(PlaqueConsts.REJECT, r.getVerdict());
        }
        assertTrue(rs.get(0).getRejectReason().contains("高于当地核验上限"));
        assertTrue(rs.get(0).getRejectReason().contains("50"));
        assertTrue(rs.get(1).getRejectReason().contains("为零"));
        assertTrue(rs.get(2).getRejectReason().contains("为负数"));
        assertTrue(rs.get(3).getRejectReason().contains("点位编码缺填"));
        assertTrue(rs.get(4).getRejectReason().contains("长度超出规范"));

        PlaqueReceipt rc = svc.receipt("B-REJ", "tester");
        assertEquals(5, rc.getTotalRows());
        assertEquals(0, rc.getAcceptedRows());
        assertEquals(5, rc.getReturnedRows());
        assertTrue(rc.isBalanced());
    }

    @Test
    public void multipleRejectReasonsOnOneRowAllKept() {
        // 为零 + 编码超长，两条都要在账上一句句回查
        String longCode = "SP" + repeat("0", 40);
        svc.receive(book("B-MULTI", null, row(longCode, null, null, "0", "0", "P")),
                PlaqueConsts.PUT_AREA, "tester");
        String reason = rowsOf("B-MULTI").get(0).getRejectReason();
        assertTrue(reason.contains("为零"));
        assertTrue(reason.contains("长度超出规范"));
    }

    // ================================================================ 第4步：出回执 + 主档为准

    @Test
    public void masterDiffListedAndMasterWins() {
        PlaqueReceipt rc = svc.receive(book("B-DIFF", null,
                row("SP0001", "报文瞎写的牌面", "DU99", "5", "5", "P")),
                PlaqueConsts.PUT_AREA, "tester");
        TOadsPlaqueRow r = rowsOf("B-DIFF").get(0);
        assertEquals(1, r.getMasterDiff().intValue());
        assertEquals("临江路一号牌匾", r.getMasterViewName());
        assertEquals("DU00", r.getMasterRoadCode());
        assertEquals(1, rc.getMasterDiffRows().size());
        assertTrue(rc.getNote().contains("以主档带出"));
    }

    @Test
    public void receiptNamesMismatchWhenLedgerCounterCorrupted() {
        svc.receive(book("B-CORRUPT", null,
                row("SP0001", null, null, "1", "1", "P"),
                row("SP0002", null, null, "1", "1", "P")), PlaqueConsts.PUT_AREA, "tester");
        TOadsPlaqueBook b = svc.requireBook("B-CORRUPT");
        b.setAcceptedRows(99); // 销账被人/故障改坏
        bookMapper.updateById(b);

        PlaqueReceipt rc = svc.receipt("B-CORRUPT", "tester");
        assertEquals(2, rc.getAcceptedRows()); // 回库逐行数仍是 2，不信销账汇总
        assertFalse(rc.isBalanced());
        assertTrue(rc.getMismatches().stream().anyMatch(m -> m.getDetail().contains("99")));
        // 不接受"基本相符"
        assertTrue(rc.getNote().contains("逐行"));
    }

    // ================================================================ 重复送达：第二回不重复入行

    @Test
    public void sameBatchTwoChannels_secondDeliveryDoesNotDuplicateRows() {
        PlaqueReceipt first = svc.receive(book("B-DUP", null,
                row("SP0001", null, null, "2", "2", "P")), PlaqueConsts.PUT_AREA, "tester");
        assertFalse(first.isDuplicate());

        PlaqueReceipt second = svc.receive(book("B-DUP", null,
                row("SP0001", null, null, "2", "2", "P")), PlaqueConsts.PUT_CITY, "tester");
        assertTrue(second.isDuplicate());
        assertEquals(1, second.getDupCount());
        assertEquals(PlaqueConsts.PUT_CITY, second.getDuplicatePutKind());
        assertEquals(1, rowsOf("B-DUP").size()); // 没有第二份行
        assertEquals(1, countFlow("B-DUP", PlaqueConsts.FLOW_RESEND));
        assertEquals(PlaqueConsts.PUT_AREA, svc.requireBook("B-DUP").getPutKind()); // 来路只记头一回
        assertEquals(1, svc.requireBook("B-DUP").getDupCount().intValue());
    }

    // ================================================================ 缺照片补挂

    @Test
    public void attachPhotoClearsPendingWhenNothingElseLeft() {
        svc.receive(book("B-ATTACH", null,
                row("SP0001", null, null, "2", "2", null)), PlaqueConsts.PUT_AREA, "tester");
        TOadsPlaqueRow updated = svc.attachPhoto("B-ATTACH", 1, "PHOTO-NEW-1", "tester");
        assertEquals(PlaqueConsts.PASS, updated.getVerdict());
        assertNotNull(updated.getPhotoAttachedAt());
        // 销账同口径：入库仍 1、退回 0，三数不变，补挂流水留痕
        assertEquals(1, countFlow("B-ATTACH", PlaqueConsts.FLOW_ATTACH));
        PlaqueReceipt rc = svc.receipt("B-ATTACH", "tester");
        assertEquals(1, rc.getAcceptedRows());
        assertEquals(0, rc.getReturnedRows());
    }

    @Test
    public void attachPhotoStaysPendingIfCodeStillUnmatched() {
        svc.receive(book("B-ATTACH2", null,
                row("SP9999", null, null, "2", "2", null)), PlaqueConsts.PUT_AREA, "tester");
        TOadsPlaqueRow updated = svc.attachPhoto("B-ATTACH2", 1, "P9", "tester");
        assertEquals(PlaqueConsts.PENDING, updated.getVerdict());
        assertTrue(updated.getRejectReason().contains("仍配不上主档"));
    }

    @Test
    public void rejectedRowDoesNotTakePhoto() {
        svc.receive(book("B-ATTACH3", null,
                row("SP0001", null, null, "0", "0", null)), PlaqueConsts.PUT_AREA, "tester");
        assertThrows(PlaqueRuleException.class,
                () -> svc.attachPhoto("B-ATTACH3", 1, "P9", "tester"));
    }

    // ================================================================ 销账与流水对账：以流水为准

    @Test
    public void reconcileRewritesLedgerFromFlowAndLogsAdjust() {
        svc.receive(book("B-REC", null,
                row("SP0001", null, null, "1", "1", "P"),
                row("SP0002", null, null, "2", "2", "P"),
                row("SP0001", null, null, "60", "60", "P")), // 退回 1
                PlaqueConsts.PUT_AREA, "tester");
        TOadsPlaqueBook b = svc.requireBook("B-REC");
        b.setCountedRows(30);
        b.setAcceptedRows(30);
        b.setReturnedRows(0);
        bookMapper.updateById(b);

        PlaqueReconcile result = svc.reconcile("tester");
        assertTrue(result.isAdjusted());
        assertTrue(result.getNote().contains("以收讫流水为准"));
        TOadsPlaqueBook fixed = svc.requireBook("B-REC");
        assertEquals(3, fixed.getCountedRows().intValue());
        assertEquals(2, fixed.getAcceptedRows().intValue());
        assertEquals(1, fixed.getReturnedRows().intValue());
        assertEquals(PlaqueConsts.RECONCILE_OWNER, fixed.getUpdateBy());
        assertEquals(1, countFlow("B-REC", PlaqueConsts.FLOW_ADJUST));
    }

    // ================================================================ 老化归档

    @Test
    public void pendingRowsTwoYearsOldMoveToHistoryLayer_nothingDeleted() {
        PlaqueBookForm old = book("B-OLD", null,
                row("SP0001", null, null, "2", "2", null),  // 待核（缺照片）
                row("SP0002", null, null, "2", "2", "P"));  // 通过
        old.setQuarterCode("2024Q1");
        svc.receive(old, PlaqueConsts.PUT_AREA, "tester");
        // 新册不归档
        svc.receive(book("B-NEW", null, row("SP0001", null, null, "2", "2", null)),
                PlaqueConsts.PUT_AREA, "tester");

        PlaqueArchiveResult arc = svc.archiveAging(2026, "tester");
        assertEquals(1, arc.getArchivedRows());
        List<TOadsPlaqueRow> oldRows = rowsOf("B-OLD");
        assertEquals(PlaqueConsts.LAYER_HISTORY, oldRows.get(0).getLayer().intValue());
        assertEquals(PlaqueConsts.LAYER_CURRENT, oldRows.get(1).getLayer().intValue());
        assertNotNull(oldRows.get(0).getArchivedAt());

        // 现行名单不再列出它，全量/历史层照常可查，一条不减
        assertEquals(1, svc.listRows("B-OLD", PlaqueConsts.LAYER_CURRENT).size());
        assertEquals(1, svc.listRows("B-OLD", PlaqueConsts.LAYER_HISTORY).size());
        assertEquals(2, svc.listRows("B-OLD", null).size());
        // 新册走不到老化规矩面前：两层都不动
        assertEquals(1, svc.listRows("B-NEW", PlaqueConsts.LAYER_CURRENT).size());
        assertEquals(0, svc.listRows("B-NEW", PlaqueConsts.LAYER_HISTORY).size());
        assertEquals(1, countFlow("B-OLD", PlaqueConsts.FLOW_ARCHIVE));
        assertEquals(0, countFlow("B-NEW", PlaqueConsts.FLOW_ARCHIVE));

        // 历史层行的回执三数仍含它（回执回库逐行数，全层）
        PlaqueReceipt rc = svc.receipt("B-OLD", "tester");
        assertEquals(2, rc.getTotalRows());
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
