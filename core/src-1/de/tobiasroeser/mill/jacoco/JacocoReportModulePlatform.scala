package de.tobiasroeser.mill.jacoco

import mill.T
import mill.api.PathRef
import mill.api.Task
import mill.api.Evaluator
import mill.api.{SelectMode}
import mill.scalalib.{CoursierModule, DepSyntax}
import mill.util.Version

trait JacocoReportModulePlatform extends CoursierModule {

  /** The Jacoco Version. */
  def jacocoVersion: T[String]

  /** The Jacoco Classpath contains the tools used to generate reports from collected coverage data. */
  def jacocoClasspath: T[Seq[PathRef]] = Task {
    defaultResolver().classpath(Seq(mvn"org.jacoco:org.jacoco.cli:${jacocoVersion()}"))
  }

  /** The Jacoco Agent is used at test-runtime. */
  def jacocoAgentJar: T[PathRef] = Task {
    val jars = defaultResolver().classpath(Seq(
      mvn"org.jacoco:org.jacoco.agent:${jacocoVersion()};classifier=runtime".exclude("*" -> "*")
    ))
    jars.head
  }

  protected[jacoco] def resolveTasks[T](tasks: String, evaluator: Evaluator): Seq[Task[T]] = {
    if (tasks.trim().isEmpty()) Seq()
    else {
      evaluator.resolveTasks(Seq(tasks), selectMode = SelectMode.Multi).get.asInstanceOf[Seq[Task[T]]]
    }
  }

  protected[jacoco] val (sourcesSelector, compileSelector, excludeSourcesSelector, excludeCompiledSelector) =
    ("__:JavaModule:^TestModule.allSources", "__:JavaModule:^TestModule.compile", "", "")

}
