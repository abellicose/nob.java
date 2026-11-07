/* ======================================
 * File: TaskPipeline.java
 * Date: 2026-05-31
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.stream.Collectors;
import java.lang.reflect.Type;
import java.lang.reflect.Type;
import nob.NobException;
import nob.Task;

public class TaskPipeline {
    private Map<TaskId, Task>        taskRegistry = new HashMap<>();
    private Map<TaskId, Integer>           degree = new HashMap<>();
    private Map<BucketId, Bucket>  bucketRegistry = new HashMap<>();
    private Map<BucketId, Set<TaskId>> producerOf = new HashMap<>();
    private Map<BucketId, Set<TaskId>> consumerOf = new HashMap<>();

/*
    private boolean chainVerified = false;

    public void createBucket(BucketId bucketId, Type type) {
        bucketRegistry.putIfAbsent(bucketId, new Bucket<>(type));
    }

    public void registerTask(TaskId taskId, Task task) {
        if (taskRegistry.containsKey(taskId)) {
           throw new NobException("Task already exists for: " + taskId);
        }

        taskRegistry.put(taskId, task);

        chainVerified = false;
    }

    public void verifyChain() {
        if (chainVerified) return;
        Logger.debug("In verifyChain");

        for (Map.Entry<TaskId, Task> entry: taskRegistry.entrySet()) {
            TaskId taskId = entry.getKey();
            Task task = entry.getValue();
            int deg = 0;
            for (BucketId consumedBucket: task.consumes()) {
                if (!producerOf.containsKey(consumedBucket)) {
                    throw new NobException("No producer found for " + consumedBucket);
                }
                deg += producerOf.get(consumedBucket).size();
            }

            degree.put(taskId, deg);

            for (BucketId populatedBucket: task.populates()) {
                if (!producerOf.containsKey(populatedBucket)) {
                    throw new NobException("No producer found for populated bucket " + populatedBucket);
                }
            }
        }

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
                    stack.poll();
                    callStack.poll();
                    visiting.remove(taskId);
                    visited.add(taskId);
                    continue;
                }

                if (degree.get(taskId) == 0) {
                    stack.poll();
                    visited.add(taskId);
                    continue;
                }
                callStack.push(taskId);
                visiting.add(taskId);

                for (BucketId consumedBucket: taskRegistry.get(taskId).consumes()) {
                    for (TaskId producer: producerOf.get(consumedBucket)) {
                        if (visiting.contains(producer)) {
                            throw new NobException("Circular dependency detected: " + describeCycle(callStack, producer));
                        }
                        if (visited.contains(producer)) continue;
                        stack.push(producer);
                    }
                }
            }
        }
        chainVerified = true;
    }

    public void run(TaskId currentTaskId, ExecutionContext ctx) {
        if (!chainVerified) {
            throw new NobException("Method TaskPipeline:verifyChain needs to be called before any task is executed.");
        }

        if (!taskRegistry.containsKey(currentTaskId)) {
            throw new NobException("Task " + currentTaskId + " is not registered! Cannot execute.");
        }

        Set<TaskId> zeroDegreeTasks = new HashSet<>();
        Map<TaskId, Integer> tempDegree = new HashMap<>();

        Set<TaskId> visited = new HashSet<>();
        Deque<TaskId> stack = new ArrayDeque<>();
        stack.push(currentTaskId);

        // on a DAG
        while (!stack.isEmpty()) {
            TaskId taskId = stack.poll();

            int taskDegree = degree.get(taskId);
            tempDegree.put(taskId, taskDegree);
            if (taskDegree == 0) {
                zeroDegreeTasks.add(taskId);
            }

            for (BucketId consumedBucket: taskRegistry.get(taskId).consumes()) {
                for (TaskId producerTask: producerOf.get(consumedBucket)) {
                    if (visited.contains(producerTask)) continue;

                    visited.add(producerTask);
                    stack.push(producerTask);
                }
            }
        }

        if (zeroDegreeTasks.isEmpty()) {
            throw new NobException("No tasks with degree=0 found. Somethings wrong.");
        }

        // stack should be cleared by now
        stack.addAll(zeroDegreeTasks);

        while(!stack.isEmpty()) {
            TaskId taskId = stack.pop();
            Task task = taskRegistry.get(taskId);
            executeTask(task, ctx);

            for (BucketId producedBucket: task.produces()) {
                for (TaskId consumerTask: consumerOf.getOrDefault(producedBucket, Set.of())) {
                    if (!visited.contains(consumerTask)) continue;

                    int newDegree = tempDegree.get(consumerTask) - 1;
                    tempDegree.put(consumerTask, newDegree);

                    if (newDegree == 0) {
                        stack.push(consumerTask);
                    }
                }
            }
        }
    }

    private void executeTask(Task task, ExecutionContext ctx) {
        task.execute(ctx);
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
*/
}

