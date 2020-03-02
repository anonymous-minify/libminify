package analysis.domains.singleLevelDomains

import org.opalj.ai.{CorrelationalDomain, domain}

/*
This domain only analyses a single method without performing method invocations.
This domain is used to analyze static initializers to get more precise information about the local variables.
 */


trait BaseSingleLevelAnalysisDomain extends CorrelationalDomain
  with domain.TheProject
  with domain.TheMethod
  with domain.DefaultDomainValueBinding
  with domain.ThrowAllPotentialExceptionsConfiguration
  with domain.l0.TypeLevelFieldAccessInstructions
  with domain.la.RefinedTypeLevelFieldAccessInstructions
  with domain.l0.TypeLevelInvokeInstructions
  with domain.l1.ReferenceValues
  //with domain.la.RefinedTypeLevelInvokeInstructions
  with domain.SpecialMethodsHandling
  //with domain.l1.DefaultIntegerRangeValues
  // [CURRENTLY ONLY A WASTE OF RESOURCES] with domain.l1.ConstraintsBetweenIntegerValues
  //with domain.l1.DefaultReferenceValuesBinding [implicitly mixed in via StringValuesBinding]
  //with domain.l1.DefaultStringValuesBinding [implicitly mixed in via ClassValuesBinding]
  with domain.l1.NullPropertyRefinement
  with domain.DefaultHandlingOfMethodResults
  with domain.IgnoreSynchronization // We want to get the special treatment of calls on "Class" objects
  // and do not want to perform invocations in this case;
  // hence, we have to mix in this domain AFTER the PerformInvocations domain!
  with domain.l1.DefaultClassValuesBinding {

}
