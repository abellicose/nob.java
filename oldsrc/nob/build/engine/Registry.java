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
    public final Set<Class<? extends Task>> tasks = new HashSet<>();
    public final Map<String, Type> buckets = new HashMap<>();
    public final Map<String, ConfigInfo> configs = new HashMap<>();

    // declareConfig
    public Registry declareConfig(String configName, Type type, Object defaultValue) {
        if (configName == null) throw new NobException("Config name cannot be null");
        if (type == null) throw new NobException("Config type cannot be null for '" + configName + "'");
        if (Signatures.isForeign(configName)) {
            throw new NobException("Cannot declare '" + configName + "': config names must not be plugin-prefixed (declare using the short name only)");
        }
        if (configs.containsKey(configName)) {
            throw new NobException("Config '" + configName + "' is already declared for this plugin");
        }
        configs.put(configName, new ConfigInfo(type, defaultValue));
        return this;
    }

    public void registerTask(Class<? extends Task> taskType) {
        if (taskType == null) {
            throw new NobException(
                "Cannot register task: taskType must not be null.");
        }
        if (tasks.contains(taskType)) {
            throw new NobException(
                "Cannot register task '" + taskType.getName() + "': this task type is already registered.");
        }
        tasks.add(taskType);
    }

    public void createBucket(String bucketName, Type bucketType) {
        if (bucketName == null || bucketName.isEmpty()) {
            throw new NobException(
                "Cannot register bucket: bucketName must not be null or empty.");
        }

        if (bucketType == null) {
            throw new NobException(
                "Cannot register bucket '" + bucketName + "': bucketType must not be null.");
        }

        if (Signatures.isForeign(bucketName)) {
            throw new NobException("Bucket " + bucketName + " to register cannot be prefixed with an id.");
        }

        if (buckets.containsKey(bucketName)) {
            throw new NobException(
                "Cannot register bucket '" + bucketName + "': a bucket with this name is already registered " +
                "(existing type: " + buckets.get(bucketName) + ", requested type: " + bucketType + ").");
        }

        buckets.put(bucketName, bucketType);
    }
}

