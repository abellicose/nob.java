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
import java.util.Stack;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import nob.NobException;

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
        Logger.debug("Files to delete: " + toDelete);
        int n = 0;

        while (!toRecompile.isEmpty()) {
            Logger.debug("Files to recompile: " + toRecompile);
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

    public static DiffResult diff(Context ctx) {
        List<String> changed = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        MerkleNode curr = MerkleNode.build(ctx.source.toString());
        DiffResult diff = new DiffResult(changed, removed);
        if (ctx.merkleCache == null) {
            MerkleNode.collectLeaves(curr, changed);
        } else {
            MerkleNode.diff(ctx.merkleCache, curr, diff);
        }
        ctx.merkleCache = curr;
        return diff;
    }

    private void scan(List<String> changed, List<String> deleted, List<String> out, Context ctx) {
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

    private String split(String str, String source) {
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
    private SourcePath stripExtension(String str, String source) {
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

// Creates a tree that makes it easy and fast to compare and find changed files
// Overengineered? Maybe. I learned. Worth it.
class MerkleNode implements Serializable {
    String path;
    long hash = 0;
    Map<String, MerkleNode> children = new HashMap<>();
    boolean leaf = false;

    public MerkleNode(String path, long hash, Map<String, MerkleNode> children, boolean leaf) {
        this.path = path;
        this.hash = hash;
        this.children = children;
        this.leaf = leaf;
    }

    public MerkleNode(String path, boolean leaf) {
        this.path = path;
        this.leaf = leaf;
    }

    public MerkleNode() {
    }

    @Override
    public String toString() {
        return "MerkleNode{path='" + path + "', hash=" + hash + ", leaf=" + leaf + ", children=" + children.keySet() + "}";
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MerkleNode)) return false;
        return path.equals(((MerkleNode) o).path);
    }

    // hash of a node is the max mtime of its children
    // if child is leaf, hash is its mtime
    static MerkleNode build(String nodeDir) {
        try {
            Path path = Path.of(nodeDir);
            MerkleNode curr = new MerkleNode(nodeDir, Files.isRegularFile(path));
            if (curr.leaf) {
                curr.hash = Files.getLastModifiedTime(path).toMillis();
                return curr;
            }

            long maxHash = 0;
            for (Path childPath: Files.list(path).toList()) {
                String childDir = childPath.toString();
                MerkleNode child = build(childDir);
                curr.children.put(childDir, child);
                if (child.hash > maxHash) 
                    maxHash = child.hash;
            }
            curr.hash = maxHash;
            return curr;
        } catch (Exception e) {
            throw new NobException("File IO error while building node.", e);
        }
    }

    static void diff(MerkleNode prev, MerkleNode curr, DiffResult diff) {
        if (prev.hash == curr.hash) return;

        if (curr.children.size() == 0) {
            diff.changed().add(curr.path);
        }

        for (MerkleNode child: curr.children.values()) {
            MerkleNode twin = prev.children.remove(child.path);
            if (twin == null) { // newly added
                collectLeaves(child, diff.changed());
                continue;
            } 

            if (twin.hash != child.hash) { // changed
                diff(twin, child, diff);
            }
        }

        for (MerkleNode removed: prev.children.values()) { // doesnt exist now
            collectLeaves(removed, diff.deleted());
        }
    }

    static void collectLeaves(MerkleNode node, List<String> leaves) {
        if (node.leaf) {
            leaves.add(node.path);
            return;
        }
        for (MerkleNode child: node.children.values())
            collectLeaves(child, leaves);
    }
}

record DiffResult(List<String> changed, List<String> deleted) {}

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
