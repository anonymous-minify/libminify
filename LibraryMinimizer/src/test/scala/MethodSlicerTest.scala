import org.opalj.br.MethodTemplate
import slicing.{MethodSlicer, SlicingSuccess}
import org.junit.Assert._
import org.opalj.br.instructions.{CompoundConditionalBranchInstruction, SimpleConditionalBranchInstruction, SimpleConditionalBranchInstructionLike, TABLESWITCH}


class MethodSlicerTest extends AnalysisTestCase {
  override val classFileName = "MethodSlicerTestCode.class"

  def testIfOffsetRewrite(): Unit = {
    val methodName = "testIfRewrite"
    val method = getMethod(methodName)
    val low = 7
    val high = 13
    val offsetBefore = method.body.get.instructions(1).asSimpleConditionalBranchInstruction.branchoffset
    val template = getSlicedMethod(methodName, low, high)
    val offset = template.body.get.instructions(1).asSimpleConditionalBranchInstruction.branchoffset //this is the if instruction
    assertEquals(offsetBefore - ((high - low) + 1), offset)
  }

  def testGotoOffsetRewrite(): Unit = {
    val methodName = "testIfRewrite"
    val low = 19
    val high = 35
    val template = getSlicedMethod(methodName, low, high)
    val offset = template.body.get.instructions.find(i => i != null && i.isGotoInstruction).get.asGotoInstruction.branchoffset //this is the if instruction

    assertEquals(3, offset)
  }

  def testGotoNegativeOffsetRewrite(): Unit = {
    val methodName = "testGotoNegativeRewriteTest"
    val low = 5
    val high = 14
    val methodTemplate = getSlicedMethod(methodName, low, high)
    val offset = methodTemplate.body.get.instructions.find(i => i != null && i.isGotoInstruction).get.asGotoInstruction.branchoffset //this is the if instruction
    assertEquals(-5, offset)
  }

  def testSwitchCaseRemoval(): Unit = {
    val methodName = "testSwitchRewrite"
    val method = getMethod(methodName)
    val instructionBefore = method.body.get.instructions.find(i => i != null && i.isInstanceOf[CompoundConditionalBranchInstruction]).get.asInstanceOf[CompoundConditionalBranchInstruction]
    val low = 30
    val high = 44
    val methodTemplate = getSlicedMethod(methodName, low, high)
    val instruction = methodTemplate.body.get.instructions.find(i => i != null && i.isInstanceOf[CompoundConditionalBranchInstruction]).get.asInstanceOf[CompoundConditionalBranchInstruction]

    val initialOffset = instruction.indexOfNextInstruction(methodTemplate.body.get.instructions.indexOf(instruction))(methodTemplate.body.get)
    val initialOffsetBefore = instructionBefore.indexOfNextInstruction(method.body.get.instructions.indexOf(instructionBefore))(method.body.get)

    //the default offset should haven been decreased by the number of instructions that have been removed (15)
    // and the number of padding instructions should also have been decreased since the table size was decreased
    val expectedDefaultOffset = instructionBefore.defaultOffset - (initialOffsetBefore - initialOffset) - (high - low + 1)
    assertEquals(expectedDefaultOffset, instruction.defaultOffset)
  }

  def testTableSwitchCaseRewrite(): Unit = {
    val methodName = "testTableSwitch"
    val method = getMethod(methodName)
    val instructionBefore = method.body.get.instructions.find(i => i != null && i.isInstanceOf[TABLESWITCH]).get.asInstanceOf[CompoundConditionalBranchInstruction]
    val low = 119
    val high = 120
    val methodTemplate = getSlicedMethod(methodName, low, high)
    val instruction = methodTemplate.body.get.instructions.find(i => i != null && i.isInstanceOf[TABLESWITCH]).get.asInstanceOf[CompoundConditionalBranchInstruction]

    //as the instructions of the second case are removed this case should have the same offset as the default case
    assertEquals(instruction.jumpOffsets.toIndexedSeq(1), instruction.defaultOffset)
  }

  def testSwitchCaseRemovalMultipleCases(): Unit = {
    val methodName = "testSwitchRewriteMultipleCases"
    val method = getMethod(methodName)
    val instructionBefore = method.body.get.instructions.find(i => i != null && i.isInstanceOf[CompoundConditionalBranchInstruction]).get.asInstanceOf[CompoundConditionalBranchInstruction]
    val low = 62
    val high = 76
    val methodTemplate = getSlicedMethod(methodName, low, high)
    val instruction = methodTemplate.body.get.instructions.find(i => i != null && i.isInstanceOf[CompoundConditionalBranchInstruction]).get.asInstanceOf[CompoundConditionalBranchInstruction]

    val initialOffset = instruction.indexOfNextInstruction(methodTemplate.body.get.instructions.indexOf(instruction))(methodTemplate.body.get)
    val initialOffsetBefore = instructionBefore.indexOfNextInstruction(method.body.get.instructions.indexOf(instructionBefore))(method.body.get)

    //the default offset should haven been decreased by the number of instructions that have been removed (15)
    // and the number of padding instructions should also have been decreased since the table size was decreased
    val expectedDefaultOffset = instructionBefore.defaultOffset - (initialOffsetBefore - initialOffset) - (high - low + 1)
    assertEquals(expectedDefaultOffset, instruction.defaultOffset)

    //the case which has the same offset as the default offset should not be removed
    assertTrue(instruction.jumpOffsets.exists(i => i == instruction.defaultOffset))
  }

  def testSwitchCaseRewriting(): Unit = {
    val methodName = "testSwitchRewrite"
    val method = getMethod(methodName)
    val instructionBefore = method.body.get.instructions.find(i => i != null && i.isInstanceOf[CompoundConditionalBranchInstruction]).get.asCompoundConditionalBranchInstruction
    val low = 34
    val high = 40
    val methodTemplate = getSlicedMethod(methodName, low, high)
    val instruction = methodTemplate.body.get.instructions.find(i => i != null && i.isInstanceOf[CompoundConditionalBranchInstruction]).get.asCompoundConditionalBranchInstruction

    val initialOffset = instruction.indexOfNextInstruction(methodTemplate.body.get.instructions.indexOf(instruction))(methodTemplate.body.get)
    val initialOffsetBefore = instructionBefore.indexOfNextInstruction(method.body.get.instructions.indexOf(instructionBefore))(method.body.get)

    //the default offset should haven been decreased by the number of instructions that have been removed (7)
    val expectedDefaultOffset = instructionBefore.defaultOffset - (initialOffsetBefore - initialOffset) - 7
    assertEquals(expectedDefaultOffset, instruction.defaultOffset)

    //the offsets of the cases should have stayed the same
    assertEquals(instructionBefore.jumpOffsets, instruction.jumpOffsets)
  }


  /*
    This test should verify that the switch rewriting works correctly when code before the switch statement is removed.
    Since switch is a variable length (according to JVM spec default offset has to be put on address mod 4) the number of padding instructions changes
   */

  def testSwitchRewritingCodeRemovalBefore(): Unit = {
    val methodName = "testSwitchRewriteCodeRemovalBefore"
    val method = getMethod(methodName)
    val instructionBefore = method.body.get.instructions.find(i => i != null && i.isInstanceOf[CompoundConditionalBranchInstruction]).get.asCompoundConditionalBranchInstruction
    val low = 0
    val high = 4
    val methodTemplate = getSlicedMethod(methodName, low, high)
    val instruction = methodTemplate.body.get.instructions.find(i => i != null && i.isInstanceOf[CompoundConditionalBranchInstruction]).get.asCompoundConditionalBranchInstruction

    // The calculation contains -(PC mod 4), since the PC changed from 8 to 1 the default offset was reduced by 1
    assertEquals(instructionBefore.defaultOffset - 1, instruction.defaultOffset)
  }

  def testNestedIfSwitchCaseRemoval(): Unit = {
    val methodName = "testSwitchRewriteCodeRemovalBefore"
    val method = getMethod(methodName)
    val switchInstructionBefore = method.body.get.instructions.find(i => i != null && i.isInstanceOf[CompoundConditionalBranchInstruction]).get.asInstanceOf[CompoundConditionalBranchInstruction]
    val ifInstructionBefore = method.body.get.instructions.find(i => i != null && i.isInstanceOf[SimpleConditionalBranchInstructionLike]).get.asSimpleConditionalBranchInstruction

    val low = 38
    val high = 52
    val methodTemplate = getSlicedMethod(methodName, low, high)
    val switchInstruction = methodTemplate.body.get.instructions.find(i => i != null && i.isInstanceOf[CompoundConditionalBranchInstruction]).get.asInstanceOf[CompoundConditionalBranchInstruction]
    val ifInstruction = methodTemplate.body.get.instructions.find(i => i != null && i.isInstanceOf[SimpleConditionalBranchInstructionLike]).get.asSimpleConditionalBranchInstruction

    val initialOffset = switchInstruction.indexOfNextInstruction(methodTemplate.body.get.instructions.indexOf(switchInstruction))(methodTemplate.body.get)
    val initialOffsetBefore = switchInstructionBefore.indexOfNextInstruction(method.body.get.instructions.indexOf(switchInstructionBefore))(method.body.get)
    val difference = initialOffsetBefore - initialOffset

    //The difference between the offsets for the if has to be greater than the number of instructions that are removed
    assertEquals(difference, ifInstructionBefore.branchoffset - ifInstruction.branchoffset - (high - low + 1))
  }

  def testNestedWhileSwitchCaseRemoval(): Unit = {
    val methodName = "testSwitchRewriteWhile"
    val method = getMethod(methodName)
    val switchInstructionBefore = method.body.get.instructions.find(i => i != null && i.isInstanceOf[CompoundConditionalBranchInstruction]).get.asInstanceOf[CompoundConditionalBranchInstruction]
    val gotoInstructionBefore = method.body.get.instructions.find(i => i != null && i.isGotoInstruction).get.asGotoInstruction

    val low = 41
    val high = 58
    val methodTemplate = getSlicedMethod(methodName, low, high)
    val switchInstruction = methodTemplate.body.get.instructions.find(i => i != null && i.isInstanceOf[CompoundConditionalBranchInstruction]).get.asInstanceOf[CompoundConditionalBranchInstruction]
    val gotoInstruction = methodTemplate.body.get.instructions.find(i => i != null && i.isGotoInstruction).get.asGotoInstruction

    val initialOffset = switchInstruction.indexOfNextInstruction(methodTemplate.body.get.instructions.indexOf(switchInstruction))(methodTemplate.body.get)
    val initialOffsetBefore = switchInstructionBefore.indexOfNextInstruction(method.body.get.instructions.indexOf(switchInstructionBefore))(method.body.get)
    val difference = initialOffsetBefore - initialOffset

    //The difference between the offsets for the if has to be greater than the number of instructions that are removed
    assertEquals(difference, (gotoInstructionBefore.branchoffset - gotoInstruction.branchoffset).abs)
  }


  def testMultipleNestedIfsRemoval(): Unit = {
    val methodName = "testMultipleNestedIfs"
    val method = getMethod(methodName)
    val aiResult = method.body.get.withFilter(t => (t._1 > 33 && t._1 < 69) || (t._1 > 73 && t._1 < 77) || (t._1 > 84 && t._1 < 115)).map(t => t).toList
    val result = MethodSlicer.sliceMethod(method, aiResult, true)
    result match {
      case SlicingSuccess(_, template, _) =>
        val result = method.body.get.instructionsCount - template.body.get.instructionsCount
        assertEquals(aiResult.size, result)
      case _ => throw new UnsupportedOperationException("Slicing failed in test")
    }

  }

  def testTryCorrection(): Unit = {
    val methodName = "testTryCorrection"
    val method = getMethod(methodName)
    val aiResult = method.body.get.withFilter(t => (t._1 > 1 && t._1 < 5) || (t._1 > 13 && t._1 < 18) || (t._1 > 53 && t._1 < 58) || (t._1 > 63 && t._1 < 69)).map(t => t).toList
    val result = MethodSlicer.sliceMethod(method, aiResult, true)
    result match {
      case SlicingSuccess(_, template, _) =>
        // 9 instructions were removed inside of the try-catch block, every exception handler needs to be corrected accordingly
        for (i <- method.body.get.exceptionHandlers.indices) {
          val oldExceptionHandler = method.body.get.exceptionHandlers(i)
          val newExceptionHandler = template.body.get.exceptionHandlers(i)
          if (oldExceptionHandler.endPC > 17)
            assertEquals(oldExceptionHandler.endPC - 9, newExceptionHandler.endPC)
        }
      case _ => throw new UnsupportedOperationException("Slicing failed in test")
    }
  }

  def testTryRemoval(): Unit = {
    val methodName = "testTryCorrection"
    val method = getMethod(methodName)
    val aiResult = method.body.get.withFilter(t => (t._1 > 1 && t._1 < 5) || (t._1 > 13 && t._1 < 18) || (t._1 > 53 && t._1 < 58) || (t._1 > 63 && t._1 < 69) || (t._1 > 35 && t._1 < 46)).map(t => t).toList
    val result = MethodSlicer.sliceMethod(method, aiResult, true)
    result match {
      case SlicingSuccess(_, template, _) =>
        // the catch block for the Exception was removed so there should be two exception handlers less (the finally handler for this catch block also was removed)
        assertEquals(method.body.get.exceptionHandlers.size - 2, template.body.get.exceptionHandlers.size)

      case _ => throw new UnsupportedOperationException("Slicing failed in test")
    }
  }

  def getSlicedMethod(methodName: String, low: Int, high: Int): MethodTemplate = {
    val method = getMethod(methodName)
    val deadInstructions = method.body.get.withFilter(t => t._1 >= low && t._1 <= high).map(t => t).toList
    val result = MethodSlicer.sliceMethod(method, deadInstructions, true)
    result match {
      case SlicingSuccess(_, template, _) => template
      case _ => throw new UnsupportedOperationException("Slicing failed in test")
    }
  }
}
