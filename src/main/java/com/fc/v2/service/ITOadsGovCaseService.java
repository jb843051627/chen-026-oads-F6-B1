package com.fc.v2.service;

import com.fc.v2.model.auto.TOadsGovCase;
import com.fc.v2.model.auto.TOadsGovCaseIntake;
import com.fc.v2.model.auto.TOadsGovCaseLog;
import com.fc.v2.model.custom.GovCaseForm;
import com.fc.v2.model.custom.GovCaseOutcome;
import com.fc.v2.model.custom.GovCaseTrace;

import java.util.Date;
import java.util.List;

/**
 * 监管办件四步流程唯一裁决处（新一套：申报→核准→设置→验收；旧 t_oads_gov_flow 不添不改）。
 *
 * <p>四问只在本处给答案：一条办件当下第几步、下一步准入是否满足、已封存接不接新意见、
 * 同步二次批语怎么存放。办件主档与逐笔留痕说的是同一件事，步次一律按留痕重数，
 * 不采信报文步次、不借邻步旧结论、不拿另一条办件的步数来套。
 *
 * <p>立件分两小段：{@link #intake} 报入暂存 → {@link #open} 正式立件；撤销 {@link #revokeIntake}
 * 只可能夹在这两小段之间，被撤记录不进主档、不占未走完位置。
 *
 * @author fuce
 * @date 2026-10-06
 */
public interface ITOadsGovCaseService {

    // ---------------------------------------------------------------- 立件前报入与撤销

    /** 报入暂存：四项输入逐项点名校验，缺一项不收；同键重放取首条。 */
    TOadsGovCaseIntake intake(GovCaseForm form, String operator);

    /** 撤销：只许撤销报入在档（OPEN）那一条；已立件的不走这里，被撤记录不进主档。 */
    TOadsGovCaseIntake revokeIntake(Long intakeId, String operator);

    /** 正式立件：报入转主档，落第一步「申报」；同点位已有在走办件则立不起来。 */
    GovCaseOutcome open(Long intakeId, String operator);

    // ---------------------------------------------------------------- 四步之间移动

    /**
     * 推进到紧邻下一步，跨步那一步不生效、整条留在原地。
     *
     * <p>准入：进设置须设置许可三段签署走完；进验收须设置步核收记为同意。
     * claimStep 为报文自称步次，只作核对：与服务端重数的下一步不一致即按跨步不生效处理。
     *
     * @param channel 通道（两通道同刻各送各的，逐笔留痕）
     * @param idemKey 动作幂等键，同键重放不添第二行
     */
    GovCaseOutcome advance(Long caseId, String note, String operator, Integer claimStep,
                           String channel, String idemKey);

    /**
     * 后退一步：被退步当轮批语按原先后转追溯（active 置 0，行不删），重走该步从新一轮重新累计；
     * 已在第一步（申报）不再后退。设置/验收各自的核收、见证老轮随同轮退出。
     */
    GovCaseOutcome returnStep(Long caseId, String note, String operator, String idemKey);

    /** 设置步核收登记：agree=true 记为同意，验收才准开始；同轮第二笔保留首次、不覆盖不追加。 */
    GovCaseOutcome recordReceipt(Long caseId, boolean agree, String note, String operator, String idemKey);

    /**
     * 验收步见证意见：两名见证岗各提交一次，同岗第二笔保留首次。
     * 两名齐备且复验凭证编码有落，同次回写主档并封存；缺一样留在验收继续等。
     */
    GovCaseOutcome submitWitness(Long caseId, String witnessPost, String opinion, String certNo,
                                 String operator, String idemKey);

    /** 申报之后撤回报备：收尾达成，不必再往下走（只受理停在申报步的在走件）。 */
    GovCaseOutcome withdraw(Long caseId, String note, String operator, String idemKey);

    /** 所属路段从目录划归邻段：收尾达成；newAreaNo 为划归的邻段代号。 */
    GovCaseOutcome realign(Long caseId, String newAreaNo, String note, String operator, String idemKey);

    /** 作废：只许从最后一步（验收）发起，且尚未封存。 */
    GovCaseOutcome voidCase(Long caseId, String note, String operator, String idemKey);

    // ---------------------------------------------------------------- 查询、追溯、计数

    /** 当下第几步：按逐笔留痕重数，不看报文、不看情面列。 */
    Integer currentStep(Long caseId);

    /** 整件追溯：留痕时序、重数步次、本轮核收与见证一次取齐。 */
    GovCaseTrace trace(Long caseId);

    /** 台账查询：在走、封存、撤回、划归、作废照常出现。 */
    List<TOadsGovCase> listCases();

    /** 按主键查（不存在/已删返回 null）。 */
    TOadsGovCase getCase(Long caseId);

    /** 未走完数量：后端一次查询，页首直接取它，不取前端缓存、不另算一套。 */
    long runningCount();

    /** 当天受理流水：当天全部落笔（含后退转追溯老轮），按提交时刻、笔序排。 */
    List<TOadsGovCaseLog> acceptanceRegister(Date day);
}
