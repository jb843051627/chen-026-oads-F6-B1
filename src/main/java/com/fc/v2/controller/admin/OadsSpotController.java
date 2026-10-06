package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TOadsSpot;
import com.fc.v2.model.custom.SafetyGradeResult;
import com.fc.v2.model.custom.SpotGradeQuery;
import com.fc.v2.service.ITOadsSpotService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 点位底册 Controller
 *
 * @author fuce
 * @date 2026-09-12
 */
@Api(value = "点位底册")
@Controller
@RequestMapping("/OadsSpotController")
public class OadsSpotController extends BaseController {

    private final String prefix = "admin/oadsSpot";

    @Autowired
    private ITOadsSpotService oadsSpotService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("oads:oadsSpot:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "点位底册集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @RequiresPermissions("oads:oadsSpot:list")
    @ResponseBody
    public ResultTable list(TOadsSpot record) {
        QueryWrapper<TOadsSpot> queryWrapper = new QueryWrapper<TOadsSpot>();
        startPage();
        com.github.pagehelper.PageInfo<TOadsSpot> page =
                new com.github.pagehelper.PageInfo<TOadsSpot>(oadsSpotService.selectTOadsSpotList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "点位底册新增", action = "add")
    @ApiOperation(value = "新增", notes = "新增")
    @PostMapping("/add")
    @RequiresPermissions("oads:oadsSpot:add")
    @ResponseBody
    public AjaxResult add(TOadsSpot record) {
        return toAjax(oadsSpotService.insertTOadsSpot(record));
    }

    @Log(title = "点位底册修改", action = "edit")
    @ApiOperation(value = "修改保存", notes = "修改保存")
    @PostMapping("/edit")
    @RequiresPermissions("oads:oadsSpot:edit")
    @ResponseBody
    public AjaxResult editSave(TOadsSpot record) {
        return toAjax(oadsSpotService.updateTOadsSpot(record));
    }

    @Log(title = "点位底册删除", action = "remove")
    @ApiOperation(value = "删除", notes = "删除")
    @DeleteMapping("/remove")
    @RequiresPermissions("oads:oadsSpot:remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(oadsSpotService.deleteTOadsSpotByIds(ids));
    }

    @Log(title = "点位安全分档（只判不固化）", action = "grade")
    @ApiOperation(value = "单点评定", notes = "挂代号走按标识取值，空着走整表取优")
    @GetMapping("/grade")
    @RequiresPermissions("oads:oadsSpot:view")
    @ResponseBody
    public AjaxResult grade(@RequestParam("spotId") Long spotId,
                            @RequestParam("score") BigDecimal score,
                            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                            @RequestParam(value = "at", required = false) Date at) {
        if (at == null) {
            at = new Date();
        }
        SafetyGradeResult result = oadsSpotService.gradeSpot(spotId, score, at);
        return AjaxResult.successData(200, result);
    }

    @Log(title = "点位安全分档固化", action = "gradeAndFix")
    @ApiOperation(value = "判定并固化", notes = "档位/代号/起用日写回点位并追加留痕，同一事务")
    @PostMapping("/gradeAndFix")
    @RequiresPermissions("oads:oadsSpot:grade")
    @ResponseBody
    public AjaxResult gradeAndFix(@RequestParam("spotId") Long spotId,
                                  @RequestParam("score") BigDecimal score,
                                  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                  @RequestParam(value = "at", required = false) Date at) {
        if (at == null) {
            at = new Date();
        }
        SafetyGradeResult result = oadsSpotService.gradeAndFix(spotId, score, at);
        return AjaxResult.successData(200, result);
    }

    @Log(title = "点位安全分档批量判定", action = "gradeBatch")
    @ApiOperation(value = "批量逐条判定", notes = "一份快照判一批，清单/条数/逐条同源")
    @PostMapping("/gradeBatch")
    @RequiresPermissions("oads:oadsSpot:view")
    @ResponseBody
    public AjaxResult gradeBatch(@RequestBody List<SpotGradeQuery> queries,
                                 @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                 @RequestParam(value = "at", required = false) Date at) {
        if (at == null) {
            at = new Date();
        }
        return AjaxResult.successData(200, oadsSpotService.gradeBatch(queries, at));
    }

    @Log(title = "悬空点位清单", action = "listDangling")
    @ApiOperation(value = "悬空点位", notes = "挂着代号、at 时刻却取不到生效版本的点位逐条列出")
    @GetMapping("/dangling")
    @RequiresPermissions("oads:oadsSpot:view")
    @ResponseBody
    public AjaxResult dangling(@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                               @RequestParam(value = "at", required = false) Date at) {
        if (at == null) {
            at = new Date();
        }
        return AjaxResult.successData(200, oadsSpotService.listDangling(at));
    }
}
