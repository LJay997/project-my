package com.qq.ijay997.mp.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.qq.ijay997.mp.interceptor.LevelInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置：
 * - {@link OptimisticLockerInnerInterceptor} 乐观锁
 * - {@link PaginationInnerInterceptor} 分页（H2 方言）
 * - {@link MapperScan} Mapper 扫描
 *
 * @author ijay997
 */
@Configuration
@MapperScan("com.qq.ijay997.mp.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        interceptor.addInnerInterceptor(new LevelInterceptor());
        // 乐观锁拦截器需在分页之前注册
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 分页拦截器
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.H2);
        pagination.setMaxLimit(500L);
        interceptor.addInnerInterceptor(pagination);

        return interceptor;
    }
}