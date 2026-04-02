package com.qq.ijay997.service.impl;

import com.qq.ijay997.service.OrderService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

//@Scope("prototype")
@Service
public class OrderServiceImpl implements OrderService {
    @Override
    public String getOrder() {
        return "";
    }
}
