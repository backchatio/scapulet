package com.mojolly.scapulet.stanza

import xml.NodeSeq

object Feature {
  def apply(name: String): NodeSeq = <feature var={name} />
}