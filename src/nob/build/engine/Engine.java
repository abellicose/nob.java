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
    private final Map<PluginId, Map<String, Type>> bucketsProducedByPlugin = new HashMap<>();
    private final Map<TaskId, Map<String, Type>> bucketsWrittenByTask = new HashMap<>();

    // phase 2 - commit
    // 2.1 - initialize tasks, register them.
    // 2.2 - read buckets, register buckets
    // 2.3 - build deps edge (producerOf, consumerOf)
    // 2.4 - count degree & verify producers
    // 2.5 - check cycle
    public final Map<TaskId, TaskInfo> taskRegistry = new HashMap<>();
    public final Map<BucketId, BucketInfo> bucketRegistry = new HashMap<>(); 

    private Map<PluginId, Plugin> pluginRegistry = new HashMap<>();
    
    public void registerPlugin(Plugin plugin) {
        PluginId pluginId = new PluginId(plugin.id());

        if (pluginRegistry.containsKey(pluginId)) {
            throw new NobException("Plugin " + pluginId + " has already been registered");
        }
        pluginRegistry.put(pluginId, plugin);

        Registry reg = new Registry();
        plugin.apply(reg);

        tasksDiscovered.put(pluginId, reg.getTasks());
        bucketsProducedByPlugin.put(pluginId, reg.getBuckets());
    }

    public void show() {
        System.out.println("Tasks:");
        taskRegistry.forEach((id, info) ->
            System.out.println("  " + id + " -> " + info)
        );

        System.out.println("\nBuckets:");
        bucketRegistry.forEach((id, info) ->
            System.out.println("  " + id + " -> " + info)
        );
    }

    // 2.1 - Instantiated Tasks, Registered it
    // 2.2 - Find buckets declared, create it
    // 2.2.1 - Find buckets declared by plugins and create
    // 2.2.2 - Find buckets declared by tasks and create them
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
                TaskInfo taskInfo = new TaskInfo(task, new HashSet<>(), new HashSet<>());
                taskRegistry.put(taskId, taskInfo);
                bucketsWrittenByTask.put(taskId, task.writes());
            }
        }

        // 2.2 - find produced buckets, create and register em
        // 2.2.1 - Process Buckets produced by plugins first
        for (var entry: bucketsProducedByPlugin.entrySet()) {
            PluginId pluginId = entry.getKey();

            for (var bucketEntry: entry.getValue().entrySet()) {
                String bucketName = bucketEntry.getKey();
                Type bucketType = bucketEntry.getValue();
                BucketId bucketId = new BucketId(pluginId, bucketName);

                if (bucketRegistry.containsKey(bucketId)) {
                    throw new NobException("Bucket " + bucketId + " was already registered by another plugin. Shouldn't be possible.");
                }

                Bucket bucket = new Bucket<>(bucketType);
                Set<TaskId> writers = new HashSet<>();
                Set<TaskId> readers = new HashSet<>();
                bucketRegistry.put(bucketId, new BucketInfo(bucket, writers, readers));
            }
        }

        // 2.2.2 - Process buckets produced by tasks
        for (var entry: bucketsWrittenByTask.entrySet()) {
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
                bucketRegistry.put(bucketId, new BucketInfo(bucket, new HashSet<>(), new HashSet<>()));
            }
        }

        // 2.3 - build edge list
        for (Map.Entry<TaskId, TaskInfo> taskEntry: taskRegistry.entrySet()) {
            TaskInfo taskInfo = taskEntry.getValue();
            TaskId taskId = taskEntry.getKey();
            PluginId pluginId = taskId.pluginId;
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
                BucketId key = new BucketId(rawBucketId, null, null);
                BucketInfo bucketInfo = bucketRegistry.get(key); // null null to avoid splitting, useless here since only bucketid is hashed
                if (bucketInfo == null) {
                    // should add if the bucket was in populates or consumes
                    throw new NobException("Bucket " + key + " wasn't produced by any plugin or task. Sussy");
                }
                bucketInfo.readers().add(taskId);
                taskInfo.reads().add(key);
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

    public Task instantiate(Class<? extends Task> taskType) {
        try {
            Task task = taskType.getDeclaredConstructor().newInstance();
            return task;
        } catch (Exception e) {
            throw new NobException("Could not create new instance for " + taskType, e);
        }
    }
}

// info derived during phase 2
record TaskInfo(Task task, Set<BucketId> writes, Set<BucketId> reads) {}
record BucketInfo(Bucket bucket, Set<TaskId> writers, Set<TaskId> readers) {}
