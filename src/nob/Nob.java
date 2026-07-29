/* ======================================
 * File: Nob.java
 * Date: 2026-05-31
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob;

import nob.build.dependencyresolver.Dependency;
import nob.build.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
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
    public TaskPipeline graph   = new TaskPipeline();

    public static boolean debug = false;

    public void init() {
        try {
            if (ctx != null) return;
            ctx = Context.load(this);
            Service service = new Service();
            PluginPipeline pp = new PluginPipeline(graph, service);

            Plugin java = new Java();
            Plugin base = new Base();
            Plugin resolve = new Resolve();
            Plugin compile = new Compile();
            Plugin pkg = new Pkg();

            pp.register(java);
            pp.register(base);
            pp.register(resolve);
            pp.register(compile);
            pp.register(pkg);

            ctx.deps.add(new Dependency("org.ow2.asm", "asm", "9.10.1"));

            Runtime.getRuntime().addShutdownHook(new Thread(() -> { 
                try {
                    pp.run();
                    graph.run("package$end", ctx);
                } catch (NobException e) {
                    handle(e);
                }
                Logger.stop();
            }));
        } catch (NobException e) {
            handle(e);
            System.exit(1);
        }
    }

    private void handle(NobException e) {
        Logger.error(e.getMessage());
        if (e.getCause() != null) e.getCause().printStackTrace();
    }
}

class Test implements Plugin {
    private final String id;
    private final List<String> deps;

    public Test(String id) {
        this.id = id;
        this.deps = List.of();
    }

    public Test(String id, String... deps) {
        this.id = id;
        this.deps = new ArrayList<>(Arrays.asList(deps));
    }

    public String id() {
        return this.id;
    }

    public List<String> dependsOn() {
        return this.deps;
    }

    public void apply(Registrar reg) {
        Logger.info("Executing testtask");
    }
}

class Base implements Plugin {
    public String id() {
        return "base";
    }

    public void apply(Registrar reg) {
        reg.createLifecycle("resolve");
        reg.createLifecycle("compile");
        reg.createLifecycle("package");

        reg.addAfter("compile", "resolve");
        reg.addAfter("package", "compile");
    }
}

class Java implements Plugin {
    public String id() {
        return "java";
    }

    public List<String> dependsOn() {
        return List.of("resolve", "comp", "pkg");
    }

    public void apply(Registrar reg) {}
}

class Resolve implements Plugin {
    public String id() {
        return "resolve";
    }

    public List<String> dependsOn() {
        return List.of("base");
    }

    public void apply(Registrar reg) {
        Task tsk = new ResolveTask();
        String sig = reg.registerTask(tsk);

        reg.addTo(sig, "resolve");
    }
}



class Pkg implements Plugin {
    public String id() {
        return "pkg";
    }

    public List<String> dependsOn() {
        return List.of("base");
    }

    public void apply(Registrar reg) {
        Task tsk = new PackageTask();
        String sig = reg.registerTask(tsk);

        reg.addTo(sig, "package");
    }
}
