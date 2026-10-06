package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.exception.permit.PermitRuleException;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TOadsPermitBill;
import com.fc.v2.model.custom.PermitBillView;
import com.fc.v2.model.custom.PermitLedgerCheck;
import com.fc.v2.model.custom.PermitSignOutcome;
import com.fc.v2.service.ITOadsPermitBillService;
import com.fc.v2.shiro.util.ShiroUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 设置许可核准单 Controller（approval-chain 形状：三段签批）。
 *
 * <p>入口只留三种只读回显（台账/整单/履历/登记簿）与两个动作：
 * 由点位主档立单 {@code createFromSpot}、一次签署 {@code sign}。
 * 没有自建单据、改旧笔、追补签署、整张撤回的旁路；段次不从报文收。
 *
 * @author fuce
 * @date 2026-10-05
 */
@Api(value = "设置许可核准单")
@Controller
@RequestMapping("/oadsPermitBill")
public class OadsPermitBillController extends BaseController {

    private final String prefix = "admin/oadsPermitBill";

    @Autowired
    private ITOadsPermitBillService oadsPermitBillService;

    @ApiOperation(value = "核准台账跳转", notes = "核准台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("oads:oadsPermitBill:view")
    public String view(ModelMap model) {
        model.put("unsealedCount", oadsPermitBillService.unsealedCount());
        return prefix + "/list";
    }

    @Log(title = "设置许可核准单台账", action = "list")
    @ApiOperation(value = "台账分页查询", notes = "台账分页查询")
    @GetMapping("/list")
    @RequiresPermissions("oads:oadsPermitBill:list")
    @ResponseBody
    public ResultTable list(TOadsPermitBill record) {
        QueryWrapper<TOadsPermitBill> queryWrapper = new QueryWrapper<TOadsPermitBill>()
                .eq("del_flag", 0)
                .orderByDesc("create_time");
        if (record.getBillNo() != null && !record.getBillNo().trim().isEmpty()) {
            queryWrapper.like("bill_no", record.getBillNo().trim());
        }
        if (record.getSpotNo() != null && !record.getSpotNo().trim().isEmpty()) {
            queryWrapper.eq("spot_no", record.getSpotNo().trim());
        }
        if (record.getStatus() != null) {
            queryWrapper.eq("status", record.getStatus());
        }
        startPage();
        com.github.pagehelper.PageInfo<TOadsPermitBill> page =
                new com.github.pagehelper.PageInfo<TOadsPermitBill>(
                        oadsPermitBillService.selectBillList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "未封存数量", notes = "建单加一、封存减一，与逐段累加同一次计算")
    @GetMapping("/unsealedCount")
    @RequiresPermissions("oads:oadsPermitBill:list")
    @ResponseBody
    public AjaxResult unsealedCount() {
        return AjaxResult.success("ok").put("count", oadsPermitBillService.unsealedCount());
    }

    @ApiOperation(value = "整单跳转", notes = "按段分节整单回显跳转")
    @GetMapping("/detail/{id}")
    @RequiresPermissions("oads:oadsPermitBill:view")
    public String detail(@PathVariable("id") Long id, ModelMap model) {
        model.put("billId", id);
        return prefix + "/detail";
    }

    @ApiOperation(value = "整单数据", notes = "单头加按段分节，当前节与人数服务端算好，老轮留痕一并带出")
    @GetMapping("/detailData/{id}")
    @RequiresPermissions("oads:oadsPermitBill:view")
    @ResponseBody
    public AjaxResult detailData(@PathVariable("id") Long id) {
        PermitBillView view = oadsPermitBillService.getBillDetail(id);
        return AjaxResult.success("ok").put("data", view);
    }

    @Log(title = "设置许可核准单立单", action = "createFromSpot")
    @ApiOperation(value = "由点位主档立单", notes = "只能由点位主档记录生成，同点位同变更在途一张")
    @PostMapping("/createFromSpot")
    @RequiresPermissions("oads:oadsPermitBill:create")
    @ResponseBody
    public AjaxResult createFromSpot(Long spotId, String changeKind) {
        try {
            TOadsPermitBill bill =
                    oadsPermitBillService.createFromSpot(spotId, changeKind, ShiroUtils.getLoginName());
            return AjaxResult.success("已立核准单 " + bill.getBillNo() + "，口头报备不算开工，以此单为准")
                    .put("data", bill);
        } catch (PermitRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "设置许可核准单签署", action = "sign")
    @ApiOperation(value = "一次签署（唯一入口）", notes = "报文段次仅核对：与当前段不符即丢弃；记账段次服务端定")
    @PostMapping("/sign")
    @RequiresPermissions("oads:oadsPermitBill:sign")
    @ResponseBody
    public AjaxResult sign(Long billId, Integer opinion, String content, Integer claimNode) {
        try {
            PermitSignOutcome outcome =
                    oadsPermitBillService.sign(billId, ShiroUtils.getLoginName(), opinion, content, claimNode);
            return AjaxResult.success(outcome.getMessage())
                    .put("outcome", outcome.getOutcome())
                    .put("data", outcome.getBill());
        } catch (PermitRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @ApiOperation(value = "台账对账", notes = "顶部数与逐段留痕重算同一次核对，对不上逐张指名")
    @GetMapping("/reconcile")
    @RequiresPermissions("oads:oadsPermitBill:list")
    @ResponseBody
    public AjaxResult reconcile() {
        PermitLedgerCheck check = oadsPermitBillService.reconcile();
        return AjaxResult.success(check.isBalanced() ? "账账相符" : "账面对不上，已逐张指名")
                .put("data", check);
    }

    @ApiOperation(value = "点位变更履历", notes = "按发生时刻倒序，由谁/依据哪张单/从哪档到哪档/何时")
    @GetMapping("/history")
    @RequiresPermissions("oads:oadsPermitBill:view")
    @ResponseBody
    public AjaxResult history(Long spotId) {
        return AjaxResult.success("ok").put("data", oadsPermitBillService.spotChangeHistory(spotId));
    }

    @ApiOperation(value = "当日受理登记簿", notes = "当日全部落笔按落单时刻排，含被清退出的老轮")
    @GetMapping("/register")
    @RequiresPermissions("oads:oadsPermitBill:list")
    @ResponseBody
    public AjaxResult register(
            @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam(value = "day", required = false) Date day) {
        return AjaxResult.success("ok")
                .put("data", oadsPermitBillService.acceptanceRegister(day == null ? new Date() : day));
    }
}
