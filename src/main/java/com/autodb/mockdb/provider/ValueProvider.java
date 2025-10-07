package com.autodb.mockdb.provider;

import java.lang.reflect.Field;

public interface ValueProvider {
    /** Provide a sample value for `field` belonging to entity `entityType`. May return null. */
    Object provideValue(Class<?> entityType, Field field, int index);
}
