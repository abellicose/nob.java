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
import nob.NobException;
import java.util.concurrent.Flow;
import java.util.concurrent.CompletionStage;

import static javax.xml.stream.XMLStreamConstants.*;

public class ResolveTask implements Task {
    private static final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

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

        for (Dependency dep: resolved) {
            Path path = downloadJar(dep, ctx);
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

    private Path fetchPom(Dependency dep, Context ctx) {
        Path pomPath = ctx.globalCache
            .resolve(dep.groupId.replace(".", "/"))
            .resolve(dep.artifactId)
            .resolve(dep.version)
            .resolve(fileName(dep, ".pom"));

        if (Files.isRegularFile(pomPath)) {
            Logger.debug("POM " + pomPath.getFileName() + " found in cache.");
            return pomPath;
        }

        Logger.debug("Downloading: " + pomPath.getFileName());
        downloadFile(pomPath, dep, ".pom");

        return pomPath;
    }

    private Path downloadJar(Dependency dep, Context ctx) {
        Path jarPath = ctx.globalCache
            .resolve(dep.groupId.replace(".", "/"))
            .resolve(dep.artifactId)
            .resolve(dep.version)
            .resolve(fileName(dep, ".jar"));

        if (Files.isRegularFile(jarPath)) {
            Logger.debug("Jar " + jarPath.getFileName() + " found in cache.");
            return jarPath;
        }
        
        HttpResponse<Path> response;

        try {
            Files.createDirectories(jarPath.getParent());

            URI fileURI = getURI(dep, ".jar");
            HttpRequest req = HttpRequest.newBuilder().uri(fileURI).GET().build();
            response = client.send(req, info -> {
                long totalSize = info.headers().firstValueAsLong("Content-Length").orElse(1);
                return new DownloadSubscriber(jarPath.getFileName().toString(), totalSize, HttpResponse.BodySubscribers.ofFile(jarPath));
            });

        } catch (Exception e) {
            throw new NobException("Failed to send fetch request for " + dep, e);
        }

        int status = response.statusCode();
        if (status == 404) throw new NobException("Dependency not found: " + dep);
        if (status == 429) throw new NobException("Rate limited by Maven Central");
        if (status / 100 != 2) throw new NobException("Failed to fetch " + dep + " status: " + status);

        return jarPath;
    }

    // resolve specific function
    private PomData fetchAndParse(Dependency dep, Context ctx) {
        Path pomPath = fetchPom(dep, ctx);
        PomData pom = parse(pomPath, dep.exclusions, ctx);
        if (pom.parent != null) {
            PomData parentData = fetchAndParse(pom.parent, ctx);
            parentData.properties.putAll(pom.properties);
            pom.properties = parentData.properties;
            pom.managements.putAll(parentData.managements);
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
    
    // download stuff
    private void downloadFile(Path filePath, Dependency dep, String ext) {
        HttpResponse<Path> response;

        try {
            Files.createDirectories(filePath.getParent());

            URI fileURI = getURI(dep, ext);
            HttpRequest req = HttpRequest.newBuilder().uri(fileURI).GET().build();
            response = client.send(req, HttpResponse.BodyHandlers.ofFile(filePath));

        } catch (Exception e) {
            throw new NobException("Failed to send fetch request for " + dep, e);
        }

        int status = response.statusCode();
        if (status == 404) throw new NobException("Dependency not found: " + dep);
        if (status == 429) throw new NobException("Rate limited by Maven Central");
        if (status / 100 != 2) throw new NobException("Failed to fetch " + dep + " status: " + status);
    }

    private URI getURI(Dependency dep, String ext) throws URISyntaxException {
        return new URI(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s", dep.groupId.replace(".", "/"), dep.artifactId, dep.version, fileName(dep, ext)));
    }

    private String fileName(Dependency dep, String ext) {
        return dep.artifactId + "-" + dep.version + ext;
    }

    // POM Parser
    private final Map<String, Set<String>> states = Map.of(
            "project",               Set.of("project"),
            "parent",                Set.of("project"),
            "properties",            Set.of("project"),
            "dependencyManagement",  Set.of("project"),
            "dependencies",          Set.of("project", "dependencyManagement"),
            "dependency",            Set.of("dependencies"),
            "exclusions",            Set.of("dependency"),
            "exclusion",             Set.of("exclusions")
            );

    public PomData parse(Path path, Set<String> exclusions, Context ctx) {
        try {

        Map<String, String> properties = new HashMap<>();
        List<Dependency> dependencies = new ArrayList<>();
        Map<Dependency, String> depMgmt = new HashMap<>();
        Dependency parent = null;

        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = factory.createXMLStreamReader(Files.newInputStream(path));

        Stack<String> stack = new Stack<>();
        stack.push("default");

        Stack<Map<String, String>> data = new Stack<>();
        List<Dependency> deps = new ArrayList<>();
        Set<String> excl = new HashSet<>();
        String currentTag = null;
        boolean save = false;

        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
                case START_ELEMENT:
                    String name = reader.getLocalName();
                    currentTag = name;

                    Set<String> validParents = states.get(name);
                    String stackTop = stack.peek();
                    stack.push(name);

                    if (validParents == null || !validParents.contains(stackTop)) 
                        break;

                    data.push(new HashMap<>());
                    save = true;

                    break;
                case CHARACTERS:
                    if (!save) break;

                    String text = reader.getText().trim();
                    if (text.isEmpty()) break;

                    data.peek().put(currentTag, text);

                    break;
                case END_ELEMENT:
                    String end = stack.pop();
                    if (!states.containsKey(end)) break;
                    if (!states.get(end).contains(stack.peek())) break;

                    Map<String, String> d = data.pop();
                    save = false;

                    switch (end) {
                        case "parent":
                            parent = new Dependency(d.get("groupId"), d.get("artifactId"), d.get("version"));
                            break;
                        case "properties":
                            properties = new HashMap<>(d);
                            break;
                        case "exclusion":
                            excl.add(d.get("groupId") + ":" + d.get("artifactId"));
                            break;
                        case "dependency":
                            String optional = d.get("optional");
                            if (optional != null && optional.equals("true")) 
                                break;

                            String scope = d.get("scope");
                            if (scope != null && !scope.equals("compile"))
                                break;

                            if (!exclusions.contains(d.get("groupId") + ":" + d.get("artifactId"))) {
                                deps.add(new Dependency(d.get("groupId"), d.get("artifactId"), d.get("version"), excl));
                            }
                            excl = new HashSet<>();
                            break;
                        case "dependencies":
                            if (stack.peek().equals("dependencyManagement")) {
                                for (Dependency dep: deps) {
                                    depMgmt.put(dep, dep.version);
                                }
                            } else {
                                dependencies = deps;
                            }
                            deps = new ArrayList<>();
                            break;
                        default:
                            break;
                    }
                    break;
            }
        }
        return new PomData(parent, properties, dependencies, depMgmt);

        } catch (Exception e) {
            throw new NobException("Couldn't parse pom file for " + path.getFileName(), e);
        }
    }
}

class PomData {
    public Dependency parent;
    public Map<String, String> properties;
    public List<Dependency> deps;
    public Map<Dependency, String> managements;

    public PomData(Dependency parent, Map<String, String> properties, List<Dependency> deps, Map<Dependency, String> managements) {
        this.parent = parent;
        this.properties = properties;
        this.deps = deps;
        this.managements = managements;
    }

    public String toString() {
        return "PomData{" +
            "parent=" + parent +
            ", properties=" + properties +
            ", deps=" + deps +
            ", managements=" + managements +
            '}';
    }
}

class DownloadSubscriber implements HttpResponse.BodySubscriber<Path> {
    String name;
    long totalSize;
    long received;
    HttpResponse.BodySubscriber<Path> downstream;

    public DownloadSubscriber(String name, long totalSize, HttpResponse.BodySubscriber<Path> downstream) {
        this.name = name;
        this.totalSize = totalSize;
        this.downstream = downstream;
    }

    public void onComplete() {
        System.out.println("\r" + name + " [====================] 100%");
        downstream.onComplete();
    } 

    public void onError(Throwable throwable) {
        downstream.onError(throwable);
    }

    public void onNext(List<ByteBuffer> buffers) {
        received += buffers.stream().mapToLong(ByteBuffer::remaining).sum();
        int pct = (int)(received * 100 / totalSize);
        int filled = pct * 20 / 100;
        String bar = "=".repeat(filled) + "-".repeat(20 - filled);
        System.out.print("\r" + name + " [" + bar + "] " + pct + "%");
        downstream.onNext(buffers);
    }

    public void onSubscribe(Flow.Subscription subscription) {
        downstream.onSubscribe(subscription);
    }

    public CompletionStage<Path> getBody() {
        return downstream.getBody();
    }
}
