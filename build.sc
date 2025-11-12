// mill plugins
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.1`
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.7.3-3-0685b8`
import $ivy.`com.lihaoyi::mill-contrib-scoverage:`
import mill._
import mill.contrib.scoverage.ScoverageModule
import mill.define.{Cross, Target, Task}
import mill.scalalib._
import mill.scalalib.api.{JvmWorkerUtil, ZincWorkerUtil}
import mill.scalalib.publish._
import de.tobiasroeser.mill.integrationtest._
import de.tobiasroeser.mill.vcs.version._
import os.Path

trait Deps {
  def millPlatform: String
  def millVersion: String
  def scalaVersion: String = Deps.scalaVersion
  def testWithMill: Seq[String]

  def scoverageVersion = Deps.scoverageVersion

  def millMain = ivy"com.lihaoyi::mill-main:${millVersion}"
  def millMainApi = ivy"com.lihaoyi::mill-main-api:${millVersion}"
  def millScalalib = ivy"com.lihaoyi::mill-scalalib:${millVersion}"
  def millScalalibApi = ivy"com.lihaoyi::mill-scalalib-api:${millVersion}"
  def millLibs = mvn"com.lihaoyi::mill-libs:${millVersion}"
  def scalaTest = ivy"org.scalatest::scalatest:3.2.3"
  def slf4j = ivy"org.slf4j:slf4j-api:1.7.25"
}
object Deps {
  def scoverageVersion = "2.4.0"
  def scalaVersion = "2.13.16"
  def jacocoVersion = "0.8.14"
  val dummyDeps = Seq(
    ivy"org.scoverage:::scalac-scoverage-plugin:${scoverageVersion}",
    ivy"org.scoverage::scalac-scoverage-runtime:${scoverageVersion}",
    ivy"org.jacoco:org.jacoco.cli:${jacocoVersion}"
  )
}

object Deps_1 extends Deps {
  override def millPlatform = "1" // only valid for exact milestone versions
  override def millVersion = "1.0.0" // scala-steward:off
  override def testWithMill = Seq("1.1.0-RC1", "1.0.6", millVersion)
  override def scalaVersion = "3.7.3"
}
object Deps_0_11 extends Deps {
  override def millPlatform = "0.11" // only valid for exact milestone versions
  override def millVersion = "0.11.0" // scala-steward:off
  override def testWithMill = Seq("0.12.15", "0.11.13", millVersion)
}
object Deps_0_10 extends Deps {
  override def millPlatform = "0.10"
  override def millVersion = "0.10.0" // scala-steward:off
  override def testWithMill = Seq("0.10.13", millVersion)
}
object Deps_0_9 extends Deps {
  override def millPlatform = "0.9"
  override def millVersion = "0.9.7" // scala-steward:off
  override def testWithMill = Seq("0.9.12", millVersion)
}

val crossDeps = Seq(Deps_1, Deps_0_11, Deps_0_10, Deps_0_9)
val millApiVersions = crossDeps.map(x => x.millPlatform -> x)
val millItestVersions = crossDeps.flatMap(x => x.testWithMill.map(_ -> x))

trait BaseModule extends ScalaModule with PublishModule with ScoverageModule with Cross.Module[String] {
  def millApiVersion: String = crossValue
  def deps: Deps = millApiVersions.toMap.apply(millApiVersion)
  override def scalaVersion = deps.scalaVersion
  override def platformSuffix = s"_mill${deps.millPlatform}"

  override def ivyDeps = T {
    if (deps.millPlatform.startsWith("0.")) {
      Agg(mvn"${scalaOrganization()}:scala-library:${scalaVersion()}")
    } else {
      Agg()
    }
  }

  def publishVersion = VcsVersion.vcsState().format()

  override def javacOptions = Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8")
  override def scalacOptions = Seq("-target:jvm-1.8", "-encoding", "UTF-8")

  def pomSettings = T {
    PomSettings(
      description = "Mill plugin to collect test coverage data with JaCoCo and generate reports",
      organization = "de.tototec",
      url = "https://github.com/lefou/mill-jacoco",
      licenses = Seq(License.`Apache-2.0`),
      versionControl = VersionControl.github("lefou", "mill-jacoco"),
      developers = Seq(Developer("lefou", "Tobias Roeser", "https.//github.com/lefou"))
    )
  }

  override def scoverageVersion = deps.scoverageVersion

}

object core extends Cross[CoreCross](millApiVersions.map(_._1))
trait CoreCross extends BaseModule {

  override def artifactName = "de.tobiasroeser.mill.jacoco"

  override def skipIdea: Boolean = deps != crossDeps.head

  def buildInfo = Task {
    os.write(
      Task.dest / "BuildInfo.scala",
      s"""|package ${artifactName()}.internal
          |object BuildInfo {
          |  val pluginVersion = "${publishVersion()}"
          |  val jacocoVersion = "${Deps.jacocoVersion}"
          |}
         |""".stripMargin,
      createFolders = true
    )
    Seq(PathRef(Task.dest))
  }

  override def generatedSources = super.generatedSources() ++ buildInfo()

  override def compileIvyDeps = {
    if (deps.millPlatform.startsWith("0.")) Agg(
      deps.millMain,
      deps.millScalalib
    )
    else Agg(
      deps.millLibs
    )

  }

  val srcPaths = {
    val versions = JvmWorkerUtil.matchingVersions(millApiVersion) ++
      JvmWorkerUtil.versionRanges(millApiVersion, millApiVersions.map(_._1))

    versions.map(v => moduleDir / s"src-${v}")
  }

  def localSources = Task.Sources(srcPaths.map(PathRef(_)))

  override def sources = super.sources() ++ localSources()
}

object itest extends Cross[ItestCross](millItestVersions.map(_._1))
trait ItestCross extends MillIntegrationTestModule with Cross.Module[String] {
  val millApiVersion = millItestVersions.toMap.apply(crossValue).millPlatform
  def deps: Deps = millApiVersions.toMap.apply(millApiVersion)
  override def millTestVersion = crossValue
  override def pluginsUnderTest = Seq(core(millApiVersion))

  val srcPaths = {
    val versions =
      JvmWorkerUtil.matchingVersions(millApiVersion) ++
        JvmWorkerUtil.versionRanges(millApiVersion, millApiVersions.map(_._1))

    versions.map(v => moduleDir / s"src-${v}")
  }
  def localSources = Task.Sources(srcPaths.map(PathRef(_)))

  override def sources = super.sources() ++ localSources()

  /** Replaces the plugin jar with a scoverage-enhanced version of it. */
  override def pluginUnderTestDetails: Task[Seq[(PathRef, (PathRef, (PathRef, (PathRef, (PathRef, Artifact)))))]] =
    Target.traverse(pluginsUnderTest) { p =>
      val jar = p match {
        case p: ScoverageModule => p.scoverage.jar
        case p => p.jar
      }
      jar zip (p.sourceJar zip (p.docJar zip (p.pom zip (p.ivy zip p.artifactMetadata))))
    }

  override def testInvocations: T[Seq[(PathRef, Seq[TestInvocation.Targets])]] = T {
    super.testInvocations().map { case (pr, _) =>
      pr -> Seq(
        TestInvocation.Targets(Seq("-d", "__.test")),
        TestInvocation.Targets(Seq("-d", "de.tobiasroeser.mill.jacoco.Jacoco/jacocoReportFull")),
        TestInvocation.Targets(Seq("-d", "verify", "--millVersion", crossValue))
      )
    }
  }

  override def perTestResources = T.sources {
    Seq(generatedSharedSrc())
  }

  def generatedSharedSrc = T {
    if (deps.millVersion.startsWith("0.")) {
      os.write(
        T.dest / "shared.sc",
        s"""
           |import $$file.plugins
           |import $$ivy.`org.scoverage::scalac-scoverage-runtime:${deps.scoverageVersion}`
           |""".stripMargin
      )

    }
    // TODO: re-enable scoverage support for 1.x itest's
    PathRef(T.dest)
  }
}

implicit object DepSegment extends Cross.ToSegments[Dep]({ dep =>
      val d = Lib.depToDependency(dep, Deps.scalaVersion)
      List(s"${d.module.organization.value}:${d.module.name.value}:${d.version}")
    })

/**
 * Dummy module(s) to let Dependency/showUpdates or Scala-Steward find
 * and bump dependency versions we use at runtime
 */
object scalaStewardDummy extends Cross[DependencyFetchDummy](Deps.dummyDeps)
trait DependencyFetchDummy extends ScalaModule with Cross.Module[Dep] {
  def scalaVersion = Deps.scalaVersion
  def compileIvyDeps = Agg(crossValue)
}
