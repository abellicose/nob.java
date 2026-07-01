/* ======================================
 * File: ResolveTask.java
 * Date: 2026-06-11
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.ArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.CompletionStage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.Runtime;
import nob.NobException;
import nob.Task;

import static javax.xml.stream.XMLStreamConstants.*;

// Pieces: Pom Fetcher, Pom Parser, Deps Resolver, JarDownloader -- Downloader, Parser, Resolver can be part of ResolveTask perhaps
// TODO: Only resolve if deps list was updated.
public class ResolveTask implements Task {
    // assuming there's only one instance of this task, might have to make them object specific and/or use ThreadLocal who knows
    private static final ScheduledExecutorService scheduledService = Executors.newSingleThreadScheduledExecutor();
    private static final ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1); 
    private static final ExecutorCompletionService service = new ExecutorCompletionService(pool);
    public static final ConcurrentHashMap<String, Float> progress = new ConcurrentHashMap<>();

    public String id() {
        return "resolve";
    }

    public void execute(Context ctx) {
        if (ctx.deps.isEmpty()) {
            Logger.debug("No dependencies to resolve.");
            return;
        }

        Stack<Dependency> stack = new Stack<>();
        Map<Dependency, List<String>> constraints = new HashMap<>();
        stack.addAll(ctx.deps);
        for (Dependency dep: ctx.deps) {
            constraints.put(dep, new ArrayList<>(List.of(dep.version)));
        }

        Logger.info("Fetching POM files and parsing...");
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

        Logger.info("Downloading jars...");

        scheduledService.scheduleWithFixedDelay(this::render, 0L, 100L, TimeUnit.MILLISECONDS);
        
        for (Dependency dep: resolved) {
            service.submit(() -> {
                return Downloader.downloadToGlobalCache(dep, ".jar", ctx);
            });
        }

        try {
            for (int i = 0; i < resolved.size(); i++) {
                Path jarPath = (Path) service.take().get();
                Path path = ctx.libs.resolve(jarPath.getFileName());
                ctx.libJars.add(path);
                if (!Files.exists(path)) {
                    Files.copy(jarPath, path);
                }
            }
        } catch (Exception e) {
            throw (NobException) e.getCause();
        } finally {
            pool.shutdown();
            scheduledService.shutdown();
            render();
        }
    }

    // resolve specific function
    private PomData fetchAndParse(Dependency dep, Context ctx) {
        Path pomPath = Downloader.downloadToGlobalCache(dep, ".pom", ctx);
        PomData pom = PomParser.parse(pomPath, dep.exclusions, ctx);
        if (pom.parent != null) {
            PomData parentData = fetchAndParse(pom.parent, ctx);
            parentData.properties.putAll(pom.properties);
            pom.properties = parentData.properties;
            pom.managements.putAll(parentData.managements);
        } else {
        }
        return pom;
    }

    private void resolveReferences(PomData pom) {
        // resolve parameters if any
        for (Map.Entry<String, String> entry: pom.properties.entrySet()) {
            String val = entry.getValue();
            if (val.startsWith("${")) {
                val = resolveProperty(val, pom.properties, 0);
                entry.setValue(val);
            }
        }
        // resolve dependency versions
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

    private String resolveProperty(String value, Map<String, String> properties, int depth) {
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
    
    private int lastActiveLineCount = 0;

    public void render() {
        StringBuilder out = new StringBuilder();
        int previousCount = lastActiveLineCount;

        synchronized (progress) {
            for (Map.Entry<String, Float> entry : progress.entrySet().stream().sorted((a, b) -> Float.compare(b.getValue(), a.getValue())).toList()) {
                out.append(line(entry.getKey(), entry.getValue()));
                if (entry.getValue() >= 100F) {
                    progress.remove(entry.getKey());
                }
            }

            lastActiveLineCount = progress.size();
        }

        out.insert(0, "\033[1A".repeat(previousCount));
        System.out.print(out);
        System.out.flush();
    }

    private String line(String name, float pct) {
        int barWidth = 50;
        int filled = Math.round(barWidth * pct / 100F);

        StringBuilder bar = new StringBuilder();
        bar.append('[');
        for (int i = 0; i < barWidth; i++) {
            bar.append(i < filled ? '#' : '-');
        }
        bar.append(']');

        return String.format("%-50s %s %6.2f%%\n", name, bar, pct);
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private int terminalWidth() {
        try {
            Process p = new ProcessBuilder("tput", "cols").redirectInput(ProcessBuilder.Redirect.INHERIT).start();
            String cols = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor();
            return cols.isEmpty() ? 80 : Integer.parseInt(cols);
        } catch (Exception e) {
            return 80;
        }
    }
}
