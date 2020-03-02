package analysis.domains

import java.net.URL

import analysis.analyses.{PreciseFieldValueInformation, StaticFieldValueInformation}
import org.opalj.ai._
import org.opalj.ai.analyses.cg.CallGraphCache
import org.opalj.ai.analyses.{FieldValueInformation, MethodReturnValueInformation}
import org.opalj.ai.domain.l2.{CalledMethodsStore, ChildPerformInvocationsWithRecursionDetection}
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.Instruction
import org.opalj.br.{PC => _, _}
import org.opalj.collection.immutable.Chain

import scala.collection.immutable.HashSet
import scala.collection.mutable

trait ImpreciseBaseAnalysisDomain extends BaseAnalysisDomain
  with domain.l0.DefaultTypeLevelIntegerValues
  with domain.l0.DefaultTypeLevelFloatValues
  with domain.l0.DefaultTypeLevelDoubleValues
  with domain.l0.TypeLevelLongValuesShiftOperators
  with domain.l0.DefaultTypeLevelLongValues
  with domain.l0.TypeLevelPrimitiveValuesConversions {
  override type CalledMethodDomain = ImpreciseInvocationAnalysisDomain
}


class ImpreciseInvocationAnalysisDomain(
                                         val project: Project[URL],
                                         val fieldValueInformation: FieldValueInformation,
                                         val methodReturnValueInformation: MethodReturnValueInformation,
                                         val cache: CallGraphCache[MethodSignature, scala.collection.Set[Method]],
                                         val allocationSites: mutable.Map[Int, AllocationSite],
                                         val maxCallChainLength: Int,
                                         val callerDomain: ImpreciseBaseAnalysisDomain,
                                         val /*current*/ method: Method,
                                         val currentCallChainLength: Int,
                                         val staticInitializerMap:Map[ObjectType,AIResult],
                                         var analyzedInstructionsCount: Int,
                                         val visitedMethods: mutable.MutableList[Method],
                                         val deadInstructionsPerMethod: mutable.Map[Method, Iterable[(PC, Instruction)]],
                                         val visitedStaticInitializers: mutable.MutableList[ReferenceType],
                                         override var libraryMethods: HashSet[Method],
                                         override val preciseFieldInformation: PreciseFieldValueInformation,
                                         override val staticFieldInformation: StaticFieldValueInformation,
                                         override var mappedParameters: mutable.Map[Int, AllocationSite],
                                         val debug: Boolean,
                                       ) extends ImpreciseBaseAnalysisDomain
  with domain.RecordMethodCallResults
  with domain.RecordLastReturnedValues
  with domain.RecordAllThrownExceptions
  with ChildPerformInvocationsWithRecursionDetection
  with BaseInvocationDomain {
  callingDomain ⇒

  override def calledMethodDomain(method: Method) = {
    new ImpreciseInvocationAnalysisDomain(
      project,
      fieldValueInformation,
      methodReturnValueInformation,
      cache,
      mutable.Map(),
      maxCallChainLength,
      callingDomain,
      method, currentCallChainLength + 1,
      staticInitializerMap,
      analyzedInstructionsCount,
      visitedMethods,
      deadInstructionsPerMethod,
      visitedStaticInitializers,
      libraryMethods,
      preciseFieldInformation,
      staticFieldInformation,
      mappedParameters,
      debug,
    )
  }

  def calledMethodAI = callerDomain.calledMethodAI
}

class ImpreciseLibraryMinimizerAnalysisDomain(
                                               val project: Project[URL],
                                               val fieldValueInformation: FieldValueInformation,
                                               val methodReturnValueInformation: MethodReturnValueInformation,
                                               val cache: CallGraphCache[MethodSignature, scala.collection.Set[Method]],
                                               val maxCallChainLength: Int,
                                               val /*current*/ method: Method,
                                               var libraryMethods: HashSet[Method],
                                               val debug: Boolean,
                                               val staticInitializerMap:Map[ObjectType,AIResult],
                                               var analyzedInstructionsCount: Int = 0,
                                               val visitedMethods: mutable.MutableList[Method] = mutable.MutableList(),
                                               val deadInstructionsPerMethod: mutable.Map[Method, Iterable[(PC, Instruction)]] = mutable.Map(),
                                               val visitedStaticInitializers: mutable.MutableList[ReferenceType] = mutable.MutableList(),
                                               override val allocationSites: mutable.Map[Int, AllocationSite] = mutable.Map(),
                                               override var mappedParameters: mutable.Map[Int, AllocationSite] = mutable.Map(),
                                               override val staticFieldInformation: StaticFieldValueInformation = mutable.Map(),
                                               override val preciseFieldInformation: PreciseFieldValueInformation = mutable.Map(),
                                               val frequentEvaluationWarningLevel: Int = 256
                                             ) extends ImpreciseBaseAnalysisDomain
  with TheAI[BaseAnalysisDomain]
  with TheMemoryLayout // required to extract the initial operands
  // the following two are required to detect instructions that always throw
  // an exception (such as div by zero, a failing checkcast, a method call that
  // always fails etc.)
  with domain.l1.RecordAllThrownExceptions
  with domain.RecordCFG {
  callingDomain ⇒

  final def currentCallChainLength: Int = 0

  final def calledMethodAI = ai

  final val coordinatingDomain = this


  // The called methods store is always only required at analysis time, at this point
  // in time the initial operands are available!
  lazy val calledMethodsStore: CalledMethodsStore {
    val domain: coordinatingDomain.type
  } = {
    val operands =
      localsArray(0).foldLeft(Chain.empty[DomainValue])((l, n) ⇒
        if (n ne null) n :&: l else l)
    CalledMethodsStore(coordinatingDomain, frequentEvaluationWarningLevel)(
      method, mapOperands(operands, coordinatingDomain)
    )
  }

  override def calledMethodDomain(method: Method) =
    new ImpreciseInvocationAnalysisDomain(
      project,
      fieldValueInformation,
      methodReturnValueInformation,
      cache,
      mutable.Map(),
      maxCallChainLength,
      callingDomain,
      method,
      currentCallChainLength + 1,
      staticInitializerMap,
      analyzedInstructionsCount,
      visitedMethods,
      deadInstructionsPerMethod,
      visitedStaticInitializers,
      libraryMethods,
      preciseFieldInformation,
      staticFieldInformation,
      mappedParameters,
      debug
    )
}