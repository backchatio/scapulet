package io.backchat.scapulet

import org.specs2.Specification

class StringUtilSpec extends Specification {
  def is =

    "The StringUtil should" ^
      "must hash a given string as hex" ! {
        StringUtil.hash("the string") must_== "e8cc75db52457e014d354b54a2c44c30dd96cbd5"
      } ^
      "must get the resource and barejid from a jid" ^
      "the resource must be Some(value) when there is a resource" ! {
        JID.unapply("someone@somewhere.com/theclient") must beSome(("someone@somewhere.com", Some("theclient")))
      } ^
      "the resource must be None when there is no resource" ! {
        JID.unapply("someone@somewhere.com") must beSome(("someone@somewhere.com", None))
      } ^
      end

}

// vim: set si ts=2 sw=2 sts=2 et: