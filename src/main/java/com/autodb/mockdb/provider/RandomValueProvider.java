package com.autodb.mockdb.provider;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.Random;

public class RandomValueProvider implements ValueProvider {
    private final Random rnd = new Random();

    public RandomValueProvider() {}

    @Override
    public Object provideValue(Class<?> entityType, Field field, int index) {
        Class<?> t = field.getType();
        String name = field.getName().toLowerCase();

        if (String.class.equals(t)) {
            if (name.contains("email")) return "user" + rnd.nextInt(1_000_000) + "@example.com";
            if (name.contains("name")) return field.getName() + "-" + Math.abs(rnd.nextInt(1_000_000));
            return "str" + rnd.nextInt(10000);
        }
        if (Integer.class.equals(t) || int.class.equals(t)) return rnd.nextInt(1000);
        if (Long.class.equals(t) || long.class.equals(t)) return Math.abs(rnd.nextLong() % 100000);
        if (Double.class.equals(t) || double.class.equals(t)) return Math.round(rnd.nextDouble() * 10000.0) / 100.0;
        if (BigDecimal.class.equals(t)) return BigDecimal.valueOf(Math.round(rnd.nextDouble() * 10000.0) / 100.0);
        if (Boolean.class.equals(t) || boolean.class.equals(t)) return rnd.nextBoolean();
        if (LocalDate.class.equals(t)) return LocalDate.now().minusDays(rnd.nextInt(3650));
        if (Date.class.equals(t)) return new Date(System.currentTimeMillis() - (long) rnd.nextInt(365) * 24L * 3600L * 1000L);

        // enums
        if (t.isEnum()) {
            Object[] consts = t.getEnumConstants();
            if (consts != null && consts.length > 0) return consts[rnd.nextInt(consts.length)];
        }

        return null;
    }
}
