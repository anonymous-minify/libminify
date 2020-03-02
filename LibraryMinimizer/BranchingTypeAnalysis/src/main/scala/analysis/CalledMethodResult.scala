package analysis

import org.opalj.br.ReferenceType

case class CalledMethodResult(methodName: String, referenceType: ReferenceType) {
  override def toString: String = s"$methodName,${referenceType.toString}"
}

object CalledMethodResult{
  def apply(methodName: String, referenceType: ReferenceType): CalledMethodResult = new CalledMethodResult(methodName, referenceType)
}
