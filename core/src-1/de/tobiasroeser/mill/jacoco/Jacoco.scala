package de.tobiasroeser.mill.jacoco

import de.tobiasroeser.mill.jacoco.internal.BuildInfo
import mill.T
import mill.api.Result.Success
import mill.api.{Discover, ExternalModule, Task}
import mill.given

object Jacoco extends ExternalModule with JacocoReportModule {

  protected override lazy val millDiscover: Discover = Discover[this.type]

}
