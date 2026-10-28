package com.qq.ijay997.support;

/**
 * 演示数据工具。
 * <p>
 * 手机号未落库，按 userId 稳定生成，保证 JSON 查询与文件导出使用同一个值。
 *
 * @author ijay997
 */
public final class DemoMockData {

    private DemoMockData() {
    }

    /**
     * 生成演示用手机号：{@code 138} + 8 位 userId，固定 11 位以满足 PHONE 脱敏规则。
     *
     * @param userId 用户主键，可为 null
     * @return 11 位手机号字符串
     */
    public static String phoneOf(Long userId) {
        long seed = userId == null ? 0L : Math.abs(userId) % 100_000_000L;
        return "138" + String.format("%08d", seed);
    }
}
