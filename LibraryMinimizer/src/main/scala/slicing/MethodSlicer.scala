package slicing

import extensions.ExtensionMethods._
import org.opalj.br.instructions.{CompoundConditionalBranchInstruction, GOTO, IFEQ, IFGE, IFGT, IFLE, IFLT, IFNE, IFNONNULL, IFNULL, IF_ACMPEQ, IF_ACMPNE, IF_ICMPEQ, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ICMPLT, IF_ICMPNE, Instruction, LOOKUPSWITCH, SimpleConditionalBranchInstruction, SimpleConditionalBranchInstructionLike, TABLESWITCH, UnconditionalBranchInstruction}
import org.opalj.br.{Code, ExceptionHandler, Method, PC}

import scala.collection.mutable

object MethodSlicer {

  /*
      The steps for slicing are as follows:
       1. Create a list which only contains kept instructions and insert padding instructions for non-switch instructions
       2. Rewrite switch statements and insert padding instructions (for the rewriting the pc is important, but is a variable length instruction)
       3. Rewrite ifs and gotos
   */
  def sliceMethod(method: Method, deadInstructions: Iterable[(PC, Instruction)], debug: Boolean): SlicingResult = {
    if (method.body.isEmpty || deadInstructions.isEmpty) return SlicingSuccess(method, method.copy(), None)
    val code = method.body.get
    val remainingInstructions = method.body.get.withFilter(i => i != null && !deadInstructions.exists(t => t._1 == i._1)).map(t => t._1)
    val modifiableDeadInstructions = deadInstructions.toMutableList
    val insertedInstructions = mutable.MutableList[(PC, Instruction)]()
    val newPCtoOldPCMap = mutable.Map[Int, Int]()
    var finalInstructionList = mutable.MutableList[Instruction]()
    val exceptionHandlers = mutable.ListBuffer.empty[ExceptionHandler]
    for (pc <- remainingInstructions) {
      val instruction = method.body.get.instructions(pc)
      finalInstructionList += instruction
      val newPC = finalInstructionList.length - 1
      newPCtoOldPCMap += ((newPC, pc))
      if (!instruction.isInstanceOf[CompoundConditionalBranchInstruction]) {
        val paddingInstructions = getPaddingInstructions(newPC, instruction, method)
        finalInstructionList += paddingInstructions
      }
    }

    try {
      //only rewrite switch statements
      val switchPCs = finalInstructionList.zipWithIndex.filter(t => t._1 != null && t._1.isInstanceOf[CompoundConditionalBranchInstruction]).map(t => t._2).toMutableList
      for (index <- switchPCs.indices) {
        val pc = switchPCs(index)
        val instr = finalInstructionList(pc)
        val rewrittenInstruction: Option[Instruction] = rewriteInstruction(newPCtoOldPCMap(pc), pc, instr, method, modifiableDeadInstructions, insertedInstructions)
        rewrittenInstruction match {
          case Some(newInstr) =>
            val paddingInstructions = getPaddingInstructions(pc, newInstr, method)
            val tempInstructionList = finalInstructionList.slice(0, pc)
            tempInstructionList += newInstr
            tempInstructionList ++= paddingInstructions
            tempInstructionList ++= finalInstructionList.slice(pc + 1, finalInstructionList.length)
            finalInstructionList = tempInstructionList
            for (shiftPC <- switchPCs.slice(index + 1, switchPCs.length)) {
              switchPCs.update(switchPCs.indexOf(shiftPC), shiftPC + paddingInstructions.length)
            }

            //update pc map
            val effectedPCs = newPCtoOldPCMap.filter(t => t._1 > pc)
            val newPCs = effectedPCs.map(t => (t._1 + paddingInstructions.length, t._2))
            //remove effected pcs
            effectedPCs.foreach(effectedPC => newPCtoOldPCMap.remove(effectedPC._1))
            //insert new ones
            newPCs.foreach(newPCtoOldPCMap.+=)
          case _ =>
        }
      }

      //only rewrite if and gotos
      val ifAndGotoPCs = finalInstructionList.zipWithIndex.filter(t => t._1 != null && (t._1.isGotoInstruction || t._1.isInstanceOf[SimpleConditionalBranchInstructionLike])).map(t => t._2)
      for (pc <- ifAndGotoPCs) {
        val instr = finalInstructionList(pc)
        val rewrittenInstruction: Option[Instruction] = rewriteInstruction(newPCtoOldPCMap(pc), pc, instr, method, modifiableDeadInstructions, insertedInstructions)
        rewrittenInstruction match {
          case Some(newInstr) =>
            finalInstructionList.update(pc, newInstr)
          case _ =>
        }
      }

      //handle exception handlers
      for (exceptionHandler <- method.body.get.exceptionHandlers) {
        val sortedPCMap = newPCtoOldPCMap.toList.sortBy(t => t._1)
        val relevantInstructions = sortedPCMap.filter(t => t._2 >= exceptionHandler.startPC && t._2 <= exceptionHandler.endPC)
        val catchBlockEndPC = findEndPCForCatchBlock(method, exceptionHandler.handlerPC)
        val catchBlockInstructions = sortedPCMap.filter(t => t._2 >= exceptionHandler.handlerPC && t._2 < catchBlockEndPC)
        if (relevantInstructions.nonEmpty && catchBlockInstructions.nonEmpty) {
          exceptionHandlers += ExceptionHandler(relevantInstructions.head._1, relevantInstructions.last._1, catchBlockInstructions.head._1, exceptionHandler.catchType)
        }
      }

    } catch {
      case e: Throwable =>
        if (debug) {
          println(method.classFile + " " + method.toJava)
          println(finalInstructionList.mkString("\n"))
          println("Slicing failed!")
          println(e)
        }
        return SlicingFailure(method, deadInstructions, finalInstructionList)
    }
    var newBody: Option[Code] = None
    if (finalInstructionList.nonEmpty) {
      try {
        newBody = Option(Code(Code.computeMaxStack(finalInstructionList.toArray), Code.computeMaxLocals(!method.isStatic, method.descriptor, finalInstructionList.toArray), finalInstructionList.toArray, exceptionHandlers = exceptionHandlers.toIndexedSeq))
      } catch {
        case e: Exception =>
          if (debug) {
            println(method.classFile + " " + method.toJava)
            println(finalInstructionList.mkString("\n"))
            println("computeMaxStack or computeMaxLocalsRequired failed!")
            println(e)
          }
          return SlicingFailure(method, deadInstructions, finalInstructionList)
      }
    } else {
      newBody = Option(Code(0, 0, finalInstructionList.toArray, exceptionHandlers = exceptionHandlers.toIndexedSeq))
    }

    SlicingSuccess(method, method.copy(body = newBody), Option(newPCtoOldPCMap.toMap))
  }

  def getPaddingInstructionsForPC(pc: PC, method: Method): List[Instruction] = {
    method.body.get.instructions.drop(pc + 1).takeWhile(i => i eq null).toList
  }

  def getPaddingInstructions(pc: PC, instr: Instruction, method: Method): List[Instruction] = {
    if (instr == null)
      return List()

    val indexOfNextInstruction = instr.indexOfNextInstruction(pc)(method.body.get)
    val paddingInstructions = List.fill(indexOfNextInstruction - pc - 1)(null)
    paddingInstructions
  }

  // the idea to find the end of a catch block is that a catch block is only entered if an exception is thrown.
  // So there are no direct jumps into the catch block, but possible into the block after the catch (if there is one)
  def findEndPCForCatchBlock(method: Method, handlerPC: PC): Int = {
    val jumpTargets = method.body.get.map(t => {
      t._2 match {
        case UnconditionalBranchInstruction(o) => Option(t._1 + o)
        case SimpleConditionalBranchInstruction(o) => Option(t._1 + o)
        case _ => None
      }
    }).collect {
      case Some(i) => i
    }

    val largerHandlerPCs = method.body.get.exceptionHandlers.filter(e => e.handlerPC > handlerPC).map(e => e.handlerPC)
    val largerJumpTargets = (jumpTargets.filter(t => t > handlerPC) ++ largerHandlerPCs).sorted
    if (largerJumpTargets.nonEmpty)
      return largerJumpTargets.head

    // the catch block is at the end of the method
    method.body.get.programCounters.max + 1
  }

  def rewriteInstruction(oldPC: PC, newPC: PC, instruction: Instruction, method: Method, deadInstructions: mutable.MutableList[(PC, Instruction)], insertedInstructions: mutable.MutableList[(PC, Instruction)]): Option[Instruction] = {
    instruction match {
      case UnconditionalBranchInstruction(offset) => rewriteIFandGOTO(oldPC, newPC, instruction, method, offset, deadInstructions, insertedInstructions)
      case SimpleConditionalBranchInstruction(offset) => rewriteIFandGOTO(oldPC, newPC, instruction, method, offset, deadInstructions, insertedInstructions)
      case _: CompoundConditionalBranchInstruction => rewriteSwitch(oldPC, newPC, instruction, method, deadInstructions, insertedInstructions)
      case instr => Option(instr)
    }
  }

  def rewriteSwitch(oldPC: PC, newPC: PC, instruction: Instruction, method: Method, deadInstructions: mutable.MutableList[(PC, Instruction)], insertedInstructions: mutable.MutableList[(PC, Instruction)]): Option[Instruction] = {
    instruction match {
      case LOOKUPSWITCH(defaultOffset, npairs) =>
        val npairsSorted = npairs.sortBy(pair => pair._2)

        val remainingPairs = npairs.filterNot(pair => {
          if (pair._2 != defaultOffset) {
            val nextOffset = getNextOffsetForPair(pair, npairsSorted, defaultOffset)
            val removedInstructionsCount = getNumberOfRemovedInstructionsForPair(oldPC, method, pair, deadInstructions, nextOffset)
            removedInstructionsCount == nextOffset - pair._2
          } else false
        })

        //taken from LookupSwitch implementation (no other method known)
        val startOffset = 1 + (3 - (newPC % 4)) + 8 + remainingPairs.size * 8
        val (newPairs, newOffset) = calculateNewOffsets(oldPC, method, deadInstructions, insertedInstructions, remainingPairs, npairsSorted, defaultOffset, startOffset)

        Option(LOOKUPSWITCH(newOffset, npairs = newPairs.sortBy(t => t._1).toIndexedSeq))

      case TABLESWITCH(defaultOffset, low, high, jumpOffsets) =>

        val startOffset = 1 + (3 - (newPC % 4)) + 12 + (high - low + 1) * 4
        //val shift = jumpOffsets.minBy(i => i) - startOffset
        //val newJumpOffsets = jumpOffsets.map(j => j - shift)

        val sortedPairs = Range(0, high - low + 1).map(i => (i, jumpOffsets(i))).sortBy(t => t._2)
        val (newJumpPairs, newOffset) = calculateNewOffsets(oldPC, method, deadInstructions, insertedInstructions, sortedPairs, sortedPairs, defaultOffset, startOffset)

        val newJumpOffsets = newJumpPairs.sortBy(p => p._1).map(p => p._2)
        Option(TABLESWITCH(newOffset, low, high, newJumpOffsets.toIndexedSeq))
      //val deadOffsets = jumpOffsets.filter(o => deadInstructions.exists(t => oldPC + o == t._1))
      //Option(table.copy(defaultOffset, jumpOffsets = jumpOffsets.filterNot(o => deadOffsets.contains(o))))
      case _ => Option(instruction)
    }
  }

  def calculateNewOffsets(oldPC: PC, method: Method, deadInstructions: mutable.MutableList[(PC, Instruction)], insertedInstructions: mutable.MutableList[(PC, Instruction)],
                          remainingPairs: IndexedSeq[(Int, Int)], sortedPairs: IndexedSeq[(Int, Int)], defaultOffset: Int, startOffset: Int): (mutable.MutableList[(PC, PC)], PC) = {
    //check if padding instructions were removed or if new instructions were inserted
    if (startOffset < sortedPairs.head._2) {
      deadInstructions += Range(oldPC + 1, oldPC + (sortedPairs.head._2 - startOffset) + 1).map(i => (i, null))
    } else if (startOffset > sortedPairs.head._2) {
      insertedInstructions += Range(oldPC + 1, oldPC + (startOffset - sortedPairs.head._2) + 1).map(i => (i, null))
    }

    //
    var defaultPairs = mutable.ListBuffer[PC]()
    //group pairs by offsets to solve problem with multiple cases with the same offset
    val groupedPairs = remainingPairs.groupBy(pair => pair._2).toList.sortBy(t => t._1)
    var offset = startOffset
    val newPairs = mutable.MutableList[(PC, PC)]()
    for ((pairOffset, pairs) <- groupedPairs) {
      //since the offset is the same for every pair it doesn't matter which pair we chose
      val pair = pairs.head
      val nextOffset = getNextOffsetForPair(pair, sortedPairs, defaultOffset)
      val numberOfRemovedInstructions = getNumberOfRemovedInstructionsForPair(oldPC, method, pair, deadInstructions, nextOffset)
      val numberOfInstructions = nextOffset - pairOffset - numberOfRemovedInstructions

      //in case numberOfInstructions is 0 the cases should be put together with the default case not with the following case
      if (numberOfInstructions == 0) {
        defaultPairs ++= pairs.map(pair => pair._1)
      } else {
        newPairs += pairs.map(pair => (pair._1, offset))
        offset = offset + numberOfInstructions
      }
    }

    newPairs ++= defaultPairs.map(pc => (pc, offset))

    (newPairs, offset)
  }

  def getNextOffsetForPair(pair: (Int, Int), sortedPairs: IndexedSeq[(Int, Int)], defaultOffset: Int): Int = {
    var nextOffset = 0
    val index = sortedPairs.indexOf(pair)
    if (index < sortedPairs.size - 1) {
      // at beginning or middle
      val nextPair = sortedPairs.slice(index + 1, sortedPairs.size).find(pair2 => pair2._2 > pair._2)
      if (nextPair.isDefined) {
        nextOffset = nextPair.get._2

      } else {
        // we didn't find a pair with a larger offset
        nextOffset = defaultOffset
      }
    } else {
      // last case
      nextOffset = defaultOffset
    }
    nextOffset
  }

  def getNumberOfRemovedInstructionsForPair(oldPC: PC, method: Method, pair: (Int, Int), deadInstructions: Iterable[(PC, Instruction)], nextOffset: Int): Int = {
    val removedInstructions = deadInstructions.filter(t => t._1 >= oldPC + pair._2 && t._1 < oldPC + nextOffset)
    if (removedInstructions.nonEmpty) {
      val numberOfRemovedInstructions = removedInstructions.map(t => getPaddingInstructions(t._1, t._2, method).size + 1).sum
      numberOfRemovedInstructions
    } else 0
  }


  def rewriteIFandGOTO(oldPC: PC, newPC: PC, instruction: Instruction, method: Method, offset: Int, deadInstructions: Iterable[(PC, Instruction)], insertedInstructions: mutable.MutableList[(PC, Instruction)]): Option[Instruction] = {
    def correctOffset(pc: PC, offset: Int, method: Method): Int = {
      var newOffset = offset
      if (offset >= 0) {
        val relevantDeadInstructions = deadInstructions.filter(p => p._1 < pc + offset && p._1 > pc)
        val relevantInsertedInstructions = insertedInstructions.filter(p => p._1 < newPC + offset && p._1 > newPC)
        if (relevantDeadInstructions.nonEmpty)
          newOffset -= relevantDeadInstructions.map(t => getPaddingInstructions(t._1, t._2, method).size + 1).sum
        if (relevantInsertedInstructions.nonEmpty)
          newOffset += relevantInsertedInstructions.size

      } else {
        val relevantInstructions = deadInstructions.filter(p => p._1 < pc && p._1 > pc + offset)
        val relevantInsertedInstructions = insertedInstructions.filter(p => p._1 < pc && p._1 > pc + offset)
        if (relevantInstructions.nonEmpty) {
          newOffset += relevantInstructions.map(t => getPaddingInstructions(t._1, t._2, method).size + 1).sum - relevantInsertedInstructions.size
        }
        if (relevantInsertedInstructions.nonEmpty)
          newOffset -= relevantInsertedInstructions.size
      }

      newOffset
    }

    val correctedOffset = correctOffset(oldPC, offset, method)
    /*if (offset - correctedOffset == 2)
    //for now only the instruction is removed but not the predecessor instructions
      return None*/

    val rewrittenInstruction: Instruction = instruction match {
      case GOTO(_) => GOTO(correctedOffset)
      case IFEQ(_) => IFEQ(correctedOffset)
      case IFNE(_) => IFNE(correctedOffset)
      case IFGE(_) => IFGE(correctedOffset)
      case IFGT(_) => IFGT(correctedOffset)
      case IFLE(_) => IFLE(correctedOffset)
      case IFLT(_) => IFLT(correctedOffset)
      case IFNULL(_) => IFNULL(correctedOffset)
      case IFNONNULL(_) => IFNONNULL(correctedOffset)
      case IF_ACMPEQ(_) => IF_ACMPEQ(correctedOffset)
      case IF_ACMPNE(_) => IF_ACMPNE(correctedOffset)
      case IF_ICMPEQ(_) => IF_ICMPEQ(correctedOffset)
      case IF_ICMPLE(_) => IF_ICMPLE(correctedOffset)
      case IF_ICMPNE(_) => IF_ICMPNE(correctedOffset)
      case IF_ICMPGT(_) => IF_ICMPGT(correctedOffset)
      case IF_ICMPGE(_) => IF_ICMPGE(correctedOffset)
      case IF_ICMPLT(_) => IF_ICMPLT(correctedOffset)
      case instr => instr
    }

    Some(rewrittenInstruction)
  }
}
