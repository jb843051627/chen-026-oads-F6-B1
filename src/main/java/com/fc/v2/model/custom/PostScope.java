package com.fc.v2.model.custom;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 名册可见范围，按辖段切。
 *
 * <p>本段经办岗（SECTION）只见本段挂靠的行；区局岗（BUREAU）三类路段都可见，
 * 但范围仍限于划归它的辖段。范围由岗位辖段授权表点出，不由页面自报。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class PostScope {

    public static final String SECTION = "SECTION";
    public static final String BUREAU = "BUREAU";

    private final String postNo;
    private final String postKind;
    private final Set<String> areaNos = new LinkedHashSet<String>();

    public PostScope(String postNo, String postKind) {
        this.postNo = postNo;
        this.postKind = postKind;
    }

    public String getPostNo() {
        return postNo;
    }

    public String getPostKind() {
        return postKind;
    }

    public Set<String> getAreaNos() {
        return areaNos;
    }

    public boolean isBureau() {
        return BUREAU.equals(postKind);
    }

    /** 一个辖段都没划给它：什么都看不见（合法的空，不拿邻段补齐）。 */
    public boolean isEmpty() {
        return areaNos.isEmpty();
    }
}
