# Nob — Context Handoff

Paste this entire document as the first message in a new chat to continue working on Nob.

## The project

Nob is a general-purpose Java build system, written in Java, with Java as the only dependency (ASM is packed into the jar for bytecode analysis). Primary use case is the author's Minecraft modding projects, designed to be general purpose. End goal is Phase 3: hotswapping with JBR and the instrumentation API without restarting the JVM.

**Three phases:**
- Phase 1 — Build: incremental compile, dependency resolution, jar packaging. **DONE.**
- Phase 2 — Daemon: long-running JVM holds cache in memory. **Design discussed at length, not yet built.**
- Phase 3 — Watch + Hotswap: file watcher + JBR instrumentation API. **Not started.**

## Design philosophy (author's own rules — follow these strictly)

- Never split prematurely. Don't create new packages, files, or abstractions until the current structure is actively causing problems. "This might get big later" is not a reason to split now.
- Related things live together. If classes always change together and have no meaning apart from each other, they go in one file as package-private classes.
- Two packages only until proven otherwise: `nob` (public API) and `nob.build` (internal). No new packages until one is genuinely overcrowded.
- Data over abstraction. Structure code around how data flows, not OOP patterns. Don't create a class because design patterns say you should.
- Cognitive load is a hard constraint. The author has ADHD. Every extra file/package/layer of indirection has a real cost. Prefer flat over deep, consolidated over separated.
- When in doubt, don't split. The cost of splitting too early is higher than splitting too late.

## Your role

Architectural guidance and code review. The author writes all code themselves — learning is the goal, not working code handed to them. Point out bugs, suggest names, explain concepts when asked, push back on over-engineering, keep the author moving forward when stuck. Don't write code unless explicitly asked. Don't over-explain. Match the pace of the conversation. The author explicitly wants pushback when something is over-engineered or under-justified — they've thanked me for it before (e.g. rejecting ECS, rejecting a premature API module split).

## Phase 1 — current state (complete and working)

**CompileTask** (one file, contains everything related to compilation):
- `MerkleNode` — tree mirroring `src/`, hash = max mtime of children (dirs) or own mtime (leaves), used purely for change detection. Subject to change later to FileTree to be able to use as a generic cached FileTree. change detection will be done by comparing cached and hot Trees.
- ASM-based `Scanner` — tracks `ownedMethods`/`calledMethods`/`methodDependents` per source file, determines transitive recompilation set when a method signature changes
- `ClassScanner`/`MethodScanner` — ASM visitor classes
- Incremental compile loop: diff → recompile changed → rescan → repeat until stable (capped at 4 iterations)
- Cache (merkle tree + method maps) persists to disk across runs, confirmed working

**ResolveDeps / ResolveTask** (one file, full from-scratch Maven dependency resolver — no Maven library used):
- `MavenRepository` — HTTP downloads via `HttpClient`, checks local `~/.m2`-style cache before hitting network, custom `BodySubscriber` wraps `BodySubscribers.ofFile` to track bytes received for progress bars
- `PomParser` — hand-rolled streaming XML state machine (two stacks: tag-name stack + data-map stack), parses `<parent>`, `<properties>`, `<dependencies>`, `<dependencyManagement>`, `<exclusions>`, handles arbitrary property tag names correctly
- `PomData` — parent ref, properties map, deps list, dependencyManagement map
- `Dependency` — mutable class (not record — needed mutable `version` field), `equals`/`hashCode` overridden to ignore version (group+artifact identity only, needed for version-conflict tracking)
- Recursive parent-POM merging (child properties/managements win over parent), depth-limited property `${...}` resolution (cycle-guarded)
- Version conflict resolution via `ComparableVersion` (copied from Maven's own source, Apache 2.0) — collects all requested versions per artifact, picks highest
- Version **ranges** (`[1.0,2.0)` etc.) are explicitly NOT supported yet — throws if encountered. Deliberate scope cut, not an oversight.
- Multi-threaded downloads: `ExecutorCompletionService` wrapping a thread pool, `Semaphore` for per-batch concurrency throttling, fail-fast error propagation (`ExecutionException` unwrapped back to `NobException`)
- Live terminal progress bars: bold-filled bar, adapts to terminal width via `tput cols`, completed lines print once and never get redrawn/erased again, only active lines get cursor-up+clear+redraw each tick (this is the same mechanic real tools like cargo/npm use). Subject to change later.
- Successfully resolved a full Spring Boot transitive dependency tree from scratch, confirmed cache hits work on second run

**PackageTask**: builds manifest (temp file), shells out to `jar cfm` via ProcessBuilder, deletes temp manifest after. Known bug already fixed: manifest lines need trailing `\n` including `Main-Class`.

**Graph**: `Task` interface (`id()`, `dependsOn()`, `execute(ctx)`), recursive DFS topo-sort with `visited`/`visiting` sets for cycle detection (confirmed throws correctly on cycles).

**Logger**: static, has `info`/`warn`/`error`/`debug` (debug gated behind `Nob.debug` flag, since cache-hit messages were spamming output).

**First real plugin exists**: `ResolveDeps implements Plugin { void apply(Graph graph) { graph.register(new ResolveTask()); } }`, invoked via `Nob.use(new nob.build.ResolveDeps())`. This was built specifically as a *minimal* test case before designing anything further — deliberately no extensions map, no lifecycle markers, nothing speculative added alongside it.

## Architectural decisions made today (not yet all implemented — see TODO at bottom)

**Plugin system, minimal version:**
- `Plugin` interface: single method `void apply(Graph graph)` — intentionally a functional interface
- `Nob.use(plugin)` calls `apply`
- Explored but did NOT settle on: `.class`-based reflection registration, static `PLUGIN` constants per task class, Kotlin-style reified-generic `apply<T>()` (not possible in Java without `.class` anyway). Current code just does `new nob.build.ResolveDeps()` directly — fine for now, not a final answer.
- `Task` interface needs to move from `nob.build` (internal) to `nob` (public) since plugin authors now implement it directly. **Not yet done.**
- Explicitly **rejected** for now: a separate API module/jar to compile plugins against (real frameworks do this, but premature with one plugin — revisit once there's a second plugin and the public/internal boundary has proven stable), an `extensions` map on `Context` for arbitrary plugin state (same reasoning — no second plugin needs it yet), lifecycle marker tasks (Gradle-style empty anchor tasks like `compile`/`assemble` that real tasks attach to so plugins don't need to reference each other's task IDs directly — correct pattern *in the abstract*, but designed with zero real plugins in front of us, shelved until a second plugin actually needs to attach near the first one's tasks)

**Threading model:**
- One shared `cpuPool` (sized ~`cores`) and one shared `ioPool` (sized larger, since IO-bound work blocks on network/disk, not CPU — JCIP-style sizing reasoning: more threads than cores is fine and good when threads spend most of their life blocked, not computing)
- **Not per-task pools.** Real danger identified and agreed on: a thread *running on* a pool that blocks waiting for *more work submitted to that same pool* causes "thread starvation deadlock" (real JCIP term, in the executor/task-execution chapter — author was pointed there specifically to revisit). Fix: things that fan out work and block waiting (`ResolveTask.resolve()`, the graph runner) must not run *on* the same pool they're submitting to and waiting on.
- Per-batch concurrency throttling via `Semaphore`, wrapping the *shared* pool through a fresh `ExecutorCompletionService` per batch (the completion service itself is cheap/disposable, doesn't own threads, just tracks completion order against whatever executor it wraps)
- `ForkJoinPool` flagged as the theoretically correct tool for recursive task-spawns-subtask-and-joins patterns specifically (e.g. POM parent-chain fetching) because `.join()` on a `ForkJoinTask` steals other work instead of truly blocking a thread — **not yet implemented anywhere**, noted as "know it exists, not urgent yet". Considers virtual threads as potential proper replacements.
- **Not yet done:** wiring `cpuPool`/`ioPool` onto `Context` as shared fields. `ResolveTask` currently uses its own local pool — this is explicitly on the TODO list to fix once the plugin/Context shape settles a bit more. Current context might get moved out to plugin specific contexts.

**Daemon / interactive shell design (Phase 2, designed in detail, NOT built yet):**
This took a long back-and-forth to arrive at — record it precisely:
- Rejected: a thin native launcher process (breaks the "Java only" dependency constraint — would need to ship a native binary)
- Rejected: separate background daemon process discovered via project-hash-named socket with a thin Java client that connects to it each time (more moving parts than needed)
- **Landed on:** the JVM process itself becomes the long-lived thing. Running `java -jar Build.jar` with **no args** drops into an **interactive shell** — a `>` prompt in the same terminal, logs print above it (same cursor-up/clear/redraw mechanic already built for progress bars), user types task names (`compile`, `jar`, etc.) directly, JVM stays warm (cache, pools, everything in memory) across commands within that session.
- Running `java -jar Build.jar compile` (**with args**) is **one-shot mode**: checks for a `build/.nob/pid` file (would contain PID + socket path). If a live interactive session is found (PID alive, socket connects), it sends the command over the socket to that *already running* warm session and prints the response — no new JVM cost paid. If no live session is found, **it just runs cold and inline, exits, and starts nothing in the background.**
- The interactive shell, while running, *also* opens a small Unix domain socket purely as a side-channel so one-shot commands (e.g. bound to a keyboard shortcut) can reach it without typing into the prompt directly.
- **The locked-in rule, stated by the author and confirmed correct:** "if one shot, it stays cold. if interactive, it opens and stays open. during interactive, one shot talks to hot vm, otherwise default." No background spawning, ever. The only thing that's ever long-lived is something the user explicitly started by running with no args, and it goes away when they exit that session.
- This resolves the earlier "should `Nob` be static or instance-based" worry — decided to move from instance-based (`new Nob() {{ ... }}`) to plain static method calls (`Nob.packageName = ...; Nob.compile();`) specifically *because* the daemon/shell model is inherently one-project-per-JVM-session, so there's no need for instance semantics at all.
- This is a **plan only** — written down, not implemented. Don't assume any of it exists in code yet.

**Explicitly rejected today:**
- ECS (Entity Component System) for Nob's task graph — wrong fit, ECS solves cache-locality/query-composition problems for thousands of homogeneous per-frame-updated objects; Nob has a handful of tasks run once per build. Recommended the author save ECS for an actual game project (Handmade Hero / Minecraft-in-C, already on their roadmap) where it'll click for real reasons instead of being forced onto something it doesn't fit.
- Kotlin/KTS build scripts — would require bundling the Kotlin compiler, breaks the "Java is the only dependency" constraint. Staying Java-only, including the unnamed-class single-file source launcher style build script.

## In-progress discussion (where the conversation left off — pick up here)

Currently mid-design on **CompileTask's caching architecture**, specifically reducing/eliminating raw `Files.walk`/`Files.list` OS calls by maintaining caches that are updated incrementally rather than rediscovered every run. Key conclusions reached, in order:

1. `MerkleNode` should stay purely a change-detection structure for `src/`. Don't bloat it with responsibilities it wasn't built for.
2. The source-file → class-file(s) mapping (`Scanner`'s internal `classToSource`, currently rebuilt via `Files.list(ctx.out.resolve(dir))` every scan) should become a **separate persisted cache** — call it `classFileCache`, shape `Map<String, Set<String>>` — living as a peer to `merkleCache` in the persisted Context cache, **not** folded into `MerkleNode`. Reasoning: different lifecycle, different update trigger (diff vs. actual compile/delete), different question being answered (what changed vs. what exists).
3. Realized: if `classFileCache` is maintained **incrementally** (updated by `CompileTask` itself whenever it actually compiles or deletes a file) rather than rediscovered by listing the output directory, there is **no remaining need to ever walk `build/classes/` at all** — every fact about build output is already a logical consequence of `classFileCache` plus what `merkleCache`'s diff reports as changed/deleted. This fully eliminates the `Files.list` calls in `Scanner`, including on a first/clean run (cache starts empty, gets populated as compilation happens, never needs a discovery walk).
4. **Just decided, not yet implemented:** split `MerkleNode.build()` — which currently both *walks the filesystem* and *assigns hashes* in one recursive pass — into two separate concerns. A pure filesystem walker (tentatively `FsWalker`, returning a `RawTreeShape` of paths + mtimes, no hashing/diffing logic at all) that `MerkleNode` then consumes/wraps to do its actual job (hashing + diffing against the cached previous tree). Justification that made this worth doing (not just clean-for-clean's-sake): there's a real second consumer — wanting to query "what's under this directory" structurally, without going through `MerkleNode`'s diff-specific semantics, which would be a semantic mismatch (asking a change-detector a structural question it wasn't designed to answer).
5. **Implemented:** MerkleNode was replaced with **FileTree** which acts as a generic cached filesystem, which can also be used to find differences, replacing the need for a MerkleNode.

**This split (#4) is the very next concrete thing to design/implement** if picking this conversation back up.

## NIO direction (separate, related thread)

- Author is reading *Java Concurrency in Practice* (JCIP), roughly halfway through, bought physically.
- Learning style confirmed repeatedly through this whole project: project-first, theory-second. Reading dense material cold doesn't stick; hitting a real bug then reading the relevant chapter does. This pattern played out explicitly today — designed shared thread pools from first principles, independently arrived at the exact failure mode JCIP calls "thread starvation deadlock" before being told the term, then was pointed to revisit that specific chapter now while it's concrete and fresh, rather than reading linearly.
- TODO list, roughly in stated priority order:
  - Logger on separate thread — **done**
  - Multi-threaded downloads — **done** (see Phase 1 section above)
  - Wire shared `cpuPool`/`ioPool` onto `Context` — **not done**, deliberately deferred slightly so the plugin/Context shape settles first
  - Replace `ProcessBuilder` calls for `javac` with the in-process `javax.tools.JavaCompiler` API — not started
  - Replace jar building (`ProcessBuilder` calling `jar cfm`) with NIO — **flagged as a likely false lead**: `ZipOutputStream` is old `java.io`, not NIO at all; true NIO jar building would mean `FileChannel` + hand-rolled zip entry construction, meaningfully more work for unclear benefit. Worth deciding deliberately rather than assuming "NIO-ify everything" applies cleanly here.
  - Build a custom HTTP client instead of `java.net.http.HttpClient` — explicitly low priority / "maybe not, at the end, not a requirement"
  - Plugin system buildout — sequenced *after* the above NIO/cache work, on the reasoning that compile/jar/resolve-deps are themselves slated to eventually become plugins, so rewriting their internals before finalizing plugin shape avoids redoing the wiring twice
- General principle agreed on for utility code: don't create a shared `NioUtils`-type file preemptively. Write helper functions inline/package-private where first needed; only extract a shared file once a third real duplicate shows up. Same "don't split prematurely" rule applied to utilities specifically.

## Longer-term learning roadmap (context, not urgent, mentioned for continuity)

Finish JCIP → a compiler book → Handmade Hero (expected to take meaningfully less time than usual since POM-parsing/ASM work already taught real parsing/scanning fundamentals) → Minecraft-in-C projects, including a "human-like" Minecraft pathfinder (deliberately not pure A*, meant to feel like a player, including making mistakes) and a from-scratch Minecraft implementation in C. Motivation is explicitly "for the love of the game," not career-driven, and explicitly project-first as a learning style — dry theory-first study doesn't work well for this author, has been stated directly multiple times. Projects are still planned, and no amount of pressure is taken by the author to stick to this path. The author has ADHD so detours and future planning like these are quite common.

One related tangent already explored: for Minecraft modding support eventually (a planned plugin), real obstacles were identified — downloading Minecraft itself requires Mojang's launcher metadata API (not Maven), Forge/Fabric environment setup is a separate complex pipeline (patching, deobfuscation), and bytecode remapping (deobfuscated ↔ obfuscated names) could plausibly be hand-built with ASM (which Nob already depends on) instead of pulling in SpecialSource, using mapping files (MCP/Yarn/Mojmap, each a different format) parsed the same way `PomParser` was hand-built. This is a *future* plugin idea, not started, just scoped as plausible and consistent with the project's "reinvent the wheel deliberately, for the learning" ethos.

## Notes on working with the author

- Has ADHD. Cognitive load and momentum matter more than theoretical completeness. Watch for over-abstraction spirals (today's session had a long one — pool design → plugin composition → lifecycle markers — that left the author explicitly saying "the architecture is getting complex now" and later "nob feels really dirty at this point, idk what to do"). When that happens: don't keep stacking more design on top: help find the smallest concrete next step, ground back into something buildable, and be honest that exploring dead ends isn't wasted thinking even when it doesn't ship code.
- Don't use clinical/diagnostic language about the author's mental state. Stay supportive without psychoanalyzing.
- The author explicitly wants pushback on over-engineering and will say so directly if something feels off — take that seriously and engage with the actual tradeoff rather than just agreeing.
- Genuinely fast, capable builder — wrote a complete from-scratch Maven dependency resolver (POM parsing, parent inheritance, property resolution, version conflict resolution, multi-threaded downloads with live progress bars) in a few days. Treat the skill level as real and high; explanations don't need to be elementary, but should stay concrete and avoid abstract design-pattern-speak without a concrete justification attached.
