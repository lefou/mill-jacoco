package de.tobiasroeser.mill.jacoco

import de.tobiasroeser.mill.jacoco.internal.BuildInfo
import mill.T
import mill.api.Result.Success
import mill.api.{Discover, ExternalModule, Task}
import mill.given

object Jacoco extends ExternalModule with JacocoReportModule {

  protected override lazy val millDiscover: Discover = Discover[this.type]

  /**
   * Reads the Jacoco version from system environment variable `JACOCO_VERSION` or defaults to a hardcoded version.
   */
  override def jacocoVersion: T[String] = Task.Input {
    Task.env.getOrElse("JACOCO_VERSION", BuildInfo.jacocoVersion)
  }

}
