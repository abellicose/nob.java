/* ======================================
 * File: BuildContext.java
 * Date: 2026-05-29
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.cache;

import java.nio.file.Files;
import java.nio.file.Path;
import nob.api.CompileConfig;
import nob.api.ProjectConfig;
import nob.cache.MerkleNode;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import static nob.util.Util.*;

public class BuildContext {
    public Path src = Path.of("src/");
    public Path dest = Path.of("build/");
    public Path out = Path.of("classes/");
    public Path libs = Path.of("build/libs");
    public Path jarOut = Path.of("build/jars");
    public Path cacheFile;
    public String packageName = null;
    public String mainClass = null;
    public String jarName = "out.jar";

    private static final int CACHE_VERSION = 1;
    public MerkleNode merkleCache = null;
    // Methods owned by a class
    public Map<String, Set<String>> methods = new HashMap<>();
    // Methods called by a class
    public Map<String, Set<String>> methodsCalled = new HashMap<>();
    // Methods and their dependencies
    public Map<String, Set<String>> methodCalls = new HashMap<>();

    public BuildContext(ProjectConfig cfg) {
        src = Path.of(cfg.src);
        dest = Path.of(cfg.dest);
        out = dest.resolve(cfg.classes);
        libs = Path.of(cfg.libs);
        cacheFile = dest.resolve("nob.cache");
        packageName = cfg.packageName.replace("\\.", "/");
        mainClass = cfg.mainClass;
        jarName = cfg.jarName;
    }

    @SuppressWarnings("unchecked")
    public static BuildContext load(ProjectConfig cfg) throws Exception {
        cfg.validate();
        BuildContext ctx = new BuildContext(cfg);

        if (!Files.exists(ctx.cacheFile)) return ctx;

        try(ObjectInputStream in = new ObjectInputStream(Files.newInputStream(ctx.cacheFile))) {
            int version = (int) in.readObject();
            if (version != CACHE_VERSION) {
                System.out.println("[nob] Cache Version Mismatch. Triggering Full Rebuild.");
                return ctx;
            }
            ctx.merkleCache = (MerkleNode) in.readObject();
            ctx.methods = (Map<String, Set<String>>) in.readObject();
            ctx.methodsCalled = (Map<String, Set<String>>) in.readObject();
            ctx.methodCalls = (Map<String, Set<String>>) in.readObject();
        } catch (Exception e) {
            System.out.println("[nob] Failed to read cache files.");
            e.printStackTrace();
            System.exit(1);
        }

        return ctx;
    }

    public void save() throws Exception {
        try {
            NOBmkdirIfNotExists(cacheFile.getParent());
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(cacheFile))) {
                out.writeObject(CACHE_VERSION);
                out.writeObject(merkleCache);
                out.writeObject(methods);
                out.writeObject(methodsCalled);
                out.writeObject(methodCalls);
            }
        } catch (Exception e) {
            System.out.println("[nob] Failed to save cache file");
            e.printStackTrace();
        }
    }
}

