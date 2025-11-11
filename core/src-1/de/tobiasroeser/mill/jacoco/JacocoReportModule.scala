package de.tobiasroeser.mill.jacoco

import mill.T
import mill.api.{PathRef, Result, Task}
import mill.util.Jvm
import os.Path
import mill.api.JsonFormatters.given
import mill.javalib.api.CompilationResult

trait JacocoReportModule extends JacocoReportModulePlatform { jacocoReportModule =>

  /** Location where collected coverage data is stored. */
  def jacocoDataDir: T[os.Path] = Task(persistent = true) { Task.dest }

  def forkArgs: T[Seq[String]] = Seq[String]()

  def jacocoCliTask(args: Task[Seq[String]]): Task[PathRef] = Task.Anon {
    Jvm.callProcess(
      mainClass = "org.jacoco.cli.internal.Main",
      classPath = jacocoClasspath().map(_.path),
      jvmArgs = forkArgs(),
      env = Map(),
      mainArgs = args(),
      cwd = Task.dest
    )
    PathRef(Task.dest)
  }

  def jacocoCli(args: String*): Task.Command[Unit] = Task.Command {
    jacocoCliTask(Task.Anon { args })()
    ()
  }

  def jacocoReportFull(evaluator: mill.api.Evaluator): Task.Command[PathRef] = Task.Command {
    jacocoReportTask(
      evaluator = evaluator,
      sources = sourcesSelector,
      compiled = compileSelector,
      excludeSources = excludeSourcesSelector,
      excludeCompiled = excludeCompiledSelector
    )()
  }

  def jacocoReportTask(
      evaluator: mill.api.Evaluator,
      sources: String,
      excludeSources: String,
      compiled: String = "",
      excludeCompiled: String = ""
  ): Task[PathRef] = {

    val sourcesTasks: Seq[Task[Seq[PathRef]]] = resolveTasks(sources, evaluator)
    val excludeSourcesTasks: Seq[Task[Seq[PathRef]]] = resolveTasks(excludeSources, evaluator)
    val compiledTasks: Seq[Task[CompilationResult]] = resolveTasks(compiled, evaluator)
    val excludeCompiledTasks: Seq[Task[CompilationResult]] = resolveTasks(excludeCompiled, evaluator)

    jacocoCliTask(
      Task.Anon {
        val sourcePaths: Seq[Path] =
          Task.sequence(excludeTasks(sourcesTasks, excludeSourcesTasks))().flatten.map(_.path).filter(os.exists)
        val compiledPaths: Seq[Path] =
          Task.sequence(excludeTasks(compiledTasks, excludeCompiledTasks))().map(_.classes.path).filter(os.exists)

        Task.log.debug(s"sourcePaths: ${sourcePaths}")
        Task.log.debug(s"compiledPaths: ${compiledPaths}")

        val execFile = jacocoDataDir() / "jacoco.exec"
        if (os.exists(execFile)) {
          val res = Seq(
            "report",
            s"${jacocoDataDir()}/jacoco.exec",
            "--html",
            s"${Task.dest / "html"}",
            "--xml",
            s"${Task.dest / "jacoco.xml"}"
          ) ++
            sourcePaths.flatMap(p => Seq("--sourcefiles", p.toIO.getAbsolutePath)) ++
            compiledPaths.flatMap(p => Seq("--classfiles", p.toIO.getAbsolutePath))

          Result.Success(res)
        } else {
          Result.Failure("Cannot find JaCoCo datafile. Did you forget to run any test?")
        }
      }
    )
  }

  private def excludeTasks[T](orig: Seq[Task[T]], exclusions: Seq[Task[T]]): Seq[Task[T]] = {
    val excludeLabels = exclusions.collect { case x: Task.Named[T] =>
      x.toString
    }
    orig.flatMap {
      case t: Task.Named[T] if excludeLabels.contains(t.toString) => Seq()
      case x => Seq(x)
    }
  }

}
