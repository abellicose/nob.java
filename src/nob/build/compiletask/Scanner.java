/* ======================================
 * File: Scanner.java
 * Date: 2026-07-01
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.NobException;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ClassReader;

import static org.objectweb.asm.Opcodes.*;

public class Scanner {
    // Everything in out is basically files the user forgot to update after
    // editing a file. We could throw an error but we'll still let this happen.
    // This wasn't an original idea but this would allow us to continue in case
    // the program or some external program decided to generate code by itself.
    // Probably not but i can dream.
    static void scan(List<String> changed, List<String> deleted, List<String> out, Context ctx) {
        Set<String> toRecompile = new HashSet<>(changed.size() * 2);

        // delete deleted class files, update dependencies
        deleted.forEach(path -> {
            ctx.cachedTree.forEachLeaf(path, leaf -> {
                SourcePath parsed = stripExtension(leaf);
                String source = parsed.extLess();
                // TODO: would this be null, not really. ill ball for this
                for (String binary: ctx.sourceToBinaries.remove(source)) { 
                    ctx.binaryToSource.remove(binary);
                    try {
                        Files.deleteIfExists(ctx.out.resolve(binary));
                    } catch (Exception e) {
                        Logger.debug("Couldn't delete " + binary);
                    }
                }

                Set<String> methods = ctx.sourceToDeclaredMethods.remove(source);
                if (methods == null) 
                    return; // if it has no methods, then it calls none either, no deps can return from here

                for (String method: methods) {
                    Set<String> deps = ctx.methodToCallerBinaries.remove(method);
                    if (deps == null) {
                        return;
                    }
                    for (String dep: deps) {
                        toRecompile.add(ctx.binaryToSource.get(dep));
                    }
                }

                methods = ctx.sourceToCalledMethods.remove(source);
                if (methods == null) 
                    return; // if it calls no methods, no methods have it as their deps, return

                for (String method: methods) {
                    ctx.methodToCallerBinaries.computeIfPresent(method, (k, v) -> {
                        v.remove(source);
                        return v;
                    });
                }
            });
        });

        Map<String, Set<String>> currMethods = new HashMap<>();
        Map<String, Set<String>> currCalls = new HashMap<>();

        // now loop over the changed files, read them
        changed.forEach(path -> {
            SourcePath parsed = stripExtension(path); // extensionless identifier, directory it's in
            String source = parsed.extLess();
            for (String binary: ctx.sourceToBinaries.get(source)) {
                Set<String> methods = new HashSet<>();
                Set<String> calls = new HashSet<>();
                ScanState state = new ScanState(ctx.packageName, methods, calls);
                parseClass(binary, state, ctx);
                currMethods.computeIfAbsent(source, k -> new HashSet<>()).addAll(methods);
                currCalls.computeIfAbsent(source, k -> new HashSet<>()).addAll(calls);
            }
        });

        // update shit
        for (Map.Entry<String, Set<String>> entry: currMethods.entrySet()) {
            String source = entry.getKey();
            Set<String> prev = ctx.sourceToDeclaredMethods.get(source);
            if (prev == null)
                continue;

            Set<String> curr = entry.getValue();
            prev.removeAll(curr);

            for (String deletedMethod: prev) { // deleted methods
                Set<String> deps = ctx.methodToCallerBinaries.remove(deletedMethod);
                ;
                if (deps == null)
                    continue;

                for (String dep: deps) {
                    toRecompile.add(ctx.binaryToSource.get(dep));
                }
            }
        }

        for (Map.Entry<String, Set<String>> entry: currCalls.entrySet()) {
            String source = entry.getKey();
            Set<String> prev = ctx.sourceToCalledMethods.get(source);
            if (prev == null)
                continue;

            Set<String> curr = entry.getValue();
            Set<String> newCalls = new HashSet<>(curr);
            newCalls.removeAll(prev);
            prev.removeAll(curr);

            for (String newMethod: newCalls) {
                ctx.methodToCallerBinaries.computeIfAbsent(newMethod, k -> new HashSet<>()).add(source);
            }

            for (String deletedMethod: prev) { // deleted calls
                ctx.methodToCallerBinaries.computeIfPresent(deletedMethod, (k, v) -> {
                    v.remove(source);
                    return v;
                });
            }
        }

        ctx.sourceToDeclaredMethods.putAll(currMethods);
        ctx.sourceToCalledMethods.putAll(currCalls);
        ctx.cachedTree = ctx.newTree;
        for (String path: toRecompile) {
            out.add(path);
        }
    }

    static void parseClass(String file, ScanState state, Context ctx) {
        Path path = Path.of(file + ".class");
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

    static SourcePath stripExtension(String str) {
        int extStart = str.length();
        int dirEnd = str.length();
        for (int i = extStart - 1; i >= 0; i--) {
            char c = str.charAt(i);
            if (c == '.') extStart = i; 
            if (c == '/') { dirEnd = i; break; }
        }
        return new SourcePath(str.substring(0, dirEnd), str.substring(0, extStart));
    }
}

record SourcePath(String classDir, String extLess) {}
record DiffResult(List<String> changed, List<String> deleted) {}

class ScanState {
    String binaryName; // this gets set from the reader
    String packageName;
    Set<String> currMethods;
    Set<String> currCalls;

    ScanState(String packageName, Set<String> currMethods, Set<String> currCalls) {
        this.packageName = packageName;
        this.currMethods = currMethods;
        this.currCalls = currCalls;
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
        state.binaryName = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        // return custom method visitor if method aint part of this class
        state.currMethods.add(state.binaryName + "/" + name + descriptor);
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
        if (owner.startsWith(state.packageName) && !owner.equals(state.binaryName)) {
            state.currCalls.add(owner + "/" + name + descriptor);
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }
}
