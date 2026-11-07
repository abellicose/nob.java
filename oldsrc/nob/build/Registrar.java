/* ======================================
 * File: Registrar.java
 * Date: 2026-07-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.Task;
import nob.NobException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Type;

public class Registrar {
    private final PluginId pluginId;
    private final TaskPipeline pipeline;
    private final Set<PluginId> dependencies = new HashSet<>();
    private final Set<PluginId> deps = new HashSet<>();

    public Registrar(PluginId pluginId, TaskPipeline pipeline) {
        this.pluginId = pluginId;
        this.pipeline = pipeline;
    }

    public void registerTask(Class<? extends Task> taskType) {
/*
        Task task;
        try {
            task = taskType.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new NobException("Could not create task instance from task class type.", e);
        }
        TaskId taskId = new TaskId(rawNamespaceIdFrom(task.id()));

        for (Map.Entry<String, Type> entry: task.produces().entrySet()) {
            pipeline.createBucket(new BucketId(rawNamespaceIdFrom(entry.getKey())), entry.getValue());
        }

        pipeline.registerTask(taskId, task);

        for (BucketId populatedBucket: task.populates()) {
            dependencies.add(populatedBucket.pluginId);
        }

        for (BucketId consumedBucket: task.consumes()) {
            dependencies.add(consumedBucket.pluginId);
        }

        for (ConfigId consumedConfig: task.consumesConfig()) {
            dependencies.add(consumedConfig.pluginId);
        }
*/
    }

    public void createBucket(String bucketName, Type type) {
/*
        pipeline.createBucket(new BucketId(rawNamespaceIdFrom(bucketName)), type);
*/
    }

    // create config. config deferred for now

    private String rawNamespaceIdFrom(String name) {
        return this.pluginId.rawId + ":" + name;
    }

    protected Set<PluginId> getDependencies() {
        return this.deps;
    }
}

