/* ======================================
 * File: Compile.java
 * Date: 2026-05-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.List;
import java.util.ArrayList;
import java.lang.ProcessBuilder;
import java.nio.file.StandardCopyOption;

import static nob.Util.*;

class Compile {
    public static void compile(Consumer<CompileConfig> consumer) {
        CompileConfig config = new CompileConfig();
        consumer.accept(config);
        compile(config);
    }

    public static void compile(CompileConfig cfg) {
        try {
            NOBmkdirIfNotExists(cfg.out);

            List<String> files = Graph.getFilesToCompile(cfg.src).stream().map(Path::toString).toList();

            if (files.isEmpty()) {
                System.out.println("No source files found!");
                System.exit(1);
            }

            List<String> cmd = new ArrayList<>(List.of("javac"));

            // --add-modules
            if (!cfg.modules.isEmpty()) {
                cmd.add("--add-modules");
                cmd.addAll(cfg.modules);
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

            new ProcessBuilder(cmd)
                .inheritIO()
                .start()
                .waitFor();

            System.out.println("Compilation succeeded");

            if (!cfg.dirsToInclude.isEmpty()) {
                System.out.println("Moving Dirs to Include");
                Path srcPathFinal = cfg.src;

                cfg.dirsToInclude.forEach(dir -> {
                    Path src = srcPathFinal.resolve(dir);
                    try {
                        Files.walk(src).forEach(file -> {
                            try {
                                Path relative = src.getParent().relativize(file);
                                Files.copy(file, cfg.out.resolve(relative), StandardCopyOption.REPLACE_EXISTING);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });                    
                    } catch (Exception e) { e.printStackTrace(); };
                });
            }
        } catch (Exception e) {
            System.out.println("Something went wrong while trying to compile the files");
            e.printStackTrace();
        }
    }
}

