package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.exception.gov.GovCaseRuleException;
import com.fc.v2.mapper.auto.TOadsGovFlowMapper;
import com.fc.v2.model.auto.TOadsGovFlow;
import com.fc.v2.service.ITOadsGovFlowService;

/**
 * 点位监管办件流转单（旧版 stage 0..3，t_oads_gov_flow）——只读留档，不再承接任何流转。
 *
 * <p>版本取舍已定死：申报/核准/设置/验收四步一律走新版「监管办件」
 * （t_oads_gov_case，见 {@link com.fc.v2.service.impl.TOadsGovCaseServiceImpl}），
 * 步次以新版逐笔留痕重数为准。旧表不添列、不改名、不动存量数据，只允许查询回看；
 * 推进、回退、改一关一记、移除四类写动作一律拒绝，不在旧表上落任何一笔。
 *
 * <p>旧实现那套毛病（到终点还能推、一次跨两格 {@code stage+2}、回退直接归 0、
 * 定稿内容还能改、物理删除丢历史、无锁无幂等连点多笔）随写入口一并封死，
 * 新件不要再经本类，本类也不替新件改数。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TOadsGovFlowServiceImpl implements ITOadsGovFlowService {

    /** 旧套写动作被拒时的统一交代：页面/接口/测试都对这一句口径。 */
    public static final String LEGACY_WRITE_REJECTED =
            "旧版点位监管办件流转单（t_oads_gov_flow）已停写，只留存量只读可查；"
            + "申报/核准/设置/验收一律走新版监管办件（t_oads_gov_case），"
            + "步次以新版逐笔留痕重数为准，旧入口不推进、不回退、不改内容、不移除。";

    @javax.annotation.Resource
    private TOadsGovFlowMapper oadsGovFlowMapper;

    @Override
    public TOadsGovFlow selectTOadsGovFlowById(Long id) {
        return this.oadsGovFlowMapper.selectById(id);
    }

    @Override
    public List<TOadsGovFlow> selectTOadsGovFlowList(QueryWrapper<TOadsGovFlow> queryWrapper) {
        return this.oadsGovFlowMapper.selectList(queryWrapper);
    }

    @Override
    public TOadsGovFlow advance(Long id, String remark) {
        throw new GovCaseRuleException(LEGACY_WRITE_REJECTED);
    }

    @Override
    public TOadsGovFlow rollback(Long id, String remark) {
        throw new GovCaseRuleException(LEGACY_WRITE_REJECTED);
    }

    @Override
    public boolean updateContent(Long id, String remark) {
        throw new GovCaseRuleException(LEGACY_WRITE_REJECTED);
    }

    @Override
    public boolean remove(Long id) {
        throw new GovCaseRuleException(LEGACY_WRITE_REJECTED);
    }

}
