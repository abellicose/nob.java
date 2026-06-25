/* ======================================
 * File: Context.java
 * Date: 2026-05-31
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.CompileConfig;
import nob.JarConfig;
import nob.Nob;
import nob.NobException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.StringBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class Context {
    Path source         = Path.of("src/");
    Path build          = Path.of("build/");
    Path out            = Path.of("build/classes");
    Path libs           = Path.of("build/libs");
    Path jarOut         = Path.of("build/jars");
    Path cacheFile      = Path.of("build/nob.cache");
    Path globalCache    = Path.of(System.getProperty("user.home"), ".m2", "repository");

    String packageName  = null;
    String mainClass    = null;
    String jarName      = "out.jar";
    // TEMP
    List<Path> libJars = new ArrayList<>();
    
    // cache and shit here
    FileTree cachedTree = null;
    FileTree newTree    = null;
    Map<String, String> binaryToSource = new HashMap<>();
    Map<String, Set<String>> sourceToBinaries = new HashMap<>();
    Map<String, Set<String>> sourceToDeclaredMethods = new HashMap<>();            // source (class and subclasses) and methods it owns
    Map<String, Set<String>> sourceToCalledMethods = new HashMap<>();           // source (class and subclasses) and methods it calls
    Map<String, Set<String>> methodToCallerBinaries = new HashMap<>();        // methods and specific classes that depend on those methods

    ExecutorService cpuPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
    ExecutorService ioPool = Executors.newFixedThreadPool(50);

    // gotta kick these out
    public CompileConfig compileConfig = new CompileConfig();
    public JarConfig jarConfig = new JarConfig();

    public List<Dependency> deps = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public static Context load(Nob nob) {
        if (nob.packageName == null) {
            throw new NobException("Missing value for Nob.packageName");
        }

        Context ctx = new Context();
        ctx.source =  Path.of(nob.sourceDir);
        ctx.build = Path.of(nob.buildDir);
        ctx.out =  ctx.build.resolve(nob.classesDir);
        ctx.libs = Path.of(nob.libsDir);
        ctx.jarOut = ctx.build.resolve("jars/");
        ctx.cacheFile = ctx.build.resolve("nob.cache");
        ctx.packageName = nob.packageName;
        ctx.mainClass = nob.mainClass;
        ctx.jarName = nob.jarName;

        if (Files.exists(ctx.cacheFile)) {
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(ctx.cacheFile))) {
                ctx.cachedTree = (FileTree) in.readObject();
                ctx.binaryToSource = (Map<String, String>) in.readObject();
                ctx.sourceToBinaries = (Map<String, Set<String>>) in.readObject();
                ctx.sourceToDeclaredMethods = (Map<String, Set<String>>) in.readObject();
                ctx.sourceToCalledMethods = (Map<String, Set<String>>) in.readObject();
                ctx.methodToCallerBinaries = (Map<String, Set<String>>) in.readObject();
            } catch (Exception e) {
                System.out.println("[nob] Could not read cache, full recompile.");
                e.printStackTrace();
            }
        }

        return ctx;
    }

    public void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(cacheFile))) {
            out.writeObject(cachedTree);
            out.writeObject(binaryToSource);
            out.writeObject(sourceToBinaries);
            out.writeObject(sourceToDeclaredMethods);
            out.writeObject(sourceToCalledMethods);
            out.writeObject(methodToCallerBinaries);
        } catch (Exception e) {
            System.out.println("[nob] Could not write cache.");
            e.printStackTrace();
        }
    }

    public String manifest() {
        StringBuilder sb = new StringBuilder();
        sb.append("Manifest-Version: 1.0\n");
        if (mainClass != null) sb.append("Main-Class: " + mainClass + "\n");
        jarConfig.mfAttribs.forEach((k, v) -> sb.append(k + ": " + v + "\n"));
        return sb.toString();
    }
}

