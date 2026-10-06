package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.fc.v2.service.ITOadsPlaqueReceiptService;
import com.fc.v2.service.plaque.PlaqueConsts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 牌匾更换名册收讫环节唯一裁决处实现。
 *
 * <p>四步固定连着走、同事务一次落账：
 * 点行数（物理行各落一条，上限只认抬头）→ 验数据（数量同到、编码全等配主档、缺照片占位、
 * 结论只三值）→ 定去向（五条退回逐句落账）→ 出回执（回库逐行数，三数互印，主档出入列出）。
 * 旧集中更换核验行与五检查点本体一律不碰。
 *
 * <p>重复送达：批次码是册的唯一身份，第二回（不管走哪条通道）只在 dup_count 加一并落 RESEND
 * 流水，行不重复入，回执同一份行账重出——前年"两回各入了账、并了两天账期"的事不再有。
 *
 * @author fuce
 * @date 2026-10-05
 */
@Service
public class TOadsPlaqueReceiptServiceImpl implements ITOadsPlaqueReceiptService {

    private static final int DEL_OK = 0;

    @javax.annotation.Resource
    private TOadsPlaqueBookMapper bookMapper;
    @javax.annotation.Resource
    private TOadsPlaqueRowMapper rowMapper;
    @javax.annotation.Resource
    private TOadsPlaqueFlowMapper flowMapper;
    @javax.annotation.Resource
    private TOadsPlaqueLimitMapper limitMapper;
    @javax.annotation.Resource
    private TOadsSpotMapper spotMapper;

    // ================================================================ 收讫：四步同事务

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlaqueReceipt receive(PlaqueBookForm form, String putKind, String operator) {
        if (form == null) {
            throw new PlaqueRuleException("未收到名册，整册无从收讫");
        }
        String batchNo = form.getBatchNo() == null ? null : form.getBatchNo().trim();
        if (batchNo == null || batchNo.isEmpty()) {
            throw new PlaqueRuleException("批次码缺填：一次报送为一册，批次码是册的身份，没它落不了账");
        }
        List<PlaqueRowForm> forms = form.getRows() == null ? new ArrayList<PlaqueRowForm>() : form.getRows();
        if (forms.isEmpty()) {
            // 提交形态固定一册一次交完整；零行不成册，也不接零散逐行报送（逐行走入口在控制器层就没有）。
            throw new PlaqueRuleException("名册 " + batchNo + " 一行都没有：一册含若干行，空册不收、零散逐行也不收");
        }
        String channel = PlaqueConsts.PUT_CITY.equals(putKind) ? PlaqueConsts.PUT_CITY : PlaqueConsts.PUT_AREA;
        Date now = new Date();

        TOadsPlaqueBook existed = findBook(batchNo);
        if (existed != null) {
            // 第二回（区级报送与市级复核各递一回也算）：行不重复入，只把回数加一、流水落一笔。
            existed.setDupCount((existed.getDupCount() == null ? 0 : existed.getDupCount()) + 1);
            existed.setUpdateBy(operator);
            existed.setUpdateTime(now);
            bookMapper.updateById(existed);
            String sameOrOther = channel.equals(existed.getPutKind()) ? "同一条" : "另一条";
            appendFlow(batchNo, PlaqueConsts.FLOW_RESEND, channel,
                    existed.getCountedRows(), existed.getAcceptedRows(), existed.getReturnedRows(),
                    "批次码 " + batchNo + " 第" + (existed.getDupCount() + 1) + "回送达，走" + channelName(channel)
                            + "（头一回走" + channelName(existed.getPutKind()) + "，系" + sameOrOther
                            + "通道）：册不另立、行不重复入，回执照原册行账回库逐行重数", operator, now);
            return buildReceipt(existed, true, channel, operator, now, false);
        }

        // -------- 第1步：点行数 --------
        // 只数物理行：报文几行就是几行；同一处点位写两行的，两条都落，不去重、不并笔。
        int counted = forms.size();
        RowLimitResolve limitResolve = resolveRowLimit(form.getRowLimitRaw());
        int rowLimit = limitResolve.limit;
        boolean overLimit = counted > rowLimit;

        TOadsPlaqueBook book = new TOadsPlaqueBook();
        book.setBatchNo(batchNo);
        book.setBookNo(trimToNull(form.getBookNo()));
        book.setUnitNo(trimToNull(form.getUnitNo()));
        book.setQuarterCode(trimToNull(form.getQuarterCode()));
        book.setPutKind(channel);
        book.setRowLimitRaw(form.getRowLimitRaw() == null ? null : form.getRowLimitRaw().trim());
        book.setRowLimit(rowLimit);
        book.setCountedRows(counted);
        book.setAcceptedRows(0);
        book.setReturnedRows(0);
        book.setOverLimit(overLimit ? 1 : 0);
        book.setDupCount(0);
        book.setReceivedAt(now);
        book.setStatus(0);
        book.setDelFlag(DEL_OK);
        book.setCreateBy(operator);
        book.setCreateTime(now);
        book.setRemark("第1步点行数：进门点数 " + counted + " 物理行（同一点位写两行的各落一条，不去重不并笔）；"
                + limitResolve.note);
        bookMapper.insert(book);

        // -------- 第2步 + 第3步：逐行验数据、定去向（一行一裁，不拖累别行） --------
        Map<String, TOadsSpot> masterMap = masterSpotMap();
        Map<String, BigDecimal> limitMap = localLimitMap();
        int accepted = 0;
        int returned = 0;
        int rowNo = 0;
        for (PlaqueRowForm rf : forms) {
            rowNo++;
            TOadsPlaqueRow row = adjudicate(book, rowNo, rf, masterMap, limitMap, operator, now);
            rowMapper.insert(row);
            if (PlaqueConsts.REJECT.equals(row.getVerdict())) {
                returned++;
            } else {
                accepted++;
            }
        }

        // -------- 第4步：出回执（三数与行同次算；落账后回执仍回库逐行数，见 buildReceipt） --------
        book.setAcceptedRows(accepted);
        book.setReturnedRows(returned);
        book.setUpdateBy(operator);
        book.setUpdateTime(now);
        bookMapper.updateById(book);

        appendFlow(batchNo, PlaqueConsts.FLOW_RECEIVE, channel, counted, accepted, returned,
                "收讫四步同次做完：物理行 " + counted + "，入库 " + accepted + "，退回 " + returned
                        + "（入库+退回=" + (accepted + returned) + "，与物理行平）；抬头上限认 " + rowLimit
                        + (overLimit ? "，本行数已越过上限，账上与回执点名，册照收不拦" : "，未越上限"),
                operator, now);

        // 收讫这笔 RECEIVE 即三数的准快照；收讫回执与收讫出自同一次计算，构造时不再落第二笔。
        return buildReceipt(book, false, channel, operator, now, false);
    }

    /**
     * 一行的裁决（第2步验数据、第3步定去向），结论只可能是 待核/通过/退回 三值，
     * 退回理由逐句攒齐；报文自称的结论不作数。
     */
    private TOadsPlaqueRow adjudicate(TOadsPlaqueBook book, int rowNo, PlaqueRowForm rf,
                                      Map<String, TOadsSpot> masterMap, Map<String, BigDecimal> limitMap,
                                      String operator, Date now) {
        TOadsPlaqueRow row = new TOadsPlaqueRow();
        row.setBookId(book.getId());
        row.setBatchNo(book.getBatchNo());
        row.setRowNo(rowNo);
        String code = rf.getSpotNo() == null ? null : rf.getSpotNo().trim();
        row.setSpotNo(trimToNull(code));
        row.setViewName(trimToNull(rf.getViewName()));
        row.setRoadCode(trimToNull(rf.getRoadCode()));
        BigDecimal qty = PlaqueRowForm.parseNumber(rf.getQtyText());
        BigDecimal chk = PlaqueRowForm.parseNumber(rf.getChkNumText());
        row.setQty(qty);
        row.setChkNum(chk);
        String photo = rf.getPhotoNo() == null ? null : rf.getPhotoNo().trim();
        row.setPhotoNo(photo == null || photo.isEmpty() ? null : photo);
        row.setMasterDiff(0);
        row.setLayer(PlaqueConsts.LAYER_CURRENT);
        row.setDelFlag(DEL_OK);
        row.setCreateBy(operator);
        row.setCreateTime(now);

        List<String> rejectReasons = new ArrayList<String>();
        List<String> pendingReasons = new ArrayList<String>();
        List<String> remarks = new ArrayList<String>();

        // ---- 第3步五条退回（逐句攒，一条不丢；顺序照规矩列的次序） ----
        boolean codeMissing = code == null || code.isEmpty();
        boolean codeTooLong = !codeMissing && code.length() > PlaqueConsts.SPOT_CODE_MAX_LEN;
        if (qty != null) {
            // 当地那条线：行的属地以主档带出的辖段为准，配不上主档时取全区缺省线。
            TOadsSpot master = codeMissing ? null : masterMap.get(code);
            String areaNo = master == null || master.getAreaNo() == null ? null : master.getAreaNo().trim();
            BigDecimal limit = localLimit(limitMap, areaNo);
            if (limit != null && qty.compareTo(limit) > 0) {
                rejectReasons.add("应更换处数(" + qty.stripTrailingZeros().toPlainString() + ")高于当地核验上限("
                        + limit.stripTrailingZeros().toPlainString() + "，属地" + (areaNo == null ? "全区缺省" : areaNo)
                        + ")");
            }
            if (qty.compareTo(BigDecimal.ZERO) == 0) {
                rejectReasons.add("应更换处数为零");
            }
            if (qty.compareTo(BigDecimal.ZERO) < 0) {
                rejectReasons.add("应更换处数为负数(" + qty.stripTrailingZeros().toPlainString() + ")");
            }
        }
        if (codeMissing) {
            rejectReasons.add("点位编码缺填（身份只认点位编码，不按版面名称近似去配）");
        }
        if (codeTooLong) {
            rejectReasons.add("点位编码长度超出规范（" + code.length() + " 位，规范至多 "
                    + PlaqueConsts.SPOT_CODE_MAX_LEN + " 位）");
        }

        // ---- 第2步：两个数量须同时到达；编码全等配主档（位数不够也配不上）；缺照片占位 ----
        if (qty == null || chk == null) {
            pendingReasons.add("应更换处数与核验合格处数未同时到达（应更换"
                    + (qty == null ? "缺" : "到") + "、核验合格" + (chk == null ? "缺" : "到")
                    + "），不由系统代填、不默认成零，挂待核下一轮核看");
        }
        TOadsSpot master = null;
        if (!codeMissing && !codeTooLong) {
            if (code.length() < PlaqueConsts.SPOT_CODE_MIN_LEN) {
                pendingReasons.add("点位编码位数不够（" + code.length() + " 位，规范至少 "
                        + PlaqueConsts.SPOT_CODE_MIN_LEN + " 位），算配不上主档，不按版面名称近似去配");
            } else {
                master = masterMap.get(code);
                if (master == null) {
                    pendingReasons.add("点位编码 " + code + " 在点位主档里全等配不上，不按版面名称近似去配，"
                            + "挂待核下一轮核看");
                }
            }
        }
        if (row.getPhotoNo() == null) {
            pendingReasons.add("现场照片未随册递到：照片编号格空着，行照样在册里占位，挂待核，下一轮核看补挂");
        }

        // ---- 主档带出与比对：报文那串只用来比对，两边有出入以主档为准，回执列出本行 ----
        if (master != null) {
            row.setMasterSpotNo(master.getSpotNo() == null ? null : master.getSpotNo().trim());
            row.setMasterViewName(trimToNull(master.getContent()));
            row.setMasterRoadCode(trimToNull(master.getAreaNo()));
            List<String> diffs = new ArrayList<String>();
            if (row.getViewName() != null && !row.getViewName().equals(row.getMasterViewName())) {
                diffs.add("版面名称报文「" + row.getViewName() + "」主档「" + row.getMasterViewName() + "」");
            }
            if (row.getRoadCode() != null && !row.getRoadCode().equals(row.getMasterRoadCode())) {
                diffs.add("路段代号报文「" + row.getRoadCode() + "」主档「" + row.getMasterRoadCode() + "」");
            }
            if (!diffs.isEmpty()) {
                row.setMasterDiff(1);
                remarks.add("报文与主档有出入，以主档为准：" + join(diffs, "；"));
            }
        }

        // ---- 报文自称的结论：三值之外的第四种写法不予写入，只扫进备注留个说法 ----
        String rawVerdict = rf.getVerdictRaw() == null ? null : rf.getVerdictRaw().trim();
        if (rawVerdict != null && !rawVerdict.isEmpty() && !isThreeValue(rawVerdict)) {
            remarks.add("报文自称结论「" + rawVerdict + "」不是待核/通过/退回三值之一，第四种写法不予写入，"
                    + "结论由收讫环节裁决");
        }

        // ---- 定去向：有退回条的先退回；没有退回条但有待核缘由的待核；全齐才通过 ----
        if (!rejectReasons.isEmpty()) {
            row.setVerdict(PlaqueConsts.REJECT);
            row.setRejectReason(join(rejectReasons, "；"));
        } else if (!pendingReasons.isEmpty()) {
            row.setVerdict(PlaqueConsts.PENDING);
            row.setRejectReason(join(pendingReasons, "；"));
        } else {
            row.setVerdict(PlaqueConsts.PASS);
        }
        if (!remarks.isEmpty()) {
            row.setRemark(join(remarks, "；"));
        }
        return row;
    }

    // ================================================================ 出回执：一律回库逐行数

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlaqueReceipt receipt(String batchNo, String operator) {
        TOadsPlaqueBook book = requireBook(batchNo);
        return buildReceipt(book, (book.getDupCount() != null && book.getDupCount() > 0), null,
                operator, new Date(), true);
    }

    /**
     * 回执三数与逐行条目的唯一构造处：回库把行账逐行点一遍，
     * 不抄册账汇总、更不抄页面；行的结论与三个数当场互印，对不上的一行一行点名。
     */
    private PlaqueReceipt buildReceipt(TOadsPlaqueBook book, boolean duplicate, String dupPutKind,
                                       String operator, Date at, boolean logFlow) {
        List<TOadsPlaqueRow> rows = listRowsOfBook(book.getId());
        int total = rows.size();
        int accepted = 0;
        int returned = 0;
        PlaqueReceipt receipt = new PlaqueReceipt();
        receipt.setBatchNo(book.getBatchNo());
        receipt.setPutKind(book.getPutKind());
        receipt.setQuarterCode(book.getQuarterCode());
        receipt.setReceivedAt(book.getReceivedAt());
        receipt.setRowLimit(book.getRowLimit() == null ? PlaqueConsts.DEFAULT_ROW_LIMIT : book.getRowLimit());
        receipt.setRowLimitRaw(book.getRowLimitRaw());
        receipt.setOverLimit(book.getOverLimit() != null && book.getOverLimit() == 1);
        receipt.setDupCount(book.getDupCount() == null ? 0 : book.getDupCount());
        receipt.setDuplicate(duplicate);
        receipt.setDuplicatePutKind(dupPutKind);

        for (TOadsPlaqueRow r : rows) {
            PlaqueReceipt.RowItem item = new PlaqueReceipt.RowItem();
            item.setRowNo(r.getRowNo() == null ? 0 : r.getRowNo());
            item.setSpotNo(r.getSpotNo());
            item.setVerdict(r.getVerdict());
            item.setRejectReason(r.getRejectReason());
            item.setQty(r.getQty());
            item.setChkNum(r.getChkNum());
            item.setPhotoPending(PlaqueConsts.PENDING.equals(r.getVerdict()) && r.getPhotoNo() == null);
            item.setMasterDiff(r.getMasterDiff() != null && r.getMasterDiff() == 1);
            item.setReportViewName(r.getViewName());
            item.setMasterViewName(r.getMasterViewName());
            item.setReportRoadCode(r.getRoadCode());
            item.setMasterRoadCode(r.getMasterRoadCode());
            item.setLayer(r.getLayer() == null ? PlaqueConsts.LAYER_CURRENT : r.getLayer());
            boolean rowAccepted;
            if (PlaqueConsts.REJECT.equals(r.getVerdict())) {
                returned++;
                rowAccepted = false;
            } else if (PlaqueConsts.PASS.equals(r.getVerdict()) || PlaqueConsts.PENDING.equals(r.getVerdict())) {
                accepted++;
                rowAccepted = true;
            } else {
                // 结论格只许三值；真出现第四种，不并句、不"基本相符"，逐行点名。
                rowAccepted = false;
                receipt.getMismatches().add(new PlaqueReceipt.Mismatch(item.getRowNo(), r.getSpotNo(),
                        "本行结论格是「" + r.getVerdict() + "」，不是待核/通过/退回三值之一，无法计入入库或退回"));
            }
            item.setAccepted(rowAccepted);
            receipt.getRows().add(item);
            if (item.isMasterDiff()) {
                receipt.getMasterDiffRows().add(item);
            }
        }

        boolean balanced = accepted + returned == total && receipt.getMismatches().isEmpty();
        // 进门点数（册账）与回库行账也要互相印得上：差了逐行/逐数点名，不收口成"基本相符"。
        if (book.getCountedRows() != null && book.getCountedRows() != total) {
            receipt.getMismatches().add(new PlaqueReceipt.Mismatch(0, null,
                    "进门点数 " + book.getCountedRows() + " 行，回库逐行数得 " + total
                            + " 行，两笔对不上（销账与流水对账时以流水为准抹平）"));
            balanced = false;
        }
        if (book.getAcceptedRows() != null && book.getAcceptedRows() != accepted) {
            receipt.getMismatches().add(new PlaqueReceipt.Mismatch(0, null,
                    "册账入库数 " + book.getAcceptedRows() + " 与回库逐行数得 " + accepted + " 不一致"));
            balanced = false;
        }
        if (book.getReturnedRows() != null && book.getReturnedRows() != returned) {
            receipt.getMismatches().add(new PlaqueReceipt.Mismatch(0, null,
                    "册账退回数 " + book.getReturnedRows() + " 与回库逐行数得 " + returned + " 不一致"));
            balanced = false;
        }

        receipt.setTotalRows(total);
        receipt.setAcceptedRows(accepted);
        receipt.setReturnedRows(returned);
        receipt.setBalanced(balanced);

        StringBuilder note = new StringBuilder();
        note.append("三数均回库逐行数出：本册行数 ").append(total).append("＝入库 ").append(accepted)
                .append("＋退回 ").append(returned).append("；页面那份只是转述。");
        if (!balanced) {
            note.append("对不齐 ").append(receipt.getMismatches().size()).append(" 笔，已逐行/逐数点名，不认「基本相符」。");
        }
        if (receipt.isOverLimit()) {
            note.append("进门行数 ").append(total).append(" 已越过抬头认下的上限 ")
                    .append(receipt.getRowLimit()).append("，册照收、账上点名。");
        }
        if (duplicate) {
            note.append("本批次码系重复送达（累计第 ").append(receipt.getDupCount() + 1)
                    .append(" 回），第二回不另立册、行不重复入，回执照原册行账重数。");
        }
        if (!receipt.getMasterDiffRows().isEmpty()) {
            note.append("有 ").append(receipt.getMasterDiffRows().size())
                    .append(" 行报文与点位主档有出入，已列出，一律以主档带出的版面名称、路段代号为准。");
        }
        receipt.setNote(note.toString());

        if (logFlow) {
            // 每次出回执都把同一次计算的三数落一笔：页面、册账、流水出自这一次逐行数。
            appendFlow(book.getBatchNo(), PlaqueConsts.FLOW_RECEIPT, null, total, accepted, returned,
                    "出回执：回库逐行数得 本册" + total + " 入库" + accepted + " 退回" + returned
                            + (balanced ? "，三数互印平齐" : "，有" + receipt.getMismatches().size() + "笔对不上已点名"),
                    operator, at);
        }
        return receipt;
    }

    // ================================================================ 缺照片补挂

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TOadsPlaqueRow attachPhoto(String batchNo, int rowNo, String photoNo, String operator) {
        TOadsPlaqueBook book = requireBook(batchNo);
        TOadsPlaqueRow row = findRow(book.getId(), rowNo);
        if (PlaqueConsts.REJECT.equals(row.getVerdict())) {
            throw new PlaqueRuleException("第" + rowNo + "行已判退回（" + row.getRejectReason()
                    + "），退回行不接照片补挂；要改只能随下一册重报");
        }
        String photo = photoNo == null ? null : photoNo.trim();
        if (photo == null || photo.isEmpty()) {
            throw new PlaqueRuleException("补挂须给出照片附件编号，空编号不算补挂，第" + rowNo + "行继续占位待核");
        }
        Date now = new Date();
        row.setPhotoNo(photo);
        row.setPhotoAttachedAt(now);

        // 补挂后同次再裁：除照片外的待核缘由若都已消尽（数量同到、编码全等配上主档），即裁为通过；
        // 只要还有一条待核缘由，继续待核——不由人手点结论。
        boolean quantitiesOk = row.getQty() != null && row.getChkNum() != null;
        boolean codeMatched = false;
        String code = row.getSpotNo();
        if (code != null && code.length() >= PlaqueConsts.SPOT_CODE_MIN_LEN
                && code.length() <= PlaqueConsts.SPOT_CODE_MAX_LEN) {
            List<TOadsSpot> masters = spotMapper.selectList(new QueryWrapper<TOadsSpot>()
                    .eq("spot_no", code).eq("del_flag", DEL_OK));
            codeMatched = !masters.isEmpty();
        }
        if (quantitiesOk && codeMatched) {
            row.setVerdict(PlaqueConsts.PASS);
            row.setRejectReason(null);
            row.setRemark(appendNote(row.getRemark(), "照片已于下一轮核看补挂，待核缘由消尽，收讫环节同次裁为通过"));
        } else {
            List<String> left = new ArrayList<String>();
            if (!quantitiesOk) {
                left.add("两个数量仍未同时到达");
            }
            if (!codeMatched) {
                left.add("点位编码仍配不上主档");
            }
            row.setVerdict(PlaqueConsts.PENDING);
            row.setRejectReason("照片已补挂，但仍有待核缘由：" + join(left, "、") + "，继续待核");
        }
        row.setUpdateBy(operator);
        row.setUpdateTime(now);
        rowMapper.updateById(row);
        // 行的去处变了，册账两个汇总与流水须跟着同口径：回库逐行重数后回写，并留一笔交代。
        recountBook(book, row, operator, now);
        return row;
    }

    // ================================================================ 销账与流水对账（账实差以流水为准）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlaqueReconcile reconcile(String operator) {
        PlaqueReconcile result = new PlaqueReconcile();
        List<TOadsPlaqueBook> books = bookMapper.selectList(new QueryWrapper<TOadsPlaqueBook>()
                .eq("del_flag", DEL_OK));
        Collections.sort(books, new Comparator<TOadsPlaqueBook>() {
            @Override
            public int compare(TOadsPlaqueBook a, TOadsPlaqueBook b) {
                return Long.compare(a.getId() == null ? 0L : a.getId(), b.getId() == null ? 0L : b.getId());
            }
        });
        result.setBookCount(books.size());
        Date now = new Date();
        int diffBooks = 0;

        for (TOadsPlaqueBook book : books) {
            // 流水那侧的准数：头一回收讫那笔 RECEIVE 快照（册就此立定；RESEND/RECEIPT 不改行账）。
            TOadsPlaqueFlow truth = null;
            for (TOadsPlaqueFlow f : flowMapper.listByBatch(book.getBatchNo())) {
                if (PlaqueConsts.FLOW_RECEIVE.equals(f.getAction())) {
                    truth = f;
                    break;
                }
            }
            if (truth == null || truth.getTotalRows() == null) {
                // 没有收讫流水可为准的（异常存量），不动它，只在校代交代里点不出就不碰。
                continue;
            }
            List<TOadsPlaqueRow> rows = listRowsOfBook(book.getId());
            int rowTotal = rows.size();
            int rowAccepted = 0;
            int rowReturned = 0;
            for (TOadsPlaqueRow r : rows) {
                if (PlaqueConsts.REJECT.equals(r.getVerdict())) {
                    rowReturned++;
                } else {
                    rowAccepted++;
                }
            }

            List<PlaqueReconcile.Diff> diffs = new ArrayList<PlaqueReconcile.Diff>();
            if (book.getCountedRows() != null && book.getCountedRows() != truth.getTotalRows()) {
                diffs.add(new PlaqueReconcile.Diff(book.getBatchNo(), "册账.本册行数",
                        book.getCountedRows(), truth.getTotalRows(), "册账点数与收讫流水不符"));
            }
            if (book.getAcceptedRows() != null && book.getAcceptedRows() != truth.getAcceptedRows()) {
                diffs.add(new PlaqueReconcile.Diff(book.getBatchNo(), "册账.入库行数",
                        book.getAcceptedRows(), truth.getAcceptedRows(), "册账入库数与收讫流水不符"));
            }
            if (book.getReturnedRows() != null && book.getReturnedRows() != truth.getReturnedRows()) {
                diffs.add(new PlaqueReconcile.Diff(book.getBatchNo(), "册账.退回行数",
                        book.getReturnedRows(), truth.getReturnedRows(), "册账退回数与收讫流水不符"));
            }
            if (rowTotal != truth.getTotalRows() || rowAccepted != truth.getAcceptedRows()
                    || rowReturned != truth.getReturnedRows()) {
                diffs.add(new PlaqueReconcile.Diff(book.getBatchNo(), "行账.逐行汇总",
                        rowTotal, truth.getTotalRows(),
                        "行账逐行数得 本册" + rowTotal + "/入库" + rowAccepted + "/退回" + rowReturned
                                + "，收讫流水为 本册" + truth.getTotalRows() + "/入库" + truth.getAcceptedRows()
                                + "/退回" + truth.getReturnedRows() + "；流水为准，差额逐笔留痕，行不凭空补造"));
            }
            if (diffs.isEmpty()) {
                continue;
            }
            diffBooks++;
            result.getDiffs().addAll(diffs);

            // 账实差以流水为准：销账（册账汇总）按流水抹平；行账若自身缺行，不凭空补造，
            // 只在流水里把差额交代清楚，回执/名单照实点名。这个取舍收讫规矩定死，校正归收讫环节认。
            book.setCountedRows(truth.getTotalRows());
            book.setAcceptedRows(truth.getAcceptedRows());
            book.setReturnedRows(truth.getReturnedRows());
            book.setUpdateBy(PlaqueConsts.RECONCILE_OWNER);
            book.setUpdateTime(now);
            bookMapper.updateById(book);

            StringBuilder content = new StringBuilder("对账校正：账实差以收讫流水为准，销账（册账三数）已按头一回收讫流水抹平；")
                    .append("校正这笔归").append(PlaqueConsts.RECONCILE_OWNER).append("认，不由页面挑。明细：");
            for (PlaqueReconcile.Diff d : diffs) {
                content.append("[").append(d.getField()).append(" 销账").append(d.getLedgerValue())
                        .append("→流水").append(d.getFlowValue()).append("]");
            }
            appendFlow(book.getBatchNo(), PlaqueConsts.FLOW_ADJUST, null,
                    truth.getTotalRows(), truth.getAcceptedRows(), truth.getReturnedRows(),
                    clip(content.toString()), PlaqueConsts.RECONCILE_OWNER, now);
        }

        result.setDiffCount(result.getDiffs().size());
        result.setAdjusted(diffBooks > 0);
        result.setNote("销账与流水联动核对 " + books.size() + " 册；对不上 " + result.getDiffs().size()
                + " 笔，账实差一律以收讫流水为准，已由" + PlaqueConsts.RECONCILE_OWNER
                + "把销账抹平，每册校正另落 ADJUST 流水；行账缺行不凭空补造，照实点名");
        return result;
    }

    // ================================================================ 老化归档（满两个年度的待核行进历史层）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlaqueArchiveResult archiveAging(Integer asOfYear, String operator) {
        int asYear = asOfYear == null ? Calendar.getInstance().get(Calendar.YEAR) : asOfYear;
        PlaqueArchiveResult result = new PlaqueArchiveResult();
        result.setAsOfYear(asYear);
        Date now = new Date();

        List<TOadsPlaqueBook> books = bookMapper.selectList(new QueryWrapper<TOadsPlaqueBook>()
                .eq("del_flag", DEL_OK));
        result.setScannedBooks(books.size());
        int archived = 0;
        for (TOadsPlaqueBook book : books) {
            Integer bookYear = yearOf(book);
            // 连着满两个年度：收讫年度与截止年度差够两年才动；新册走不到它面前。
            if (bookYear == null || asYear - bookYear < 2) {
                continue;
            }
            List<TOadsPlaqueRow> rows = rowMapper.listByBookLayer(book.getId(), PlaqueConsts.LAYER_CURRENT);
            List<Integer> rowNos = new ArrayList<Integer>();
            for (TOadsPlaqueRow r : rows) {
                if (PlaqueConsts.PENDING.equals(r.getVerdict())) {
                    r.setLayer(PlaqueConsts.LAYER_HISTORY);
                    r.setArchivedAt(now);
                    r.setUpdateBy(operator);
                    r.setUpdateTime(now);
                    rowMapper.updateById(r);
                    rowNos.add(r.getRowNo());
                    archived++;
                }
            }
            if (!rowNos.isEmpty()) {
                PlaqueArchiveResult.BookArchive ba = new PlaqueArchiveResult.BookArchive();
                ba.setBatchNo(book.getBatchNo());
                ba.setRowNos(rowNos);
                result.getBooks().add(ba);
                // 归档只挪卷宗层、不改结论、不删行、不换册；回执三数仍含历史层，存量一条不减。
                appendFlow(book.getBatchNo(), PlaqueConsts.FLOW_ARCHIVE, null, null, null, null,
                        "老化归档：结论连续停在待核、收讫年度 " + bookYear + " 至 " + asYear
                                + " 已满两个年度的第 " + join(rowNos, "、")
                                + " 行转入历史卷宗层；现行名单不再列出，历史层照常可查，行一条不减",
                        operator, now);
            }
        }
        result.setArchivedRows(archived);
        return result;
    }

    // ================================================================ 查询

    @Override
    public TOadsPlaqueBook requireBook(String batchNo) {
        String no = batchNo == null ? null : batchNo.trim();
        if (no == null || no.isEmpty()) {
            throw new PlaqueRuleException("须给出批次码才查得到册");
        }
        TOadsPlaqueBook book = findBook(no);
        if (book == null) {
            throw new PlaqueRuleException("批次码 " + no + " 的名册从未在本环节收讫，账上无此册");
        }
        return book;
    }

    @Override
    public List<TOadsPlaqueBook> listBooks(String quarterCode) {
        List<TOadsPlaqueBook> books = bookMapper.selectList(new QueryWrapper<TOadsPlaqueBook>()
                .eq("del_flag", DEL_OK));
        List<TOadsPlaqueBook> out = new ArrayList<TOadsPlaqueBook>();
        String q = trimToNull(quarterCode);
        for (TOadsPlaqueBook b : books) {
            if (q == null || q.equals(b.getQuarterCode())) {
                out.add(b);
            }
        }
        Collections.sort(out, new Comparator<TOadsPlaqueBook>() {
            @Override
            public int compare(TOadsPlaqueBook a, TOadsPlaqueBook b) {
                Date da = a.getReceivedAt();
                Date db = b.getReceivedAt();
                int byTime = 0;
                if (da != null && db != null) {
                    byTime = db.compareTo(da);
                } else if (da != null) {
                    byTime = -1;
                } else if (db != null) {
                    byTime = 1;
                }
                if (byTime != 0) {
                    return byTime;
                }
                return Long.compare(b.getId() == null ? 0L : b.getId(), a.getId() == null ? 0L : a.getId());
            }
        });
        return out;
    }

    @Override
    public List<TOadsPlaqueRow> listRows(String batchNo, Integer layer) {
        TOadsPlaqueBook book = requireBook(batchNo);
        if (layer == null) {
            return listRowsOfBook(book.getId());
        }
        int l = layer.intValue() == PlaqueConsts.LAYER_HISTORY ? PlaqueConsts.LAYER_HISTORY : PlaqueConsts.LAYER_CURRENT;
        return rowMapper.listByBookLayer(book.getId(), l);
    }

    // ================================================================ 内部工具

    private List<TOadsPlaqueRow> listRowsOfBook(Long bookId) {
        List<TOadsPlaqueRow> rows = rowMapper.listByBook(bookId);
        return rows == null ? new ArrayList<TOadsPlaqueRow>() : rows;
    }

    private TOadsPlaqueRow findRow(Long bookId, int rowNo) {
        for (TOadsPlaqueRow r : listRowsOfBook(bookId)) {
            if (r.getRowNo() != null && r.getRowNo() == rowNo) {
                return r;
            }
        }
        throw new PlaqueRuleException("这一册里没有第 " + rowNo + " 行：行不游移到别册，只在本册行次里点");
    }

    private TOadsPlaqueBook findBook(String batchNo) {
        List<TOadsPlaqueBook> list = bookMapper.selectList(new QueryWrapper<TOadsPlaqueBook>()
                .eq("batch_no", batchNo).eq("del_flag", DEL_OK));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 抬头上限唯一来源的认法：空着按 200；不是整数、负数、大得没边（越过硬界）都按没填认，也认 200。
     * 返回认下的上限与给账上/回执的说法。
     */
    private RowLimitResolve resolveRowLimit(String raw) {
        String t = raw == null ? null : raw.trim();
        if (t == null || t.isEmpty()) {
            return RowLimitResolve.of(PlaqueConsts.DEFAULT_ROW_LIMIT,
                    "抬头那格空着，按没填认，单册行数上限认 " + PlaqueConsts.DEFAULT_ROW_LIMIT + "（别处不设第二条来源）");
        }
        java.math.BigInteger parsed;
        try {
            parsed = new java.math.BigInteger(t);
        } catch (NumberFormatException e) {
            return RowLimitResolve.of(PlaqueConsts.DEFAULT_ROW_LIMIT,
                    "抬头写的「" + t + "」不是整数，按没填认，上限认 " + PlaqueConsts.DEFAULT_ROW_LIMIT);
        }
        if (parsed.signum() < 0) {
            return RowLimitResolve.of(PlaqueConsts.DEFAULT_ROW_LIMIT,
                    "抬头写成负数「" + t + "」，按没填认，上限认 " + PlaqueConsts.DEFAULT_ROW_LIMIT);
        }
        if (parsed.compareTo(java.math.BigInteger.valueOf(PlaqueConsts.HARD_ROW_LIMIT)) > 0) {
            return RowLimitResolve.of(PlaqueConsts.DEFAULT_ROW_LIMIT,
                    "抬头写的 " + t + " 大得没边（越过硬界 " + PlaqueConsts.HARD_ROW_LIMIT
                            + "），按没填认，上限认 " + PlaqueConsts.DEFAULT_ROW_LIMIT);
        }
        int v = parsed.intValue();
        return RowLimitResolve.of(v, "抬头上限照本册抬头那一格认 " + v + "（唯一来源）");
    }

    /** 抬头上限认下的值与说法（值唯一来自抬头；说法只落账，不参与计数）。 */
    private static class RowLimitResolve {
        private final int limit;
        private final String note;

        private RowLimitResolve(int limit, String note) {
            this.limit = limit;
            this.note = note;
        }

        private static RowLimitResolve of(int limit, String note) {
            return new RowLimitResolve(limit, note);
        }
    }

    private Map<String, TOadsSpot> masterSpotMap() {
        List<TOadsSpot> all = spotMapper.selectList(new QueryWrapper<TOadsSpot>().eq("del_flag", DEL_OK));
        Map<String, TOadsSpot> map = new HashMap<String, TOadsSpot>();
        for (TOadsSpot s : all) {
            if (s.getSpotNo() != null && !s.getSpotNo().trim().isEmpty() && !map.containsKey(s.getSpotNo().trim())) {
                map.put(s.getSpotNo().trim(), s);
            }
        }
        return map;
    }

    private Map<String, BigDecimal> localLimitMap() {
        Map<String, BigDecimal> map = new HashMap<String, BigDecimal>();
        for (TOadsPlaqueLimit l : limitMapper.selectList(new QueryWrapper<TOadsPlaqueLimit>()
                .eq("del_flag", DEL_OK).eq("status", 0))) {
            if (l.getAreaNo() != null && l.getQtyLimit() != null) {
                map.put(l.getAreaNo().trim(), l.getQtyLimit());
            }
        }
        return map;
    }

    private BigDecimal localLimit(Map<String, BigDecimal> map, String areaNo) {
        if (areaNo != null) {
            BigDecimal v = map.get(areaNo);
            if (v != null) {
                return v;
            }
        }
        return map.get(PlaqueConsts.LIMIT_DEFAULT_AREA);
    }

    /** 补挂改了行去处后，册账汇总回库逐行重数回写（销账与行账同口径），流水另记一笔。 */
    private void recountBook(TOadsPlaqueBook book, TOadsPlaqueRow changed, String operator, Date now) {
        List<TOadsPlaqueRow> rows = listRowsOfBook(book.getId());
        int accepted = 0;
        int returned = 0;
        for (TOadsPlaqueRow r : rows) {
            if (PlaqueConsts.REJECT.equals(r.getVerdict())) {
                returned++;
            } else {
                accepted++;
            }
        }
        book.setCountedRows(rows.size());
        book.setAcceptedRows(accepted);
        book.setReturnedRows(returned);
        book.setUpdateBy(operator);
        book.setUpdateTime(now);
        bookMapper.updateById(book);
        String tail = changed == null ? "" : "（第" + changed.getRowNo() + "行结论为"
                + PlaqueConsts.verdictName(changed.getVerdict()) + "）";
        appendFlow(book.getBatchNo(), PlaqueConsts.FLOW_ATTACH, null, rows.size(), accepted, returned,
                "照片补挂：行去处变动，销账三数回库逐行重数回写（本册" + rows.size() + " 入库" + accepted
                        + " 退回" + returned + "），销账与流水同口径" + tail, operator, now);
    }

    private Integer yearOf(TOadsPlaqueBook book) {
        String q = book.getQuarterCode();
        if (q != null) {
            String t = q.trim();
            if (t.length() >= 4) {
                try {
                    return Integer.valueOf(t.substring(0, 4));
                } catch (NumberFormatException ignore) {
                    // 落到收讫时刻取年
                }
            }
        }
        if (book.getReceivedAt() != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(book.getReceivedAt());
            return c.get(Calendar.YEAR);
        }
        return null;
    }

    private void appendFlow(String batchNo, String action, String putKind, Integer total, Integer accepted,
                            Integer returned, String content, String operator, Date at) {
        TOadsPlaqueFlow f = new TOadsPlaqueFlow();
        f.setBatchNo(batchNo);
        f.setAction(action);
        f.setPutKind(putKind);
        f.setTotalRows(total);
        f.setAcceptedRows(accepted);
        f.setReturnedRows(returned);
        f.setContent(clip(content));
        f.setOperator(operator);
        f.setActedAt(at);
        f.setCreateBy(operator);
        f.setCreateTime(at);
        flowMapper.insert(f);
    }

    private boolean isThreeValue(String raw) {
        return PlaqueConsts.PENDING.equals(raw) || PlaqueConsts.PASS.equals(raw) || PlaqueConsts.REJECT.equals(raw)
                || "待核".equals(raw) || "通过".equals(raw) || "退回".equals(raw);
    }

    private String channelName(String putKind) {
        return PlaqueConsts.PUT_CITY.equals(putKind) ? "市级复核通道" : "区级报送通道";
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String join(List<?> items, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    private static String appendNote(String origin, String add) {
        if (origin == null || origin.isEmpty()) {
            return add;
        }
        return origin + "；" + add;
    }

    private static String clip(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
