/* ======================================
 * File: CompileCache.java
 * Date: 2026-07-23
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class CompileCache implements Serializable {
    FileTree cachedTree = null;
    FileTree newTree    = null;
    Map<String, String> binaryToSource = new HashMap<>();
    Map<String, Set<String>> sourceToBinaries = new HashMap<>();
    Map<String, Set<String>> sourceToDeclaredMethods = new HashMap<>();            // source (class and subclasses) and methods it owns
    Map<String, Set<String>> sourceToCalledMethods = new HashMap<>();           // source (class and subclasses) and methods it calls
    Map<String, Set<String>> methodToCallerBinaries = new HashMap<>();        // methods and specific classes that depend on those methods
    
    public void load(Path cacheFile) {
        if (!Files.exists(cacheFile)) return;

        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(cacheFile))) {
            cachedTree = (FileTree) in.readObject();
            binaryToSource = (Map<String, String>) in.readObject();
            sourceToBinaries = (Map<String, Set<String>>) in.readObject();
            sourceToDeclaredMethods = (Map<String, Set<String>>) in.readObject();
            sourceToCalledMethods = (Map<String, Set<String>>) in.readObject();
            methodToCallerBinaries = (Map<String, Set<String>>) in.readObject();
        } catch (Exception e) {
            Logger.warn("Could not read cache, commencing full recompile");
            e.printStackTrace();
        }
    }

    public void save(Path cacheFile) {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(cacheFile))) {
            out.writeObject(cachedTree);
            out.writeObject(binaryToSource);
            out.writeObject(sourceToBinaries);
            out.writeObject(sourceToDeclaredMethods);
            out.writeObject(sourceToCalledMethods);
            out.writeObject(methodToCallerBinaries);
        } catch (Exception e) {
            Logger.error("Could not write cache");
            e.printStackTrace();
        }
    }
}

