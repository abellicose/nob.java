/* ======================================
 * File: Compile.java
 * Date: 2026-05-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

import static nob.Util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.List;
import java.util.ArrayList;
import java.lang.ProcessBuilder;
import java.nio.file.StandardCopyOption;

class Compile {
    public static void compile(Consumer<CompileConfig> consumer) {
        CompileConfig config = new CompileConfig();
        consumer.accept(config);
        compile(config);
    }

    public static void compile(CompileConfig cfg) {
        if (!NOBmkdirIfNotExists(cfg.out)) {
            System.out.println("Failed to make nobPath");
            System.exit(1);
        }

        List<String> files = null;
        try {
            files = Files.walk(cfg.src)
                .filter(path -> path.toString().endsWith(".java"))
                .map(Path::toString).toList();
        } catch (Exception e) {
            System.out.println("Failed to find source files.");
            e.printStackTrace();
            System.exit(1);
        }

        if (files != null && files.isEmpty()) {
            System.out.println("No source files found!");
            System.exit(1);
        }

        List<String> cmd = new ArrayList<>(List.of("javac"));

        // --add-modules
        if (!cfg.modules.isEmpty()) {
            cmd.add("--add-modules");
            cmd.addAll(config.modules);
        }

        // classpath
        if (!cfg.classpath.isEmpty()) {
            cmd.add("-cp");
            cmd.add(".:" + String.join(":", cfg.classpath));
        }

        // build dir
        cmd.add("-d");
        cmd.add(cfg.out.toString());

        cmd.addAll(cfg.flags);
        cmd.add("-Xlint");
        cmd.addAll(files);

        int processStatus = 1;
        try {
            processStatus = new ProcessBuilder(cmd)
                                .inheritIO()
                                .start()
                                .waitFor();
        } catch(Exception e) {
            e.printStackTrace();
        }

        if (processStatus != 0) {
            System.out.println("Compilation command failed");
            System.exit(1);
        }
        System.out.println("Compilation succeeded");

        if (!cfg.dirsToInclude.isEmpty()) {
            System.out.println("Moving Dirs to Include");
            Path srcPathFinal = cfg.src;

            cfg.dirsToInclude.forEach(dir -> {
                Path src = srcPathFinal.resolve(dir);
                try {
                    Files.walk(src).forEach(file -> {
                        Path relative = src.getParent().relativize(file);
                        Files.copy(file, cfg.out.resolve(relative), StandardCopyOption.REPLACE_EXISTING);
                    });
                } catch (Exception e) { e.printStackTrace(); };
            });
        }
    }

}

