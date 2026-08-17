/* ======================================
 * File: Bucket.java
 * Date: 2026-08-07
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Type;
import nob.NobException;

public class Bucket<T> {
    private final Type type;
    private final List<T> values = new ArrayList<>();

    public Bucket(Type type) {
        this.type = type;
    }
    
    public void set(T value) {
        values.clear();
        values.add(value);
    }

    public void add(T value) {
        values.add(value);
    }

    public T get() {
        if (values.size() != 1) {
            throw new NobException("Bucket expected exactly one value, has " + values.size());
        }
        return values.get(0);
    }

    public List<T> getAll() {
        return List.copyOf(values);
    }

    public Type getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return "Bucket[" + "type=" + type.getTypeName() + ", values=" + values + "]";
    }
}

