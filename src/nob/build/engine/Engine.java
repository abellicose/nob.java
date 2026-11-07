/* ======================================
 * File: Engine.java
 * Date: 2026-08-15
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.NobException;
import nob.Plugin;
import nob.Task;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Deque;
import java.util.ArrayDeque;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.stream.Collectors;

public class Engine {
    // phase 1
    private final Map<PluginId, Set<Class<? extends Task>>> tasksDiscovered = new HashMap<>();
    private final Map<PluginId, Map<String, ConfigInfo>> configsDiscovered = new HashMap<>();
    private final Map<PluginId, Map<String, Type>> bucketsDiscoveredFromPlugins = new HashMap<>();
    private final Map<TaskId, Map<String, Type>> bucketsDiscoveredFromTasks = new HashMap<>();

    // phase 2 - commit
    // 2.1 - initialize tasks, register them.
    // 2.2 - read buckets, register buckets
    // 2.3 - build deps edge (producerOf, consumerOf)
    // 2.4 - count degree & verify producers
    // 2.5 - check cycle
    public final Map<PluginId, PluginMeta> pluginRegistry = new HashMap<>();
    public final Map<TaskId, TaskInfo> taskRegistry = new HashMap<>();
    public final Map<BucketId, BucketInfo> bucketRegistry = new HashMap<>(); 
    public final Map<ConfigId, ConfigInfo> configRegistry = new HashMap<>();
    
    public void registerPlugin(Plugin plugin) {
        PluginId pluginId = new PluginId(plugin.id());

        if (pluginRegistry.containsKey(pluginId)) {
            throw new NobException("Plugin " + pluginId + " has already been registered");
        }

        PluginMeta pluginMeta = PluginMeta.from(plugin);
        pluginRegistry.put(pluginId, pluginMeta);

        Registry reg = new Registry();
        plugin.apply(reg);

        tasksDiscovered.put(pluginId, reg.tasks);
        bucketsDiscoveredFromPlugins.put(pluginId, reg.buckets);
        configsDiscovered.put(pluginId, reg.configs);
    }

    // 2.1 - Instantiated Tasks, Registered it
    // 2.2 - Find config and buckets declared, create them
    // 2.2.1 - Find buckets declared by plugins and create
    // 2.2.2 - Find buckets declared by tasks and create them
    // 2.2.3 - Find default config declared by plugins and create them
    // 2.3 - Create edge list
    //      In Task Writes
    //      There are buckets it creates
    //      There are buckets it populates
    //      // we gotta recheck if its foreign or not is it worth it. -- KEY DECISION MAKING FACTOR - lets do it for now
    //      We need to get bucketId of both,
    //      add that task in bucketinfo.writers()
    //      add that bucket in taskInfo.writes()
    //
    //      In reads
    //      there will be buckets it reads
    //      we need to add that bucket in taskinfo.reads()
    //      we need to add that task   in bucketinfo.readers()
    public void commit() {
        // 2.1 - initialization and registry
        for (var entry: tasksDiscovered.entrySet()) {
            PluginId pluginId = entry.getKey();

            for (Class<? extends Task> taskType: entry.getValue()) {
                Task task = instantiate(taskType);
                TaskId taskId = new TaskId(pluginId, task.id());
                TaskInfo taskInfo = TaskInfo.from(task);
                taskRegistry.put(taskId, taskInfo);
                bucketsDiscoveredFromTasks.put(taskId, task.writes());
            }
        }

        // 2.2 - find produced buckets, create and register em
        // 2.2.1 - Process Buckets produced by plugins first
        for (var entry: bucketsDiscoveredFromPlugins.entrySet()) {
            PluginId pluginId = entry.getKey();

            for (var bucketEntry: entry.getValue().entrySet()) {
                String bucketName = bucketEntry.getKey();
                Type bucketType = bucketEntry.getValue();
                BucketId bucketId = new BucketId(pluginId, bucketName);

                if (bucketRegistry.containsKey(bucketId)) {
                    throw new NobException("Bucket " + bucketId + " was already registered by another plugin. Shouldn't be possible.");
                }

                Bucket bucket = new Bucket<>(bucketType);
                bucketRegistry.put(bucketId, BucketInfo.from(bucket));
            }
        }

        // 2.2.2 - Process buckets produced by tasks
        for (var entry: bucketsDiscoveredFromTasks.entrySet()) {
            TaskId taskId = entry.getKey();
            TaskInfo taskInfo = taskRegistry.get(taskId);
            PluginId pluginId = taskId.pluginId;

            for (var bucketEntry: entry.getValue().entrySet()) {
                String bucketName = bucketEntry.getKey();
                if (Signatures.isForeign(bucketName)) {
                    if (bucketName.startsWith(pluginId.rawId + ":")) {
                        throw new NobException("Task in plugin '" + pluginId.rawId + "' foreign-referenced its own bucket '" + bucketName + "' — use the unprefixed local name instead.");
                    }
                    continue;
                }

                Type bucketType = bucketEntry.getValue();
                BucketId bucketId = new BucketId(pluginId, bucketName);
 
                if (bucketRegistry.containsKey(bucketId)) {
                    throw new NobException("Bucket " + bucketId + " already exists");
                }

                Bucket bucket = new Bucket<>(bucketType);
                bucketRegistry.put(bucketId, BucketInfo.from(bucket));
            }
        }

        // 2.2.3 - Produce default configs 
        for (var pluginEntry: configsDiscovered.entrySet()) {
            PluginId pluginId = pluginEntry.getKey();

            for (var entry: pluginEntry.getValue().entrySet()) {
                String configName = entry.getKey(); // should be valid, checked at creation
                ConfigInfo configInfo = entry.getValue();
                ConfigId configId = new ConfigId(pluginId, configName);
                configRegistry.put(configId, configInfo);
            }
        }

        // 2.3 - build edge list
        for (Map.Entry<TaskId, TaskInfo> taskEntry: taskRegistry.entrySet()) {
            TaskInfo taskInfo = taskEntry.getValue();
            TaskId taskId = taskEntry.getKey();
            PluginId pluginId = taskId.pluginId;
            PluginMeta pluginMeta = pluginRegistry.get(pluginId);
            Task task = taskInfo.task();

            // 2.3.1 - Build bucket <--> writer edgelist
            for (Map.Entry<String, Type> bucketEntry: task.writes().entrySet()) {
                String bucketName = bucketEntry.getKey();
                BucketId bucketId = Signatures.isForeign(bucketName) ? new BucketId(bucketName, null, null) : new BucketId(pluginId, bucketName);

                BucketInfo bucketInfo = bucketRegistry.get(bucketId);
                if (bucketInfo == null) {
                    throw new NobException("Cannot write to unknown bucket: " + bucketId);
                }
                bucketInfo.writers().add(taskId);
                taskInfo.writes().add(bucketId);
            }

            // 2.3.2 - Build bucket <--> reader edgelist
            for (String rawBucketId: task.reads()) {
                BucketId bucketId = new BucketId(rawBucketId);
                BucketInfo bucketInfo = bucketRegistry.get(bucketId);
                if (bucketInfo == null) {
                    // should add if the bucket was in populates or consumes
                    throw new NobException("Bucket " + bucketId + " wasn't produced by any plugin or task. Sussy!");
                }
                bucketInfo.readers().add(taskId);
                taskInfo.reads().add(bucketId);
            }
        }

        // 2.4 - verify all producers and consumers - do i need to
        // we're already creating all prdouced buckets. we needa ensure that all consumed buckets exist
        // we're already doing that. we're getting all consumed buckets, and we're adding the consumer tasks task id to that bucket. if that bucket doesnt exist, it will throw
        // 2.5 - check cycle
        Set<TaskId> visiting = new HashSet<>();
        Set<TaskId> visited = new HashSet<>();
        Deque<TaskId> stack = new ArrayDeque<>();
        Deque<TaskId> callStack = new ArrayDeque<>();

        for (TaskId currentTaskId: taskRegistry.keySet()) {
            if (!visited.contains(currentTaskId)) {
                stack.push(currentTaskId);
            }

            while (!stack.isEmpty()) {
                TaskId taskId = stack.peek();

                if (taskId.equals(callStack.peek())) {
                    stack.poll(); callStack.poll();
                    visiting.remove(taskId); visited.add(taskId);
                    continue;
                }
                if (visited.contains(taskId)) {
                    stack.poll(); continue;
                }

                callStack.push(taskId);
                visiting.add(taskId);

                for (BucketId bucket: taskRegistry.get(taskId).writes()) {
                    for (TaskId reader: bucketRegistry.get(bucket).readers()) {
                        if (visiting.contains(reader)) throw new NobException("Circular dependency detected: " + describeCycle(callStack, reader));
                        if (visited.contains(reader)) continue;
                        stack.push(reader);
                    }
                }
            }
        }
    }

    public void run(String rawTaskId) {
        if (rawTaskId == null || rawTaskId.isEmpty() || !Signatures.isForeign(rawTaskId)) {
            throw new NobException(
                "Cannot run task: provided taskID must not be null/empty/a proper foreign id");
        }

        TaskId currentTaskId = new TaskId(rawTaskId);
        TaskInfo curentTaskInfo = taskRegistry.get(currentTaskId);

        if (curentTaskInfo == null) {
            throw new NobException(
                "Cannot run task '" + rawTaskId + "': task wasn't registered.");
        }

        Set<TaskId> zeroDegreeTask = new HashSet<>();
        Map<TaskId, Integer> degree = new HashMap<>();

        Set<TaskId> visiting = new HashSet<>();
        Set<TaskId> visited = new HashSet<>();
        Deque<TaskId> stack = new ArrayDeque<>();
        stack.push(currentTaskId);

        while (!stack.isEmpty()) {
            TaskId taskId = stack.poll();
            TaskInfo taskInfo = taskRegistry.get(taskId);

            int taskDegree = 0;

            for (BucketId read: taskInfo.reads()) {
                for (TaskId writer: bucketRegistry.get(read).writers()) {
                    taskDegree++;
                    if (visited.contains(writer)) continue;
                    visited.add(writer);
                    stack.push(writer);
                }
            }

            degree.put(taskId, taskDegree);
            if (taskDegree == 0) {
                zeroDegreeTask.add(taskId);
            }
        }

        if (zeroDegreeTask.isEmpty()) {
            throw new NobException("No tasks with degree=0 found. Somethings wrong.");
        }
        // stack should be cleared by now
        stack.addAll(zeroDegreeTask);

        while (!stack.isEmpty()) {
            TaskId taskId = stack.pop();
            TaskInfo taskInfo = taskRegistry.get(taskId);
            execute(taskId, taskInfo);

            for (BucketId writtenBucket: taskInfo.writes()) {
                for (TaskId reader: bucketRegistry.get(writtenBucket).readers()) {
                    int tempDegree = degree.get(reader) - 1;
                    degree.put(reader, tempDegree);
                    if (tempDegree == 0) {
                        stack.push(reader);
                    }
                }
            }
        }
    }

    private void execute(TaskId taskId, TaskInfo taskInfo) {
        ExecutionContext ctx = new ExecutionContext(taskId.pluginId, bucketRegistry);
        taskInfo.task().execute(ctx);
    }

    private String describeCycle(Deque<TaskId> callStack, TaskId ancestor) {
        List<TaskId> path = new ArrayList<>();
        for (TaskId node : callStack) {
            path.add(node);
            if (node.equals(ancestor)) {
                break;
            }
        }
        Collections.reverse(path);
        path.add(ancestor);
        return path.stream()
        .map(TaskId::toString)
        .collect(Collectors.joining(" -> "));
    }

    private Task instantiate(Class<? extends Task> taskType) {
        try {
            Task task = taskType.getDeclaredConstructor().newInstance();
            return task;
        } catch (Exception e) {
            throw new NobException("Could not create new instance for " + taskType, e);
        }
    }
}

// info derived during phase 2

// what does plugin reads means
// it reads buckets, configs. we prob dont care about configs because its merged
// it writes to buckets, writes configs. if it wants to write configs, it must be after the plugin that produces it. but the plugin runs first, its already produced. so we dont need to care about that. its already produced, just run it in the order of dependency made by tasks, and update
record PluginMeta(Plugin plugin, Set<PluginId> dependsOn, Set<PluginId> dependedOnBy) {
    public static PluginMeta from(Plugin plugin) {
        return new PluginMeta(plugin, new HashSet<>(), new HashSet<>());
    }
}
record TaskInfo(Task task, Set<BucketId> reads, Set<BucketId> writes) {
    public static TaskInfo from(Task task) {
        return new TaskInfo(task, new HashSet<>(), new HashSet<>());
    }
}
record BucketInfo(Bucket bucket, Set<TaskId> readers, Set<TaskId> writers) {
    public static BucketInfo from(Bucket bucket) {
        return new BucketInfo(bucket, new HashSet<>(), new HashSet<>());
    }
}
record ConfigInfo(Type type, Object value) {}
