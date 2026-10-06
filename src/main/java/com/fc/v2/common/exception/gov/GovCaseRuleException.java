package com.fc.v2.common.exception.gov;

/**
 * 监管办件四步流程口径违例：立件四项输入、步次推进与准入、后退、封存、在走独占、
 * 撤销/撤回/作废/划归各路径上不让办的情形。
 *
 * <p>每处交代都点到具体条目与步次，消息原样回到页面，不许合并成一句"信息不全"。
 *
 * @author fuce
 * @date 2026-10-06
 */
public class GovCaseRuleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public GovCaseRuleException(String message) {
        super(message);
    }
}
