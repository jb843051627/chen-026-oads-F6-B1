package com.fc.v2.service.spot;

import java.math.BigDecimal;

/**
 * 辖段围栏包含判定（唯一实现）。
 *
 * <p>围栏文本记矩形两对角点 {@code lng1,lat1;lng2,lat2}，不记次序，判定取闭区间。
 * 坐标与辖段挂靠都来自现场采集，这里只判"在不在围栏内"，不做任何改写。
 *
 * @author fuce
 * @date 2026-10-05
 */
public final class AreaFence {

    private AreaFence() {
    }

    /** 围栏是否配了可用文本。 */
    public static boolean isUsable(String fenceText) {
        if (fenceText == null) {
            return false;
        }
        String[] corners = fenceText.trim().split(";");
        if (corners.length != 2) {
            return false;
        }
        for (String c : corners) {
            String[] parts = c.trim().split(",");
            if (parts.length != 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                return false;
            }
            try {
                new BigDecimal(parts[0].trim());
                new BigDecimal(parts[1].trim());
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * 坐标是否落在围栏闭区间内。围栏缺失或畸形、坐标缺失，一律按"在围栏之外"不收，
     * 由调用处给"该点位在围栏之外"的提示。
     */
    public static boolean contains(String fenceText, BigDecimal lng, BigDecimal lat) {
        if (!isUsable(fenceText) || lng == null || lat == null) {
            return false;
        }
        String[] corners = fenceText.trim().split(";");
        BigDecimal[] a = point(corners[0]);
        BigDecimal[] b = point(corners[1]);
        BigDecimal minLng = a[0].min(b[0]);
        BigDecimal maxLng = a[0].max(b[0]);
        BigDecimal minLat = a[1].min(b[1]);
        BigDecimal maxLat = a[1].max(b[1]);
        return lng.compareTo(minLng) >= 0 && lng.compareTo(maxLng) <= 0
                && lat.compareTo(minLat) >= 0 && lat.compareTo(maxLat) <= 0;
    }

    private static BigDecimal[] point(String corner) {
        String[] parts = corner.trim().split(",");
        return new BigDecimal[]{new BigDecimal(parts[0].trim()), new BigDecimal(parts[1].trim())};
    }
}
