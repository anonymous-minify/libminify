package analysis

import org.opalj.ai.Domain
import org.opalj.br.{AllocationSite, Field}

import scala.collection.{Map, mutable}

package object analyses {
  type PreciseFieldValueInformation = mutable.Map[AllocationSite, mutable.Map[String, Domain#DomainValue]]
  type StaticFieldValueInformation = mutable.Map[Field, Domain#DomainValue]

}
