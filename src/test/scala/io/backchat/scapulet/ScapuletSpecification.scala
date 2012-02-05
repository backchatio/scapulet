package io.backchat.scapulet

import org.specs2.main.{ ArgumentsShortcuts, ArgumentsArgs }
import org.specs2.matcher.{ MatchResult, StandardMatchResults, ShouldThrownMatchers, MustThrownMatchers }
import org.specs2.execute.{ Result, PendingUntilFixed, StandardResults }
import org.specs2.specification._

trait ScapuletSpecification extends BaseSpecification
    with ArgumentsArgs
    with ArgumentsShortcuts
    with MustThrownMatchers
    with ShouldThrownMatchers
    with FormattingFragments
    with StandardResults
    with StandardMatchResults
    with PendingUntilFixed
    with Contexts {

  /** transform a context to a result to allow the implicit passing of a context to each example */
  implicit def contextToResult[T](t: MatchResult[T])(implicit context: Context = defaultContext): Result = context(asResult(t))
  /** use an available outside context to transform a function returning a MatchResult into a result */
  implicit def outsideFunctionToResult[T, S](implicit o: Outside[T]) = (f: T ⇒ MatchResult[S]) ⇒ { o((t1: T) ⇒ f(t1).toResult) }

}