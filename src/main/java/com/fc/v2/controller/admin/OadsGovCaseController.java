package com.fc.v2.controller.admin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.exception.gov.GovCaseRuleException;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TOadsGovCase;
import com.fc.v2.model.custom.GovCaseForm;
import com.fc.v2.model.custom.GovCaseOutcome;
import com.fc.v2.service.ITOadsGovCaseService;
import com.fc.v2.service.gov.GovCaseStep;
import com.fc.v2.shiro.util.ShiroUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 监管办件 Controller——四步流程（申报/核准/设置/验收）的唯一门面。
 *
 * <p>所有判定都在 {@link ITOadsGovCaseService} 一处给出：控制器只收报文、回原句提示。
 * 步次报文里带了只作核对参考；封存/收尾后的件在台账照常出现，只是页面不给任何操作入口。
 *
 * @author fuce
 * @date 2026-10-06
 */
@Api(value = "监管办件")
@Controller
@RequestMapping("/oadsGovCase")
public class OadsGovCaseController extends BaseController {

    private final String prefix = "admin/oadsGovCase";

    @Autowired
    private ITOadsGovCaseService govCaseService;

    @ApiOperation(value = "办件台账跳转", notes = "办件台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("oads:oadsGovCase:view")
    public String view(ModelMap model) {
        model.put("runningCount", govCaseService.runningCount());
        return prefix + "/list";
    }

    @Log(title = "监管办件台账", action = "list")
    @ApiOperation(value = "办件台账", notes = "在走与封存照常出现，封存不给操作入口")
    @GetMapping("/list")
    @RequiresPermissions("oads:oadsGovCase:list")
    @ResponseBody
    public ResultTable list() {
        startPage();
        List<TOadsGovCase> rows = govCaseService.listCases();
        com.github.pagehelper.PageInfo<TOadsGovCase> page = new com.github.pagehelper.PageInfo<TOadsGovCase>(rows);
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "未走完数量", notes = "后端一次查询，页首与它同一个数")
    @GetMapping("/runningCount")
    @RequiresPermissions("oads:oadsGovCase:list")
    @ResponseBody
    public AjaxResult runningCount() {
        return AjaxResult.successData(200, govCaseService.runningCount());
    }

    // ---------------------------------------------------------------- 立件前报入与撤销

    @Log(title = "监管办件报入", action = "intake")
    @ApiOperation(value = "立件前报入", notes = "四项输入逐项点名，同点位报入在档/在走唯一")
    @PostMapping("/intake")
    @RequiresPermissions("oads:oadsGovCase:open")
    @ResponseBody
    public AjaxResult intake(GovCaseForm form) {
        try {
            return AjaxResult.successData(200, govCaseService.intake(form, ShiroUtils.getLoginName()))
                    .put("msg", "报入已暂存，正式立件前可撤销");
        } catch (GovCaseRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "监管办件报入撤销", action = "revokeIntake")
    @ApiOperation(value = "报入撤销", notes = "只受理正式立件之前那一小段，不进主档")
    @PostMapping("/revokeIntake")
    @RequiresPermissions("oads:oadsGovCase:open")
    @ResponseBody
    public AjaxResult revokeIntake(@RequestParam("intakeId") Long intakeId) {
        try {
            return AjaxResult.successData(200,
                    govCaseService.revokeIntake(intakeId, ShiroUtils.getLoginName()))
                    .put("msg", "报入已撤销，不进主档、不占未走完位置");
        } catch (GovCaseRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "监管办件正式立件", action = "open")
    @ApiOperation(value = "正式立件", notes = "报入转主档，落第一步申报")
    @PostMapping("/open")
    @RequiresPermissions("oads:oadsGovCase:open")
    @ResponseBody
    public AjaxResult open(@RequestParam("intakeId") Long intakeId) {
        try {
            GovCaseOutcome o = govCaseService.open(intakeId, ShiroUtils.getLoginName());
            return AjaxResult.successData(200, o).put("msg", o.getMessage());
        } catch (GovCaseRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    // ---------------------------------------------------------------- 四步之间移动

    @Log(title = "监管办件推进", action = "advance")
    @ApiOperation(value = "推进到紧邻下一步", notes = "跨步不生效；进设置看三段签署，进验收看核收同意")
    @PostMapping("/advance")
    @RequiresPermissions("oads:oadsGovCase:flow")
    @ResponseBody
    public AjaxResult advance(@RequestParam("caseId") Long caseId,
                              @RequestParam(value = "note", required = false) String note,
                              @RequestParam(value = "claimStep", required = false) Integer claimStep,
                              @RequestParam(value = "channel", required = false) String channel,
                              @RequestParam(value = "idemKey", required = false) String idemKey) {
        try {
            GovCaseOutcome o = govCaseService.advance(caseId, note, ShiroUtils.getLoginName(),
                    claimStep, channel, idemKey);
            return AjaxResult.successData(200, o).put("msg", o.getMessage());
        } catch (GovCaseRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "监管办件后退", action = "returnStep")
    @ApiOperation(value = "后退一步", notes = "被退步批语转追溯，第一步不再后退")
    @PostMapping("/returnStep")
    @RequiresPermissions("oads:oadsGovCase:flow")
    @ResponseBody
    public AjaxResult returnStep(@RequestParam("caseId") Long caseId,
                                 @RequestParam(value = "note", required = false) String note,
                                 @RequestParam(value = "idemKey", required = false) String idemKey) {
        try {
            GovCaseOutcome o = govCaseService.returnStep(caseId, note, ShiroUtils.getLoginName(), idemKey);
            return AjaxResult.successData(200, o).put("msg", o.getMessage());
        } catch (GovCaseRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "监管办件设置核收", action = "recordReceipt")
    @ApiOperation(value = "设置步核收登记", notes = "记为同意才准验收，同轮第二笔不收")
    @PostMapping("/recordReceipt")
    @RequiresPermissions("oads:oadsGovCase:flow")
    @ResponseBody
    public AjaxResult recordReceipt(@RequestParam("caseId") Long caseId,
                                    @RequestParam("agree") boolean agree,
                                    @RequestParam(value = "note", required = false) String note,
                                    @RequestParam(value = "idemKey", required = false) String idemKey) {
        try {
            GovCaseOutcome o = govCaseService.recordReceipt(caseId, agree, note,
                    ShiroUtils.getLoginName(), idemKey);
            return AjaxResult.successData(200, o).put("msg", o.getMessage());
        } catch (GovCaseRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "监管办件验收见证", action = "submitWitness")
    @ApiOperation(value = "验收见证意见", notes = "两名见证各一笔＋凭证回写才封存")
    @PostMapping("/submitWitness")
    @RequiresPermissions("oads:oadsGovCase:flow")
    @ResponseBody
    public AjaxResult submitWitness(@RequestParam("caseId") Long caseId,
                                    @RequestParam("witnessPost") String witnessPost,
                                    @RequestParam("opinion") String opinion,
                                    @RequestParam(value = "certNo", required = false) String certNo,
                                    @RequestParam(value = "idemKey", required = false) String idemKey) {
        try {
            GovCaseOutcome o = govCaseService.submitWitness(caseId, witnessPost, opinion, certNo,
                    ShiroUtils.getLoginName(), idemKey);
            return AjaxResult.successData(200, o).put("msg", o.getMessage());
        } catch (GovCaseRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "监管办件申报后撤回", action = "withdraw")
    @ApiOperation(value = "申报之后撤回报备", notes = "收尾达成，不再往下走")
    @PostMapping("/withdraw")
    @RequiresPermissions("oads:oadsGovCase:flow")
    @ResponseBody
    public AjaxResult withdraw(@RequestParam("caseId") Long caseId,
                               @RequestParam(value = "note", required = false) String note,
                               @RequestParam(value = "idemKey", required = false) String idemKey) {
        try {
            GovCaseOutcome o = govCaseService.withdraw(caseId, note, ShiroUtils.getLoginName(), idemKey);
            return AjaxResult.successData(200, o).put("msg", o.getMessage());
        } catch (GovCaseRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "监管办件路段划归", action = "realign")
    @ApiOperation(value = "路段划归邻段", notes = "收尾达成，不再往下走")
    @PostMapping("/realign")
    @RequiresPermissions("oads:oadsGovCase:flow")
    @ResponseBody
    public AjaxResult realign(@RequestParam("caseId") Long caseId,
                              @RequestParam("newAreaNo") String newAreaNo,
                              @RequestParam(value = "note", required = false) String note,
                              @RequestParam(value = "idemKey", required = false) String idemKey) {
        try {
            GovCaseOutcome o = govCaseService.realign(caseId, newAreaNo, note,
                    ShiroUtils.getLoginName(), idemKey);
            return AjaxResult.successData(200, o).put("msg", o.getMessage());
        } catch (GovCaseRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "监管办件作废", action = "voidCase")
    @ApiOperation(value = "作废", notes = "只许从最后一步验收发起")
    @PostMapping("/voidCase")
    @RequiresPermissions("oads:oadsGovCase:flow")
    @ResponseBody
    public AjaxResult voidCase(@RequestParam("caseId") Long caseId,
                               @RequestParam(value = "note", required = false) String note,
                               @RequestParam(value = "idemKey", required = false) String idemKey) {
        try {
            GovCaseOutcome o = govCaseService.voidCase(caseId, note, ShiroUtils.getLoginName(), idemKey);
            return AjaxResult.successData(200, o).put("msg", o.getMessage());
        } catch (GovCaseRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    // ---------------------------------------------------------------- 查询与追溯

    @ApiOperation(value = "办件当下第几步", notes = "服务端按留痕重数，报文步次只作参考")
    @GetMapping("/currentStep")
    @RequiresPermissions("oads:oadsGovCase:list")
    @ResponseBody
    public AjaxResult currentStep(@RequestParam("caseId") Long caseId) {
        Integer step = govCaseService.currentStep(caseId);
        return AjaxResult.successData(200, step == null ? null : step)
                .put("stepName", GovCaseStep.nameOf(step));
    }

    @ApiOperation(value = "办件追溯", notes = "逐笔留痕时序、重数步次、核收与见证一次取齐")
    @GetMapping("/trace")
    @RequiresPermissions("oads:oadsGovCase:list")
    @ResponseBody
    public AjaxResult trace(@RequestParam("caseId") Long caseId) {
        try {
            return AjaxResult.successData(200, govCaseService.trace(caseId));
        } catch (GovCaseRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @ApiOperation(value = "当天受理流水", notes = "当天全部落笔按时刻排，与追溯笔数逐笔对")
    @GetMapping("/register")
    @RequiresPermissions("oads:oadsGovCase:list")
    @ResponseBody
    public AjaxResult register(@RequestParam(value = "day", required = false) String day) {
        Date at = null;
        if (day != null && !day.trim().isEmpty()) {
            try {
                at = new SimpleDateFormat("yyyy-MM-dd").parse(day.trim());
            } catch (Exception e) {
                return AjaxResult.error("日期格式须为 yyyy-MM-dd：" + day);
            }
        }
        return AjaxResult.successData(200, govCaseService.acceptanceRegister(at));
    }
}
