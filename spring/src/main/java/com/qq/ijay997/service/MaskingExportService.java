package com.qq.ijay997.service;

import java.io.OutputStream;

/**
 * 脱敏演示导出服务。
 *
 * @author ijay997
 */
public interface MaskingExportService {

    /**
     * 导出租户内全部用户（按当前请求上下文自动脱敏）。
     *
     * @param outputStream 响应输出流
     */
    void exportUsers(OutputStream outputStream);
}
