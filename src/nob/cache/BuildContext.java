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
import nob.cache.MerkleNode;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.HashMap;

import static nob.util.Util.*;

public class BuildContext {
    public final Path src;
    public final Path dest;
    public final Path out;
    public final Path libs;
    public final Path cacheFile;
    public final String packageName;

    public MerkleNode merkleCache = null;
    // Methods owned by a class
    public Map<String, List<String>> methods = new HashMap<>();
    // Methods called by a class
    public Map<String, List<String>> methodsCalled = new HashMap<>();
    // Methods and their dependencies
    public Map<String, List<String>> methodCalls = new HashMap<>();

    public BuildContext(CompileConfig cfg) {
        src = Path.of(cfg.src);
        dest = Path.of(cfg.dest);
        out = dest.resolve(cfg.classes);
        libs = dest.resolve(cfg.libs);
        cacheFile = dest.resolve("nob.cache");
        packageName = cfg.packageName.replace("\\.", "/");
    }

    public static BuildContext load(CompileConfig cfg) throws Exception {
        cfg.validate();
        BuildContext ctx = new BuildContext(cfg);

        if (!Files.exists(ctx.cacheFile)) return ctx;

        try(ObjectInputStream in = new ObjectInputStream(Files.newInputStream(ctx.cacheFile))) {
            ctx.merkleCache = (MerkleNode) in.readObject();
            ctx.classToMethods = (Map<String, List<String>>) in.readObject();
            ctx.methodToCallers = (Map<String, List<String>>) in.readObject();
        } catch (Exception e) {
            System.out.println("[nob] Failed to read cache files.");
        }

        return ctx;
    }

    public void save() throws Exception {
        try {
            NOBmkdirIfNotExists(cacheFile.getParent());
            try (ObjectOutputStraem out = new ObjectOutputStream(Files.newOutputStream(cacheFile))) {
                out.writeObject(merkleCache);
                out.writeObject(classToMethods);
                out.writeObject(methodToCallers);
            }
        } catch (Exception e) {
            System.out.println("[nob] Failed to save cache file");
            e.printStackTrace();
        }
    }
}

