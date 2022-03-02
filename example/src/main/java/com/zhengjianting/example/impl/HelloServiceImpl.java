package com.zhengjianting.example.impl;

import com.zhengjianting.service.HelloService;

public class HelloServiceImpl implements HelloService {
    @Override
    public String hello(String name) {
        return "Hello, " + name + "!";
    }
}