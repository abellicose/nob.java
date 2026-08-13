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
import nob.NobException;
import nob.Task;

// Im autistic and its hard to hold it in my head without using metaphors
// inlet — where a node's flow enters
// outlet — where a node's flow exits
// downstream — the map recording which inlets each outlet feeds into
// the stream itself — not a variable, just what run() does when it walks outlet → inlet → outlet → inlet...
public class TaskPipeline {
    private Map<TaskId, Task> taskRegistry = new HashMap<>();
    private Map<BucketId, Bucket> bucketRegistry = new HashMap<>();
    private Map<BucketId, Set<TaskId>> producerOf = new HashMap<>();
    private Map<BucketId, Set<TaskId>> consumerOf = new HashMap<>();
    private boolean chainVerified = false;

    // TODO: Currently exists: Pipeline derives dependency from what task consumes() and produces()
    //       Allow tasks to be added before or after tasks
    //       Allow lifecycles

    // register task
    public void registerTask(TaskId taskId, Task task) {
        // this should never happen
        // two plugins with the same name shouldn't exist in the first place.
        // here only to prevent registering same task twice until we figure out how to do that
        if (taskRegistry.containsKey(taskId)) {
           throw new NobException("Task already exists for: " + taskId);
        }
        taskRegistry.put(taskId, task);
        for (BucketId producedBucket: task.produces()) {
            if (producedBucket.pluginId.equals(taskId.pluginId)) {
                bucketRegistry.putIfAbsent(producedBucket, new Bucket());
            }
            producerOf.computeIfAbsent(producedBucket, k -> new HashSet<>()).add(taskId);
        }

        for (BucketId consumedBucket: task.consumes()) {
            consumerOf.computeIfAbsent(consumedBucket, k -> new HashSet<>()).add(taskId);
        }

        chainVerified = false;
    }

    public void verifyChain() {
        for (Task task: taskRegistry.values()) {
            for (BucketId consumedBucket: task.consumes()) {
                if (!producerOf.containsKey(consumedBucket)) {
                    throw new NobException("No producer found for " + consumedBucket);
                }
            }
        }

        Set<TaskId> visiting = new HashSet<>();
        Set<TaskId> visited = new HashSet<>();
        Deque<TaskId> stack = new ArrayDeque<>();
        Deque<TaskId> callStack = new ArrayDeque<>();

        for (TaskId currentTaskId: taskRegistry.keySet()) {
            if (!visited.contains(entry.getKey())) {
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

    public void run(TaskId currentTaskId, Context ctx) {
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

                    visited.add(taskId);
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
            executeTask(task);

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

    private void executeTask(Task task, Context ctx) {
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
}

class Lifecycle implements Task {
    private String id;

    Lifecycle(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public void execute(Context ctx) {
        if (this.id.endsWith("$start")) {
            Logger.info("============================================");
        }
        Logger.info("Group task: " + this.id);
        if (this.id.endsWith("$end")) {
            Logger.info("============================================");
        }
    }
}
