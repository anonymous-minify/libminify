package analysis

import org.opalj.br.{ComputationalType, ReferenceType}

trait BranchType {

}

case class ComputationalBranchType(computationalType: ComputationalType) extends BranchType {
  override def toString: String = computationalType.toString
}

object ComputationalBranchType {
  def apply(computationalType: ComputationalType): ComputationalBranchType = new ComputationalBranchType(computationalType)
}

case class ReferenceBranchType(referenceType: ReferenceType) extends BranchType {
  override def toString: String = referenceType.toString
}

object ReferenceBranchType {
  def apply(referenceType: ReferenceType): ReferenceBranchType = new ReferenceBranchType(referenceType)
}