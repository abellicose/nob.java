/* ======================================
 * File: DependencyResolver.java
 * Date: 2026-06-11
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.deps;

import java.nio.file.Files;
import java.nio.file.Path;
import nob.build.Context;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import nob.NobException;

public class DependencyResolver {

    public static void resolve(Context ctx) {
        if (ctx.deps.isEmpty())
            return;

        Stack<Dependency> stack = new Stack<>();
        Map<Dependency, List<String>> constraints = new HashMap<>();
        stack.addAll(ctx.deps);
        for (Dependency dep: ctx.deps) {
            constraints.put(dep, new ArrayList<>(List.of(dep.version)));
        }

        while (!stack.isEmpty()) {
            Dependency dep = stack.pop();
            PomData pom = fetchAndParse(dep, ctx);
            resolveReferences(pom);

            for (Dependency transitive : pom.deps) {
                if (transitive.version == null) continue;
                if (!constraints.containsKey(transitive)) {
                    constraints.computeIfAbsent(transitive, k -> new ArrayList<>()).add(transitive.version);
                    stack.push(transitive);
                } else {
                    constraints.get(transitive).add(transitive.version);
                }
            }        
        }

        List<Dependency> resolved = new ArrayList<>();

        try {
            for (Map.Entry<Dependency, List<String>> entry: constraints.entrySet()) {
                ComparableVersion highest = new ComparableVersion(entry.getValue().get(0));

                if (entry.getValue().size() > 1) {
                    for (int i = 1; i < entry.getValue().size(); i++) {
                        ComparableVersion v = new ComparableVersion(entry.getValue().get(i));
                        if (v.compareTo(highest) > 0) {
                            highest =  v;
                        }
                    }
                }
                String version = highest.toString();
                entry.getKey().version = version;
                resolved.add(entry.getKey());
            }
        } catch (Exception e) {
            throw new NobException("Version resolution had probably failed.", e);
        }

        for (Dependency dep: resolved) {
            Path path = MavenRepository.downloadJar(dep, ctx);
            try {
                Path target = ctx.libs.resolve(path.getFileName());
                if (!Files.exists(target)) {
                    Files.copy(path, ctx.libs.resolve(path.getFileName()));
                }
            } catch (Exception e) {
                throw new NobException("Failed to copy jar from global cache to local libs folder", e);
            }
        }
    }

    private static PomData fetchAndParse(Dependency dep, Context ctx) {
        Path pomPath = MavenRepository.fetchPom(dep, ctx);
        PomData pom = PomParser.parse(pomPath, dep.exclusions, ctx);
        if (pom.parent != null) {
            PomData parentData = fetchAndParse(pom.parent, ctx);
            parentData.properties.putAll(pom.properties);
            pom.properties = parentData.properties;
            pom.managements.putAll(parentData.managements);
        }
        return pom;
    }

    private static void resolveReferences(PomData pom) {
        for (Map.Entry<String, String> entry: pom.properties.entrySet()) {
            String val = entry.getValue();
            if (val.startsWith("${")) {
                val = resolveProperty(val, pom.properties, 0);
                entry.setValue(val);
            }
        }
        for (Dependency dep: pom.deps) {
            String newVersion = dep.version;
            if (newVersion == null) {
                newVersion = pom.managements.get(dep);
                if (newVersion == null) {
                    continue;
                }
            }

            if (newVersion.startsWith("${")) {
                newVersion = pom.properties.get(newVersion.substring(2, newVersion.length() - 1));
            }

            dep.version = newVersion;
        }
    }

    private static String resolveProperty(String value, Map<String, String> properties, int depth) {
        if (!value.startsWith("${"))
            return value;
        if (depth > 15)
            throw new NobException("Property resolution exceeded max depth, possible cycle: " + value);
        String key = value.substring(2, value.length() - 1);
        String raw = properties.get(key);
        if (raw == null) return value;
        String resolved = resolveProperty(raw, properties, depth + 1);
        properties.put(key, resolved);
        return resolved;
    }

}


