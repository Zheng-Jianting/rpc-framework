package com.zhengjianting.example.impl;

import com.zhengjianting.service.GoodByeService;

public class GoodByeServiceImpl implements GoodByeService {
    @Override
    public String goodBye(String name) {
        return "GoodBye, " + name + "!";
    }
}