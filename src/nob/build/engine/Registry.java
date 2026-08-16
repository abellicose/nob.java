/* ======================================
 * File: Registry.java
 * Date: 2026-08-15
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.lang.reflect.Type;

public class Registry {
    private final Set<Class<? extends Task>> tasks = new HashSet<>();
    private final Map<String, Type> buckets = new HashMap<>();

    public void registerTask(Class<? extends Task> taskType) {
        tasks.add(taskType);
    }

    public void registerBucket(String bucketName, Type bucketType) {
        if (bucketName == null || bucketName.isEmpty()) {
            throw new NobException("BucketName is empty, cannot register it.");
        }
        if (bucketName == null) {
            throw new NobException("BucketType cant be null");
        }

        buckets.put(bucketName, bucketType);
    }

    // config later

    public Set<Class<? extends Task>> getTasks() {
        return this.tasks;
    }

    public Map<String, Type> getBuckets() {
        return this.buckets;
    }
}
