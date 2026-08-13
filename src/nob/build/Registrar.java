/* ======================================
 * File: Registrar.java
 * Date: 2026-07-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.Task;
import nob.NobException;

public class Registrar {
    private final String pluginId;
    private final TaskPipeline taskPipeline;
    private final Service service;

    private final Set<String> deps = new HashSet<>();

    public Registrar(String pluginId, TaskPipeline taskPipeline, Service service) {
        this.pluginId = pluginId;
        this.taskPipeline = taskPipeline;
        this.service = service;
    }

    public String registerTask(Class<? extends Task> taskType) {
        Task task = taskType.getDeclaredConstructors().newInstance();
        this.taskPipeline.registerTask(new TaskId(formatNamespaceId(task.id())), task);
        for (BucketId consumedBucket: task.consumes()) {
            deps.add(consumedBucket.pluginId());
        }
        return signature;
    }

    public String formatNamespaceId(String sigId) {
        return this.pluginId + ":" + sigId;
    }

    // POST REFACTOR

    public void addTo(String signature, String prerequisite) {
        this.taskPipeline.addTo(signature, prerequisite);
    }

    public void addAfter(String signature, String prerequisite) {
        this.taskPipeline.addAfter(signature, prerequisite);
    }

    public void createLifecycle(String lifecycle) {
        this.taskPipeline.createLifecycle(lifecycle);
    }

    public void setState(String stateId, Object object) {
        service.setState(formatNamespaceId(stateId), object);
    }

    public <T> T getState(String sig, Class<T> type) {
        return service.getState(sig, type);
    }

    // TODO: Check if config being set for exists in the plugins dependencies or not.
    public void setConfig(String configSignature, Object object) {
        service.setConfig(configSignature, object);
    }

    public <T> T getConfig(String configSignature, Class<T> type) {
        return service.getConfig(configSignature, type);
    }
}

