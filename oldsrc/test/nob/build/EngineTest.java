/* ======================================
 * File: EngineTest.java
 * Date: 2026-08-16
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.lang.reflect.Type;

public class EngineTest {

    @Test
    public void happypath_taskwriteslocalbuckets() {
        Engine engine = new Engine();
        engine.registerPlugin(new FakePluginA());
        engine.commit();

        BucketId bucketId = new BucketId("a:output");
        assertTrue(engine.bucketRegistry.containsKey(bucketId));
    }

    @Test
    public void foreignwritetoexistingbucket() {
        Engine engine = new Engine();
        engine.registerPlugin(new FakePluginA());
        engine.registerPlugin(new FakePluginB());
        engine.commit();

        BucketId bucketId = new BucketId("a:output");
        TaskId readerTaskId = new TaskId("b:taskb");
        assertTrue(engine.bucketRegistry.get(bucketId).readers().contains(readerTaskId));
    }

    @Test
    public void foreignwritetofakebucket_throws() {
        Engine engine = new Engine();
        engine.registerPlugin(new FakePluginA());
        engine.registerPlugin(new FakePluginC());

        assertThrows(NobException.class, () -> engine.commit());
    }

    @Test
    public void twotaskscreatessamebucket_throws() {
        Engine engine = new Engine();
        engine.registerPlugin(new FakePluginD());

        assertThrows(NobException.class, () -> engine.commit());
    }

    @Test
    public void localbucketnameprefixedwithpluginid_throws() {
        Engine engine = new Engine();
        engine.registerPlugin(new FakePluginE());

        assertThrows(NobException.class, () -> engine.commit());
    }

    @Test
    public void circulardependency_throws() {
        Engine engine = new Engine();
        engine.registerPlugin(new FakePluginF());
        engine.registerPlugin(new FakePluginG());

        // printError(engine);
        assertThrows(NobException.class, () -> engine.commit());
    }

    private void printError(Engine engine) {
        try {
            engine.commit();
        } catch (NobException e) {
            System.out.println("THROWN: " + e.getMessage());
        }
    }
}

class FakePluginA implements Plugin {
    public String id() {
        return "a"; 
    }

    public void apply(Registry r) {
        r.registerBucket("output", String.class);
    }
}

class FakePluginB implements Plugin {
    public String id() {
        return "b";
    }

    public void apply(Registry r) {
        r.registerTask(TaskB.class);
    }
}

class TaskB implements Task {
    public String id() {
        return "taskb";
    }

    public List<String> reads() {
        return List.of("a:output");
    }

    public void execute(ExecutionContext ctx) {}
}

class FakePluginC implements Plugin {
    public String id() {
        return "c";
    }

    public void apply(Registry r) {
        r.registerTask(TaskC.class);
    }
}

class TaskC implements Task {
    public String id() {
        return "taskc";
    }

    public Map<String, Type> writes() {
        return Map.of(
            "a:fake", String.class
        );
    }

    public void execute(ExecutionContext ctx) {}
}

class FakePluginD implements Plugin {
    public String id() {
        return "d";
    }

    public void apply(Registry r) {
        r.registerTask(TaskD1.class);
        r.registerTask(TaskD2.class);
    }
}

class TaskD1 implements Task {
    public String id() {
        return "taskd1";
    }

    public Map<String, Type> writes() {
        return Map.of(
            "collide", String.class
        );
    }

    public void execute(ExecutionContext ctx) {}
}

class TaskD2 implements Task {
    public String id() {
        return "taskd2";
    }

    public Map<String, Type> writes() {
        return Map.of(
            "collide", String.class
        );
    }

    public void execute(ExecutionContext ctx) {}
}

class FakePluginE implements Plugin {
    public String id() {
        return "e";
    }

    public void apply(Registry r) {
        r.registerTask(TaskE.class);
    }
}

class TaskE implements Task {
    public String id() {
        return "taske";
    }

    public Map<String, Type> writes() {
        return Map.of(
            "e:prefixed", String.class
        );
    }

    public void execute(ExecutionContext ctx) {}
}

class FakePluginF implements Plugin {
    public String id() {
        return "f";
    }

    public void apply(Registry r) {
        r.registerTask(TaskF.class);
    }
}

class TaskF implements Task {
    public String id() {
        return "taskf";
    }

    public Map<String, Type> writes() {
        return Map.of(
            "writes", String.class
        );
    }

    public List<String> reads() {
        return List.of("g:writes");
    }

    public void execute(ExecutionContext ctx) {}
}

class FakePluginG implements Plugin {
    public String id() {
        return "g";
    }

    public void apply(Registry r) {
        r.registerTask(TaskG.class);
    }
}

class TaskG implements Task {
    public String id() {
        return "taskg";
    }

    public Map<String, Type> writes() {
        return Map.of(
            "writes", String.class
        );
    }

    public List<String> reads() {
        return List.of("f:writes");
    }

    public void execute(ExecutionContext ctx) {}
}
