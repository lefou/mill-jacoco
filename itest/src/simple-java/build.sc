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

def verify(): Command[Unit] = T.command {
  assert(os.exists(os.pwd / "out" / "de" / "tobiasroeser" / "mill"))
}
