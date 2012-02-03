package com.mojolly.scapulet.stanza

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import io.backchat.scapulet.stanza.Feature

class FeatureSpec extends WordSpec with MustMatchers {

  "A feature" when {

    "extracting" should {

      "get the feature name for a valid stanza" in {
        val stanza = <feature var="hello world" />
        stanza match {
          case Feature(nm) => nm must equal ("hello world")
          case _ => fail("Couldn't match a valid stanza")
        }
      }

      "not match when the stanza is not a feature with a name" in {
        val stanza = <feature />
        Feature.unapply(stanza) must be ('empty) 
      }
    }

    "generating" should {

      "throw an exception when the name is null" in {
         evaluating { Feature(null) } must produce [Exception]
      }
      "throw an exception when the name is blank" in {
        evaluating { Feature("  ") } must produce [Exception]
      }
      "generate a nodeseq when the name is provided" in {
        Feature("hello world") must be (<feature var="hello world" />)
      }
    }
  }
}