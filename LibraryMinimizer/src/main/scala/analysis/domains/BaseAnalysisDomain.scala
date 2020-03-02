package analysis.domains

import analysis.analyses.{PreciseFieldValueInformation, StaticFieldValueInformation}
import org.opalj.{Answer, No, Unknown, Yes}
import org.opalj.ai._
import org.opalj.ai.domain.l2.{ChildPerformInvocationsWithRecursionDetection, PerformInvocationsWithRecursionDetection}
import org.opalj.ai.domain.la.PerformInvocationsWithBasicVirtualMethodCallResolution
import org.opalj.ai.domain.{MethodCallResults, PerInstructionPostProcessing}
import org.opalj.ai.util.XHTML
import org.opalj.br.instructions.{Instruction, NEW, NEWARRAY, PUTFIELD, PUTSTATIC}
import org.opalj.br.{PC => _, _}
import org.opalj.collection.immutable.Chain

import scala.collection.immutable.HashSet
import scala.collection.mutable
import scala.collection.mutable.ListBuffer


trait BaseInvocationDomain extends CorrelationalDomain
  with ChildPerformInvocationsWithRecursionDetection with MethodCallResults with domain.TheProject
  with domain.TheMethod with BaseAnalysisDomain {

}

/*
  This trait builds the foundation for all datatype specific domains. In this trait all the logic is implemented that belongs to the tracking of dead instructions.
  This includes features like field value tracking, static initializer consideration, improved method resolution.
 */

trait BaseAnalysisDomain
  extends CorrelationalDomain
    with domain.TheProject
    with domain.TheMethod
    with domain.DefaultDomainValueBinding
    with domain.ThrowAllPotentialExceptionsConfiguration
    with domain.l0.TypeLevelFieldAccessInstructions
    with domain.la.RefinedTypeLevelFieldAccessInstructions
    with domain.l0.TypeLevelInvokeInstructions
    with domain.l1.ReferenceValues
    with domain.SpecialMethodsHandling
    with domain.l1.NullPropertyRefinement
    with domain.DefaultHandlingOfMethodResults
    with domain.IgnoreSynchronization
    with PerformInvocationsWithRecursionDetection
    with PerformInvocationsWithBasicVirtualMethodCallResolution
    with domain.l1.DefaultClassValuesBinding
    with PerInstructionPostProcessing {
  callingDomain ⇒

  def debug: Boolean

  var libraryMethods: HashSet[Method]

  val allocationSites: mutable.Map[RefId, AllocationSite]
  var mappedParameters: mutable.Map[Int, AllocationSite]

  var analyzedInstructionsCount: Int
  val preciseFieldInformation: PreciseFieldValueInformation
  val staticFieldInformation: StaticFieldValueInformation
  val deadInstructionsPerMethod: mutable.Map[Method, Iterable[(PC, Instruction)]]
  val visitedMethods: mutable.MutableList[Method]
  val visitedStaticInitializers: mutable.MutableList[ReferenceType]
  val staticInitializerMap: Map[ObjectType, AIResult]
  var unresolvedInterfaceCalls: mutable.Map[ObjectType, ListBuffer[String]] = mutable.Map()
  var failedMethodCalls: mutable.ListBuffer[Method] = mutable.ListBuffer()

  type CalledMethodDomain <: BaseInvocationDomain

  //AI doInvoke
  override protected[this] def doInvoke(method: Method,
                                        calledMethodDomain: CalledMethodDomain
                                       )(
                                         parameters: calledMethodDomain.Locals
                                       ): AIResult {val domain: calledMethodDomain.type} = {
    visitedMethods += method

    val result: AIResult {val domain: calledMethodDomain.type} = super.doInvoke(method, calledMethodDomain)(parameters)

    if (result.wasAborted) {
      failedMethodCalls += method
    } else {
      visitedMethods += method
    }

    processAIResult(method, result)
    unresolvedInterfaceCalls ++= result.domain.unresolvedInterfaceCalls
    failedMethodCalls ++= result.domain.failedMethodCalls
    analyzedInstructionsCount = result.domain.analyzedInstructionsCount + result.evaluated.size


    /*if (debug) { //uncomment to see detailed HTML Report for every analyzed method
      import result._
      org.opalj.io.writeAndOpen(
        org.opalj.ai.common.XHTML.dump(
          Some(method.classFile),
          Some(method),
          method.body.get,
          Some(
            "Created: " + (new java.util.Date).toString + "<br>" +
              "Domain: " + result.domain.getClass.getName + "<br>" +
              XHTML.evaluatedInstructionsToXHTML(result.evaluated)
          ),
          result.domain
        )(cfJoins, result.operandsArray, result.localsArray),
        "AIResult",
        ".html"
      )
    }*/

    result


  }

  def processAIResult(method: Method, result: AIResult): Unit = {
    if (libraryMethods.contains(method) && !result.wasAborted) {
      val instructions = result.code.withFilter(tuple => !result.wasEvaluted(tuple._1)).map(t => t)
      if (deadInstructionsPerMethod.contains(method)) {
        val currentPCs = deadInstructionsPerMethod(method).map(t => t._1).toList
        val newPCs = instructions.map(t => t._1)
        val remainingPCs = currentPCs.intersect(newPCs).distinct
        deadInstructionsPerMethod(method) = instructions.filter(t => remainingPCs.contains(t._1))
      } else {
        deadInstructionsPerMethod(method) = instructions
      }
    }
  }

  override def doInvoke(
                         pc: PC,
                         method: Method,
                         operands: callingDomain.Operands,
                         fallback: () ⇒ MethodCallResult
                       ): MethodCallResult = {
    /*if (method.body.isEmpty)
      return getFakeValue(pc, method.descriptor)*/

    val mappedParametersBefore = mappedParameters
    val tempMappedParameters = mutable.Map[Int, AllocationSite]()

    var refIdCounter = 101
    for (parameter <- operands.reverse) {
      parameter match {
        case referenceValue: ReferenceValue =>
          val allocationSite = findAllocationSite(pc, referenceValue)
          allocationSite match {
            case Some(a) =>
              refIdCounter += 1
              tempMappedParameters += ((refIdCounter, a))
            case _ =>
              refIdCounter += 1
          }

        case _ =>
      }
    }

    mappedParameters = tempMappedParameters

    val result = super.doInvoke(pc, method, operands, fallback)

    if (result.hasResult) {
      result.result match {
        case DomainReferenceValue(referenceValue) =>
        //allocationSites += ((referenceValue.refId, FakeAllocationSite(this.method,pc)))
        case _ =>
      }
    }

    mappedParameters = mappedParametersBefore

    if (debug) {
      println("the result of calling " + method.toJava + " is " + result)
    }
    result
  }


  val maxCallChainLength: Int

  def currentCallChainLength: Int

  def shouldInvocationBePerformed(calledMethod: Method): Boolean = {

    val result =
    //maxCallChainLength > currentCallChainLength &&
      method.body.isDefined
    if (debug) {
      val i = if (result) " invokes " else " does not invoke "
      println(s"[$currentCallChainLength]" +
        method.toJava +
        i +
        calledMethod.toJava)
    }

    result
  }


  override def doInvokeVirtual(pc: PC, declaringClass: ReferenceType, isInterface: Boolean, name: String, descriptor: MethodDescriptor, operands: Operands, fallback: () => MethodCallResult): MethodCallResult = {

    super.doInvokeVirtual(pc, declaringClass, isInterface, name, descriptor, operands, fallback)
  }


  override def invokevirtual(
                              pc: PC,
                              declaringClass: ReferenceType,
                              name: String,
                              descriptor: MethodDescriptor,
                              operands: Operands
                            ): MethodCallResult = {
    def fallback(): MethodCallResult = {

      //failedMethodCalls += VirtualMethod(declaringClass, name, descriptor)

      super.invokevirtual(pc, declaringClass, name, descriptor, operands)
    }


    var result: MethodCallResult = null
    val receiver = operands(descriptor.parametersCount)
    receiver match {
      case DomainReferenceValue(refValue) if //if refValue.isPrecise &&
      //refValue.isNull.isNo && // IMPROVE support the case that null is unknown
      refValue.upperTypeBound.isSingletonSet &&
        refValue.upperTypeBound.head.isObjectType ⇒

        val receiverClass = refValue.upperTypeBound.head.asObjectType
        classHierarchy.isInterface(receiverClass) match {
          case Yes ⇒
            result = doInvokeNonVirtual(
              pc,
              receiverClass, true, name, descriptor,
              operands, fallback
            )
          case No ⇒
            result = doInvokeNonVirtual(
              pc,
              receiverClass, false, name, descriptor,
              operands, fallback
            )
          case Unknown ⇒
            result = fallback()
        }

      case _ ⇒
        result = fallback()
    }

    //val result = doInvokeVirtual(pc, declaringClass, false, name, descriptor, operands, fallback _)
    if (debug) {
      println(s"[$currentCallChainLength] call result of " +
        declaringClass.toJava + " " + descriptor.toJava(name) + result)
    }
    result
  }


  override def invokeinterface(
                                pc: PC,
                                declaringClass: ObjectType,
                                name: String,
                                descriptor: MethodDescriptor,
                                operands: Operands
                              ): MethodCallResult = {
    def fallback(): MethodCallResult = {
      //failedMethodCalls += VirtualMethod(declaringClass, name, descriptor)

      if (unresolvedInterfaceCalls.contains(declaringClass))
        unresolvedInterfaceCalls(declaringClass) += name
      else {
        unresolvedInterfaceCalls(declaringClass) = ListBuffer(name)
      }

      super.invokeinterface(pc, declaringClass, name, descriptor, operands)
    }

    doInvokeVirtual(pc, declaringClass, true, name, descriptor, operands, fallback _)
  }

  override def invokespecial(
                              pc: PC,
                              declaringClass: ObjectType,
                              isInterface: Boolean,
                              name: String,
                              descriptor: MethodDescriptor,
                              operands: Operands
                            ): MethodCallResult = {
    def fallback(): MethodCallResult = {
      //failedMethodCalls += VirtualMethod(declaringClass, name, descriptor)
      super.invokespecial(pc, declaringClass, isInterface, name, descriptor, operands)
    }

    doInvokeNonVirtual(pc, declaringClass, isInterface, name, descriptor, operands, fallback _)
  }

  override def invokestatic(
                             pc: PC,
                             declaringClass: ObjectType,
                             isInterface: Boolean,
                             name: String,
                             descriptor: MethodDescriptor,
                             operands: Operands
                           ): MethodCallResult = {
    def fallback(): MethodCallResult = {
      //failedMethodCalls += VirtualMethod(declaringClass, name, descriptor)
      super.invokestatic(pc, declaringClass, isInterface, name, descriptor, operands)
    }

    if (!visitedStaticInitializers.contains(declaringClass)) {
      analyzeStaticInitializer(pc, declaringClass)
    }

    doInvokeNonVirtual(pc, declaringClass, isInterface, name, descriptor, operands, fallback _)
  }


  override def flow(currentPC: PC, currentOperands: Operands, currentLocals: Locals, successorPC: PC, isSuccessorScheduled: Answer, isExceptionalControlFlow: Boolean, abruptSubroutineTerminationCount: Opcode, wasJoinPerformed: Boolean, worklist: Chain[PC], operandsArray: OperandsArray, localsArray: LocalsArray, tracer: Option[AITracer]): Chain[PC] = {
    if (method.body.nonEmpty) {
      val instruction = code.instructions(currentPC)
      instruction match {
        case PUTFIELD(_, name, _) => {
          val value = currentOperands(0)
          val allocationSiteOpt = findAllocationSite(currentPC, currentOperands(1).asDomainReferenceValue)
          allocationSiteOpt match {
            case Some(allocationSite) =>
              val fieldValue = value.asInstanceOf[Domain#DomainValue]
              if (preciseFieldInformation.contains(allocationSite)) {
                if (preciseFieldInformation(allocationSite).contains(name)) {
                  preciseFieldInformation(allocationSite)(name) = fieldValue
                } else {
                  preciseFieldInformation(allocationSite) += ((name, fieldValue))
                }
              } else {
                preciseFieldInformation += ((allocationSite, mutable.Map[String, Domain#DomainValue]((name, fieldValue))))
              }

            case _ =>
          }
        }

        case n: NEW => {
          val refId = operandsArray(successorPC).head.asDomainReferenceValue.refId
          allocationSites += ((refId, ObjectAllocationSite(method, currentPC)))
          if (!visitedStaticInitializers.contains(n.objectType)) {
            analyzeStaticInitializer(currentPC, n.objectType)
          }
        }

        case n: NEWARRAY =>
          val refId = operandsArray(successorPC).head.asDomainReferenceValue.refId
          allocationSites += ((refId, ArrayAllocationSite(method, currentPC)))
          if (!visitedStaticInitializers.contains(n.arrayType)) {
            analyzeStaticInitializer(currentPC, n.arrayType)
          }

        case _ =>
      }
    }

    super.flow(currentPC, currentOperands, currentLocals, successorPC, isSuccessorScheduled, isExceptionalControlFlow, abruptSubroutineTerminationCount, wasJoinPerformed, worklist, operandsArray, localsArray, tracer)
  }

  def findAllocationSite(pc: PC, referenceValue: ReferenceValue): Option[AllocationSite] = {
    val allocationSiteOpt = allocationSites.get(referenceValue.refId)
    allocationSiteOpt match {
      case Some(allocationSite) =>
        return Option(allocationSite)
      case _ if mappedParameters.contains(referenceValue.refId) =>
        //the referenceValue was passed as a parameter
        return Option(mappedParameters(referenceValue.refId))
      case _ =>
    }

    None
  }

  override def getfield(pc: PC, objectref: Value, declaringClass: ObjectType, fieldName: String, fieldType: FieldType): Computation[Value, ExceptionValue] = {
    objectref match {
      case referenceValue: ObjectValue =>
        val allocationSite = findAllocationSite(pc, referenceValue)
        allocationSite match {
          case Some(site) =>
            if (preciseFieldInformation.contains(site) && preciseFieldInformation(site).contains(fieldName)) {
              val value = preciseFieldInformation(site)(fieldName).adapt(this, pc)
              return doGetfield(pc, objectref, value)
            }
          case _ =>
        }
      case _ =>
    }

    super.getfield(pc, objectref, declaringClass, fieldName, fieldType)
  }

  override def getstatic(pc: PC, declaringClass: ObjectType, name: String, fieldType: FieldType): Computation[Value, Nothing] = {
    /* val field = project.resolveFieldReference(declaringClass, name, fieldType)
     if (field.isDefined) {
       val fieldValue = staticFieldInformation.get(field.get)
       if (fieldValue.isEmpty) {
         //val staticInitializer = project.classFile(declaringClass).get.staticInitializer //OPAL doesn't recognize static initializer if methods are not sorted
         if (!visitedStaticInitializers.contains(declaringClass)) {
           analyzeStaticInitializer(pc, declaringClass)
         }

         if (staticFieldInformation.contains(field.get))
           return ComputedValue(staticFieldInformation(field.get).adapt(this, pc))
       } else {
         return ComputedValue(fieldValue.get.adapt(this, pc))
       }
     }*/

    super.getstatic(pc, declaringClass, name, fieldType)
  }

  override def putstatic(pc: PC, value: Value, declaringClass: ObjectType, fieldName: String, fieldType: FieldType): Computation[Nothing, Nothing] = {
    val field = project.resolveFieldReference(declaringClass, fieldName, fieldType)
    if (field.isDefined) {
      val fieldValue = staticFieldInformation.get(field.get)
      staticFieldInformation(field.get) = value.adapt(this, pc)
    }

    super.putstatic(pc, value, declaringClass, fieldName, fieldType)
  }


  def analyzeStaticInitializer(pc: PC, referenceType: ReferenceType): Unit = {
    if (visitedStaticInitializers.contains(referenceType)) return

    def handleObjectType(declaringClass: ObjectType) = {
      val classFileOpt = project.classFile(declaringClass)
      classFileOpt match {
        case Some(classFile) =>
          val staticInitializer = classFile.methods.find(m => m.isStaticInitializer)
          if (staticInitializer.isDefined && libraryMethods.contains(staticInitializer.get)) {
            // a simple doInvoke on this domain causes memory problems
            val result = staticInitializerMap(declaringClass)
            staticInitializer.get.body.get.collectWithIndex({
              case (pc2, instruction) =>
                instruction match {
                  case PUTSTATIC(objectType, name, fieldType) =>
                    val field = project.resolveFieldReference(objectType, name, fieldType)
                    if (field.isDefined) {
                      val value = result.operandsArray(pc2).head
                      staticFieldInformation(field.get) = value.adapt(this, pc2)
                    }
                  case _ =>
                }
            })
          }

        case _ =>

      }
    }

    visitedStaticInitializers += referenceType
    referenceType match {
      case declaringClass: ObjectType =>
        handleObjectType(declaringClass)

      case a: ArrayType =>
        var currentType = a.elementType
        var previousType: FieldType = null
        while (currentType != previousType) {
          previousType = currentType

          currentType match {
            case o: ObjectType => handleObjectType(o)
            case a: ArrayType => currentType = a.elementType
            case _ =>
          }
        }

      case _ =>
    }
  }
}