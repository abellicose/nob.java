/* ======================================
 * File: DefaultRegistry.java
 * Date: 2026-11-07
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.core;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import java.lang.reflect.Type;
import java.lang.NullPointerException;
import java.lang.IllegalArgumentException;

import nob.api.plugin.ConfigEdit;
import nob.api.plugin.Registry;
import nob.api.error.NobException;
import nob.api.task.Task;
import nob.util.Signatures;

class DefaultRegistry implements Registry {
    private final Set<Class<? extends Task>> tasks = new HashSet<>();
    private final Set<BucketMeta> buckets = new HashSet<>();
    private final Set<ConfigMeta> config = new HashSet<>();
    private final String pluginId;
    private boolean closed = false;

    protected DefaultRegistry(String pluginId) {
        this.pluginId = pluginId;
    }

    public ConfigEdit edit(String configId) {
        return null;
    }

    public void task(Class<? extends Task> type) {
        checkOpen("task");
        Objects.requireNonNull(type, "Cannot register task. type must not be null");
        putOnce("task", type.getName(), tasks, type);
    }

    public void config(String name, Type type, Object defaultValue) {
        checkOpen("config");
        checkLocalName("config", name);
        Objects.requireNonNull(type, "Config '" + name + "': type must not be null.");
        putOnce("config", name, config, new ConfigMeta(name, type, defaultValue));
    }

    public void bucket(String name, Type type) {
        checkOpen("bucket");
        checkLocalName("bucket", name);
        Objects.requireNonNull(type, "Bucket '" + name + "': type must not be null.");
        putOnce("bucket", name, buckets, new BucketMeta(name, type));
    }

    public Set<Class<? extends Task>> getTasks() {
        return this.tasks;
    }

    public Set<ConfigMeta> getConfig() {
        return this.config;
    }

    public Set<BucketMeta> getBuckets() {
        return this.buckets;
    }

    public void close() {
        this.closed = true;        
    }

    private void checkOpen(String kind) {
        if (this.closed) {
            throw new NobException(
                "Cannot register " + kind + " in plugin '" + pluginId + "': the registry is closed. "
                + "Declare tasks, configs, buckets and edits inside apply(), "
                + "not in a task or after apply() has returned.");
        }
    }

    private static void checkLocalName(String kind, String name) {
        if (name == null || name.isEmpty())
        throw new IllegalArgumentException(kind + " name must not be null or empty.");
        if (Signatures.isForeign(name))
        throw new NobException(kind + " '" + name + "' must not be plugin-prefixed (declare it with the short name only).");
    }

    private static <M> void putOnce(String kind, String name, Set<M> into, M meta) {
        if (!into.add(meta))
            throw new NobException(kind + " '" + name + "' is already declared in this plugin.");
    }
    
}
