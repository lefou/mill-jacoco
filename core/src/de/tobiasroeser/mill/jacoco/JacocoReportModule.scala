package de.tobiasroeser.mill.jacoco

import mill.api.PathRef
import mill.define.{Command, Input, NamedTask, Task}
import mill.api.Result
import mill.main.RunScript
import mill.modules.Jvm
import mill.scalalib.api.CompilationResult
import mill.scalalib.{CoursierModule, DepSyntax}
import mill.{Agg, T}
import os.Path

trait JacocoReportModule extends CoursierModule { jacocoReportModule =>

  /** The Jacoco Version. */
  def jacocoVersion: Input[String]

  /** Location where collected coverage data is stored. */
  def jacocoDataDir: T[PathRef] = T.persistent { PathRef(T.dest) }

  /** The Jacoco Agent is used at test-runtime. */
  def jacocoAgentJar: T[PathRef] = T {
    val jars = resolveDeps(T.task {
      Agg(ivy"org.jacoco:org.jacoco.agent:${jacocoVersion()};classifier=runtime".exclude("*" -> "*"))
    })()
    jars.iterator.next()
  }

  /** THe Jacococ Classpath contains the tools used to generate reports from collected coverage data. */
  def jacocoClasspath: T[Agg[PathRef]] = T {
    resolveDeps(T.task { Agg(ivy"org.jacoco:org.jacoco.cli:${jacocoVersion()}") })()
  }

  def forkArgs: T[Seq[String]] = Seq[String]()

  def jacocoCliTask(args: Task[Seq[String]]): Task[PathRef] = T.task {
    Jvm.runSubprocess(
      mainClass = "org.jacoco.cli.internal.Main",
      classPath = jacocoClasspath().map(_.path),
      jvmArgs = forkArgs(),
      envArgs = Map(),
      mainArgs = args(), // .map(_.replaceAll("\\Q$$REPORTDIR$$\\E", T.dest.toIO.getAbsolutePath())),
      workingDir = T.dest
    )
    PathRef(T.dest)
  }

  def jacocoCli(args: String*): Command[Unit] = T.command {
    jacocoCliTask(T.task { args })()
    ()
  }

  def jacocoReportFull(evaluator: mill.eval.Evaluator): Command[PathRef] = T.command {
    jacocoReportTask(
      evaluator = evaluator,
      sources = "__.allSources",
      compiled = "__.compile",
      excludeSources = "__.test.allSources",
      excludeCompiled = "__.test.compile"
    )()
  }

  def jacocoReportTask(
      evaluator: mill.eval.Evaluator,
      sources: String,
      excludeSources: String,
      compiled: String = "",
      excludeCompiled: String = ""
  ): Task[PathRef] = {

    def resolveTasks[T](tasks: String): Seq[Task[T]] = RunScript.resolveTasks(
      mill.main.ResolveTasks,
      evaluator,
      Seq(tasks),
      multiSelect = true
    ) match {
      case Left(err)    => throw new Exception(err)
      case Right(tasks) => tasks.asInstanceOf[Seq[Task[T]]]
    }

    val sourcesTasks: Seq[Task[Seq[PathRef]]] = resolveTasks(sources)
    val excludeSourcesTasks: Seq[Task[Seq[PathRef]]] = resolveTasks(excludeSources)
    val compiledTasks: Seq[Task[CompilationResult]] = resolveTasks(compiled)
    val excludeCompiledTasks: Seq[Task[CompilationResult]] = resolveTasks(excludeCompiled)

    jacocoCliTask(
      T.task {
        val sourcePaths: Seq[Path] =
          T.sequence(excludeTasks(sourcesTasks, excludeSourcesTasks))().flatten.map(_.path).filter(os.exists)
        val compiledPaths: Seq[Path] =
          T.sequence(excludeTasks(compiledTasks, excludeCompiledTasks))().map(_.classes.path).filter(os.exists)

        T.log.debug(s"sourcePaths: ${sourcePaths}")
        T.log.debug(s"compiledPaths: ${compiledPaths}")

        val execFile = jacocoDataDir().path / "jacoco.exec"
        if (os.exists(execFile)) {
          val res = Seq(
            "report",
            s"${jacocoDataDir().path}/jacoco.exec",
            "--html",
            s"${T.dest / "html"}",
            "--xml",
            s"${T.dest / "jacoco.xml"}"
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
    val excludeLabels = exclusions.collect { case x: NamedTask[T] =>
      x.toString
    }
    orig.flatMap {
      case t: NamedTask[T] if excludeLabels.contains(t.toString) => Seq()
      case x                                                     => Seq(x)
    }
  }

}
