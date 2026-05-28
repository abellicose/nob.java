/* ======================================
 * File: JarConfig.java
 * Date: 2026-05-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.nio.file.Path;

class JarConfig {
    Path classes = Path.of("build/classes/");
    Path out = Path.of("build/jars");
    String name = "out.jar";

    String mainClass = null;
    List<String> classpath = new ArrayList<>();
    Map<String, String> mfAttribs = new HashMap<>();

    void addToClasspath(String... args) {
        classpath.addAll(List.of(args));
    }

    void addMfAttributes(String... args) {
        for (int i = 0; i < args.length; i+=2) {
            mfAttribs.put(args[i], args[i+1]);
        }
    }

    protected String buildManifest() {
        StringBuilder builder = new StringBuilder("Manifest-Version: 1.0\n");

        if (mainClass != null) builder.append("Main-Class: " + mainClass + "\n");
        if (!classpath.isEmpty()) builder.append("Class-Path: " + String.join(" ", classpath) + "\n");
        mfAttribs.forEach((k, v) -> builder.append(k + ": " + v + "\n"));
        return builder.toString();
    }
}

