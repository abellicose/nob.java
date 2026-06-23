/* ======================================
 * File: Graph.java
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
import java.util.Stack;
import nob.NobException;
import nob.Task;

public class Graph {
    private Map<String, Task> registry = new HashMap<>();
    private Set<String> queue = new HashSet<>();

    public void register(Task task) {
        registry.put(task.id(), task);
    }

    public void enqueue(String taskId) {
        if (!registry.containsKey(taskId)) {
            throw new NobException("No task with id: " + taskId + " registered.");
        }
        if (!queue.contains(taskId)) {
            queue.add(taskId);
        }
    }

    public void run(Context ctx) {
        int queueSize = queue.size();
        if (queueSize == 0) {
            throw new NobException("No tasks queued for execution.");
        }

        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        Stack<Node> stack = new Stack<>();

        for (String task: queue) {
            if (visited.contains(task)) continue;
            stack.push(new Node(task, false));
            while (!stack.isEmpty()) {
                Node curr = stack.pop();

                if (curr.done()) {
                    visiting.remove(curr.task());
                    visited.add(curr.task());
                    result.add(curr.task());
                    continue;
                }

                if (visiting.contains(curr.task())) {
                    throw new NobException("Circular dependency at: " + curr.task());
                }

                visiting.add(curr.task());
                stack.push(new Node(curr.task(), true));

                for (String dep: registry.get(curr.task()).dependsOn()) {
                    if (!visited.contains(dep)) {
                        stack.push(new Node(dep, false));
                    }
                }
            }
        }
        for (String taskId : result) {
            Logger.info("Running task: " + taskId);
            registry.get(taskId).execute(ctx);
            Logger.info("Task " + taskId + " ended");
        }
    }

    record Node(String task, boolean done){}
}

