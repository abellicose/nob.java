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
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.StringBuilder;

public class Context {
    Path source       = Path.of("src/");
    Path build        = Path.of("build/");
    Path out          = Path.of("build/classes");
    Path libs         = Path.of("build/libs");
    Path jarOut       = Path.of("build/jars");
    Path cacheFile    = Path.of("build/nob.cache");

    public Path globalCache  = Path.of(System.getProperty("user.home"), ".m2", "repository");

    String packageName  = null;
    String mainClass    = null;
    String jarName      = "out.jar";
    
    // merkle cache and shit here
    MerkleNode merkleCache = null;
    Map<String, Set<String>> ownedMethods = new HashMap<>();
    Map<String, Set<String>> calledMethods = new HashMap<>();
    Map<String, Set<String>> methodDependents = new HashMap<>();

    public CompileConfig compileConfig = new CompileConfig();
    public JarConfig jarConfig = new JarConfig();

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
                ctx.merkleCache = (MerkleNode) in.readObject();
                ctx.ownedMethods = (Map<String, Set<String>>) in.readObject();
                ctx.calledMethods = (Map<String, Set<String>>) in.readObject();
                ctx.methodDependents = (Map<String, Set<String>>) in.readObject();
            } catch (Exception e) {
                System.out.println("[nob] Could not read cache, full recompile.");
            }
        }

        return ctx;
    }

    public void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(cacheFile))) {
            out.writeObject(merkleCache);
            out.writeObject(ownedMethods);
            out.writeObject(calledMethods);
            out.writeObject(methodDependents);
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

