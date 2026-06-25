/* ======================================
 * File: CompileTask.java
 * Date: 2026-06-02
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Stack;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.function.Consumer;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import java.io.IOException;
import nob.NobException;
import nob.Task;

import static org.objectweb.asm.Opcodes.*;

public class CompileTask implements Task {
    public String id() {
        return "compile";
    }

    public List<String> dependsOn() { 
        return List.of("resolve"); 
    }

    public void execute(Context ctx) {
        DiffResult diff = diff(ctx);

        List<String> toRecompile = diff.changed();
        List<String> toDelete = diff.deleted();
        Logger.info("Files to delete: " + toDelete);
        int n = 0;

        while (!toRecompile.isEmpty()) {
            Logger.info("Files to recompile: " + toRecompile);
            runJavac(toRecompile, ctx);
            List<String> out = new ArrayList<>(toRecompile.size());
            scan(toRecompile, toDelete, out, ctx);
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

    void runJavac(List<String> files, Context ctx) {
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
        cmd.add("-proc:none");
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

    public DiffResult diff(Context ctx) {
        List<String> changed = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        DiffResult diff = new DiffResult(changed, removed);

        FileTree curr = FileTree.build(ctx.source.toString());
        System.out.println(curr);

        if (ctx.cachedTree == null) {
            curr.collectLeaves(ctx.source.toString(), changed);
        } else {
            FileTree.diff(ctx.cachedTree, curr, diff);
        }

        ctx.newTree = curr;
        return diff;
    }

    // deletes deleted class files, opens recompiled class files, scans them, creates the dep tree and cache stuff, saves them
    // finds the method signatures that changed, recompiles classes (saves to out) that used the old method signature.
    // even i dont remember how this works anymore
    private void scan(List<String> changed, List<String> deleted, List<String> out, Context ctx) {
        Set<String> toRecompile = new HashSet<>(changed.size() * 2);

        // delete deleted class files, update dependencies
        deleted.forEach(path -> {
            ctx.cachedTree.forEachLeaf(path, leaf -> {
                SourcePath parsed = stripExtension(leaf);
                String source = parsed.extLess;
                // TODO: would this be null, not really. ill ball for this
                // remove the source binary, loop through them and delete .class
                for (String binary: ctx.sourceToBinaries.remove(source)) { 
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
                        toRecompile.add(split(source));
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
            String source = parsed.extLess;
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

            for (String deletedSrc: prev) { // deleted methods
                Set<String> deps = ctx.sourceToBinaries.remove(deletedSrc);
                if (deps == null)
                    continue;

                for (String dep: deps) {
                    toRecompile.add(split(dep));
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

    private void parseClass(String file, ScanState state, Context ctx) {
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

    private String split(String str) {
        int extStart = str.length();
        for(int i = str.length() - 1; i >= 0; i--) {
            char c = str.charAt(i);
            if (c == '.') extStart = i;
            if (c == '$') extStart = i;
            if (c == '/') break;
        }
        return str.substring(0, extStart);
    }

    // paths are like nob/build/Scanner.java
    // I walk from back, return everything from beginning until that .
    private SourcePath stripExtension(String str) {
        int extStart = str.length();
        int dirEnd = str.length();
        for (int i = extStart - 1; i >= 0; i--) {
            char c = str.charAt(i);
            if (c == '.') extStart = i; 
            if (c == '/') { dirEnd = i; break; }
        }
        return new SourcePath(str.substring(0, dirEnd), str.substring(0, extStart));
    }

    record SourcePath(String classDir, String extLess) {}
}

// FileSystem cache, exists so that we dont need to call os to walk again and again and again
// Also a MerkleTree
class FileEntry implements Serializable {
    public String pathStr;
    public long mTime;
    public Set<String> children;

    public FileEntry(String pathStr, long mTime, Set<String> children) {
        this.pathStr = pathStr;
        this.mTime =  mTime;
        this.children = children;
    }
    
    public void addChild(String path) {
        children.add(path);
    }

    public boolean isFile() { 
        return children == null; 
    }

    public static FileEntry file(String pathStr, long mTime) {
        return new FileEntry(pathStr, mTime, null);
    }

    public static FileEntry dir(String pathStr, long mTime) {
        return new FileEntry(pathStr, mTime, new HashSet<>());
    }

    @Override
    public String toString() {
        return (isFile() ? "File" : "Dir") + "[" + pathStr + ", mTime=" + mTime + ", children=" + children + "]";
    }
}

class FileTree implements Serializable {
    public FileEntry root;
    private Map<String, FileEntry> entries = new HashMap<>();

    public FileTree(FileEntry root, Map<String, FileEntry> entries) {
        this.root = root;
        this.entries = entries;
    }

    public boolean exists(String entry) {
        return entries.containsKey(entry);
    }

    public FileEntry get(String entry) {
        return entries.get(entry);
    }

    public FileEntry remove(String entry) {
        return entries.remove(entry);
    }

    public void collectLeaves(String path, List<String> result) {
        FileEntry entry = entries.get(path);
        if (entry.isFile()) {
            result.add(path);
            return;
        }

        for (String childPath: entry.children) {
            collectLeaves(childPath, result);
        }
    }

    public void forEachLeaf(String path, Consumer<String> action) {
        FileEntry entry = get(path);
        if (entry.isFile()) {
            action.accept(path);
            return;
        }
        for (String child : entry.children) {
            forEachLeaf(child, action);
        }
    }

    public static FileTree build(String root) {
        Map<String, FileEntry> entries = new HashMap<>();
        Deque<FileEntry> stack = new ArrayDeque<>();
        int offset = root.length() + 1;

        try {
            Files.walkFileTree(Path.of(root), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    String path = dir.toString();
                    if (!path.equals(root)) {
                        path = path.substring(offset, path.length());
                    }
                    FileEntry d = FileEntry.dir(path, attrs.lastModifiedTime().toMillis());
                    stack.push(d);
                    entries.put(path, d);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String path = file.toString();
                    if (!path.endsWith(".java")) return FileVisitResult.CONTINUE;

                    path = path.substring(offset, path.length() - 5);
                    long mTime = attrs.lastModifiedTime().toMillis();
                    FileEntry parent = stack.peek();
                    if (mTime > parent.mTime) {
                        parent.mTime = mTime;
                    }
                    FileEntry f = FileEntry.file(path, mTime);
                    entries.put(path, f);
                    parent.addChild(path);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException ex) throws IOException {
                    if (stack.size() > 1) {
                        String path = dir.toString();
                        if (!path.equals(root)) {
                            path = path.substring(offset, path.length());
                        }
                        FileEntry child = stack.pop();
                        FileEntry parent = stack.peek();
                        if (child.mTime > parent.mTime) {
                            parent.mTime = child.mTime; 
                        }
                        parent.addChild(path);
                    }  
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            throw new NobException("File IO error while building node.", e);
        }

        return new FileTree(stack.pop(), entries);
    }

    static void diff(FileTree prev, FileTree curr, DiffResult diff) {
        if (prev.root.mTime == curr.root.mTime) return;

        Deque<String> deque = new ArrayDeque<>();
        deque.addAll(prev.root.children);

        while (!deque.isEmpty()) {
            String path = deque.pop();
            FileEntry oldChild = prev.get(path);
            FileEntry newChild = curr.get(path);

            if (newChild == null) { // removed
                diff.deleted().add(path);
            } else if (oldChild.mTime != newChild.mTime) { // changed
                if (newChild.isFile()) {
                    diff.changed().add(path);
                } else {
                    deque.addAll(oldChild.children);
                }
            }
        }

        for (Map.Entry<String, FileEntry> entry: curr.entries.entrySet()) {
            if (!prev.entries.containsKey(entry.getKey()) && entry.getValue().isFile()) {
                diff.changed().add(entry.getKey());
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FileTree[root=").append(root).append("]\n");
        buildString(sb, root.pathStr, 0);
        return sb.toString();
    }

    private void buildString(StringBuilder sb, String path, int depth) {
        sb.append("  ".repeat(depth))
            .append(path)
            .append("\n");
        Set<String> children = entries.get(path).children;
        if (children != null) {
            for (String child : children) {
                buildString(sb, child, depth + 1);
            }
        }
    }
}

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
