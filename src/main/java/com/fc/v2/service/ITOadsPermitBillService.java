package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TOadsPermitBill;
import com.fc.v2.model.auto.TOadsPermitSign;
import com.fc.v2.model.auto.TOadsSpotChange;
import com.fc.v2.model.custom.PermitBillView;
import com.fc.v2.model.custom.PermitLedgerCheck;
import com.fc.v2.model.custom.PermitSignOutcome;

import java.util.Date;
import java.util.List;

/**
 * 设置许可核准单 Service接口（approval-chain 形状：三段签批）。
 *
 * <p>旁路约定：另起单只许由点位主档记录生成；改旧笔、追补签署没有入口；
 * 一次签署只有 {@link #sign} 这一个口子。报文里写了第几段不作数，
 * 段次与人数一概以服务端规则本点出的结果为凭。
 *
 * @author fuce
 * @date 2026-10-05
 */
public interface ITOadsPermitBillService {

    /**
     * 由点位主档那一行立一张核准单。同一点位、同一种变更在途只许一张。
     *
     * @param spotId     点位主档行主键（单据唯一来源）
     * @param changeKind HANG挂上去 / MOVE改位置 / REMOVE整块拆除
     * @param operator   建单人（口头报备不算，须落这张单）
     */
    TOadsPermitBill createFromSpot(Long spotId, String changeKind, String operator);

    /** 整单回查：单头加按段分节，当前节、每节人数由服务端算好回显，老轮留痕也带出 */
    PermitBillView getBillDetail(Long id);

    /** 台账查询（分页由控制器 PageHelper 接管） */
    List<TOadsPermitBill> selectBillList(Wrapper<TOadsPermitBill> queryWrapper);

    /**
     * 唯一签署入口。
     *
     * @param billId     核准单
     * @param signer     签署人（所在岗由服务端按段点出）
     * @param opinion    1同意 2不同意
     * @param content    签署文本，原文入库留痕
     * @param claimNode  报文自称的段次（可空）。只用于核对：与单据当前段不一致即丢弃，
     *                   整单留在原段；记账段次永远以服务端单据当前段为凭
     */
    PermitSignOutcome sign(Long billId, String signer, Integer opinion, String content, Integer claimNode);

    /** 封存完成前的未封存数量：建单加一、封存减一 */
    long unsealedCount();

    /** 台账对账：顶部数与逐段累加同一次计算，对不上逐张指名单据号 */
    PermitLedgerCheck reconcile();

    /** 点位主档变更履历，按发生时刻倒序，一段往回看 */
    List<TOadsSpotChange> spotChangeHistory(Long spotId);

    /** 当日受理登记簿：当日三段全部落笔（含被清退出的老轮），按落单时刻排 */
    List<TOadsPermitSign> acceptanceRegister(Date day);
}
