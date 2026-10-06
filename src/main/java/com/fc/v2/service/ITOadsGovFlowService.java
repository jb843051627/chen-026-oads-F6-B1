package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TOadsGovFlow;

/**
 * 点位监管办件流转单（旧版 stage 0..3，t_oads_gov_flow）接口——只读留档。
 *
 * <p>版本取舍已定死：本套已停写，申报/核准/设置/验收四步一律走新版监管办件
 * （t_oads_gov_case，{@link ITOadsGovCaseService}），步次以新版逐笔留痕重数为准。
 * 旧表不添列、不改名、不动存量数据，本接口只剩按主键查与台账只读列表；
 * 推进/回退/改内容/移除四个写方法一律抛 GovCaseRuleException 拒绝，不落任何一笔。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITOadsGovFlowService {

    /** 按主键查旧流转单（只读留档）。 */
    TOadsGovFlow selectTOadsGovFlowById(Long id);

    /** 旧流转单台账（只读列表，只用于存量回看）。 */
    List<TOadsGovFlow> selectTOadsGovFlowList(QueryWrapper<TOadsGovFlow> queryWrapper);

    /**
     * @deprecated 旧版流转已停写：调用即拒绝。推进请到新版 {@link ITOadsGovCaseService#advance}。
     */
    @Deprecated
    TOadsGovFlow advance(Long id, String remark);

    /**
     * @deprecated 旧版流转已停写：调用即拒绝。后退请到新版 {@link ITOadsGovCaseService#returnStep}。
     */
    @Deprecated
    TOadsGovFlow rollback(Long id, String remark);

    /**
     * @deprecated 旧版流转已停写：定稿内容只许在新版留痕里追加，旧表 content 不改。
     */
    @Deprecated
    boolean updateContent(Long id, String remark);

    /**
     * @deprecated 旧版流转已停写：旧单只留档不移除（历史不删），调用即拒绝。
     */
    @Deprecated
    boolean remove(Long id);
}
