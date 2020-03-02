package analysis

import java.net.URL

import org.opalj.Answer
import org.opalj.ai._
import org.opalj.br._
import org.opalj.br.analyses.Project
import org.opalj.br.instructions._
import org.opalj.collection.immutable.Chain

import scala.collection.mutable

class AnalysisDomain(val project: Project[URL], val method: Method)
  extends CorrelationalDomain
    with domain.DefaultDomainValueBinding
    with domain.DefaultHandlingOfMethodResults
    with domain.IgnoreSynchronization
    with domain.ThrowAllPotentialExceptionsConfiguration
    with domain.l0.DefaultTypeLevelFloatValues
    with domain.l0.DefaultTypeLevelDoubleValues
    with domain.l0.TypeLevelFieldAccessInstructions
    with domain.l0.TypeLevelInvokeInstructions
    with domain.l1.DefaultReferenceValuesBinding
    with domain.l1.DefaultIntegerRangeValues
    with domain.l1.DefaultLongValues
    with domain.l1.ConcretePrimitiveValuesConversions
    with domain.l1.LongValuesShiftOperators
    with domain.TheProject
    with domain.TheMethod
    with domain.TheCode
    with domain.PerInstructionPostProcessing {

  val branchingTypes: mutable.MutableList[BranchType] = mutable.MutableList()
  val calledMethodsWithType: mutable.MutableList[CalledMethodResult] = mutable.MutableList()
  val uncoveredInstructions: mutable.HashSet[Class[_]] = mutable.HashSet()


  override def flow(currentPC: PC, currentOperands: Operands, currentLocals: Locals, successorPC: PC, isSuccessorScheduled: Answer, isExceptionalControlFlow: Boolean, abruptSubroutineTerminationCount: RefId, wasJoinPerformed: Boolean, worklist: Chain[PC], operandsArray: OperandsArray, localsArray: LocalsArray, tracer: Option[AITracer]): Chain[PC] = {
    if (method.body.nonEmpty) {
      var computationalType: Option[BranchType] = None

      val instruction = method.body.get.instructions(currentPC)
      //we need to look for compare and load instructions where the successor instruction is an if
      val successorInstruction = method.body.get.instructions(successorPC)
      computationalType =
        successorInstruction match {
          case IFICMPInstruction(_, _) | IFGT(_) | IFLT(_) =>
            Option(ComputationalBranchType(ComputationalTypeInt))
          case _: IFXNullInstruction[_] | _: IFACMPInstruction[_] => {
            // commented out code checks for type of variables that compared to null, probably doesn't make sense
            /*val operand = operandsArray(successorPC)(instr.operandCount - 1).asDomainReferenceValue
            val t = operand.upperTypeBound
            if (t.nonEmpty && ObjectType.String == t.head)
              Option(ReferenceBranchType(t.head))
            else None*/
            None
          }
          case SimpleConditionalBranchInstruction(_) =>
            instruction match {
              // same as above, variables are only loaded before null checking
              /*case LoadLocalVariableInstruction(fieldType, lvIndex) =>
                fieldType match {
                  case ComputationalTypeReference => {
                    val local = localsArray(successorPC)(lvIndex).asDomainReferenceValue
                    val t = local.upperTypeBound
                    if (t.nonEmpty && ObjectType.String == t.head)
                      Option(ReferenceBranchType(t.head))
                    else None
                  }
                  case _ => Option(ComputationalBranchType(fieldType))
                }*/
              case LoadLocalVariableInstruction(fieldType, _) =>
                Option(ComputationalBranchType(fieldType))
              case VirtualMethodInvocationInstruction(_, name, descriptor) =>
                //consider type of variable that the method is executed on
                val relevantOperand = currentOperands(descriptor.parametersCount).asDomainReferenceValue
                if (relevantOperand.upperTypeBound.nonEmpty && relevantOperand.upperTypeBound.head.asObjectType.packageName.matches("^(java/lang|java/util)")) {
                  calledMethodsWithType += CalledMethodResult(name, relevantOperand.upperTypeBound.head)
                  Option(ReferenceBranchType(relevantOperand.upperTypeBound.head))
                } else None
              case DCMPG => Option(ComputationalBranchType(DCMPG.computationalType))
              case DCMPL => Option(ComputationalBranchType(DCMPL.computationalType))
              case FCMPG => Option(ComputationalBranchType(FCMPG.computationalType))
              case FCMPL => Option(ComputationalBranchType(FCMPL.computationalType))
              case LCMP => Option(ComputationalBranchType(LCMP.computationalType))
              case IConstInstruction(_) => Option(ComputationalBranchType(ComputationalTypeInt))
              case StackBasedBinaryArithmeticInstruction(compType) => Option(ComputationalBranchType(compType))
              case FieldReadAccess(_, _, fieldType) =>
                fieldType match {
                  case _: CTIntType => Option(ComputationalBranchType(ComputationalTypeInt))
                  case _: DoubleType => Option(ComputationalBranchType(ComputationalTypeDouble))
                  case _: LongType => Option(ComputationalBranchType(ComputationalTypeLong))
                  case _: FloatType => Option(ComputationalBranchType(ComputationalTypeFloat))
                  case _ => Option(ReferenceBranchType(fieldType.asReferenceType))
                }

              case numericInstruction: NumericConversionInstruction =>
                numericInstruction.targetType match {
                  case _: CTIntType =>
                    Option(ComputationalBranchType(ComputationalTypeInt))
                  case _ => None
                }

              case _ =>
                //observed instruction that are covered by this case: GOTO, ARRAYLENGTH, BALOAD, INVOKESTATIC and SPECIAL, INSTANCEOF
                uncoveredInstructions += instruction.getClass
                None
            }
          case _ => None
        }

      if (computationalType.nonEmpty)
        branchingTypes += computationalType.get
    }

    super.flow(currentPC, currentOperands, currentLocals, successorPC, isSuccessorScheduled, isExceptionalControlFlow, abruptSubroutineTerminationCount, wasJoinPerformed, worklist, operandsArray, localsArray, tracer)
  }
}
