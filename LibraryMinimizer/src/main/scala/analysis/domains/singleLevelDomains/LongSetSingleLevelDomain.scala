package analysis.domains.singleLevelDomains

import org.opalj.ai.analyses.FieldValueInformation
import org.opalj.ai.domain
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject

trait BaseLongSetSingleLevelDomain extends BaseSingleLevelAnalysisDomain
  with domain.l0.DefaultTypeLevelFloatValues
  with domain.l0.DefaultTypeLevelDoubleValues
  with domain.l1.DefaultIntegerRangeValues
  with domain.l1.DefaultLongSetValues
  with domain.l1.LongValuesShiftOperators
  with domain.l0.TypeLevelPrimitiveValuesConversions {
}


class LongSetSingleLevelDomain(val project: SomeProject,
                                  val fieldValueInformation: FieldValueInformation,
                                  val method: Method)
  extends BaseLongSetSingleLevelDomain