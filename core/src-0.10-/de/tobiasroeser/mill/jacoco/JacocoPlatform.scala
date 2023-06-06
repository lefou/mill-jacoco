package de.tobiasroeser.mill.jacoco

trait JacocoPlatform {
  implicit def millScoptTasksReads[T] = new mill.main.Tasks.Scopt[T]()
  implicit def millScoptEvaluatorReads[T] = new mill.main.EvaluatorScopt[T]()
}
