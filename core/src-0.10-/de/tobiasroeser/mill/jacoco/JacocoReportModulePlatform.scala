package de.tobiasroeser.mill.jacoco

import mill.{Agg, T}
import mill.api.PathRef
import mill.define.{Input, Task}
import mill.eval.Evaluator
import mill.main.RunScript
import mill.scalalib.{CoursierModule, DepSyntax}

trait JacocoReportModulePlatform extends CoursierModule {

  /** The Jacoco Version. */
  def jacocoVersion: Input[String]

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

  protected[jacoco] def resolveTasks[T](tasks: String, evaluator: Evaluator): Seq[Task[T]] = RunScript.resolveTasks(
    mill.main.ResolveTasks,
    evaluator,
    Seq(tasks),
    multiSelect = true
  ) match {
    case Left(err) => throw new Exception(err)
    case Right(tasks) => tasks.asInstanceOf[Seq[Task[T]]]
  }


}
