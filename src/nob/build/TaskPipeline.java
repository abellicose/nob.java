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
    //          TaskId       Task
    private Map<NamespaceId, Task> taskRegistry = new HashMap<>();
    //          BucketId     Bucket
    private Map<NamespaceId, Bucket> bucketRegistry = new HashMap<>();
    //          BucketId         Tasks that consumed it
    private Map<NamespaceId, Set<NamespaceId>> producerOf = new HashMap<>();

    // TODO: Currently exists: Pipeline derives dependency from what task consumes() and produces()
    //       Allow tasks to be added before or after tasks
    //       Allow lifecycles

    // register task
    public void registerTask(NamespaceId taskId, Task task) {
        // this should never happen
        // two plugins with the same name shouldn't exist in the first place.
        // if (taskRegistry.containsKey(taskId)) {
        //    throw new NobException("Task already exists for: " + taskId);
        // }
        taskRegistry.put(taskId, task);
        for (NamespaceId producedBucket: task.produces()) {
            bucketRegistry.putIfAbsent(producedBucket, new Bucket());
            producerOf.computeIfAbsent(producedBucket, k -> new HashSet<>()).add(taskId);
        }
    }

    public boolean verifyChain(NamespaceId taskId) {
        Task task = taskRegistry.get(taskId);
        if (task == null) {
            throw new NobException("[verifyChain] Task " + taskId + " was not registered.");
        }
        
        Deque<Task> stack = new ArrayDeque<>();
        stack.push(task);
        while(!stack.isEmpty()) {
            Task task = stack.pop();
            for (NamespaceId consumedBucket: task.consumes()) {
                
            }
        }
    }

    public void run(NamespaceId taskId, Context ctx) {
        NamespaceId targetOutlet = taskId; // outlet, needs to end here. the task that needs to end
        if (!taskRegistry.containsKey(targetOutlet)) {
            throw new NobException("Task " + taskId + " is not registered! Cannot execute.");
        }

        Set<NamespaceId> valves = new HashSet<>();
        Set<NamespaceId> zeroDegreeValves = new HashSet<>();
        Map<NamespaceId, Integer> tmpDegree = new HashMap<>();

        Deque<NamespaceId> stack = new ArrayDeque<>();
        stack.add(targetOutlet);

        while(!stack.isEmpty()) {
            NamespaceId inlet = stack.pop();
            if (!valves.add(inlet)) continue; 

            int inletDegree = degree.get(inlet);
            tmpDegree.put(inlet, inletDegree);
            if (inletDegree == 0) {
                zeroDegreeValves.add(inlet);
                continue;
            }

            for (NamespaceId outlet: upstream.get(inlet)) {
                stack.push(outlet);
            }
        }

        stack.clear();
        stack.add(targetOutlet);

        Set<NamespaceId> visiting = new HashSet<>();
        Set<NamespaceId> visited = new HashSet<>();
        while(!stack.isEmpty()) {
            NamespaceId outlet = stack.pop();
            Set<NamespaceId> inlets = downstream.get(outlet);
            int inletCount = 0;

            visiting.add(outlet);
            for (NamespaceId inlet: inlets) {
                if (visited.contains(inlet)) continue;
                stack.push(inlet);
                inletCount++;
                if (visiting.contains(inlet)) {
                    // report the circular dep here
                    throw new NobException("Circular dependency spotted between tasks.");
                }
            }

            visiting.remove(outlet);
            visited.add(outlet);
        }

        stack.clear();
        stack.addAll(zeroDegreeValves);
        while (!stack.isEmpty()) {
            NamespaceId outlet = stack.pop();
            executeTask(taskRegistry.get(outlet), ctx);

            for (NamespaceId inlet: downstream.get(outlet)) {
                if (!valves.contains(inlet)) continue;

                int newDegree = tmpDegree.get(inlet) - 1;
                tmpDegree.put(inlet, newDegree);

                if (newDegree == 0) {
                    stack.push(inlet);
                }
            }
        }
    }


    public void executeTask(Task task, Context ctx) {
        // placeholder
        task.execute(ctx);
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
