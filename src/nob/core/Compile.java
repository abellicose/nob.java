/* ======================================
 * File: Compile.java
 * Date: 2026-05-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.List;
import java.util.ArrayList;
import java.lang.ProcessBuilder;
import java.nio.file.StandardCopyOption;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassReader;
import nob.api.CompileConfig;
import nob.analysis.DependencyBuilder;
import nob.cache.BuildContext;
import nob.util.NobException;
import nob.cache.DiffResult;
import nob.analysis.Scanner;
import nob.cache.Stale;

import static nob.util.Util.*;

public class Compile {

    public static void compile(BuildContext ctx, Consumer<CompileConfig> consumer) {
        CompileConfig config = new CompileConfig();
        consumer.accept(config);
        compile(ctx, config);
    } 

    public static void compile(BuildContext ctx, CompileConfig cfg) {
        try {
            DiffResult diff = Stale.check(ctx);
            System.out.println("[nob] DiffList: " + diff);
            if (diff.changed().isEmpty() && diff.deleted().isEmpty()) { 
                System.out.println("[nob] All files are unchanged."); return; 
            }

            runJavac(diff.changed(), cfg, ctx);

            List<String> secondPass = Scanner.scan(diff, ctx);
            int n = 0; // to ensure infinite loops dont happen, just in case cuz im still building it
            while (!secondPass.isEmpty()) {
                runJavac(secondPass, cfg, ctx);
                secondPass = Scanner.scan(new DiffResult(secondPass, List.of()), ctx);
                if (++n > 4) throw new NobException("SecondPass is running too many times");
            }

            ctx.save();

            System.out.println("[nob] Compilation Finished Successfully");
            
        } catch (Exception e) {
            System.out.println("[nob] Something went wrong while compiling");
            System.out.println("---------------------------------------------");
            e.printStackTrace();
        }
    }

    static void runJavac(List<String> files, CompileConfig cfg, BuildContext ctx) throws Exception {
        List<String> cmd = new  ArrayList<>(List.of("javac"));

        if (!cfg.modules.isEmpty()) {
            cmd.add("--add-modules");
            cmd.addAll(cfg.modules);
        }

        String sep = System.getProperty("path.separator");
        List<String> cpOpts = new ArrayList<>();
        cpOpts.add(ctx.out.toString());
        cpOpts.add(".");
        cpOpts.add(ctx.libs + "/*");
        cpOpts.addAll(cfg.classpath);
        cmd.add("-cp");
        cmd.add(String.join(sep, cpOpts));

        cmd.add("-d");
        cmd.add(ctx.out.toString());

        cmd.addAll(cfg.flags);
        cmd.addAll(files);

        int exit = new ProcessBuilder(cmd).inheritIO().start().waitFor();

        if (exit != 0) throw new NobException("Compilation failed with exit code " + exit);
    }
}

