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
    private final TaskPipeline graph;
    private final Service service;

    private final Set<String> deps = new HashSet<>();

    public Registrar(String pluginId, TaskPipeline graph, Service service) {
        this.pluginId = pluginId;
        this.graph = graph;
        this.service = service;
    }

    public void createLifecycle(String lifecycle) {
        this.graph.createLifecycle(lifecycle);
    }

    public String registerTask(Class<? extends Task> taskType) {
        Task task = taskType.getDeclaredConstructors().newInstance();
        String taskId = formatNamespaceId(task.id());
        this.graph.registerTask(taskId, task);
        for (NamespaceId consumedBucket: task.consumes()) {
            deps.add(consumedBucket.pluginId());
        }
        return signature;
    }

    public void addTo(String signature, String prerequisite) {
        this.graph.addTo(signature, prerequisite);
    }

    public void addAfter(String signature, String prerequisite) {
        this.graph.addAfter(signature, prerequisite);
    }

    public String formatNamespaceId(String sigId) {
        return this.pluginId + ":" + sigId;
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

