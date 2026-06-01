nob/
  []  Nob.java              everything the user sees. compile(), buildJar(), etc.

  []  NobException.java     extends RuntimeException

nob/build/
  []  Context.java          all paths + cache state + task configs. load(Nob), save()

  []  Task.java             interface: id(), dependsOn(), execute(Context)

  [x] Graph.java            register tasks, topological sort, run(target)

  [x]  Changes.java         merkle tree + diff + DiffResult — all in one file

  []  Scanner.java          all ASM code in one file (visitors, scan context, etc.)

  []  CompileTask.java      implements Task — the compile pipeline

  []  PackageTask.java      implements Task — jar building

