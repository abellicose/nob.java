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
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import java.lang.reflect.Type;

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

            Engine engine = new Engine();
            engine.registerPlugin(new PluginRunsFirst());
            engine.registerPlugin(new PluginRunsSecond());

            engine.commit();
            engine.run("second:task2");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> { 
/*
                System.out.println("HI");
                try {
                    taskPipeline.run(new TaskId("second:task2"), new ExecutionContext());
                } catch (NobException e) {
                    handle(e);
                }
*/
                Logger.stop();
            }));
        } catch (NobException e) {
            handle(e);
            System.exit(1);
        }
    }

    private void handle(NobException e) {
        System.out.println(e.getMessage());
        if (e.getCause() != null) e.getCause().printStackTrace();
    }
}

class PluginRunsFirst implements Plugin {
    public String id() {
        return "first";
    }

    public void apply(Registry registry) {
        registry.registerTask(FirstTask.class);
    }

    public static class FirstTask implements Task {
        public String id() {
            return "task1";
        }

        public Map<String, Type> writes() {
            return Map.of(
                "testValue", String.class
            );
        }

        public void execute(ExecutionContext ctx) {
            Logger.info("From first");
            ctx.addToBucket("testValue", "TESTVALUE");
            Logger.info("ADDED");
        }
    }
}

class PluginRunsSecond implements Plugin {
    public String id() {
        return "second";
    }

    public void apply(Registry registry) {
        registry.registerTask(SecondTask.class);
    }

    public static class SecondTask implements Task {
        public String id() {
            return "task2";
        }

        public List<String> reads() {
            return List.of("first:testValue");
        }

        public void execute(ExecutionContext ctx) {
            Logger.info("From second");
            Bucket<String> bucket = ctx.getBucket("first:testValue", String.class);
            Logger.info("Message from plugin 1: " + bucket.get());
        }
    }
}
