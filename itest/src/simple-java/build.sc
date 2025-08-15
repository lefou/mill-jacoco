// mill plugins under test
import $file.shared

import de.tobiasroeser.mill.jacoco._

import mill._
import mill.scalalib._
import mill.define.Command

object main extends JavaModule {
  object test extends JavaModuleTests with JacocoTestModule with TestModule.Junit4 {
    override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Agg(
      ivy"com.novocode:junit-interface:0.11",
      ivy"junit:junit:4.12"
    )
  }
}

def verify(millVersion: String): Command[Unit] = T.command {
  val destDir =
    if (millVersion.startsWith("0.9")) "jacocoReportFull" :: "dest" :: Nil
    else "jacocoReportFull.dest" :: Nil
  val jacocoPath = T.workspace / "out" / "de" / "tobiasroeser" / "mill" / "jacoco" / "Jacoco" / destDir

  val xml = jacocoPath / "jacoco.xml"
  assert(os.exists(jacocoPath))
  assert(os.exists(xml))
  val contents = os.read(xml)
  assert(contents.contains("""<class name="test/Main" sourcefilename="Main.java">"""))
  assert(contents.contains("""<sourcefile name="Main.java">"""))
  ()
}
