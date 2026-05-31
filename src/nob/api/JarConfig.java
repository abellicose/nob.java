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
import nob.cache.BuildContext;

public class JarConfig {
    public Map<String, String> mfAttribs = new HashMap<>();
    public List<String> classpath = new ArrayList<>();

    public void addMfAttributes(String... args) {
        for (int i = 0; i < args.length; i+=2) {
            mfAttribs.put(args[i], args[i+1]);
        }
    }

    public void addToClasspath(String... cp) {
        classpath.addAll(List.of(cp));
    }

    public String buildManifest(BuildContext ctx) {
        StringBuilder builder = new StringBuilder("Manifest-Version: 1.0\n");

        if (ctx.mainClass != null) builder.append("Main-Class: " + ctx.mainClass + "\n");
        if (!classpath.isEmpty()) builder.append("Class-Path: " + String.join(" ", classpath) + "\n");
        mfAttribs.forEach((k, v) -> builder.append(k + ": " + v + "\n"));
        return builder.toString();
    }
}

