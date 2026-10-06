package com.fc.v2.common.exception.spot;

/**
 * 点位名册五检查点口径违例：身份、空间挂靠、数量、名录依据、同步与展示各处不让办的情形。
 *
 * <p>五个检查点各有各的提示语，消息原样回到页面，作为这一行没落进库或这一步没走成的交代，
 * 不许合并成一句"数据有误"。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class SpotRuleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SpotRuleException(String message) {
        super(message);
    }
}
