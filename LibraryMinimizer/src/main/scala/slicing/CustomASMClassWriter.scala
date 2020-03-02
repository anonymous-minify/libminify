package slicing

import org.opalj.br.ClassHierarchy

/*
This custom class writer is needed in order to do class resolution with the OPAL type hierarchy.
First the class is loaded per reflection (to get precise information for JDK classes), but if that fails the OPAL class library is used.
 */
class CustomASMClassWriter(val flags: Int, classHierarchy: ClassHierarchy) extends org.objectweb.asm.ClassWriter(flags) {
  override def getCommonSuperClass(type1: String, type2: String): String = {
    try {
      super.getCommonSuperClass(type1, type2)
    } catch {
      case e: RuntimeException =>
        val class1Opt = classHierarchy.supertypes.keys.find(c => c.fqn.equals(type1))
        val class2Opt = classHierarchy.supertypes.keys.find(c => c.fqn.equals(type2))
        class1Opt match {
          case Some(class1) =>
            class2Opt match {
              case Some(class2) =>
                if (classHierarchy.isSubtypeOf(class2, class1).isYes) {
                  class1.fqn
                }
                else if (classHierarchy.isSubtypeOf(class1, class2).isYes) {
                  class2.fqn
                } else {
                  //if (classHierarchy.isSupertypeInformationComplete(class1) && classHierarchy.isSupertypeInformationComplete(class2)) {
                  val supertypes1 = classHierarchy.allSuperclassTypesInInitializationOrder(class1).s.reverse.toList
                  val supertypes2 = classHierarchy.allSuperclassTypesInInitializationOrder(class2).s.reverse.toList
                  val intersect = supertypes1.intersect(supertypes2)
                  if (intersect.nonEmpty) {
                    return intersect.head.fqn
                  }

                  //}
                  throw e
                }
              case _ => throw e
            }
          case _ => throw e
        }
      case t: Throwable => throw t
    }
  }
}
