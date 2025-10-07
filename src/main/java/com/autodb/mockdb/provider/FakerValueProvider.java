package com.autodb.mockdb.provider;

import net.datafaker.Faker;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Random;

public class FakerValueProvider implements ValueProvider {

    private final Faker faker = new Faker();
    private final Random rnd = new Random();

    public FakerValueProvider() {}

    @Override
    public Object provideValue(Class<?> entityType, Field field, int index) {
        Class<?> t = field.getType();
        String name = field.getName().toLowerCase();

        if (String.class.equals(t)) {
            if (name.contains("email")) return faker.internet().emailAddress();
            if (name.contains("first") && name.contains("name")) return faker.name().firstName();
            if (name.contains("last") && name.contains("name")) return faker.name().lastName();
            if (name.contains("name")) return faker.name().fullName();
            if (name.contains("phone")) return faker.phoneNumber().phoneNumber();
            if (name.contains("address")) return faker.address().fullAddress();
            if (name.contains("company")) return faker.company().name();
            if (name.contains("title")) return faker.book().title();
            if (name.contains("description") || name.contains("desc")) return faker.lorem().sentence();
            if (name.contains("category")) return faker.commerce().department();
            if (name.contains("product")) return faker.commerce().productName();
            return faker.lorem().word() + "-" + Math.abs(rnd.nextInt(10000));
        }

        if (Integer.class.equals(t) || int.class.equals(t)) return rnd.nextInt(1000);
        if (Long.class.equals(t) || long.class.equals(t)) return Math.abs(rnd.nextLong() % 100000);
        if (Double.class.equals(t) || double.class.equals(t)) return Math.round(faker.number().randomDouble(2, 1, 10000) * 100.0) / 100.0;
        if (BigDecimal.class.equals(t)) return BigDecimal.valueOf(Math.round(faker.number().randomDouble(2, 1, 10000) * 100.0) / 100.0);
        if (Boolean.class.equals(t) || boolean.class.equals(t)) return rnd.nextBoolean();
        if (LocalDate.class.equals(t)) {
            Date d = faker.date().birthday();
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (Date.class.equals(t)) {
            return faker.date().past(3650, java.util.concurrent.TimeUnit.DAYS);
        }
        if (t.isEnum()) {
            Object[] consts = t.getEnumConstants();
            if (consts != null && consts.length > 0) return consts[rnd.nextInt(consts.length)];
        }
        return null;
    }
}
