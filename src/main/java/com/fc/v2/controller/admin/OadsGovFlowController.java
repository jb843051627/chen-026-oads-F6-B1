package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.exception.gov.GovCaseRuleException;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TOadsGovFlow;
import com.fc.v2.service.ITOadsGovFlowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 点位监管办件流转单（旧版 stage 0..3）Controller——只读留档入口。
 *
 * <p>版本取舍已定死：本套不添列、不改名、不动存量数据，只保留旧台账只读回看；
 * 申报/核准/设置/验收一切流转都走新版「监管办件」（{@code /oadsGovCase}，
 * t_oads_gov_case），步次以新版逐笔留痕重数为准。旧入口的推进/回退一律拒绝，
 * 不在旧表落一笔，页面访问本入口直接跳到新台账，避免"列表一件、详情另一件"两本账。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Api(value = "点位监管办件流转单（旧版，只读留档）")
@Controller
@RequestMapping("/oadsGovFlow")
public class OadsGovFlowController extends BaseController {

    @Autowired
    private ITOadsGovFlowService oadsGovFlowService;

    @ApiOperation(value = "旧流转台账跳转", notes = "已由新版监管办件承接，直接跳到新台账")
    @GetMapping("/view")
    @RequiresPermissions("oads:oadsGovCase:view")
    public String view() {
        // 旧页面已撤：只有一本台账，访问旧入口直接进新版，避免两本账各看各的。
        return "redirect:/oadsGovCase/view";
    }

    @Log(title = "点位监管办件流转单旧台账", action = "list")
    @ApiOperation(value = "旧流转台账（只读）", notes = "存量旧单只读回看，不给任何写操作入口")
    @GetMapping("/list")
    @RequiresPermissions("oads:oadsGovCase:list")
    @ResponseBody
    public ResultTable list(TOadsGovFlow record) {
        QueryWrapper<TOadsGovFlow> queryWrapper = new QueryWrapper<TOadsGovFlow>();
        startPage();
        com.github.pagehelper.PageInfo<TOadsGovFlow> page =
                new com.github.pagehelper.PageInfo<TOadsGovFlow>(oadsGovFlowService.selectTOadsGovFlowList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "点位监管办件流转单推进（旧入口已停写）", action = "advance")
    @ApiOperation(value = "推进一档（旧入口已停写）", notes = "一律拒绝，推进请走 /oadsGovCase/advance")
    @PostMapping("/advance")
    @RequiresPermissions("oads:oadsGovCase:flow")
    @ResponseBody
    public AjaxResult advance(Long id, String remark) {
        try {
            oadsGovFlowService.advance(id, remark);
            return AjaxResult.error("旧版流转已停写，本不该走到这里");
        } catch (GovCaseRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "点位监管办件流转单回退（旧入口已停写）", action = "rollback")
    @ApiOperation(value = "回退一档（旧入口已停写）", notes = "一律拒绝，后退请走 /oadsGovCase/returnStep")
    @PostMapping("/rollback")
    @RequiresPermissions("oads:oadsGovCase:flow")
    @ResponseBody
    public AjaxResult rollback(Long id, String remark) {
        try {
            oadsGovFlowService.rollback(id, remark);
            return AjaxResult.error("旧版流转已停写，本不该走到这里");
        } catch (GovCaseRuleException e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
