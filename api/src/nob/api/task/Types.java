/* ======================================
 * File: Types.java
 * Date: 2026-11-07
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.api.task;

import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class Types {
    private Types() {}

    public static Type listOf(Type type) {
        return parameterize(List.class, type);
    }

    public static Type mapOf(Type keyType, Type valueType) {
        return parameterize(Map.class, keyType, valueType);
    }

    public static Type parameterize(Class<?> rawType, Type... args) {
        return new ParameterizedTypeImpl(rawType, args);
    }
}

record ParameterizedTypeImpl(Type rawType, Type[] args) implements ParameterizedType {

    @Override
    public Type[] getActualTypeArguments() {
        return this.args.clone();
    }

    @Override
    public Type getRawType() {
        return this.rawType;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ParameterizedType p
            && this.rawType.equals(p.getRawType())
           && Arrays.equals(this.args, p.getActualTypeArguments());
    }

    @Override 
    public int hashCode() {
        return Arrays.hashCode(args) ^ rawType.hashCode(); 
    }

    @Override 
    public String toString() {
        String argStr = Arrays.stream(args).map(Type::getTypeName)
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
        return ((Class<?>) rawType).getName() + "<" + argStr + ">";
    }}

