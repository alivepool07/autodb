package com.autodb.mockdb.util;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReflectionUtils {

    public static List<Field> getAllFields(Class<?> cls) {
        List<Field> res = new ArrayList<>();
        for (Class<?> t = cls; t != null && t != Object.class; t = t.getSuperclass()) {
            Collections.addAll(res, t.getDeclaredFields());
        }
        return res;
    }

    public static Class<?> extractGenericListType(Field field) {
        try {
            Type t = field.getGenericType();
            if (t instanceof ParameterizedType pt) {
                Type[] args = pt.getActualTypeArguments();
                if (args.length > 0 && args[0] instanceof Class<?> c) return c;
            }
        } catch (Throwable ignored) {}
        return null;
    }
}

