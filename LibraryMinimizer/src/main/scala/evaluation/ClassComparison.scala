package evaluation

import org.opalj.br.ClassFile

import scala.collection.mutable

/*
  This class compares the original class and the sliced class based on the comparison results of the methods
 */
class ClassComparison(val originalClass: ClassFile, val modifiedClass: ClassFile) {
  def methodCountReduction(): Double = 1 - (modifiedClass.methods.size.toDouble / originalClass.methods.size)

  def instructionReductionByMethod(): Iterable[MethodComparison] = {
    val list = mutable.MutableList[MethodComparison]()
    for (method <- modifiedClass.methods) {
      originalClass.methods.find(m => m.name == method.name && m.signature.equals(method.signature)) match {
        case Some(originalMethod) if originalMethod.body.nonEmpty =>
          list += new MethodComparison(originalMethod, method)
        case _ =>
          print()
      }
    }

    list
  }

}


