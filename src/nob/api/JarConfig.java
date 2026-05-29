/* ======================================
 * File: JarConfig.java
 * Date: 2026-05-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.api;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.nio.file.Path;

public class JarConfig {
    public Path classes = Path.of("build/classes/");
    public Path out = Path.of("build/jars");
    public String name = "out.jar";
    public String mainClass = null;

    public List<String> classpath = new ArrayList<>();
    public Map<String, String> mfAttribs = new HashMap<>();

    public void addToClasspath(String... args) {
        classpath.addAll(List.of(args));
    }

    public void addMfAttributes(String... args) {
        for (int i = 0; i < args.length; i+=2) {
            mfAttribs.put(args[i], args[i+1]);
        }
    }

    public String buildManifest() {
        StringBuilder builder = new StringBuilder("Manifest-Version: 1.0\n");

        if (mainClass != null) builder.append("Main-Class: " + mainClass + "\n");
        if (!classpath.isEmpty()) builder.append("Class-Path: " + String.join(" ", classpath) + "\n");
        mfAttribs.forEach((k, v) -> builder.append(k + ": " + v + "\n"));
        return builder.toString();
    }
}

