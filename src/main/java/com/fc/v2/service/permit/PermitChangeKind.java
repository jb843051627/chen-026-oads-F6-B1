package com.fc.v2.service.permit;

import com.fc.v2.common.exception.permit.PermitRuleException;

/**
 * 临江路牌子的三种变更情形：挂上去、改位置、整块拆除。
 *
 * <p>每种情形从点位主档的哪一档起步、封存后改到哪一档、档位进不进位，都在这一处点死；
 * 建单时照此把 from/to 两档写到单面上，封存时照同一张单子改回主档。
 *
 * @author fuce
 * @date 2026-10-05
 */
public enum PermitChangeKind {

    /** 挂上去：未挂设(0) → 挂设在册(1)，档位进一 */
    HANG("HANG", 0, 1, true),
    /** 改位置：挂设在册(1) → 挂设在册(1)，档位进一 */
    MOVE("MOVE", 1, 1, true),
    /** 整块拆除：挂设在册(1) → 整块拆除(2)，档位不进位 */
    REMOVE("REMOVE", 1, 2, false);

    /** 未挂设 */
    public static final int STATE_UNSET = 0;
    /** 挂设在册 */
    public static final int STATE_HUNG = 1;
    /** 整块拆除 */
    public static final int STATE_REMOVED = 2;

    private final String code;
    private final int fromState;
    private final int toState;
    private final boolean bumpVer;

    PermitChangeKind(String code, int fromState, int toState, boolean bumpVer) {
        this.code = code;
        this.fromState = fromState;
        this.toState = toState;
        this.bumpVer = bumpVer;
    }

    /** 解析报文情形码；不在三种之内按错误处理，不替它猜 */
    public static PermitChangeKind of(String code) {
        if (code != null) {
            for (PermitChangeKind kind : values()) {
                if (kind.code.equals(code.trim())) {
                    return kind;
                }
            }
        }
        throw new PermitRuleException("变更情形只认 HANG挂上去/MOVE改位置/REMOVE整块拆除，报来的是：" + code);
    }

    /** 校验点位主档当下这一档与情形对得上，对不上不许建单 */
    public void requireCurrentState(int currentState) {
        if (currentState != fromState) {
            throw new PermitRuleException("点位主档当前为第" + currentState + "档，" + code
                    + " 只能从第" + fromState + "档起单，不许借档");
        }
    }

    public String getCode() {
        return code;
    }

    public int getFromState() {
        return fromState;
    }

    public int getToState() {
        return toState;
    }

    public boolean isBumpVer() {
        return bumpVer;
    }
}
