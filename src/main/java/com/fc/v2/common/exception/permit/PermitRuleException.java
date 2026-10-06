package com.fc.v2.common.exception.permit;

/**
 * 设置许可核准单口径违例：段次数法、跳段、同人重提、封存后补笔等八条口径不让办的情形。
 *
 * <p>消息原样回到页面，作为这一笔没落进库的交代。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class PermitRuleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PermitRuleException(String message) {
        super(message);
    }
}
