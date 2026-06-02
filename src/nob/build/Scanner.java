/* ======================================
 * File: Scanner.java
 * Date: 2026-06-01
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;
import nob.NobException;

public class Scanner {

    static void scan(List<String> changed, List<String> deleted, List<String> out, Context ctx) {
        try {
        // Start Preprocessing Files
        int changedSize = changed.size();
        int deletedSize = deleted.size();
        int totalSize = changedSize + deletedSize;

        Set<String> toRecompile = new HashSet<>(changedSize * 2);
        Map<String, String> classToSource = new HashMap<>(totalSize * 2);

        Set<String> changedNames = new HashSet<>(changedSize * 2);
        Set<String> changedDirs = new HashSet<>(changedSize * 2);
        List<String> changedClasses = new ArrayList<>(changedSize * 2);

        Set<String> deletedNames = new HashSet<>(deletedSize * 2);
        Set<String> deletedDirs = new HashSet<>(deletedSize * 2);
        List<String> deletedClasses = new ArrayList<>(deletedSize * 2);

        deleted.forEach(file -> {
            SourcePath parsed = stripExtension(file, ctx.source.toString());
            deletedNames.add(parsed.extLess);
            deletedDirs.add(parsed.classDir);
        });

        // im deleting classes, but not removing them from ownedMethods, calledMethods, methodDependents and adding those to rebuild
        for (String dir: deletedDirs) {
            try {
                for (Path path: Files.list(ctx.out.resolve(dir)).toList()) {
                    String classFile = path.toString();
                    String sourceName = split(classFile, ctx.out.toString());
                    if (deletedNames.contains(sourceName)) {
                        classToSource.put(classFile, sourceName);
                        deletedClasses.add(classFile);
                    }
                }
            } catch (Exception e) {
                throw new NobException("Could not open deleted files", e);
            }
        }

        changed.forEach(file -> {
            SourcePath parsed = stripExtension(file, ctx.source.toString());
            changedNames.add(parsed.extLess);
            changedDirs.add(parsed.classDir);
        });

        for (String dir: changedDirs) {
            try {
                for (Path path: Files.list(ctx.out.resolve(dir)).toList()) {
                    String classFile = path.toString();
                    String sourceName = split(classFile, ctx.source.toString());
                    if (changedNames.contains(sourceName)) {
                        classToSource.put(classFile, sourceName);
                        changedClasses.add(classFile);
                    }
                }
            } catch (Exception e) {
                throw new NobException("Could not open changed classes", e);
            }
        }
        // End Preprocessing Files

        for (String classFile: deletedClasses) {
            String sourceName = classToSource.get(classFile);
            if (sourceName == null) {
                throw new NobException("Couldn't find source for " + sourceName);
            }

            // we got the main class
            // we get all of the methods of this class, cuz shit didnt change so we dont need to rescan it
            // we remove all of that methods dependents, we get all of the called methods and remove ourselves from their dependents
            try {
                Files.deleteIfExists(Path.of(classFile));
            } catch (Exception ignored){}

            Set<String> methods = ctx.ownedMethods.remove(sourceName);
            if (methods == null)
                continue;

            for (String method: methods) {
                Set<String> deps = ctx.methodDependents.remove(method);
                if (deps == null)
                    continue;
                toRecompile.addAll(deps);
            }

            methods = ctx.calledMethods.remove(sourceName);
            if (methods == null)
                continue;

            for (String method: methods) {
                ctx.methodDependents.computeIfPresent(method, (k, v) -> {
                    v.remove(sourceName);
                    return v;
                });
            }
        }

        Map<String, Set<String>> newOwned = new HashMap<>();
        Map<String, Set<String>> newCalled = new HashMap<>();

        // now to loop over the changed files, read them and update shit
        for (String classFile: changedClasses) {
            Set<String> ownedMethods = new HashSet<>();
            Set<String> calledMethods = new HashSet<>();
            ScanState state = new ScanState(ctx.packageName, ownedMethods, calledMethods);
            parseClass(classFile, state, ctx);

            String sourceName = classToSource.get(classFile);
            if (sourceName == null) {
                throw new NobException("Couldn't find source for " + classFile);
            }

            newOwned.computeIfAbsent(sourceName, k -> new HashSet<>()).addAll(ownedMethods);
            newCalled.computeIfAbsent(sourceName, k -> new HashSet<>()).addAll(calledMethods);
        }

        for (Map.Entry<String, Set<String>> entry: newOwned.entrySet()) {
            String name = entry.getKey();
            Set<String> oldMethods = ctx.ownedMethods.get(name);
            if (oldMethods == null)
                continue;

            Set<String> newMethods = entry.getValue();

/*
            for (String method: newMethods) {
                !oldMethods.remove(method); // !oldMethods.contains(method), new method
            }
*/
            oldMethods.removeAll(newMethods);

            for (String method: oldMethods) { // deleted methods
                Set<String> deps = ctx.methodDependents.remove(method);
                if (deps == null)
                    continue;

                toRecompile.addAll(deps);
            }
        }

        for (Map.Entry<String, Set<String>> entry: newCalled.entrySet()) {
            String name = entry.getKey();
            Set<String> oldMethods = ctx.calledMethods.get(name);
            if (oldMethods == null)
                continue;

            Set<String> newMethods = entry.getValue();
            for (String method: newMethods) {
                if (!oldMethods.remove(method)) { // new call
                    ctx.methodDependents.computeIfAbsent(method, k -> new HashSet<>()).add(name);
                }
            }

            for (String method: oldMethods) {  // deleted calls
                ctx.methodDependents.computeIfPresent(method, (k, v) -> {
                    v.remove(name);
                    return v;
                });
            }
        }

        ctx.ownedMethods.putAll(newOwned);
        ctx.calledMethods.putAll(newCalled);
        for (String path: toRecompile) {
            if (!changedNames.contains(path)) {
                out.add(path + ".java");
            }
        }

        } catch (Exception e) {
            throw new NobException("File IO error while scanning files.", e);
        }
    }

    private static void parseClass(String file, ScanState state, Context ctx) {
        Path path = Path.of(file);
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(ctx.out.resolve(path));
        } catch (Exception e) {
            throw new NobException("Could not read class file", e);
        }
        ClassReader cr = new ClassReader(bytes);
        ClassVisitor cv = new ClassScanner(ASM9, state);
        cr.accept(cv, 0);
    }

    private static String split(String str, String source) {
        int extStart = str.length();
        for(int i = str.length() - 1; i >= 0; i--) {
            char c = str.charAt(i);
            if (c == '.') extStart = i;
            if (c == '$') extStart = i;
            if (c == '/') break;
        }
        return str.substring(source.length() + 1, extStart);
    }

    // paths are like nob/build/Scanner.java
    // I walk from back, return everything from beginning until that .
    private static SourcePath stripExtension(String str, String source) {
        int start = source.length() + 1;
        int extStart = str.length();
        int dirEnd = str.length();
        for (int i = extStart - 1; i >= 0; i--) {
            char c = str.charAt(i);
            if (c == '.') extStart = i; 
            if (c == '/') { dirEnd = i; break; }
        }
        return new SourcePath(str.substring(start, dirEnd), str.substring(start, extStart));
    }

    record SourcePath(String classDir, String extLess) {}
}

class ScanState {
    String className;
    String packageName;
    Set<String> ownedMethods;
    Set<String> calledMethods;

    ScanState(String packageName, Set<String> ownedMethods, Set<String> calledMethods) {
        this.packageName = packageName;
        this.ownedMethods = ownedMethods;
        this.calledMethods = calledMethods;
    }
}

class ClassScanner extends ClassVisitor {
    ScanState state;
    public ClassScanner(int api, ScanState state) {
        super(api);
        this.state = state;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        // track class name
        state.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        // return custom method visitor if method aint part of this class
        state.ownedMethods.add(state.className + "/" + name + descriptor);
        MethodScanner scanner = new MethodScanner(api, state);
        return scanner;
    }
}

class MethodScanner extends MethodVisitor {
    ScanState state;
    public MethodScanner(int api, ScanState state) {
        super(api);
        this.state = state;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        // track method calls in this class
        if (owner.startsWith(state.packageName) && !owner.equals(state.className)) {
            state.calledMethods.add(owner + "/" + name + descriptor);
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }
}
