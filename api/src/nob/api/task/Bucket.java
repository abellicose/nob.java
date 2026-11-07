/* ======================================
 * File: Bucket.java
 * Date: 2026-11-06
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.api.task;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.lang.reflect.Type;

public class Bucket<T> {
    private String taskId;
    private final Type type;
    private final Map<String, List<T>> values;

    public Bucket(Type type, String taskId, Map<String, List<T>> values) {
        this.type = type;
        this.taskId = taskId;
        this.values = values;
    }
    
    public void set(T value) {
        values.clear();
        values.put(taskId, new ArrayList<T>(List.of(value)));
    }

    public void add(T value) {
        values.computeIfAbsent(taskId, k -> new ArrayList<>()).add(value);
    }

    // TODO: NEED EXCEPTIONS IN API
/*
    public T get() {
        if (values.size() != 1) {
            throw new NobException("Bucket expected exactly one value, has " + values.size());
        }
        return values.values().get(0);
    }

    public List<T> getAll() {
        return Stream.of(values.values()).flatMap(Collection::stream).collect(Collectors.toList);
    }
*/

    public Type getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return "Bucket[" + "type=" + type.getTypeName() + ", values=" + values.values() + "]";
    }
}

