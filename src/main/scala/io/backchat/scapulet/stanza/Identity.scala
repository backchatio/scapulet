package io.backchat.scapulet
package stanza

import xml._

/**
 * A Service Discovery Identity model
 *
 * @param category The category for this identity
 * @param `type` The type of identity
 * @param name An optional name for the identity, only used in some cases
 */
class Identity(val category: String, val `type`: String, val name: Option[String] = None) extends Product3[String, String, Option[String]] {

  require(category.nonBlank, "You have to provide a category")
  require(`type`.nonBlank, "You have to provide a type")

  /**
   * Product interface implementation represents the category
   */
  val _1 = category

  /**
   * Product interface implementation represents the type
   */
  val _2 = `type`

  /**
   * Product interface implementation represents the name
   */
  val _3 = name

  /**
   * Whether this can equal that
   * @param that
   * @return Boolean
   */
  def canEqual(that: Any) = that.isInstanceOf[Identity]

  /**
   * Convert this Identity to its XML representation
   *
   * @return [[scala.xml.Node]]
   */
  def toXml = name match {
    case Some(nm) ⇒ <identity category={ category } type={ `type` } name={ nm }/>
    case _        ⇒ <identity category={ category } type={ `type` }/>
  }

  override def hashCode() = scala.runtime.ScalaRunTime._hashCode(this)

  override def equals(p1: Any) = canEqual(p1) && scala.runtime.ScalaRunTime._equals(this, p1)
}

/**
 * Companion object of [[io.backchat.scapulet.stanza.Identity]]
 *
 * @see [[io.backchat.scapulet.stanza.Identity]]
 */
object Identity {

  /**
   * Create an identity with the specified data
   * @see [[io.backchat.scapulet.stanza.Identity]]
   */
  def apply(category: String, `type`: String, name: Option[String] = None): Identity = {
    new Identity(category, `type`, name)
  }

  /**
   * Extract an identity out of a [[scala.xml.Node]]
   * @param elem [[scala.xml.Node]]
   * @return [[scala.Option]]
   */
  def unapply(elem: Node) = elem match {
    case identity @ Elem(_, "identity", _, _, _*) if (identity.attribute("category").isDefined && identity.attribute("type").isDefined) ⇒ {
      val name = identity.attribute("name") flatMap {
        a ⇒ Option(a.text)
      }
      Some(((identity \ "@category").text, (identity \ "@type").text, name))
    }
    case _ ⇒ None
  }

  /**
   * The "account" category is to be used by a server when responding to a disco request sent
   * to the bare JID (user@host addresss) of an account hosted by the server.
   */
  object account {

    /**
     * The user@host is an administrative account
     *
     * {{{
     * <identity category='account' type='admin'/>
     * }}}
     */
    object admin extends Identity("account", "admin")

    /**
     * The user@host is a "guest" account that allows anonymous login by any user
     *
     * {{{
     * <identity category='account' type='anonymous'/>
     * }}}
     */
    object anonymous extends Identity("account", "anonymous")

    /**
     * The user@host is a registered or provisioned account associated with a particular non-administrative user
     *
     * {{{
     * <identity category='account' type='registered'/>
     * }}}
     */
    object registered extends Identity("account", "registered")
  }

  /**
   * The "auth" category consists of server components that provide authentication services within a server implementation.
   */
  object auth {

    /**
     * A server component that authenticates based on external certificates
     *
     * {{{
     * <identity category='auth' type='cert'/>
     * }}}
     */
    object cert extends Identity("auth", "cert")

    /**
     * A server authentication component other than one of the registered types
     *
     * {{{
     * <identity category='auth' type='generic'/>
     * }}}
     */
    object generic extends Identity("auth", "generic")

    /**
     * A server authentication component other than one of the registered types
     *
     * {{{
     * <identity category='auth' type='ldap'/>
     * }}}
     */
    object ldap extends Identity("auth", "ldap")

    /**
     * A server component that authenticates against an NT domain
     *
     * {{{
     * <identity category='auth' type='ntlm'/>
     * }}}
     */
    object ntlm extends Identity("auth", "ntlm")

    /**
     * A server component that authenticates against a PAM system
     *
     * {{{
     * <identity category='auth' type='pam'/>
     * }}}
     */
    object pam extends Identity("auth", "pam")

    /**
     * A server component that authenticates against a Radius system
     *
     * {{{
     * <identity category='auth' type='radius'/>
     * }}}
     */
    object radius extends Identity("auth", "radius")
  }

  /**
   * The "automation" category consists of entities and nodes that provide automated or programmed interaction.
   */
  object automation {

    /**
     * The node for a list of commands; valid only for the node "http://jabber.org/protocol/commands"
     *
     * {{{
     * <identity category='automation' type='command-list'/>
     * }}}
     */
    object `command-list` extends Identity("automation", "command-list")

    /**
     * A node for a specific command; the "node" attribute uniquely identifies the command
     *
     * {{{
     * <identity category='automation' type='command-node'/>
     * }}}
     */
    object `command-node` extends Identity("automation", "command-node")

    /**
     * An entity that supports Jabber-RPC.
     *
     * {{{
     * <identity category='automation' type='rpc'/>
     * }}}
     */
    object rpc extends Identity("automation", "rpc")

    /**
     * An entity that supports the SOAP XMPP Binding.
     *
     * {{{
     * <identity category='automation' type='soap'/>
     * }}}
     */
    object soap extends Identity("automation", "soap")

    /**
     * An entity that provides automated translation services.
     *
     * {{{
     * <identity category='automation' type='translation'/>
     * }}}
     */
    object translation extends Identity("automation", "translation")
  }

  /**
   * The "client" category consists of different types of clients, mostly for instant messaging.
   */
  object client {

    /**
     * An automated client that is not controlled by a human user
     *
     * {{{
     * <identity category='client' type='bot'/>
     * }}}
     */
    object bot extends Identity("client", "bot")

    /**
     * Minimal non-GUI client used on dumb terminals or text-only screens
     *
     * {{{
     * <identity category='client' type='console'/>
     * }}}
     */
    object console extends Identity("client", "console")

    /**
     * A client running on a gaming console
     *
     * {{{
     * <identity category='client' type='game'/>
     * }}}
     */
    object game extends Identity("client", "game")

    /**
     * A client running on a PDA, RIM device, or other handheld
     *
     * {{{
     * <identity category='client' type='handheld'/>
     * }}}
     */
    object handheld extends Identity("client", "handheld")

    /**
     * Standard full-GUI client used on desktops and laptops
     *
     * {{{
     * <identity category='client' type='pc'/>
     * }}}
     */
    object pc extends Identity("client", "pc")

    /**
     * A client running on a mobile phone or other telephony device
     *
     * {{{
     * <identity category='client' type='phone'/>
     * }}}
     */
    object phone extends Identity("client", "phone")

    /**
     * A client that is not actually using an instant messaging client; however,
     * messages sent to this contact will be delivered as Short Message Service (SMS) messages
     *
     * {{{
     * <identity category='client' type='sms'/>
     * }}}
     */
    object sms extends Identity("client", "sms")

    /**
     * A client operated from within a web browser
     *
     * {{{
     * <identity category='client' type='web'/>
     * }}}
     */
    object web extends Identity("client", "web")
  }

  /**
   * The "collaboration" category consists of services that enable multiple individuals to work together in real time.
   */
  object collaboration {

    /**
     * Multi-user whiteboarding service
     *
     * {{{
     * <identity category='collaboration' type='whiteboard'/>
     * }}}
     */
    object whiteboard extends Identity("collaboration", "whiteboard")
  }

  /**
   * The "component" category consists of services that are internal to server implementations and not normally exposed outside a server.
   */
  object component {

    /**
     * A server component that archives traffic
     *
     * {{{
     * <identity category='component' type='archive'/>
     * }}}
     */
    object archive extends Identity("component", "archive")

    /**
     * A server component that handles client connections
     *
     * {{{
     * <identity category='component' type='c2s'/>
     * }}}
     */
    object c2s extends Identity("component", "c2s")

    /**
     * A server component other than one of the registered types
     *
     * {{{
     * <identity category='component' type='generic'/>
     * }}}
     */
    object generic extends Identity("component", "generic")

    /**
     * A server component that handles load balancing
     *
     * {{{
     * <identity category='component' type='load'/>
     * }}}
     */
    object load extends Identity("component", "load")

    /**
     * A server component that logs server information
     *
     * {{{
     * <identity category='component' type='log'/>
     * }}}
     */
    object log extends Identity("component", "log")

    /**
     * A server component that provides presence information
     *
     * {{{
     * <identity category='component' type='presence'/>
     * }}}
     */
    object presence extends Identity("component", "presence")

    /**
     * A server component that handles core routing logic
     *
     * {{{
     * <identity category='component' type='router'/>
     * }}}
     */
    object router extends Identity("component", "router")

    /**
     * A server component that handles server connections
     *
     * {{{
     * <identity category='component' type='s2s'/>
     * }}}
     */
    object s2s extends Identity("component", "s2s")

    /**
     * A server component that manages user sessions
     *
     * {{{
     * <identity category='component' type='sm'/>
     * }}}
     */
    object sm extends Identity("component", "sm")

    /**
     * A server component that provides server statistics
     *
     * {{{
     * <identity category='component' type='stats'/>
     * }}}
     */
    object stats extends Identity("component", "stats")
  }

  /**
   * The "conference" category consists of online conference services such as multi-user chatroom services.
   */
  object conference {

    /**
     * Internet Relay Chat service
     *
     * {{{
     * <identity category='conference' type='irc'/>
     * }}}
     */
    object irc extends Identity("conference", "irc")

    /**
     * Text conferencing service
     *
     * {{{
     * <identity category='component' type='text'/>
     * }}}
     */
    object text extends Identity("conference", "text")

  }

  /**
   * The "directory" category consists of information retrieval services that enable users to search online directories
   * or otherwise be informed about the existence of other XMPP entities.
   */
  object directory {

    /**
     * A directory of chat rooms
     *
     * {{{
     * <identity category='directory' type='chatroom'/>
     * }}}
     */
    object chatroom extends Identity("directory", "chatroom")

    /**
     * A directory that provides shared roster groups
     *
     * {{{
     * <identity category='directory' type='group'/>
     * }}}
     */
    object group extends Identity("directory", "group")

    /**
     * A directory of end users (e.g., JUD)
     *
     * {{{
     * <identity category='directory' type='user'/>
     * }}}
     */
    object user extends Identity("directory", "user")

    /**
     * A directory of waiting list entries
     * 
     * {{{
     * <identity category='directory' type='waitinglist'/>
     * }}}
     */
    object waitinglist extends Identity("directory", "waitinglist")
  }

  /**
   * The "gateway" category consists of translators between Jabber/XMPP services and non-XMPP services.
   */
  object gateway {
    
    /**
     * Gateway to AOL Instant Messenger
     *
     * {{{
     * <identity category='gateway' type='aim'/>
     * }}}
     */
    object aim extends Identity("gateway", "aim")

    /**
     * Gateway to the Facebook IM service
     *
     * {{{
     * <identity category='gateway' type='facebook'/>
     * }}}
     */
    object facebook extends Identity("gateway", "facebook")

    /**
     * Gateway to ICQ
     *
     * {{{
     * <identity category='gateway' type='icq'/>
     * }}}
     */
    object icq extends Identity("gateway", "icq")

    /**
     * Gateway to the Gadu-Gadu IM service
     *
     * {{{
     * <identity category='gateway' type='gadu-gadu'/>
     * }}}
     */
    object `gadu-gadu` extends Identity("gateway", "gadu-gadu")

    /**
     * Gateway that provides HTTP Web Services access
     *
     * {{{
     * <identity category='gateway' type='http-ws'/>
     * }}}
     */
    object `http-ws` extends Identity("gateway", "http-ws")

    /**
     * Gateway to IRC
     *
     * {{{
     * <identity category='gateway' type='irc'/>
     * }}}
     */
    object irc extends Identity("gateway", "irc")

    /**
     * Gateway to Microsoft Live Communications Server
     *
     * {{{
     * <identity category='gateway' type='lcs'/>
     * }}}
     */
    object lcs extends Identity("gateway", "lcs")

    /**
     * Gateway to MSN Messenger
     *
     * {{{
     * <identity category='gateway' type='msn'/>
     * }}}
     */
    object msn extends Identity("gateway", "aim")

    /**
     * Gateway to the mail.ru IM service
     *
     * {{{
     * <identity category='gateway' type='mrim'/>
     * }}}
     */
    object mrim extends Identity("gateway", "mrim")

    /**
     * Gateway to the MySpace IM service
     *
     * {{{
     * <identity category='gateway' type='aim'/>
     * }}}
     */
    object myspaceim extends Identity("gateway", "myspaceim")

    /**
     * Gateway to Microsoft Office Communications Server
     *
     * {{{
     * <identity category='gateway' type='ocs'/>
     * }}}
     */
    object ocs extends Identity("gateway", "ocs")

    /**
     * Gateway to the QQ IM service
     *
     * {{{
     * <identity category='gateway' type='qq'/>
     * }}}
     */
    object qq extends Identity("gateway", "qq")

    /**
     * Gateway to IBM Lotus Sametime
     *
     * {{{
     * <identity category='gateway' type='sametime'/>
     * }}}
     */
    object sametime extends Identity("gateway", "sametime")

    /**
     * Gateway to SIP for Instant Messaging and Presence Leveraging Extensions (SIMPLE)
     *
     * {{{
     * <identity category='gateway' type='simple'/>
     * }}}
     */
    object simple extends Identity("gateway", "simple")

    /**
     * Gateway to the Skype service
     *
     * {{{
     * <identity category='gateway' type='skype'/>
     * }}}
     */
    object skype extends Identity("gateway", "skype")

    /**
     * Gateway to Short Message Service
     *
     * {{{
     * <identity category='gateway' type='sms'/>
     * }}}
     */
    object sms extends Identity("gateway", "sms")

    /**
     * Gateway to the SMTP (email) network
     *
     * {{{
     * <identity category='gateway' type='smtp'/>
     * }}}
     */
    object smtp extends Identity("gateway", "smtp")

    /**
     * Gateway to the Tlen IM service
     *
     * {{{
     * <identity category='gateway' type='tlen'/>
     * }}}
     */
    object tlen extends Identity("gateway", "tlen")

    /**
     * Gateway to the Xfire gaming and IM service
     *
     * {{{
     * <identity category='gateway' type='xfire'/>
     * }}}
     */
    object xfire extends Identity("gateway", "xfire")

    /**
     * Gateway to another XMPP service (NOT via native server-to-server communication)
     *
     * {{{
     * <identity category='gateway' type='xmpp'/>
     * }}}
     */
    object xmpp extends Identity("gateway", "xmpp")

    /**
     * Gateway to Yahoo! Instant Messenger
     *
     * {{{
     * <identity category='gateway' type='yahoo'/>
     * }}}
     */
    object yahoo extends Identity("gateway", "yahoo")
  }

  /**
   * The "headline" category consists of services that provide real-time news or information
   * (often but not necessarily in a message of type "headline").
   */
  object headline {

    /**
     * Service that notifies a user of new email messages.
     *
     * {{{
     * <identity category='headline' type='newmail'/>
     * }}}
     */
    object newmail extends Identity("headline", "newmail")

    /**
     * RSS notification service.
     *
     * {{{
     * <identity category='headline' type='rss'/>
     * }}}
     */
    object rss extends Identity("headline", "rss")

    /**
     * Service that provides weather alerts.
     *
     * {{{
     * <identity category='headline' type='weather'/>
     * }}}
     */
    object weather extends Identity("headline", "weather")
  }

  /**
   * The "hierarchy" category is used to describe nodes within a hierarchy of nodes; the "branch" and "leaf" types are exhaustive.
   */
  object hierarchy {

    /**
     * A service discovery node that contains further nodes in the hierarchy.
     *
     * {{{
     * <identity category='hierarchy' type='branch'/>
     * }}}
     */
    object branch extends Identity("hierarchy", "branch")

    /**
     * A service discovery node that does not contain further nodes in the hierarchy.
     *
     * {{{
     * <identity category='hierarchy' type='leaf'/>
     * }}}
     */
    object leaf extends Identity("hierarchy", "leaf")
  }

  /**
   * The "proxy" category consists of servers or services that act as special-purpose proxies or intermediaries between two or more XMPP endpoints.
   */
  object proxy {

    /**
     * SOCKS5 bytestreams proxy service
     *
     * {{{
     * <identity category='proxy' type='bytestreams'/> 
     * }}}
     */
    object bytestreams extends Identity("proxy", "bytestreams")
  }

  /**
   * Services and nodes that adhere to XEP-0060.
   */
  object pubsub {

    /**
     * A pubsub node of the "collection" type.
     *
     * {{{
     * <identity category='pubsub' type='collection'/>
     * }}}
     */
    object collection extends Identity("pubsub", "collection")

    /**
     * A pubsub node of the "leaf" type.
     *
     * {{{
     * <identity category='pubsub' type='leaf'/>
     * }}}
     */
    object leaf extends Identity("pubsub", "leaf")

    /**
     * A personal eventing service that supports the publish-subscribe subset defined in XEP-0163.
     *
     * {{{
     * <identity category='pubsub' type='pep'/>
     * }}}
     */
    object pep extends Identity("pubsub", "pep")

    /**
     * A pubsub service that supports the functionality defined in XEP-0060.
     *
     * {{{
     * <identity category='pubsub' type='service'/>
     * }}}
     */
    object service extends Identity("pubsub", "service")
  }

  /**
   * The "server" category consists of any Jabber/XMPP server.
   */
  object server {

    /**
     * Standard Jabber/XMPP server used for instant messaging and presence
     *
     * {{{
     * <identity category='server' type='im'/>
     * }}}
     */
    object im extends Identity("server", "im")
  }

  /**
   * The "store" category consists of internal server components that provide data storage and retrieval services.
   */
  object store {

    /**
     * A server component that stores data in a Berkeley database
     *
     * {{{
     * <identity category='store' type='berkeley'/>
     * }}}
     */
    object berkeley extends Identity("store", "berkeley")

    /**
     * A server component that stores data on the file system
     *
     * {{{
     * <identity category='store' type='file'/>
     * }}}
     */
    object file extends Identity("store", "file")

    /**
     * A server data storage component other than one of the registered types
     *
     * {{{
     * <identity category='store' type='generic'/>
     * }}}
     */
    object generic extends Identity("store", "generic")

    /**
     * A server component that stores data in an LDAP database
     *
     * {{{
     * <identity category='store' type='ldap'/>
     * }}}
     */
    object ldap extends Identity("store", "ldap")

    /**
     * A server component that stores data in a MySQL database
     *
     * {{{
     * <identity category='store' type='mysql'/>
     * }}}
     */
    object mysql extends Identity("store", "mysql")

    /**
     * A server component that stores data in an Oracle database
     *
     * {{{
     * <identity category='store' type='oracle'/>
     * }}}
     */
    object oracle extends Identity("store", "oracle")

    /**
     * A server component that stores data in a PostgreSQL database
     *
     * {{{
     * <identity category='store' type='postgres'/>
     * }}}
     */
    object postgres extends Identity("store", "postgres")

  }
}

