package slicing

import org.opalj.br.instructions.Instruction
import org.opalj.br.{Method, MethodTemplate, PC}

abstract class SlicingResult {

}

case class SlicingSuccess(originalMethod: Method, methodTemplate: MethodTemplate, newPCtoOldPCMap: Option[Map[Int, Int]]) extends SlicingResult

case class SlicingFailure(originalMethod: Method, deadInstructions: Iterable[(PC, Instruction)], instructionList: Iterable[Instruction]) extends SlicingResult
