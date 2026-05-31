/* ======================================
 * File: BuildJar.java
 * Date: 2026-05-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.core;

import java.nio.file.Path;
import java.nio.file.Files;
import java.lang.ProcessBuilder;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import nob.api.JarConfig;
import nob.util.NobException;

import static nob.util.Util.*;

public class BuildJar {
    public static void buildJar(Consumer<JarConfig> consumer) {
        JarConfig config = new JarConfig();
        consumer.accept(config);
        buildJar(config);
    }

    public static void buildJar(JarConfig cfg) {
        Path manifestFile = null;
        try {
            NOBmkdirIfNotExists(Path.of(cfg.out));

            manifestFile = createManifest(cfg);

            List<String> cmd = new ArrayList<>(
                    List.of("jar", "cfm",
                        Path.of(cfg.out).resolve(cfg.name).toString(),
                        manifestFile.toString(),
                        "-C", cfg.classes, ".")
                    );

        int exit = new ProcessBuilder(cmd).inheritIO().start().waitFor();
        if (exit != 0) throw new NobException("Compilation failed with exit code " + exit);

        } catch (Exception e) {
            System.out.println("Error During Jar Building Process");
            e.printStackTrace();
        } finally {
            if (manifestFile != null) try {Files.deleteIfExists(manifestFile);} catch (Exception ignored){}
        }
    }

    static Path createManifest(JarConfig cfg) throws Exception {
        Path manifest = Files.createTempFile("MANIFEST", ".MF");
        cfg.mfAttribs.put("Created-By", "Nob");
        Files.writeString(manifest, cfg.buildManifest());
        return manifest;
    }
}

