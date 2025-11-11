package de.tobiasroeser.mill.jacoco

import mill.scalalib.{Dep, DepSyntax, JavaModule, TestModule}
import mill.T
import mill.api.{ModuleRef, Task}

trait JacocoTestModule extends JavaModule with TestModule {

  def jacocoReportModule: ModuleRef[JacocoReportModule] = ModuleRef(Jacoco)

  def jacocoAgentDep: T[Seq[Dep]] = Task {
    Seq(mvn"org.jacoco:org.jacoco.agent:${jacocoReportModule().jacocoVersion()}")
  }

  /**
   * Add the Jacoco Agent to the runtime dependencies.
   */
  override def runMvnDeps: T[Seq[Dep]] = Task {
    super.runMvnDeps() ++ jacocoAgentDep().filter(_ => jacocoEnabled())
  }

  /**
   * If `true`, enable the jacoco report agent.
   * Defaults to `true` but support the existence of environment variable `JACOCO_DISABLED`.
   */
  def jacocoEnabled: T[Boolean] = Task.Input {
    !Task.env.contains("JACOCO_DISABLED")
  }

  /**
   * Add Jacoco specific javaagent options.
   */
  override def forkArgs: T[Seq[String]] = super.forkArgs() ++ {
    Seq(
      s"-javaagent:${jacocoReportModule().jacocoAgentJar().path}=destfile=${jacocoReportModule().jacocoDataDir()}/jacoco.exec"
    ).filter(_ => jacocoEnabled())
  }
}
