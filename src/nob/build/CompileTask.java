/* ======================================
 * File: CompileTask.java
 * Date: 2026-06-02
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.NobException;
import java.util.List;
import java.util.ArrayList;

public class CompileTask implements Task {
    public String id() {
        return "compile";
    }

    public void execute(Context ctx) {
        DiffResult diff = Changes.diff(ctx);

        List<String> toRecompile = diff.changed();
        List<String> toDelete = diff.deleted();
        Logger.info("Files to delete: " + toDelete);
        int n = 0;

        while (!toRecompile.isEmpty()) {
            Logger.info("Files to recompile: " + toRecompile);
            runJavac(toRecompile, ctx);
            List<String> out = new ArrayList<>(toRecompile.size());
            Scanner.scan(toRecompile, toDelete, out, ctx);
            toRecompile = out;
            toDelete = List.of();

            if (++n > 4) 
                throw new NobException("Recompile loop executed too many times.");
        }

        if (n == 0) {
            Logger.info("All files are UP-TO-DATE");
        } else {
            ctx.save(); 
        }
    }

    static void runJavac(List<String> files, Context ctx) {
        List<String> cmd = new  ArrayList<>(List.of("javac"));

        if (!ctx.compileConfig.modules.isEmpty()) {
            cmd.add("--add-modules");
            cmd.addAll(ctx.compileConfig.modules);
        }

        String sep = System.getProperty("path.separator");
        List<String> cpOpts = new ArrayList<>();
        cpOpts.add(ctx.out.toString());
        cpOpts.add(".");
        cpOpts.add(ctx.libs.toString() + "/*");
        cpOpts.addAll(ctx.compileConfig.classpath);
        cmd.add("-cp");
        cmd.add(String.join(sep, cpOpts));

        cmd.add("-d");
        cmd.add(ctx.out.toString());

        cmd.addAll(ctx.compileConfig.compilerFlags);
        cmd.addAll(files);

        try {
            int exit = new ProcessBuilder(cmd).inheritIO().start().waitFor();
            if (exit != 0) throw new NobException("javac failed with exit code " + exit);
        } catch (NobException e) {
            throw e;
        } catch (Exception e) {
            throw new NobException("failed to start javac", e);
        }
    }
}

