package com.fc.v2.controller.admin;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.custom.SafetyGradeResult;
import com.fc.v2.model.custom.SafetyRuleSnapshot;
import com.fc.v2.service.ITOadsRuleLineService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * 牌面安全分档判据线 Controller（rule-eval 形状：只读求值，无增删改）。
 *
 * <p>页面看板上的可用清单、清单上方条数、逐条判定走的是 {@link SafetyRuleSnapshot}
 * 同一次计算，三处一处出，不许各起一轮。档位只在服务端这一处区间比对给出，页面不算第二遍。
 *
 * @author fuce
 * @date 2026-10-05
 */
@Api(value = "牌面安全分档判据线")
@Controller
@RequestMapping("/OadsRuleLineController")
public class OadsRuleLineController extends BaseController {

    @Autowired
    private ITOadsRuleLineService oadsRuleLineService;

    @Log(title = "分档看板一次计算", action = "board")
    @ApiOperation(value = "分档看板", notes = "可用清单、条数、脏配置点名同一次计算")
    @GetMapping("/board")
    @RequiresPermissions("oads:oadsRuleLine:view")
    @ResponseBody
    public AjaxResult board(
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            @RequestParam(value = "at", required = false) Date at) {
        if (at == null) {
            at = new Date();
        }
        // 一处出：count 与 available、dirtyNotes 都取自这同一个快照对象。
        SafetyRuleSnapshot snap = oadsRuleLineService.snapshot(at);
        Map<String, Object> board = new LinkedHashMap<String, Object>();
        board.put("at", snap.getAt());
        board.put("count", snap.count());
        board.put("available", snap.getAvailable());
        board.put("dirtyNotes", snap.getDirtyNotes());
        return AjaxResult.successData(200, board);
    }

    @Log(title = "按判据线代号判定档位", action = "gradeByCode")
    @ApiOperation(value = "按标识取值", notes = "按判据线代号取 at 时刻生效版本判档")
    @GetMapping("/gradeByCode")
    @RequiresPermissions("oads:oadsRuleLine:view")
    @ResponseBody
    public AjaxResult gradeByCode(@RequestParam("ruleCode") String ruleCode,
                                  @RequestParam("score") BigDecimal score,
                                  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                  @RequestParam(value = "at", required = false) Date at) {
        if (at == null) {
            at = new Date();
        }
        SafetyGradeResult result = oadsRuleLineService.gradeByCode(ruleCode, score, at);
        return AjaxResult.successData(200, result);
    }

    @Log(title = "整表取优判定档位", action = "gradeTop")
    @ApiOperation(value = "整表取优", notes = "at 时刻可用规则中先后序最大者判档")
    @GetMapping("/gradeTop")
    @RequiresPermissions("oads:oadsRuleLine:view")
    @ResponseBody
    public AjaxResult gradeTop(@RequestParam("score") BigDecimal score,
                               @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                               @RequestParam(value = "at", required = false) Date at) {
        if (at == null) {
            at = new Date();
        }
        SafetyGradeResult result = oadsRuleLineService.gradeTop(score, at);
        return AjaxResult.successData(200, result);
    }
}
