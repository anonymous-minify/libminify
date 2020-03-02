package analysis

import util.control.Breaks._
import extensions.ExtensionMethods._
import org.opalj._
import org.opalj.ai.PC
import org.opalj.br.instructions.{CompoundConditionalBranchInstruction, Instruction, LOOKUPSWITCH, MethodInvocationInstruction, SimpleConditionalBranchInstruction, TABLESWITCH}
import org.opalj.br.{Code, Method}
import slicing.MethodSlicer


import scala.collection.mutable.ListBuffer

/*
  This object is used to find additional removable instructions based on the results of the Abstract Interpretation analysis.
  For the different branching instructions (if,goto, switch, try/catch) it is possible to remove additional instructions in certain situations.
 */
object DeadCodeAnalysis {

  val methodWhitelist: List[String] = List("String.equals", "String.length", "String.startsWith", "String.endsWith", "Integer.equals", "Double.equals", "Float.equals")


  def apply(deadInstructions: List[(PC, Instruction)], method: Method): Iterable[(PC, Instruction)] = {
    if (method.body.isEmpty) return deadInstructions


    val sortedDeadInstructions = deadInstructions.sortBy(tuple => tuple._1)
    val code = method.body.get

    /*
      Detect all instructions that were not evaluated in the current block (e.g. else branch or catch block) including padding instructions
     */
    def getDeadInstructionsInBlock(startingPC: PC): Iterable[(PC, Instruction)] = {
      val instructionsBehindOffset = sortedDeadInstructions.filter(t => t._1 >= startingPC)
      val deadBranchInstructions = instructionsBehindOffset.takeWhile(t1 =>
        sortedDeadInstructions.exists(t2 => t2._1 == code.pcOfNextInstruction(t1._1))).toListBuffer
      if (deadBranchInstructions.isEmpty) {
        if (instructionsBehindOffset.nonEmpty)
        //there is only one instruction
          deadBranchInstructions += instructionsBehindOffset.head
      } else {
        //also include last statement and padding instructions
        val lastStatementPC = code.pcOfNextInstruction(deadBranchInstructions.last._1)
        deadBranchInstructions += ((lastStatementPC, code.instructions(lastStatementPC)))
      }
      deadBranchInstructions
    }

    val instructions = ListBuffer[(PC, Instruction)]()

    /*
      We cannot simply remove all instructions that are not evaluated by the Abstract Interpretation (this caused trouble during initial development).
      For that reason only instructions are removed that belong to some kind of branching instruction. This loop detects these removable code blocks
    */

    for ((pc, instr) <- code) {
      instr match {
        case SimpleConditionalBranchInstruction(offset) if code.instructions(pc + offset) != null && offset > 0 &&
          (deadInstructions.exists(t => t._1 == code.pcOfNextInstruction(pc)) || deadInstructions.exists(t => t._1 == pc + offset)) =>

          var removeIfStatement = false
          if (deadInstructions.exists(t => t._1 == code.pcOfNextInstruction(pc))) {
            //then branch
            val deadBranchInstructions = sortedDeadInstructions.filter(t => t._1 >= code.pcOfNextInstruction(pc) && t._1 < pc + offset)
            val (lastPC, lastInstruction) = deadBranchInstructions.last
            val indexOfNextInstruction = lastInstruction.indexOfNextInstruction(lastPC)(method.body.get)
            val paddingInstructions = List.fill(indexOfNextInstruction - lastPC - 1)(null)
            // the if statement is only removed if the whole then branch is removed (for an else branch this is hard to detect)
            if (lastPC + paddingInstructions.size + 1 == pc + offset) {
              instructions += deadBranchInstructions
              removeIfStatement = true
            }
          } else if (deadInstructions.exists(t => t._1 == pc + offset)) {
            //else branch
            val deadBranchInstructions = getDeadInstructionsInBlock(pc + offset)
            instructions += deadBranchInstructions
            removeIfStatement = true
          }

          if (removeIfStatement) {
            //Also remove the if statement that causes the "dead code"
            val removableInstructions = getRemovableInstructions(pc, instr, code)
            instructions ++= removableInstructions
            instructions += ((pc, instr))
          }

        case switchInstruction: CompoundConditionalBranchInstruction =>

          val deadCases = deadInstructions.filter(t => switchInstruction.jumpOffsets.exists(i => t._1 == i + pc))

          for (deadCase <- deadCases) {
            instructions += getDeadInstructionsInBlock(deadCase._1)
          }

          val sortedPairs = switchInstruction.jumpOffsets.flatMap(i => switchInstruction.caseValueOfJumpOffset(i)._1.toList.map(v => (v, i))).toIndexedSeq.distinct.sortBy(t => t._2)
          val deadOffsets = sortedPairs.filter(pair => {
            val nextOffset = MethodSlicer.getNextOffsetForPair(pair, sortedPairs, switchInstruction.defaultOffset)
            val numberOfRemovedInstructions = MethodSlicer.getNumberOfRemovedInstructionsForPair(pc, method, pair, deadInstructions, nextOffset)
            numberOfRemovedInstructions == nextOffset - pair._2
          }).toList

          if (deadOffsets.size == switchInstruction.jumpOffsets.size) {
            //only the default branch is remaining
            instructions += ((pc, instr))
            val removableInstructions = getRemovableInstructions(pc, switchInstruction, code)
            instructions ++= removableInstructions
          }

        case _ =>
      }

    }

    // see if some catch block needs to be removed as well
    for (exceptionHandler <- code.exceptionHandlers) {
      val relevantInstructions = code.associateWithIndex().filter(t => t._1 >= exceptionHandler.startPC && t._1 <= exceptionHandler.endPC)
      if (relevantInstructions.forall(t => instructions.contains(t))) {
        val endPC = MethodSlicer.findEndPCForCatchBlock(method, exceptionHandler.handlerPC)
        val catchBlockInstructions = code.associateWithIndex().filter(t => t._1 >= exceptionHandler.handlerPC && t._1 < endPC)
        catchBlockInstructions.foreach(t => instructions += t)
      }
    }

    val sortedInstructions = instructions.distinct.sortBy(tuple => tuple._1)

    if (instructions.nonEmpty) {
      for ((pc, instr) <- code) {
        instr match {
          case lookupSwitch: LOOKUPSWITCH if !sortedInstructions.exists(t => t._1 == pc) =>
            //for now default branches will be not removed
            if (deadInstructions.exists(t => t._1 == pc + lookupSwitch.defaultOffset)) {
              val currentPC = pc + lookupSwitch.defaultOffset
              sortedInstructions -= getDeadInstructionsInBlock(currentPC)
            }
          case tableSwitch: TABLESWITCH if !sortedInstructions.exists(t => t._1 == pc) =>
            if (deadInstructions.exists(t => t._1 == pc + tableSwitch.defaultOffset)) {
              val currentPC = pc + tableSwitch.defaultOffset
              sortedInstructions -= getDeadInstructionsInBlock(currentPC)
            }

          case _ =>
        }
      }
    }
    sortedInstructions
  }

  def getRemovableInstructions(pc: PC, instr: Instruction, code: Code): Iterable[(PC, Instruction)] = {
    val instructions = ListBuffer[(PC, Instruction)]()
    var numberOfOperands = instr.numberOfPoppedOperands(NotRequired)
    var counter = 0
    var previousPC = pc
    breakable {
      while (counter < numberOfOperands) {
        previousPC = code.pcOfPreviousInstruction(previousPC)
        val previousInstruction = code.instructions(previousPC)
        counter += previousInstruction.numberOfPushedOperands(NotRequired)
        numberOfOperands += previousInstruction.numberOfPoppedOperands(NotRequired)
        if (checkForWhitelist(previousInstruction)) {
          instructions += ((previousPC, previousInstruction))
        } else
          break
      }
    }
    instructions
  }

  def checkForWhitelist(instruction: Instruction): Boolean = {
    if (instruction.isMethodInvocationInstruction) {
      val invocationInstruction = instruction.asInstanceOf[MethodInvocationInstruction]
      val virtualMethod = invocationInstruction.asVirtualMethod
      val objectType = virtualMethod.declaringClassType.asObjectType
      if (methodWhitelist.contains(objectType.simpleName + "." + virtualMethod.name)) {
        return true
      } else return false
    }
    true
  }

}



