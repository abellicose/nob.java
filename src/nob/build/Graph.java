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
    //          Task    Dependents
    //          Parent  Child
    //          Marker  Node
    private Map<String, Set<String>> edges = new HashMap<>();
    private Map<String, Integer> degree = new HashMap<>();
    private Set<String> queue = new HashSet<>();

    public void createLifecycle(String dependency) {
        String depStart = dependency + "$start";
        String depEnd = dependency + "$end";
        if (registry.containsKey(depStart) || registry.containsKey(depEnd)) {
            throw new NobException("Task or lifecycle " + dependency + " already exists");
        }
        registry.put(depStart, new Lifecycle(start));
        registry.put(depEnd, new Lifecycle(end));
        degrees.put(depStart, 0); degrees.put(depEnd, 0);
    }

    // TODO: same task can't run twice. do we want that?
    public void registerTo(Task task, String dependency) {
        String id = task.id();
        if (registry.containsKey(id)) {
            throw new NobException("Task " + id + " already exists!");
        }
        String start = dependency + "$start";
        String end = dependency + "$end";
        // lifecycles need to be defined first because they have specific configuration (orders and such)
        if (!registry.containsKey(start) || !registry.containsKey(end)) {
            throw new NobException("Lifecycle "  + dependency + " not found!");
        }

        // Children of start, tasks that runs after $start
        //                    This <----- depends on <-------- This
        edges.computeIfAbsent(start, k -> new HashSet<>()).add(id);
        edges.computeIfAbsent(id, k -> new HashSet<>()).add(end);
        registry.put(id, task);
        degrees.put(degrees.get());
    }

    public void addAfter(String task, String dependency) {
        String depKey = dependency;
        if (!registry.containsKey(depKey)) { // if it contains, its a normal task. otherwise we check for group
            depKey = dependency + "$end";
            if (!registry.containsKey(depKey)) {
                // create new or throw?
                throw new NobException("Task or lifecycle "  + dependency + " not found!");
            }
        }

        String tskKey = task;
        if (!registry.containsKey(tskKey)) {
            tskKey = task + "$start";
            if (!registry.containsKey(tskKey)) {
                throw new NobException("Task or lifecycle "  + dependency + " not found!");
            }
        }

        edges.get(depKey).add(tskKey);
    }

    public void putAfter(String task, String dependency) {
        String depKey = dependency;
        if (!registry.containsKey(depKey)) { // if it contains, its a normal task. otherwise we check for group
            depKey = dependency + "$end";
            if (!registry.containsKey(depKey)) {
                // create new or throw?
                throw new NobException("Task or lifecycle "  + dependency + " not found!");
            }
        }

        String tskStart = task;
        String tskEnd = task;
        if (!registry.containsKey(taskStart)) {
            tskKey = task + "$start";
            tskEnd = task + "$end";
            if (!registry.containsKey(tskKey)) {
                throw new NobException("Task or lifecycle "  + dependency + " not found!");
            }
        }

        edges.put(tskEnd, edges.get(depKey));
        edges.put(depKey, Set.of(tskStart));
    }

    public void enqueue(String taskId) {
        if (!registry.containsKey(taskId)) {
            throw new NobException("No task with id: " + taskId + " registered.");
        }
        if (!queue.contains(taskId)) {
            queue.add(taskId);
        }
    }

    public void run(Context ctx, String temp) {
        int queueSize = queue.size();
        if (queueSize == 0) {
            throw new NobException("No tasks queued for execution.");
        }
        List<String> plan = plan();
    }

    // TODO: Revert Edge Detection
    // execution plan
    private Set<String> plan() {
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        List<String> result = new ArrayList<>();

        for (String task: queue) {
            if (visited.contains(task)) continue;
            topoSort(task, visiting, visited, result);
        }

        return result;
    }

    private void topoSort(String node, Set<String> visiting, Set<String> visited, List<String> result) {
        // TODO: Visited Check here instead of caller? (maybe not, check runs for each subtree, waste)
        if (visiting.contains(node)) {
            throw new NobException("Circular dependency at: " + node);
        }
        visiting.add(node);
        Set<String> edgeList = edges.get(node);

        if (edgeList != null) {
            for (String edge: edgeList) {
                if (!visited.contains(edge)) {
                    topoSort(edge, visiting, visited, result);
                }
            }
        }

        visiting.remove(node);
        visited.add(node);
        result.add(node);
    }

    // TODO: Rewrite THIS SHIT - Change LLM CODE
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

                for (String dep: edges.get(curr.task())) {
                    if (!visited.contains(dep)) {
                        stack.push(new Node(dep, false));
                    }
                }
            }
        }

        for (String taskId : result) {
            Logger.info("Running " + taskId);
            registry.get(taskId).execute(ctx);
            Logger.info("Task " + taskId + " ended");
        }
    }

    record Node(String task, boolean done){}
}


class Lifecycle implements Task {
    private String id;

    Plugin(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }
    public void execute(Context ctx) {
    }
}
