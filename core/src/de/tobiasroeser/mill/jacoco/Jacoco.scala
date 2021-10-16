package de.tobiasroeser.mill.jacoco

import mill.T
import mill.api.Result.Success
import mill.define.{Discover, ExternalModule, Input}

object Jacoco extends ExternalModule with JacocoReportModule {

  implicit def millScoptTasksReads[T] = new mill.main.Tasks.Scopt[T]()
  implicit def millScoptEvaluatorReads[T] = new mill.main.EvaluatorScopt[T]()

  override def millDiscover: Discover[Jacoco.this.type] = Discover[this.type]

  /**
   * Reads the Jacoco version from system environment variable `JACOCO_VERSION` or defaults to a hardcoded version.
   */
  override def jacocoVersion: Input[String] = T.input {
    Success[String](T.env.getOrElse("JACOCO_VERSION", "0.8.7"))
  }

}
