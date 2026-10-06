package com.fc.v2.common.exception.plaque;

/**
 * 牌匾更换名册收讫口径违例：册不成册、批次码缺填等四步连着做不下去的情形。
 *
 * <p>行上的毛病不抛本异常——哪一行因为哪一条退回、哪一行缺什么挂待核，都逐行落账、
 * 逐行在回执点名；本异常只管"整册交不进来"这一层。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class PlaqueRuleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PlaqueRuleException(String message) {
        super(message);
    }
}
