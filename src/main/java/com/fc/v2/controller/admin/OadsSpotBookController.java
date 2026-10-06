package com.fc.v2.controller.admin;

import java.util.List;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.exception.spot.SpotRuleException;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TSysUser;
import com.fc.v2.model.custom.CitySpot;
import com.fc.v2.model.custom.CitySyncOutcome;
import com.fc.v2.model.custom.PostScope;
import com.fc.v2.model.custom.SpotBookQuery;
import com.fc.v2.model.custom.SpotFlowOutcome;
import com.fc.v2.model.custom.SpotForm;
import com.fc.v2.model.custom.SpotViewCounts;
import com.fc.v2.service.ITOadsCheckRowService;
import com.fc.v2.service.ITOadsSpotRegisterService;
import com.fc.v2.shiro.util.ShiroUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;

/**
 * 点位名册 Controller——五个检查点流程的唯一门面。
 *
 * <p>所有判定都在 {@link ITOadsSpotRegisterService} 一处给出：控制器只收报文、回原句提示。
 * 建行只有两种来路（建档岗逐条 MANUAL、集中更换带出 BATCH），共用同一个表单与同一套判定，
 * 页面不长第二种样式；没有旁路能绕开裁决处。
 *
 * @author fuce
 * @date 2026-10-05
 */
@Api(value = "点位名册")
@Controller
@RequestMapping("/oadsSpotBook")
public class OadsSpotBookController extends BaseController {

    private final String prefix = "admin/oadsSpotBook";

    @Autowired
    private ITOadsSpotRegisterService registerService;

    @Autowired
    private ITOadsCheckRowService checkRowService;

    @ApiOperation(value = "名册跳转", notes = "名册跳转")
    @GetMapping("/view")
    @RequiresPermissions("oads:oadsSpotBook:view")
    public String view(ModelMap model) {
        model.put("pageSize", registerService.pageSize());
        model.put("quarterCode", registerService.currentQuarter());
        return prefix + "/list";
    }

    @Log(title = "点位名册分页查询", action = "list")
    @ApiOperation(value = "名册分页", notes = "三入口全等/前缀，范围按岗位辖段切")
    @GetMapping("/list")
    @RequiresPermissions("oads:oadsSpotBook:list")
    @ResponseBody
    public ResultTable list(SpotBookQuery query,
                            @RequestParam(value = "postNo", required = false) String postNo,
                            @RequestParam(value = "page", required = false) Integer page,
                            @RequestParam(value = "limit", required = false) Integer limit) {
        PostScope scope = registerService.scopeOf(resolvePostNo(postNo));
        int pageNum = page == null || page < 1 ? 1 : page;
        int pageSize = limit == null || limit < 1 ? registerService.pageSize() : limit;
        List rows = registerService.pageBook(query, scope, pageNum, pageSize);
        long total = registerService.countBook(query, scope);
        // 页首总数与分页同源：同一份基础数据，前端不再数第二遍。
        return pageTable(rows, total);
    }

    @ApiOperation(value = "三视图处数", notes = "按辖段类别/路段代号/建档情形，同一批行同一总数")
    @GetMapping("/viewCounts")
    @RequiresPermissions("oads:oadsSpotBook:list")
    @ResponseBody
    public AjaxResult viewCounts(@RequestParam(value = "quarterCode", required = false) String quarterCode,
                                 @RequestParam(value = "postNo", required = false) String postNo) {
        PostScope scope = registerService.scopeOf(resolvePostNo(postNo));
        SpotViewCounts counts = registerService.viewCounts(quarterCode, scope);
        return AjaxResult.successData(200, counts);
    }

    @Log(title = "点位名册建档岗建行", action = "build")
    @ApiOperation(value = "建档岗逐条建立", notes = "五检查点固定先后，待补服务端重算")
    @PostMapping("/build")
    @RequiresPermissions("oads:oadsSpotBook:build")
    @ResponseBody
    public AjaxResult build(SpotForm form) {
        try {
            return AjaxResult.successData(200,
                    registerService.build(form, com.fc.v2.service.spot.SpotFlowStep.BUILD_MANUAL,
                            ShiroUtils.getLoginName()));
        } catch (SpotRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "集中更换核验完成带出建行", action = "bringOut")
    @ApiOperation(value = "集中更换带出（第二通道）", notes = "与逐条建立同一套五检查点与数量逻辑")
    @PostMapping("/bringOut")
    @RequiresPermissions("oads:oadsSpotBook:build")
    @ResponseBody
    public AjaxResult bringOut(@RequestParam("batchNo") String batchNo,
                               @RequestBody List<SpotForm> forms) {
        return AjaxResult.successData(200,
                checkRowService.bringOutSpots(batchNo, forms, ShiroUtils.getLoginName()));
    }

    @Log(title = "已核验落地处数更正", action = "correctGot")
    @PostMapping("/correctGot")
    @RequiresPermissions("oads:oadsSpotBook:edit")
    @ResponseBody
    public AjaxResult correctGot(@RequestParam("spotId") Long spotId, @RequestParam("newGot") Integer newGot) {
        try {
            return AjaxResult.successData(200,
                    registerService.correctGot(spotId, newGot, ShiroUtils.getLoginName()));
        } catch (SpotRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "点位行改挂路段", action = "rehang")
    @PostMapping("/rehang")
    @RequiresPermissions("oads:oadsSpotBook:edit")
    @ResponseBody
    public AjaxResult rehang(@RequestParam("spotId") Long spotId,
                             @RequestParam("newAreaNo") String newAreaNo,
                             @RequestParam("longitude") BigDecimal longitude,
                             @RequestParam("latitude") BigDecimal latitude) {
        try {
            return AjaxResult.successData(200,
                    registerService.rehang(spotId, newAreaNo, longitude, latitude, ShiroUtils.getLoginName()));
        } catch (SpotRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "点位在走推进一步", action = "advance")
    @PostMapping("/advance")
    @RequiresPermissions("oads:oadsSpotBook:flow")
    @ResponseBody
    public AjaxResult advance(@RequestParam("spotId") Long spotId,
                              @RequestParam(value = "note", required = false) String note,
                              @RequestParam(value = "claimStep", required = false) Integer claimStep) {
        try {
            SpotFlowOutcome o = registerService.advance(spotId, note, ShiroUtils.getLoginName(), claimStep);
            return AjaxResult.successData(200, o).put("msg", o.getMessage());
        } catch (SpotRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "点位在走退回一步", action = "returnStep")
    @PostMapping("/returnStep")
    @RequiresPermissions("oads:oadsSpotBook:flow")
    @ResponseBody
    public AjaxResult returnStep(@RequestParam("spotId") Long spotId,
                                 @RequestParam(value = "note", required = false) String note) {
        try {
            SpotFlowOutcome o = registerService.returnStep(spotId, note, ShiroUtils.getLoginName());
            return AjaxResult.successData(200, o).put("msg", o.getMessage());
        } catch (SpotRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "点位在走就地停住", action = "hold")
    @PostMapping("/hold")
    @RequiresPermissions("oads:oadsSpotBook:flow")
    @ResponseBody
    public AjaxResult hold(@RequestParam("spotId") Long spotId,
                           @RequestParam(value = "note", required = false) String note) {
        try {
            SpotFlowOutcome o = registerService.hold(spotId, note, ShiroUtils.getLoginName());
            return AjaxResult.successData(200, o).put("msg", o.getMessage());
        } catch (SpotRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @ApiOperation(value = "行停在哪一步", notes = "系统按步序留痕自己数，屏上填的不算数")
    @GetMapping("/currentStep")
    @RequiresPermissions("oads:oadsSpotBook:list")
    @ResponseBody
    public AjaxResult currentStep(@RequestParam("spotId") Long spotId) {
        Integer step = registerService.currentStep(spotId);
        return AjaxResult.successData(200, step == null ? null : step)
                .put("stepName", com.fc.v2.service.spot.SpotFlowStep.nameOf(step));
    }

    @Log(title = "市里每日增量同步", action = "syncCity")
    @ApiOperation(value = "市里增量比对", notes = "游标唯一、同次推进；冲突逐条以本局为准")
    @PostMapping("/syncCity")
    @RequiresPermissions("oads:oadsSpotBook:sync")
    @ResponseBody
    public AjaxResult syncCity(@RequestBody(required = false) List<CitySpot> cityRows) {
        CitySyncOutcome outcome = registerService.syncCity(cityRows, ShiroUtils.getLoginName());
        return AjaxResult.successData(200, outcome).put("msg", outcome.getNote());
    }

    @Log(title = "名册每页条数配置", action = "configurePageSize")
    @PostMapping("/configurePageSize")
    @RequiresPermissions("oads:oadsSpotBook:cfg")
    @ResponseBody
    public AjaxResult configurePageSize(@RequestParam("pageSize") int pageSize) {
        try {
            registerService.configurePageSize(pageSize, ShiroUtils.getLoginName());
            return AjaxResult.success("每页条数已由区局改为 " + pageSize);
        } catch (SpotRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /** 岗位代号取登录人岗位；页面显式传了 postNo 以传入为准（演示/核对用）。 */
    private String resolvePostNo(String paramPostNo) {
        if (paramPostNo != null && !paramPostNo.trim().isEmpty()) {
            return paramPostNo.trim();
        }
        try {
            TSysUser user = ShiroUtils.getUser();
            if (user != null && user.getPosId() != null) {
                return String.valueOf(user.getPosId());
            }
        } catch (Exception ignore) {
            // 无登录上下文（任务/测试）时落匿名岗：什么辖段都没划，只见空，不拿邻段补齐。
        }
        return null;
    }
}
