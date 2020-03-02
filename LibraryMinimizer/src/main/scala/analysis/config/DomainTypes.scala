package analysis.config

object DomainTypes extends Enumeration {
  val PreciseDomain,
  PreciseWithIntegerRange,
  ImpreciseDomain,
  IntegerSetDomain,
  IntegerRangeDomain,
  LongSetDomain,
  StringDomain = Value
}
