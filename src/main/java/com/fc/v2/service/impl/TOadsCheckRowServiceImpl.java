package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.exception.spot.SpotRuleException;
import com.fc.v2.mapper.auto.TOadsCheckRowMapper;
import com.fc.v2.model.auto.TOadsCheckRow;
import com.fc.v2.model.custom.SpotBatchBringOutcome;
import com.fc.v2.model.custom.SpotBuildResult;
import com.fc.v2.model.custom.SpotForm;
import com.fc.v2.service.ITOadsCheckRowService;
import com.fc.v2.service.ITOadsSpotRegisterService;
import com.fc.v2.service.spot.SpotFlowStep;

/**
 * 集中更换核验行 Service业务层处理（batch-process 形状：整批提交）
 *
 * <p>原 {@link #submitBatch} 收什么回什么维持原状；另写一条等价去处
 * {@link #bringOutSpots} 承接"集中更换业务在核验完成后带出建行"，该通道不另起一套
 * 数量逻辑与判定，逐行走点位名册唯一裁决处（BATCH 通道），本体不动。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TOadsCheckRowServiceImpl implements ITOadsCheckRowService {

    private static final int MAX_ROWS = 500;
    private static final int STATUS_OK = 1;
    private static final int STATUS_FAIL = 2;

    @javax.annotation.Resource
    private TOadsCheckRowMapper oadsCheckRowMapper;

    @Autowired
    private ITOadsSpotRegisterService spotRegisterService;

    @Override
    public TOadsCheckRow selectTOadsCheckRowById(Long id) {
        return this.oadsCheckRowMapper.selectById(id);
    }

    @Override
    public int submitBatch(String batchNo, List<TOadsCheckRow> rows) {
        String no = rows.get(0).getBatchNo();
        java.util.List<TOadsCheckRow> errors = new java.util.ArrayList<TOadsCheckRow>();
        int seq = 0;
        for (TOadsCheckRow r : rows) {
            if (r.getItemCode() == null || r.getItemCode().trim().isEmpty()
                    || r.getQty() == null
                    || r.getQty().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                seq++;
                r.setRowNo(Integer.valueOf(seq));
                r.setBatchNo(no);
                r.setStatus(STATUS_FAIL);
                this.oadsCheckRowMapper.insert(r);
                errors.add(r);
            }
        }
        if (!errors.isEmpty()) {
            return 0;
        }
        int ok = 0;
        for (TOadsCheckRow r : rows) {
            r.setBatchNo(no);
            r.setStatus(STATUS_OK);
            this.oadsCheckRowMapper.insert(r);
            ok++;
        }
        return ok;
    }

    @Override
    public List<TOadsCheckRow> listErrors(String batchNo) {
        return this.oadsCheckRowMapper.selectList(new QueryWrapper<TOadsCheckRow>()
                .eq("batch_no", batchNo).eq("status", STATUS_FAIL));
    }

    /**
     * 建行第二通道：核验完成后带出。逐行交名册唯一裁决处，复用同一套五检查点与同一套数量逻辑；
     * 一行不过带回那一个检查点自己的提示语，不拖累同批别行，也不合并成"数据有误"。
     */
    @Override
    public SpotBatchBringOutcome bringOutSpots(String batchNo, List<SpotForm> forms, String operator) {
        SpotBatchBringOutcome outcome = new SpotBatchBringOutcome();
        outcome.setBatchNo(batchNo);
        if (forms == null || forms.isEmpty()) {
            return outcome;
        }
        if (forms.size() > MAX_ROWS) {
            throw new SpotRuleException("集中更换单批带出" + forms.size() + "行，超过单批上限 " + MAX_ROWS
                    + " 行，整批不带出");
        }
        int ok = 0;
        for (SpotForm form : forms) {
            String spotNo = form == null || form.getSpotNo() == null ? null : form.getSpotNo().trim();
            try {
                SpotBuildResult r = spotRegisterService.build(form, SpotFlowStep.BUILD_BATCH, operator);
                outcome.getItems().add(SpotBatchBringOutcome.Item.ok(spotNo, r));
                ok++;
            } catch (SpotRuleException e) {
                // 每个检查点各有各的提示语，原样逐行带回，不许并句。
                outcome.getItems().add(SpotBatchBringOutcome.Item.fail(spotNo, e.getMessage()));
            }
        }
        outcome.setTotal(forms.size());
        outcome.setSuccessCount(ok);
        outcome.setFailureCount(forms.size() - ok);
        return outcome;
    }
}
