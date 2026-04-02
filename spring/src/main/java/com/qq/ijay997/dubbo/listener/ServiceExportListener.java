package com.qq.ijay997.dubbo.listener;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.ExporterListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dubbo 服务暴露监听器
 * 
 * @author ijay997
 */
@Activate
public class ServiceExportListener implements ExporterListener {

    private static final Logger logger = LoggerFactory.getLogger(ServiceExportListener.class);

    @Override
    public void exported(Exporter<?> exporter) {
        String serviceName = exporter.getInvoker().getInterface().getSimpleName();
        logger.info("=================================");
        logger.info("✅ 服务已暴露：{}", serviceName);
        logger.info("服务地址：{}", exporter.getInvoker().getUrl());
        logger.info("=================================");
    }

    @Override
    public void unexported(Exporter<?> exporter) {
        String serviceName = exporter.getInvoker().getInterface().getSimpleName();
        logger.info("=================================");
        logger.info("❌ 服务已撤销：{}", serviceName);
        logger.info("=================================");
    }
}
