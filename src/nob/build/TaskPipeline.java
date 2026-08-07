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
    //          TaskId       TaskDegree
    private Map<NamespaceId, Integer> degree = new HashMap<>();
    // BucketId/ValveId      Tasks that depend on bucket, downstream
    private Map<NamespaceId, Set<NamespaceId>> downstream = new HashMap<>();

/*
    public void createLifecycle(String lifecycle) {
        String inlet = lifecycle + INLET_SUFFIX;
        String outlet = lifecycle + OUTLET_SUFFIX;
        if (registry.containsKey(inlet) || registry.containsKey(outlet)) {
            throw new NobException("Cannot create lifecycle " + lifecycle + ", already exists!");
        }

        registry.put(inlet, new Lifecycle(inlet));
        downstream.put(inlet, new HashSet<>());
        degree.put(inlet, 0);

        registry.put(outlet, new Lifecycle(outlet));
        downstream.put(outlet, new HashSet<>());
        degree.put(outlet, 0);
        Logger.debug("Lifecycle " + lifecycle + " created");
    }
*/

    // register task
    public void registerTask(NamespaceId taskId, Class<? extends Task> taskType) {
        if (taskRegistry.containsKey(taskId)) {
            throw new NobException("Task already exists for: " + taskId);
        }

        Task task;
        try {
            task = taskType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NobException("Failed to instantiate task: " + taskType.getName(), e);
        }

        taskRegistry.put(taskId, task);
        // create produced buckets, this is the only place buckets are produced
        for (NamespaceId outlet: task.produces()) {
            bucketRegistry.putIfAbsent(outlet, new Bucket());
        }
        
        int inletCount = 0;
        for (NamespaceId inlet: task.consumes()) {
            downstream.computeIfAbsent(inlet, k -> new HashSet<>()).add(taskId);
            inletCount++;
        }

        degree.put(taskId, inletCount);
    }

    public void registerTask(String signature, Task task) {
        if (registry.containsKey(signature)) {
            throw new NobException("Cannot register task " + signature + ", already exists!");
        }

        if (registry.containsKey(signature + INLET_SUFFIX)) {
            throw new NobException("Cannot register task, a lifecycle with name " + signature + " exists!");
        }

        registry.put(signature, task);
        downstream.put(signature, new HashSet<>());
        degree.put(signature, 0);

        Logger.debug("Task " + signature + " registered");
    }

    public void addTo(String signature, String prerequisite) {
        String depOutlet = prerequisite;
        boolean depIsTask = true;
        if (!registry.containsKey(depOutlet)) {
            depOutlet += INLET_SUFFIX;
            if (!registry.containsKey(depOutlet)) {
                throw new NobException("Cannot add " + signature + " to " + prerequisite + ", dependency not found!");
            }
            depIsTask = false;
        }

        String taskInlet = signature;
        if (!registry.containsKey(taskInlet)) {
            taskInlet += INLET_SUFFIX;
            if (!registry.containsKey(taskInlet)) {
                throw new NobException("Cannot add " + signature + " to " + prerequisite + ", dependent not found!");
            }
        }

        String taskOutlet = signature;
        if (!registry.containsKey(taskOutlet)) {
            taskOutlet += OUTLET_SUFFIX;
            if (!registry.containsKey(taskOutlet)) {
                throw new NobException("Cannot add " + signature + " to " + prerequisite + ", dependent not found!");
            }
        }

        String depInlet = prerequisite;
        if (!registry.containsKey(depInlet)) {
            depInlet += OUTLET_SUFFIX;
            if (!registry.containsKey(depInlet)) {
                throw new NobException("Cannot add " + signature + " to " + prerequisite + ", dependency not found!");
            }
        }

        if (downstream.get(depOutlet).add(taskInlet)) {
            degree.put(taskInlet, degree.get(taskInlet) + 1);
            Logger.debug("Task or lifestyle " + taskInlet + " added after " + depOutlet + ". Degree: " + degree.get(taskInlet));
        }

        if (!depIsTask && downstream.get(taskOutlet).add(depInlet)) {
            degree.put(depInlet, degree.get(depInlet) + 1);
            Logger.debug("Task or lifestyle " + depInlet + " added after " + taskOutlet + ". Degree: " + degree.get(depInlet));
        }

    }

    public void addAfter(String signature, String prerequisite) {
        String outlet = prerequisite;
        if (!registry.containsKey(outlet)) {
            outlet += OUTLET_SUFFIX;
            if (!registry.containsKey(outlet)) {
                throw new NobException("Cannot add " + signature + " to " + prerequisite + ", dependency not found!");
            }
        }

        String inlet = signature;
        if (!registry.containsKey(inlet)) {
            inlet += INLET_SUFFIX;
            if (!registry.containsKey(inlet)) {
                throw new NobException("Cannot add " + signature + " to " + prerequisite + ", dependent not found!");
            }
        }

        if (downstream.get(outlet).add(inlet)) {
            degree.put(inlet, degree.get(inlet) + 1);
            Logger.debug("Task or lifestyle " + signature + " added to " + prerequisite + ". Degree: " + degree.get(inlet));
        }
    }

    public void run(String signature, Context ctx) {
        String targetOutlet = signature; // outlet, needs to end here. the task that needs to end
        if (!registry.containsKey(targetOutlet)) {
            targetOutlet += INLET_SUFFIX;
            if (!registry.containsKey(targetOutlet)) {
                throw new NobException("Task or group " + signature + " is not registered! Cannot execute.");
            }
        }

        //  if downstream is maps of inlets to streams, upstream is maps of streams to inlets
        Map<String, Set<String>> upstream = new HashMap<>();

        for (Map.Entry<String, Set<String>> ds: downstream.entrySet()) {
            String outlet = ds.getKey();
            for (String inlet: ds.getValue()) {
                upstream.computeIfAbsent(inlet, k -> new HashSet<>()).add(outlet);
            }
        }

        // collect
        //          valves for stream to go through
        Set<String> valves = new HashSet<>();
        Map<String, Integer> tmpDegree = new HashMap<>();
        Deque<String> stack = new ArrayDeque<>();
        Set<String> zeroDegreeValves = new HashSet<>();
        stack.add(targetOutlet);

        while(!stack.isEmpty()) {
            String inlet = stack.pop();
            if (!valves.add(inlet)) continue; 

            int inletDegree = degree.get(inlet);
            tmpDegree.put(inlet, inletDegree);
            for (String outlet: upstream.getOrDefault(inlet, Set.of())) {
                stack.push(outlet);
            }

            if (inletDegree == 0) {
                zeroDegreeValves.add(inlet);
            }
        }

        stack.clear();
        stack.add(targetOutlet);

        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        Deque<String> path = new ArrayDeque<>();
        while(!stack.isEmpty()) {
            String outlet = stack.peek();
            Set<String> inlets = downstream.get(outlet);
            int inletCnt = 0;

            visiting.add(outlet);
            path.push(outlet);
            for (String inlet: inlets) {
                if (visited.contains(inlet)) continue;
                stack.push(inlet);
                inletCnt++;
                if (visiting.contains(inlet)) {
                    List<String> ring = new ArrayList<>();
                    path.push(inlet);
                    while(!path.isEmpty()) {
                        ring.add(path.pop());
                    }
                    throw new NobException(renderRing(ring));
                }
            }

            if (inletCnt == 0) {
                stack.pop();
                visiting.remove(outlet);
                visited.add(outlet);
                path.pop();
            }
        }

        stack.clear();
        stack.addAll(zeroDegreeValves);
        Logger.debug("Stack: " + zeroDegreeValves);
        while (!stack.isEmpty()) {
            String outlet = stack.pop();
            executeTask(registry.get(outlet), ctx);

            for (String inlet: downstream.get(outlet)) {
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

    // made by claude <3
    private String renderRing(List<String> ring) {
        String anchor = ring.get(0);
        int close = -1;
        for (int i = 1; i < ring.size(); i++) {
            if (ring.get(i).equals(anchor)) { close = i; break; }
        }
        List<String> loop = (close < 0) ? ring : ring.subList(0, close + 1);

        List<String> tokens = collapse(loop);
        List<String> nodes = new ArrayList<>(tokens);
        nodes.add(tokens.get(0));

        List<String> rows = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            boolean last = (i == nodes.size() - 1);
            rows.add("  " + nodes.get(i) + (last ? "   ↺ back to top" : ""));
            if (!last) rows.add("   ▼");
        }

        String title = " Circular dependency ";
        int inner = title.length();
        for (String r : rows) inner = Math.max(inner, r.length() + 1);

        StringBuilder sb = new StringBuilder();
        sb.append(Logger.RESET).append('\n');
        sb.append('╭').append('─').append(title);
        for (int k = 0; k < inner - title.length() + 1; k++) sb.append('─');
        sb.append('╮').append('\n');
        for (String r : rows) {
            sb.append('│').append(' ').append(r);
            for (int k = r.length(); k < inner; k++) sb.append(' ');
            sb.append(' ').append('│').append('\n');
        }
        sb.append('╰');
        for (int k = 0; k < inner + 2; k++) sb.append('─');
        sb.append('╯');
        return sb.toString();
    }

    private List<String> collapse(List<String> loop) {
        List<String> cyc = new ArrayList<>(loop);
        if (cyc.size() > 1 && cyc.get(0).equals(cyc.get(cyc.size() - 1))) {
            cyc.remove(cyc.size() - 1);
        }
        for (int i = 0; i < cyc.size(); i++) {
            if (cyc.get(i).endsWith(INLET_SUFFIX)) { Collections.rotate(cyc, -i); break; }
        }

        List<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < cyc.size()) {
            String node = cyc.get(i);
            if (node.endsWith(INLET_SUFFIX)) {
                String group = node.substring(0, node.length() - INLET_SUFFIX.length());
                String end = group + OUTLET_SUFFIX;
                List<String> members = new ArrayList<>();
                int j = i + 1;
                while (j < cyc.size() && !cyc.get(j).equals(end)) {
                    String m = cyc.get(j);
                    if (!m.endsWith(INLET_SUFFIX) && !m.endsWith(OUTLET_SUFFIX)) members.add(m);
                    j++;
                }
                if (members.isEmpty()) tokens.add(group);
                else tokens.addAll(members);
                i = (j < cyc.size()) ? j + 1 : j;
            } else {
                tokens.add(node);
                i++;
            }
        }
        return tokens;
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
