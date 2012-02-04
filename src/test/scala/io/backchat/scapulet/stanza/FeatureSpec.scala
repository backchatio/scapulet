package io.backchat.scapulet.stanza

import org.specs2.Specification

class FeatureSpec extends Specification {
  def is =
    "A feature should" ^
      "when extracting" ^
      "get the feature name for a valid stanza" ! {
        Feature.unapply(<feature var="hello world"/>) must beSome("hello world")
      } ^
      "not match when the stanza is not a feature with a name" ! {
        Feature.unapply(<feature/>) must beNone
      } ^ bt ^
      "when generating" ^
      "throw an exception when the name is null" ! {
        Feature(null) must throwA[Exception]
      } ^
      "throw an exception when the name is blank" ! {
        Feature("  ") must throwA[Exception]
      } ^
      "generate a nodeseq when the name is provided" ! {
        Feature("hello world") must ==/(<feature var="hello world"/>)
      } ^
      end
}