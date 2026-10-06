package com.fc.v2.controller.admin;

import java.util.List;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.exception.plaque.PlaqueRuleException;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TOadsPlaqueBook;
import com.fc.v2.model.auto.TOadsPlaqueRow;
import com.fc.v2.model.custom.PlaqueArchiveResult;
import com.fc.v2.model.custom.PlaqueBookForm;
import com.fc.v2.model.custom.PlaqueReceipt;
import com.fc.v2.model.custom.PlaqueReconcile;
import com.fc.v2.service.ITOadsPlaqueReceiptService;
import com.fc.v2.service.plaque.PlaqueConsts;
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

/**
 * 牌匾更换名册收讫 Controller——区局收讫环节唯一门面（新名目自有一条，旧本体不动）。
 *
 * <p>提交形态固定为一册一次交完整：本门面只有整册入口（区级报送 {@code receiveArea}、
 * 市级复核 {@code receiveCity}），没有零散逐行报送入口；各单位先归并到区里、按季度合成一册，
 * 行不许游移到别册。四步判定全在 {@link ITOadsPlaqueReceiptService} 一处，控制器只收报文、回原句。
 *
 * @author fuce
 * @date 2026-10-05
 */
@Api(value = "牌匾更换名册收讫")
@Controller
@RequestMapping("/oadsPlaqueReceipt")
public class OadsPlaqueReceiptController extends BaseController {

    private final String prefix = "admin/oadsPlaqueReceipt";

    @Autowired
    private ITOadsPlaqueReceiptService plaqueReceiptService;

    @ApiOperation(value = "收讫页跳转", notes = "收讫页跳转")
    @GetMapping("/view")
    @RequiresPermissions("oads:oadsPlaqueReceipt:view")
    public String view(ModelMap model) {
        model.put("defaultRowLimit", PlaqueConsts.DEFAULT_ROW_LIMIT);
        model.put("codeMinLen", PlaqueConsts.SPOT_CODE_MIN_LEN);
        model.put("codeMaxLen", PlaqueConsts.SPOT_CODE_MAX_LEN);
        return prefix + "/list";
    }

    @Log(title = "牌匾更换名册区级报送收讫", action = "receiveArea")
    @ApiOperation(value = "区级报送整册收讫", notes = "一册一次交完整，四步连着做完")
    @PostMapping("/receiveArea")
    @RequiresPermissions("oads:oadsPlaqueReceipt:receive")
    @ResponseBody
    public AjaxResult receiveArea(@RequestBody PlaqueBookForm form) {
        return receive(form, PlaqueConsts.PUT_AREA);
    }

    @Log(title = "牌匾更换名册市级复核通道收讫", action = "receiveCity")
    @ApiOperation(value = "市级复核通道整册收讫", notes = "同一批次码第二回不重复入行，回执同册重出")
    @PostMapping("/receiveCity")
    @RequiresPermissions("oads:oadsPlaqueReceipt:review")
    @ResponseBody
    public AjaxResult receiveCity(@RequestBody PlaqueBookForm form) {
        return receive(form, PlaqueConsts.PUT_CITY);
    }

    private AjaxResult receive(PlaqueBookForm form, String putKind) {
        try {
            PlaqueReceipt receipt = plaqueReceiptService.receive(form, putKind, ShiroUtils.getLoginName());
            return AjaxResult.successData(200, receipt).put("msg", receipt.getNote());
        } catch (PlaqueRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @ApiOperation(value = "收讫回执", notes = "三数回库逐行数，逐行结论互印；同次落 RECEIPT 流水")
    @GetMapping("/receipt")
    @RequiresPermissions("oads:oadsPlaqueReceipt:view")
    @ResponseBody
    public AjaxResult receipt(@RequestParam("batchNo") String batchNo) {
        try {
            PlaqueReceipt receipt = plaqueReceiptService.receipt(batchNo, ShiroUtils.getLoginName());
            return AjaxResult.successData(200, receipt).put("msg", receipt.getNote());
        } catch (PlaqueRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @ApiOperation(value = "册账清单", notes = "按季度过滤，按收讫时间倒序")
    @GetMapping("/listBooks")
    @RequiresPermissions("oads:oadsPlaqueReceipt:list")
    @ResponseBody
    public AjaxResult listBooks(@RequestParam(value = "quarterCode", required = false) String quarterCode) {
        List<TOadsPlaqueBook> books = plaqueReceiptService.listBooks(quarterCode);
        return AjaxResult.successData(200, books);
    }

    @ApiOperation(value = "册内行（现行/历史）", notes = "layer 缺省全量；0现行层 1历史卷宗层")
    @GetMapping("/listRows")
    @RequiresPermissions("oads:oadsPlaqueReceipt:list")
    @ResponseBody
    public AjaxResult listRows(@RequestParam("batchNo") String batchNo,
                               @RequestParam(value = "layer", required = false) Integer layer) {
        try {
            List<TOadsPlaqueRow> rows = plaqueReceiptService.listRows(batchNo, layer);
            return AjaxResult.successData(200, rows);
        } catch (PlaqueRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "牌匾更换名册缺照片补挂", action = "attachPhoto")
    @ApiOperation(value = "缺照片补挂", notes = "为待核行补挂照片，待核缘由消尽同次裁为通过")
    @PostMapping("/attachPhoto")
    @RequiresPermissions("oads:oadsPlaqueReceipt:attach")
    @ResponseBody
    public AjaxResult attachPhoto(@RequestParam("batchNo") String batchNo,
                                  @RequestParam("rowNo") int rowNo,
                                  @RequestParam("photoNo") String photoNo) {
        try {
            TOadsPlaqueRow row = plaqueReceiptService.attachPhoto(batchNo, rowNo, photoNo,
                    ShiroUtils.getLoginName());
            return AjaxResult.successData(200, row).put("msg",
                    "第" + rowNo + "行照片已补挂，本行结论：" + PlaqueConsts.verdictName(row.getVerdict()));
        } catch (PlaqueRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "牌匾更换名册销账流水对账", action = "reconcile")
    @ApiOperation(value = "销账与流水对账", notes = "账实差以流水为准，销账抹平归收讫环节认")
    @PostMapping("/reconcile")
    @RequiresPermissions("oads:oadsPlaqueReceipt:reconcile")
    @ResponseBody
    public AjaxResult reconcile() {
        PlaqueReconcile result = plaqueReceiptService.reconcile(ShiroUtils.getLoginName());
        return AjaxResult.successData(200, result).put("msg", result.getNote());
    }

    @Log(title = "牌匾更换名册老化归档", action = "archiveAging")
    @ApiOperation(value = "满两年度待核行老化归档", notes = "逐行转入历史卷宗层，存量一条不减")
    @PostMapping("/archiveAging")
    @RequiresPermissions("oads:oadsPlaqueReceipt:archive")
    @ResponseBody
    public AjaxResult archiveAging(@RequestParam(value = "asOfYear", required = false) Integer asOfYear) {
        PlaqueArchiveResult result = plaqueReceiptService.archiveAging(asOfYear, ShiroUtils.getLoginName());
        return AjaxResult.successData(200, result).put("msg",
                "扫描 " + result.getScannedBooks() + " 册，满两个年度仍待核的 " + result.getArchivedRows()
                        + " 行已转入历史卷宗层（存量一条不减，照常可查）");
    }
}
