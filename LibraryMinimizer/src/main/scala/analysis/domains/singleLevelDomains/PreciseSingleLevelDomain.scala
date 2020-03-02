package analysis.domains.singleLevelDomains

import analysis.DefaultStringValues
import org.opalj.ai._
import org.opalj.ai.analyses.FieldValueInformation
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject

trait PreciseSingleLevelBaseAnalysisDomain extends BaseSingleLevelAnalysisDomain with CorrelationalDomainSupport with IntegerValuesDomain with TypedValuesFactory with Configuration with TheClassHierarchy
  with domain.l1.DefaultIntegerSetValues
  with domain.l1.DefaultStringValuesBinding
  with domain.l1.DefaultLongSetValues
  with domain.l1.LongSetValuesShiftOperators
  with domain.l0.DefaultTypeLevelFloatValues
  with domain.l0.DefaultTypeLevelDoubleValues
  with domain.l1.ConcretePrimitiveValuesConversions
  with DefaultStringValues {
}


class PreciseSingleLevelDomain(val project: SomeProject,
                               val fieldValueInformation: FieldValueInformation,
                               val method: Method)
  extends PreciseSingleLevelBaseAnalysisDomain