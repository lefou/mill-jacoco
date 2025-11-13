package de.tobiasroeser.mill.jacoco

import de.tobiasroeser.mill.jacoco.internal.BuildInfo
import mill.T
import mill.api.Result.Success
import mill.define.{Discover, ExternalModule, Input}

object Jacoco extends ExternalModule with JacocoReportModule with JacocoPlatform {

  override def millDiscover: Discover[Jacoco.this.type] = Discover[this.type]

}
