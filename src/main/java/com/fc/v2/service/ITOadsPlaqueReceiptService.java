package com.fc.v2.service;

import java.util.List;

import com.fc.v2.model.auto.TOadsPlaqueBook;
import com.fc.v2.model.auto.TOadsPlaqueRow;
import com.fc.v2.model.custom.PlaqueArchiveResult;
import com.fc.v2.model.custom.PlaqueBookForm;
import com.fc.v2.model.custom.PlaqueReceipt;
import com.fc.v2.model.custom.PlaqueReconcile;

/**
 * 牌匾更换名册收讫环节唯一裁决处（新名目自有一条，不动旧集中更换/五检查点本体）。
 *
 * <p>收讫手上只有四个动作，四步连着做完、同一次落账，中间没有人工兜底：
 * <ol>
 *   <li><b>点行数</b>：进门只数物理行，同一点位写两行的各落一条（不去重不并笔）；
 *       单册行数上限只认本册抬头那一格，空/负数/大得没边按 200 认，别处没有第二条来源。</li>
 *   <li><b>验数据</b>：应更换处数与核验合格处数须同时到达，缺一项不代填不默零，挂待核；
 *       点位身份只认编码全等配主档，近似、位数不够都算配不上，挂待核；缺照片留空占位挂待核；
 *       结论只认 待核/通过/退回 三值，由本处裁决，第四种写法不予写入。</li>
 *   <li><b>定去向</b>：应更换高于当地核验上限、为零、为负、编码缺填、编码超长，五条判退回，
 *       哪行哪条逐句落账可回查；同一批次码两通道各递一回，第二回不重复入行，回执同册重出。</li>
 *   <li><b>出回执</b>：本册行数/入库行数/退回行数一律回库逐行数，入库+退回=本册行数；
 *       逐行结论与三数互相印证，对不齐逐行点名；主档有出入的行列出来，以主档为准。</li>
 * </ol>
 *
 * <p>另担三件老规矩：提交形态固定一册一次交完整（入口层保）；销账与流水联动、账实差以流水为准，
 * 抹平销账归收讫环节认；待核连续满两个年度的行逐行转入历史卷宗层，存量一条不减。
 *
 * @author fuce
 * @date 2026-10-05
 */
public interface ITOadsPlaqueReceiptService {

    /**
     * 整册收讫：四步同事务做完，返回与销账/流水出自同一次计算的回执。
     *
     * @param form    整册报文（册不拆、行不跨册）
     * @param putKind 收自哪条通道 {@code AREA}区级报送 / {@code CITY}市级复核，由入口点
     */
    PlaqueReceipt receive(PlaqueBookForm form, String putKind, String operator);

    /** 出回执：不看页面那份，回库逐行重数三数并逐行印证；同次落一笔 RECEIPT 流水。 */
    PlaqueReceipt receipt(String batchNo, String operator);

    /** 按批次码取册账（不存在抛收讫口径违例）。 */
    TOadsPlaqueBook requireBook(String batchNo);

    /** 册账清单（按收讫时间倒序）。 */
    List<TOadsPlaqueBook> listBooks(String quarterCode);

    /**
     * 取一册的行：layer 传 null 给全量（含历史卷宗层）；0 只给现行层，1 只给历史层。
     * 行一律按原报行次排，同码两行各在各的行次。
     */
    List<TOadsPlaqueRow> listRows(String batchNo, Integer layer);

    /**
     * 缺照片补挂：下一轮核看为待核行补上现场照片附件编号，行不游移到别册。
     * 补挂后若该行待核的原因已消尽（数量齐、编码配上主档），本处同次裁为通过；仍有待核缘由的继续待核。
     * 已退回的行不接补挂。
     */
    TOadsPlaqueRow attachPhoto(String batchNo, int rowNo, String photoNo, String operator);

    /**
     * 销账与流水对账：两边对不上的那一笔，账实差以流水为准，由收讫环节按流水抹平销账，
     * 每笔抹平再落 ADJUST 流水（归"收讫环节对账"认）。这个取舍本处定死，不由页面挑。
     */
    PlaqueReconcile reconcile(String operator);

    /**
     * 老化归档：结论长期停在待核、连着满两个年度的行，逐行转入历史卷宗层。
     * 新册走不到它面前（只扫收讫年度早于 asOfYear 两个年度的册子）；行不删不换册，一条不减。
     *
     * @param asOfYear 截止年度（含），传 null 用当前年
     */
    PlaqueArchiveResult archiveAging(Integer asOfYear, String operator);
}
