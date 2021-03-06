// mill plugins
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.1.4`
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.4.1-26-70d7c9`
import $ivy.`com.lihaoyi::mill-contrib-scoverage:`
import mill._
import mill.contrib.scoverage.ScoverageModule
import mill.define.{Command, Target, Task, TaskModule}
import mill.scalalib._
import mill.scalalib.publish._
import de.tobiasroeser.mill.integrationtest._
import de.tobiasroeser.mill.vcs.version._
import os.Path

val baseDir = build.millSourcePath

trait Deps {
  def millPlatform: String
  def millVersion: String
  def scalaVersion: String
  def testWithMill: Seq[String]

  def millMain = ivy"com.lihaoyi::mill-main:${millVersion}"
  def millMainApi = ivy"com.lihaoyi::mill-main-api:${millVersion}"
  def millScalalib = ivy"com.lihaoyi::mill-scalalib:${millVersion}"
  def millScalalibApi = ivy"com.lihaoyi::mill-scalalib-api:${millVersion}"
  def scalaTest = ivy"org.scalatest::scalatest:3.2.3"
  def slf4j = ivy"org.slf4j:slf4j-api:1.7.25"
}

object Deps_0_10 extends Deps {
  override def millPlatform = "0.10"
  override def millVersion = "0.10.0" // scala-steward:off
  override def scalaVersion = "2.13.8"
  override def testWithMill = Seq(millVersion, "0.10.1", "0.10.2", "0.10.3")
}
object Deps_0_9 extends Deps {
  override def millPlatform = "0.9"
  override def millVersion = "0.9.7" // scala-steward:off
  override def scalaVersion = "2.13.7"
  override def testWithMill = Seq(millVersion, "0.9.8", "0.9.9", "0.9.10", "0.9.11", "0.9.12")
}

val crossDeps = Seq(Deps_0_10, Deps_0_9)
val millApiVersions = crossDeps.map(x => x.millPlatform -> x)
val millItestVersions = crossDeps.flatMap(x => x.testWithMill.map(_ -> x))

trait BaseModule extends CrossScalaModule with PublishModule with ScoverageModule {
  def millApiVersion: String
  def deps: Deps = millApiVersions.toMap.apply(millApiVersion)
  def crossScalaVersion = deps.scalaVersion
  override def artifactSuffix: T[String] = s"_mill${deps.millPlatform}_${artifactScalaVersion()}"

  override def ivyDeps = T {
    Agg(ivy"${scalaOrganization()}:scala-library:${scalaVersion()}")
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

  override def scoverageVersion = "1.4.1"

  trait Tests extends ScoverageTests

}

object core extends Cross[CoreCross](millApiVersions.map(_._1): _*)
class CoreCross(override val millApiVersion: String) extends BaseModule {

  override def artifactName = "de.tobiasroeser.mill.jacoco"

  override def skipIdea: Boolean = deps != crossDeps.head

  override def compileIvyDeps = Agg(
    deps.millMain,
    deps.millScalalib
  )

//  object test extends Tests with TestModule.ScalaTest {
//    override def ivyDeps = Agg(deps.scalaTest)
//  }
}

object itest extends Cross[ItestCross](millItestVersions.map(_._1): _*) with TaskModule {
  override def defaultCommandName(): String = "test"
  def test(args: String*): Command[Seq[TestCase]] = T.command {
    T.traverse(millModuleDirectChildren.collect { case m: ItestCross => m }.headOption.toSeq)(_.test(args: _*))()
      .flatten
  }
  def testCached: T[Seq[TestCase]] = T {
    T.traverse(millModuleDirectChildren.collect { case m: ItestCross => m }.headOption.toSeq)(_.testCached)().flatten
  }
}
class ItestCross(millItestVersion: String) extends MillIntegrationTestModule {
  val millApiVersion = millItestVersions.toMap.apply(millItestVersion).millPlatform
  override def millSourcePath: Path = super.millSourcePath / os.up
  override def millTestVersion = millItestVersion
  override def pluginsUnderTest = Seq(core(millApiVersion))

  /** Replaces the plugin jar with a scoverage-enhanced version of it. */
  override def pluginUnderTestDetails: Task.Sequence[(PathRef, (PathRef, (PathRef, (PathRef, (PathRef, Artifact)))))] =
    Target.traverse(pluginsUnderTest) { p =>
      val jar = p match {
        case p: ScoverageModule => p.scoverage.jar
        case p => p.jar
      }
      jar zip (p.sourceJar zip (p.docJar zip (p.pom zip (p.ivy zip p.artifactMetadata))))
    }

  override def testInvocations: Target[Seq[(PathRef, Seq[TestInvocation.Targets])]] = T {
    super.testInvocations().map { case (pr, _) =>
      pr -> Seq(
        TestInvocation.Targets(Seq("-d", "__.test")),
        TestInvocation.Targets(Seq("-d", "de.tobiasroeser.mill.jacoco.Jacoco/jacocoReportFull")),
        TestInvocation.Targets(Seq("-d", "verify"))
      )
    }
  }

}
