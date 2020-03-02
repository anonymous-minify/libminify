package slicing

import org.opalj.br.ClassFile

import scala.collection.mutable

case class ClassModificationResult(modifiedClassFiles: mutable.Iterable[ClassFile], analysisDeadMethodCount: Int) {

}
