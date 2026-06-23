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
            curr.collectLeaves(changed);
        } else {
            FileTree.diff(ctx.cachedTree, curr, diff);
        }

        ctx.cachedTree = curr;
        return diff;
    }

    // deletes deleted class files, opens recompiled class files, scans them, creates the dep tree and cache stuff, saves them
    // finds the method signatures that changed, recompiles classes (saves to out) that used the old method signature.
    // even i dont remember how this works anymore
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

// FileSystem cache, exists so that we dont need to call os to walk again and again and again
// Also a MerkleTree
public class FileEntry implements Serializable {
    public String pathStr;
    public long mTime;
    private Set<String> children;

    transient public Path path;

    public FileEntry(Path path, long mTime, Set<String> children) {
        this.pathStr = path.toString();
        this.mTime =  mTime;
        this.children = children;
        this.path = path;
    }
    
    public Path path() {
        if (path == null) path = Path.of(pathStr);
        return path;
    }

    public boolean isFile() { 
        return children == null; 
    }

    public Set<String> children() {
        if (children == null) return Set.of();
        return children;
    }

    public static FileEntry file(Path path, long mTime) {
        return new FileEntry(path, mTime, null);
    }

    public static FileEntry dir(Path path, long mTime) {
        return new FileEntry(path, mTime, new HashSet<>());
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

        for (String childPath: entry.children()) {
            collectLeaves(childPath, result);
        }
    }

    public static FileTree build(String root) {
        Map<String, FileEntry> entries = new HashMap<>();
        Deque<FileEntry> stack = new ArrayDeque<>();

        try {
            Files.walkFileTree(Path.of(root), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    FileEntry d = FileEntry.dir(dir, attrs.lastModifiedTime().toMillis());
                    stack.push(d);
                    entries.put(dir.toString(), d);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    long mTime = attrs.lastModifiedTime().toMillis();
                    FileEntry parent = stack.peek();
                    if (mTime > parent.mTime) {
                        parent.mTime = mTime;
                    }
                    FileEntry f = FileEntry.file(file, mTime);
                    entries.put(file.toString(), f);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException ex) throws IOException {
                    if (stack.size() > 1) {
                        FileEntry child = stack.pop();
                        FileEntry parent = stack.peek();
                        if (child.mTime > parent.mTime) {
                            parent.mTime = child.mTime; 
                        }
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
        deque.addAll(curr.root.children());
        while (!deque.isEmpty()) {
            String currStr = deque.pop();
            FileEntry child = curr.get(currStr); // cannot be null
            FileEntry twin = prev.remove(currStr); // can be null

            if (twin == null || twin.mTime != child.mTime) { // added/changed
                if (child.isFile()) {
                    diff.changed().add(currStr);
                } else {
                    deque.addAll(child.children());
                }
            } 
        }
        
        diff.removed().addAll(prev.entries.keySet());
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
