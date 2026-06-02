/* ======================================
 * File: Nob.java
 * Date: 2026-05-31
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob;

import nob.build.Graph;
import nob.build.CompileTask;
import nob.build.PackageTask;
import nob.build.Context;
import java.util.function.Consumer;

public class Nob {
    public String sourceDir     = "src/";
    public String buildDir      = "build/";
    public String classesDir    = "classes/";   // Relative to buildDir
    public String libsDir       = "build/libs"; // Relative to project root
    public String mainClass     = null;
    public String packageName   = null;
    public String jarName       = "out.jar";

    public Context ctx          = null;
    public Graph graph          = new Graph();

    public void init() {
        try {
            if (ctx != null) return;

            ctx = Context.load(this);
            graph.register(new CompileTask());
            graph.register(new PackageTask());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> graph.run(ctx)));
        } catch (NobException e) {
            handle(e);
        }
    }

    public void compile(Consumer<CompileConfig> consumer) {
        init();
        try {
            consumer.accept(ctx.compileConfig);
            graph.enqueue("compile");
        } catch (NobException e) {
            handle(e);
        }
    }

    public void compile() {
        init();
        try {
            graph.enqueue("compile");
        } catch (NobException e) {
            handle(e);
        }
    }

    private void handle(NobException e) {
        System.err.println(e.getMessage());
        if (e.getCause() != null) e.getCause().printStackTrace();
        System.exit(1);
    }

    public void jar() {
        init();
        graph.enqueue("package");
    }

    public void jar(Consumer<JarConfig> consumer) {
        init();
        try {
            consumer.accept(ctx.jarConfig);
            graph.enqueue("package");
        } catch (NobException e) {
            handle(e);
        }
    }
}

