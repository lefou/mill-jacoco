// mill plugins under test
import $file.shared

import de.tobiasroeser.mill.jacoco._

import mill._
import mill.scalalib._
import mill.define.Command

object main extends JavaModule {
  // no tests in here
  object test extends JavaModuleTests with JacocoTestModule with TestModule.Junit4
  // but here
  object customTest extends JavaModuleTests with JacocoTestModule with TestModule.Junit4 {
    override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Agg(
      ivy"com.novocode:junit-interface:0.11",
      ivy"junit:junit:4.12"
    )
  }
}

def verify(millVersion: String): Command[Unit] = T.command {
  val jacocoPath = os.pwd / "out" / "de" / "tobiasroeser" / "mill" / "jacoco" / "Jacoco" / "jacocoReportFull.dest"
  val xml = jacocoPath / "jacoco.xml"
  assert(os.exists(jacocoPath))
  assert(os.exists(xml))
  val contents = os.read(xml)
  assert(contents.contains("""<class name="test/Main" sourcefilename="Main.java">"""))
  assert(contents.contains("""<sourcefile name="Main.java">"""))
  if (millVersion.startsWith("0.11.") && millVersion.drop(5).takeWhile(_.isDigit).toInt >= 7) {
    // this file should be excluded from report, as it belongs to the test sources
    assert(!contents.contains("""<class name="test/MainTest" sourcefilename="MainTest.java">"""))
  }
  ()
}
