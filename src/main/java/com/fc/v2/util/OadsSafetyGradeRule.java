package com.fc.v2.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 牌面安全分档规则——全市统一实现（chen-026）。
 *
 * <p>区级只做配置（维护 t_oads_rule_line 各格），不得改写本类逻辑。四个问题只认这一处给的答案：
 * 本轮取哪一条规则、没有可用规则时给不给说法、多条并列时采信谁、折算分压在上限时落哪一档。
 *
 * <p>口径：
 * <ul>
 *   <li>三个定值与折算分一律先按两位小数、四舍五入取齐，取齐之后才贴区间；
 *       上一季度事故：取数进了位、比对没进位（8.996 对够线限值 9.00）。</li>
 *   <li>第1档：分 &lt; 起算限值；第2档：起算限值 ≤ 分 &lt; 够线限值；
 *       第3档：够线限值 ≤ 分 &lt; 封顶限值；第4档：分 ≥ 封顶限值。
 *       恰好压在上限上，不进号更小的档，落号更大的档（边界一律左闭右开）。</li>
 *   <li>折算分量程 0..100，取齐后出格按不分档（0）处理。</li>
 * </ul>
 */
public final class OadsSafetyGradeRule {

    /** 不分档 */
    public static final int LEVEL_NONE = 0;
    public static final int LEVEL_MIN = 1;
    public static final int LEVEL_MAX = 4;

    private static final int SCALE = 2;
    private static final BigDecimal FLOOR = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
    private static final BigDecimal CEIL = new BigDecimal("100.00");

    private OadsSafetyGradeRule() {
    }

    /** 定值/折算分取齐：两位小数、四舍五入。比对之前先对齐精度，取齐之后才贴区间。 */
    public static BigDecimal align(BigDecimal v) {
        if (v == null) {
            return null;
        }
        return v.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** 折算分取齐后是否在 0..100 量程内；出格按不分档处理。 */
    public static boolean inRange(BigDecimal alignedInput) {
        return alignedInput != null
                && alignedInput.compareTo(FLOOR) >= 0
                && alignedInput.compareTo(CEIL) <= 0;
    }

    /**
     * 贴区间落档。入参必须是已经 {@link #align} 取齐过的四个数，脏配置由调用方先剔除。
     * 左闭右开：等于某个上限不进号更小的档，落号更大的档。
     *
     * @return 1..4；入参缺失或折算分越界返回 0
     */
    public static int levelOf(BigDecimal alignedInput, BigDecimal th1, BigDecimal th2, BigDecimal th3) {
        if (alignedInput == null || th1 == null || th2 == null || th3 == null) {
            return LEVEL_NONE;
        }
        if (!inRange(alignedInput)) {
            return LEVEL_NONE;
        }
        if (alignedInput.compareTo(th1) < 0) {
            return 1;
        }
        if (alignedInput.compareTo(th2) < 0) {
            return 2;
        }
        if (alignedInput.compareTo(th3) < 0) {
            return 3;
        }
        return 4;
    }
}
