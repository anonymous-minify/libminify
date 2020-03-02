package analysis.domains.singleLevelDomains

import org.opalj.ai.analyses.FieldValueInformation
import org.opalj.ai.domain
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject


trait BaseImpreciseSingleLevelAnalysisDomain extends BaseSingleLevelAnalysisDomain
  with domain.l0.DefaultTypeLevelIntegerValues
  with domain.l0.DefaultTypeLevelFloatValues
  with domain.l0.DefaultTypeLevelDoubleValues
  with domain.l0.TypeLevelLongValuesShiftOperators
  with domain.l0.DefaultTypeLevelLongValues
  with domain.l0.TypeLevelPrimitiveValuesConversions {

}

class ImpreciseSingleLevelAnalysisDomain(val project: SomeProject,
                                         val fieldValueInformation: FieldValueInformation,
                                         val method: Method)
  extends BaseImpreciseSingleLevelAnalysisDomain
