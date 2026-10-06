package com.fc.v2.service;

import java.util.List;

import com.fc.v2.model.auto.TOadsCheckRow;
import com.fc.v2.model.custom.SpotBatchBringOutcome;
import com.fc.v2.model.custom.SpotForm;

/**
 * 集中更换核验行 Service接口（batch-process 形状：整批提交，无单条增删改）
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITOadsCheckRowService {

    /** 按主键回查单行 */
    TOadsCheckRow selectTOadsCheckRowById(Long id);

    /**
     * 整批提交：逐行校验，合法的入库、非法的记入失败明细（保留原始行号），返回**成功行数**。
     * 同一批次重复提交不得重复入库；行数超过单批上限返回 -1 且整批不入库；空批次返回 0。
     */
    int submitBatch(String batchNo, List<TOadsCheckRow> rows);

    /** 该批次的失败行明细（按行号升序） */
    List<TOadsCheckRow> listErrors(String batchNo);

    /**
     * 建行第二通道：集中更换业务在核验完成后，把核验行带出成名册行。
     *
     * <p>这是与建档岗逐条建立等价的去处——不改动 {@link #submitBatch} 本体（收什么回什么维持原状），
     * 另写此处承接。逐行交点位名册唯一裁决处按 BATCH 通道走同一套五个检查点与同一套数量逻辑，
     * 界面上不长第二种样式；某行不过就带回该检查点自己的提示语，不合并成"数据有误"，也不拖累别行。
     */
    SpotBatchBringOutcome bringOutSpots(String batchNo, List<SpotForm> forms, String operator);
}
