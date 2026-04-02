package com.qq.ijay997.dubbo.filter;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dubbo 自定义过滤器
 * 用于记录请求日志和性能监控
 * 
 * @author ijay997
 */
@Activate(group = {"provider", "consumer"})
public class CustomFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CustomFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 请求开始时间
        long startTime = System.currentTimeMillis();
        
        // 获取调用信息
        String serviceName = invoker.getInterface().getSimpleName();
        String methodName = invocation.getMethodName();
        Object[] arguments = invocation.getArguments();
        
        logger.info("====== Dubbo 调用开始 ======");
        logger.info("服务名：{}", serviceName);
        logger.info("方法名：{}", methodName);
        logger.info("参数：{}", formatArguments(arguments));
        logger.info("调用方：{}", RpcContext.getContext().getRemoteAddressString());
        
        try {
            // 执行调用
            Result result = invoker.invoke(invocation);
            
            // 计算耗时
            long costTime = System.currentTimeMillis() - startTime;
            
            logger.info("响应结果：{}", result.getValue());
            logger.info("调用耗时：{} ms", costTime);
            logger.info("====== Dubbo 调用结束 ======\n");
            
            return result;
            
        } catch (RpcException e) {
            logger.error("Dubbo 调用异常：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 格式化参数
     */
    private String formatArguments(Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(arguments[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
