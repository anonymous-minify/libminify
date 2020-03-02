package analysis

import java.lang

import org.opalj.ai._
import org.opalj.ai.domain.l1.{DefaultIntegerSetValues, StringBuilderValues}
import org.opalj.br.{MethodDescriptor, ObjectType, ReferenceType}

/*
  This trait provides an extension to the normal string handling of OPAL.
  It covers the most common methods on String like equals and brings support for the concatenation of Strings using the StringBuilder class.
 */
trait DefaultStringValues extends StringBuilderValues {
  domain: Domain with CorrelationalDomainSupport with Configuration with DefaultIntegerSetValues with TypedValuesFactory with TheClassHierarchy ⇒

  val StringBuilderObjectType = ObjectType("java/lang/StringBuilder")
  type DomainStringBuilderValue <: StringBuilderValue with DomainObjectValue

  abstract override def invokevirtual(pc: PC, declaringClass: ReferenceType, name: String, methodDescriptor: MethodDescriptor, operands: Operands): MethodCallResult = {
    if (declaringClass.isObjectType) {
      declaringClass.asObjectType match {
        case StringBuilderValues.JavaStringBuilder =>
          name match {
            case "append" =>
              operands(1) match {
                case previousValue: StringValue =>
                  val parameter = operands(0)
                  parameter match {
                    case stringParameter: StringValue =>
                      val newValue = StringValue(previousValue.origin, previousValue.value + stringParameter.value, previousValue.refId)
                      return ComputedValue(newValue)
                    case domain.IntegerSet(set) if set.size == 1 =>
                      val newValue = StringValue(previousValue.origin, previousValue.value + set.firstKey, previousValue.refId)
                      return ComputedValue(newValue)
                    case _ =>
                  }

                case _ =>
              }

            case "toString" => return ComputedValue(operands(0))
            case _ =>
          }

        case ObjectType.String =>
          name match {
            case "equals" =>
              (operands.head, operands(1)) match {
                case (StringValue(s1), StringValue(s2)) =>
                  return ComputedValue(BooleanValue(pc, s2.equals(s1)))
                case _ =>
              }

            case "startsWith" =>
              (operands.head, operands(1)) match {
                case (StringValue(s1), StringValue(s2)) =>
                  return ComputedValue(BooleanValue(pc, s2.startsWith(s1)))
                case _ =>
              }

            case "endsWith" =>
              (operands.head, operands(1)) match {
                case (StringValue(s1), StringValue(s2)) =>
                  return ComputedValue(BooleanValue(pc, s2.endsWith(s1)))
                case _ =>
              }

            case "length" =>
              operands.head match {
                case StringValue(s1) =>
                  return ComputedValue(IntegerSet(s1.length))
                case _ =>
              }
            case _ =>
          }

        case _ =>
      }
    }

    super.invokevirtual(pc, declaringClass, name, methodDescriptor, operands)
  }

  abstract override def NewObject(origin: ValueOrigin, objectType: ObjectType): DomainObjectValue = {
    objectType match {
      case StringBuilderObjectType => StringValue(origin, "")
      case _ => super.NewObject(origin, objectType)
    }
  }


  override def StringBuilderValue(origin: ValueOrigin, builderType: ObjectType, builder: lang.StringBuilder): StringBuilderValue = {
    StringBuilderValue(origin, builderType)
  }

}
