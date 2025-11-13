package de.tobiasroeser.mill.jacoco

import de.tobiasroeser.mill.jacoco.internal.BuildInfo
import mill.{Agg, T}
import mill.api.PathRef
import mill.api.Result.Success
import mill.define.{Input, Task}
import mill.eval.Evaluator
import mill.main.RunScript
import mill.scalalib.{CoursierModule, DepSyntax}

trait JacocoReportModulePlatform extends CoursierModule {

  /**
   * The Jacoco version.
   * Reads the Jacoco version from system environment variable `JACOCO_VERSION` or defaults to a hardcoded version.
   */
  def jacocoVersion: Input[String] = T.input {
    Success[String](T.env.getOrElse("JACOCO_VERSION", BuildInfo.jacocoVersion))
  }

  /** The Jacoco Classpath contains the tools used to generate reports from collected coverage data. */
  def jacocoClasspath: T[Agg[PathRef]] = T {
    resolveDeps(T.task {
      Agg(ivy"org.jacoco:org.jacoco.cli:${jacocoVersion()}")
    })()
  }

  /** The Jacoco Agent is used at test-runtime. */
  def jacocoAgentJar: T[PathRef] = T {
    val jars = resolveDeps(T.task {
      Agg(ivy"org.jacoco:org.jacoco.agent:${jacocoVersion()};classifier=runtime".exclude("*" -> "*"))
    })()
    jars.iterator.next()
  }

  protected[jacoco] def resolveTasks[T](tasks: String, evaluator: Evaluator): Seq[Task[T]] =
    if (tasks.trim().isEmpty()) Seq()
    else RunScript.resolveTasks(
      mill.main.ResolveTasks,
      evaluator,
      Seq(tasks),
      multiSelect = true
    ) match {
      case Left(err) => throw new Exception(err)
      case Right(tasks) => tasks.asInstanceOf[Seq[Task[T]]]
    }

  protected[jacoco] val (sourcesSelector, compileSelector, excludeSourcesSelector, excludeCompiledSelector) = {
    ("__.allSources", "__.compile", "__.test.allSources", "__.test.compile")
  }

}
