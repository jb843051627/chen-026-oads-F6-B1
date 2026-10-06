import com.fc.v2.util.OadsSafetyGradeRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全市统一分档算法纯逻辑用例：精度取齐、左闭右开、压线落高档、量程。
 */
public class OadsSafetyGradeRuleTest {

    private static int level(String score, String t1, String t2, String t3) {
        return OadsSafetyGradeRule.levelOf(
                OadsSafetyGradeRule.align(new BigDecimal(score)),
                OadsSafetyGradeRule.align(new BigDecimal(t1)),
                OadsSafetyGradeRule.align(new BigDecimal(t2)),
                OadsSafetyGradeRule.align(new BigDecimal(t3)));
    }

    @Test
    public void fourNineEighteenExample() {
        // 题面原例：定值 4、9、18
        assertEquals(1, level("3.00", "4", "9", "18"));
        assertEquals(2, level("8.90", "4", "9", "18"));
        assertEquals(3, level("9.10", "4", "9", "18"));
        // 恰好压在上限：不进号更小的档，落号更大的档
        assertEquals(2, level("4.00", "4", "9", "18"), "压起算限值落第2档");
        assertEquals(3, level("9.00", "4", "9", "18"), "压够线限值落第3档");
        assertEquals(4, level("18.00", "4", "9", "18"), "压封顶限值落第4档");
    }

    @Test
    public void lastQuarterIncidentRoundingThenCompare() {
        // 上季度事故：取数进了位、比对没进位——8.996 对够线限值 9.00，
        // 取齐之后 9.00 不再小于 9.00，本应第3档却落成第2档。
        assertEquals(0, OadsSafetyGradeRule.align(new BigDecimal("8.996")).compareTo(
                new BigDecimal("9.00")), "8.996 四舍五入两位即 9.00");
        assertEquals(3, level("8.996", "4", "9", "18"), "进位后再贴区间，落第3档");
        assertEquals(3, level("8.995", "4", "9", "18"), "8.995 四舍五入同为 9.00，落第3档");
        assertEquals(2, level("8.994", "4", "9", "18"), "8.994 取齐为 8.99，仍落第2档");
    }

    @Test
    public void rangeOutOfBandIsNoLevel() {
        assertEquals(0, level("-0.01", "4", "9", "18"));
        assertEquals(0, level("100.01", "4", "9", "18"));
        assertEquals(4, level("100.00", "4", "9", "18"), "100 在量程内，压顶落第4档");
        assertEquals(1, level("0.00", "4", "9", "18"), "0 在量程内");
    }

    @Test
    public void nullsYieldNoLevel() {
        assertEquals(0, OadsSafetyGradeRule.levelOf(null,
                new BigDecimal("4.00"), new BigDecimal("9.00"), new BigDecimal("18.00")));
        assertEquals(0, OadsSafetyGradeRule.levelOf(new BigDecimal("5.00"),
                null, new BigDecimal("9.00"), new BigDecimal("18.00")));
        assertNull(OadsSafetyGradeRule.align(null));
    }
}
