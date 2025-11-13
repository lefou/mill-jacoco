package de.tobiasroeser.mill.jacoco

import mill.T
import mill.define.{Discover, ExternalModule, Input}

object Jacoco extends ExternalModule with JacocoReportModule with JacocoPlatform {

  override def millDiscover: Discover[Jacoco.this.type] = Discover[this.type]

}
