/* ======================================
 * File: BuildJar.java
 * Date: 2026-05-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

import static Util.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.lang.ProcessBuilder;
import java.util.List;
import java.util.function.Consumer;

public class BuildJar {
    public static void buildJar(Consumer<JarConfig> consumer) {
        JarConfig config = new JarConfig();
        consumer.accept(config);
        return buildJar(config);
    }

    public static void buildJar(JarsConfig cfg) {
        try {
            NOBmkdirIfNotExists(cfg.out);

            int classFileCount = Files.walk(cfg.classes)
                .filter(path -> path.toString().endsWith(".class"))
                .toList().size();
            if (classFileCount == 0) {
                System.out.println("No class files found.");
                System.exit(1);
            }

            cfg.mfAttribs.put("Created-By", "Nob");
            Path manifestFile = Files.createTempFile("MANIFEST", ".MF");
            Files.writeString(manifestFile, cfg.buildManifest());

            List<String> cmd = new ArrayList<>(
                    List.of("jar", "cfm",
                        cfg.out.resolve(cfg.name).toString(),
                        manifestFile.toString(),
                        "-C", cfg.classes.toString(), ".")
                    );

            int processStatus = new ProcessBuilder(cmd)
                .inheritIO()
                .start()
                .waitFor();

            try {
                Files.delete(manifestFile);
            }
            catch (Exception e) {
                System.out.println("Failed to delete temp manifest file");
                e.printStackTrace();
            }

            if (processStatus != 0) {
                System.out.println("Jar Creation Process Failed For Some Reason. FIGURE IT OUT");
                System.exit(1);
            }
            System.out.println("Jar creation succeeded");

        } catch (Exception e) {
            System.out.println("Error During Jar Building Process");
            e.printStackTrace();
        }
    }
}

