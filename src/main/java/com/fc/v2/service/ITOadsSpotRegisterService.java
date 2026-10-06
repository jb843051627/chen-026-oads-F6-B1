package com.fc.v2.service;

import java.util.List;

import com.fc.v2.model.auto.TOadsSpot;
import com.fc.v2.model.custom.CitySpot;
import com.fc.v2.model.custom.CitySyncOutcome;
import com.fc.v2.model.custom.PostScope;
import com.fc.v2.model.custom.SpotBookQuery;
import com.fc.v2.model.custom.SpotBuildResult;
import com.fc.v2.model.custom.SpotFlowOutcome;
import com.fc.v2.model.custom.SpotForm;
import com.fc.v2.model.custom.SpotViewCounts;

/**
 * 点位名册五检查点唯一裁决处。
 *
 * <p>一行能否落库、待补处数算多少、辖段视图里出现哪几行、页首与回执那几个数从哪里取，
 * 四样只认本接口这一处给出的答案；旁处不得保留第二条能绕开它的判定路径。
 *
 * <ul>
 *   <li>检查点1 身份：点位码报送自带；本季同码已有在走行时拒绝，操作对象就是该行本身时按更新；
 *       压卡或就地停住后名额才空出，同点位可另立新行。</li>
 *   <li>检查点2 空间挂靠：坐标落在辖段围栏之外一律不收，提示写明"该点位在围栏之外"；
 *       坐标与挂靠现场采集，不提供人工改写口子。</li>
 *   <li>检查点3 数量：待补=核定应设-已核验，每次写库前重算，覆盖旧值；不合规点名字段。
 *       MANUAL/BATCH 两通道、存量/新增行同一套逻辑。</li>
 *   <li>检查点4 名录依据：代号在目录里找不到建行失败；注销段不接新行，旧行仍列出仍计数；
 *       冗余全称与目录有出入以目录为准修正。</li>
 *   <li>检查点5 同步与展示：分页条数区局配置；三查询入口全等/前缀；可见范围按辖段切；
 *       三视图处数同一份基础数据；市增量游标唯一、同事务推进、冲突逐条不静默。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-10-05
 */
public interface ITOadsSpotRegisterService {

    /**
     * 建行/更新，两条通道（{@code MANUAL} 建档岗逐条、{@code BATCH} 集中更换带出）同一套逻辑。
     * form 带 id 且即本季同码那一行本身时按更新；同码另有在走行时拒绝。
     */
    SpotBuildResult build(SpotForm form, String buildKind, String operator);

    /** 已核验落地处数被更正：写库前重算待补处数；压卡行改不动。 */
    SpotBuildResult correctGot(Long spotId, Integer newGot, String operator);

    /** 该行改挂到别的路段目录：重走空间挂靠与名录依据，写库前重算待补处数。 */
    SpotBuildResult rehang(Long spotId, String newAreaNo,
                           java.math.BigDecimal longitude, java.math.BigDecimal latitude, String operator);

    /** 往上推一步：这一档没过下一步不给走；步序系统按留痕数，报文自称档不作数。 */
    SpotFlowOutcome advance(Long spotId, String note, String operator);

    /**
     * 往上推一步并核对屏上填的档：claimStep 与"下一步"对不上即按跨档拒绝；
     * 传 null 不核对。记账档仍只由系统按留痕数。
     */
    SpotFlowOutcome advance(Long spotId, String note, String operator, Integer claimStep);

    /** 行还没到压卡，前面的已核退回待核重来；上一回的东西保留，不与后补的并在一起。 */
    SpotFlowOutcome returnStep(Long spotId, String note, String operator);

    /** 就地停住：不再往上推；停住后同点位才立得起第二行。 */
    SpotFlowOutcome hold(Long spotId, String note, String operator);

    /** 这一行目前停在哪一步，系统按步序留痕自己数（旧行留痕空时返回 null，不算压卡）。 */
    Integer currentStep(Long spotId);

    /** 名册分页：行数照区局配置，查询三入口、全等/前缀，范围按岗位辖段切。 */
    List<TOadsSpot> pageBook(SpotBookQuery query, PostScope scope, int pageNum, int pageSize);

    /** 名册总数：与分页、三视图出自同一份基础数据。 */
    long countBook(SpotBookQuery query, PostScope scope);

    /** 三种视图处数：一次快照分组，总数必须一致；空视图合法，不拿邻段补齐。 */
    SpotViewCounts viewCounts(String quarterCode, PostScope scope);

    /** 区局配置：名册每页条数。 */
    int pageSize();

    /** 区局配置：当前在册季度码。 */
    String currentQuarter();

    /** 区局改每页条数，只此一处给值。 */
    void configurePageSize(int pageSize, String operator);

    /** 岗位可见范围：从辖段授权点出，不由页面自报。 */
    PostScope scopeOf(String postNo);

    /**
     * 市里每日增量：游标之前不复核，游标之后才参与；冲突逐条、以本局为准、不静默覆盖；
     * 游标随本次同步同事务推进，别处不得自行调整。
     */
    CitySyncOutcome syncCity(List<CitySpot> cityRows, String operator);
}
