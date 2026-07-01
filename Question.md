Question 1 — "Who owns this state?"

For any piece of mutable state, list who writes it and who reads it. The owner is the cluster that does both. State with many writers across unrelated modules has no owner — that's the bug.

Worked example — your five incremental caches in Context:

┌─────────────────────────┬───────────────────────────────┬──────────────────┐
│          Field          │          Written by           │     Read by      │
├─────────────────────────┼───────────────────────────────┼──────────────────┤
│ binaryToSource          │ FileManager, CompileTask.scan │ CompileTask.scan │
├─────────────────────────┼───────────────────────────────┼──────────────────┤
│ sourceToBinaries        │ FileManager, CompileTask.scan │ CompileTask.scan │
├─────────────────────────┼───────────────────────────────┼──────────────────┤
│ sourceToDeclaredMethods │ CompileTask.scan              │ CompileTask.scan │
├─────────────────────────┼───────────────────────────────┼──────────────────┤
│ methodToCallerBinaries  │ CompileTask.scan              │ CompileTask.scan │
├─────────────────────────┼───────────────────────────────┼──────────────────┤
│ cachedTree / newTree    │ CompileTask.diff/scan         │ CompileTask      │
└─────────────────────────┴───────────────────────────────┴──────────────────┘

Every one is touched only by the compile subsystem. So they don't belong to Context — they belong to the compile engine. Context is just where they're parked, and parking state in a shared global is what makes everything feel entangled.

Now contrast libJars: written by ResolveTask.downloadAndCopy, read by CompileTask (as classpath). That one genuinely crosses a boundary — it's an output of resolve that's an input to compile. That's not god-object sloppiness; that's a real hand-off. The lesson: the ownership map tells you which fields are misplaced (the five caches) versus which represent a true interface between subsystems (libJars).

Your rep: do this table for the rest of Context's fields (packageName, deps, compileConfig, cpuPool, ioPool…). You'll find three buckets: immutable config (one writer at startup, many readers), single-owner state (the caches), and genuine hand-offs (libJars). Those three buckets are the seams. Want to try it and show me?

Question 2 — "Which way does the arrow point, and should it?"

Draw "A uses B" as an arrow A→B. Then ask: does A change more often than B? Arrows should point toward the things that change least. An arrow from stable→volatile is a future breakage.

Worked example — you have one pointing the wrong way right now. CompileTask.dependsOn() returns List.of("resolve"). But resolve is only registered if the user calls use(new ResolveDeps()) — it's a plugin. So your core compile task depends on an optional plugin. If someone builds without that plugin, Graph.run throws "No task with id: resolve." A core thing should never depend on an optional thing. The arrow points from stable (compile) to volatile (a plugin that may not be there).

You don't need to fix it yet — just notice it, because it tells you something: "resolve" isn't really optional, so maybe it isn't really a plugin. The arrow direction exposed a muddled decision. That's what this question is for.

Question 3 — "Say what this does in one sentence — without 'and'."

If the honest sentence needs an "and," the unit has more than one reason to change. That's your split line.

Worked example — CompileTask.java: "It orchestrates compilation, and walks the filesystem into a Merkle tree, and diffs two trees, and scans bytecode with ASM for method dependencies, and adapts javac's file manager." Five "and"s → five things that can each change for independent reasons (you change your diff strategy; that shouldn't touch your ASM scanner). Each "and" is a file waiting to be born: FileTree, TreeDiff, ClassScanner, FileManager already are separate classes — they're just cohabiting one .java file. Splitting them is nearly free and immediately makes the compile engine legible.

Your rep: run the sentence test on ResolveTask. Count the "and"s. You'll get the exact list of files it wants to become.

Question 4 — "Can I understand this function from its signature alone?"

Look at a method's parameters and return type. Could you predict what it does and what it touches? If you have to know the contents of a shared global to answer, the function is coupled through that global, not through its interface.

Worked example — void scan(List<String> changed, List<String> deleted, List<String> out, Context ctx). The signature says almost nothing, because the real inputs and outputs are five fields buried inside ctx that it reads and mutates as a side effect. Compare a version whose signature took an IncrementalState and returned the next set of files to recompile — you'd read the signature and know the data flow. This is the same lesson as Q1 seen from the caller's side: prefer inputs→outputs over reach-into-global. It's also what makes things testable — you can't unit-test scan today without constructing the entire world.

---
The meta-principle tying it together

All four questions are one idea: make the dependencies you already have visible and intentional, instead of hidden inside Context and static fields. You're not adding structure — you're revealing the structure that's already implied by who-touches-what, and deleting the couplings that shouldn't exist.

And because you're "not sure yet" where this goes: this is exactly the right work regardless of destination. Whether nob becomes a toy or a real tool, "each subsystem owns its own state and hands off through clear interfaces" keeps every door open. You never have to bet on a future you can't see.

Where I'd start you: Question 1, the full Context ownership table. It's the highest-leverage rep because it simultaneously teaches ownership (Q1), reveals the true hand-offs (the libJars-style seams), and shrinks the god-object that's making everything else hard. Do the table, post it here, and I'll coach you on what the buckets are telling you — including where you get it "wrong," because those are the interesting cases.
