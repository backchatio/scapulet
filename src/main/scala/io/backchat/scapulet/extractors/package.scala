package io.backchat.scapulet

package object extractors {

  // Splitter to apply two pattern matches on the same scrutinee.
  // http://stackoverflow.com/questions/2261358/pattern-matching-with-conjunctions-patterna-and-patternb
  object && {
    def unapply[A](a: A) = Some((a, a))
  }
}