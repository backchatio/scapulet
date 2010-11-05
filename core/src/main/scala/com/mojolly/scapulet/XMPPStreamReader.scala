package com.mojolly.scapulet

import xml._
import xml.pull._
import io.Source
import akka.util.Logging
import java.io.InputStream
import StringUtil.UTF_8
// THIS is not in use currently it needs more work before it can be tested
//object XMPPStreamReader {
//  private case class ParentRef(current: Elem, parent: Option[ParentRef] = None)
//}
//
//class XMPPStreamReader(in: InputStream) extends Logging {
//
//  import XMPPStreamReader._
//
//  private def addChild(nodeRef: ParentRef, childNode: Node) = {
//    ParentRef(nodeRef.current.copy(child = nodeRef.current.child ++ childNode), nodeRef.parent)
//  }
//
//  private val input = Source.fromInputStream(in, UTF_8)
//  private val reader = new XMLEventReader(input)
//
//
//  def read = {
//    var doc: Seq[Node] = Seq[Node]()
//    var current: ParentRef = null
//    var keepGoing = reader.hasNext
//
//    // We don't need to respond to more xml events because XMPP forbids the use of Processing instructions
//    // as well as comments in the XML stream. http://xmpp.org/rfcs/rfc3920.html#xml-restrictions
//    while(keepGoing) {
//      reader.next match {
//        case EvElemStart(prefix, label, attrs, scope) => {
//          current = ParentRef(Elem(prefix, label, attrs, scope), Option(current))
//        }
//        case EvElemEnd(_, label) => {
//          keepGoing = current.parent.isDefined
//          if(current.parent.isDefined) {
//            current = addChild(current.parent.get, current.current)
//          } else {
//            doc = current.current
//          }
//        }
//        case EvText(text) => {
//          current = addChild(current, Text(text))
//        }
//        case EvEntityRef(entity) => {
//          current = addChild(current, EntityRef(entity))
//        }
//        case EvProcInstr(_, _)  =>
//        case EvComment(comment) => {
//          //This needs to change to support html entities in html message stanza's
//          log error "The XMPP Stream reader encountered an unknown entity.\n%s".format(comment)
//        }
//      }
//    }
//    doc
//  }
//
//
//}