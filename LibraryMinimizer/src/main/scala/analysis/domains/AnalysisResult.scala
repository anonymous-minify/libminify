package analysis.domains
import org.opalj.br.instructions.Instruction
import org.opalj.br.{Method, ObjectType, PC}

import scala.collection.mutable.ListBuffer

case class AnalysisResult(deadInstructionsPerMethod: Map[Method, Iterable[(PC, Instruction)]],
                          analyzedInstructionsCount:Int,
                          visitedMethods: Iterable[Method],
                          unresolvedInterfaceCalls: Map[ObjectType, ListBuffer[String]],
                          failedMethodCalls: Iterable[Method]) {

}
