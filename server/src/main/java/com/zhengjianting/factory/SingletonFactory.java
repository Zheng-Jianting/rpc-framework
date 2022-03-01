package com.zhengjianting.factory;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SingletonFactory {
    private static final Map<String, Object> MAP = new ConcurrentHashMap<>();

    private SingletonFactory() {

    }

    public static <T> T getInstance(Class<T> tClass) {
        if (tClass == null) {
            throw new IllegalArgumentException();
        }

        if (MAP.containsKey(tClass.toString())) {
            return tClass.cast(MAP.get(tClass.toString()));
        }
        else {
            return tClass.cast(MAP.computeIfAbsent(tClass.toString(), instance -> {
                try {
                    return tClass.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException("error from SingleFactory.");
                }
            }));
        }
    }
}