package io.backchat.scapulet

import xml.{ Node, XML }
import javax.security.sasl._
import javax.security.auth.callback._
import collection.JavaConversions._
import io.backchat.scapulet.Scapulet.ScapuletConnection
import net.iharder.Base64

object SASLAuthentication {

  sealed trait SASLFailure

  abstract class Failure(condition: String) extends SASLFailure {
    def apply() = {
      <failure xmlns={ ns.Sasl }>
        { XML.loadString("<%s />" format condition) }
      </failure>
    }

    def unapply(node: Node) = node match {
      case f @ <failure>{ ch @ _* }</failure> if !(f \ condition).isEmpty => {
        Some(condition)
      }
      case _ => None
    }
  }

  object Aborted extends Failure("aborted")

  object IncorrectEncoding extends Failure("incorrect-encoding")

  object InvalidAuthzid extends Failure("invalid-authzid")

  object InvalidMechanism extends Failure("invalid-mechanism")

  object MechanismTooWeak extends Failure("mechanism-too-weak")

  object NotAuthorized extends Failure("not-authorized")

  object TemporaryAuthFailure extends Failure("temporary-auth-failure")

  object SASLFailure extends SASLFailure {
    def unapply(node: Node) = node match {
      case f @ <failure>{ condition @ _* }</failure> if !condition.isEmpty => condition.headOption.map(_.label)
      case _ => None
    }
  }

  object Success {
    def apply(data: String) = <success xmlns={ ns.Sasl }>
                                { data }
                              </success>

    def unapply(node: Node) = node match {
      case s @ <success>{ _* }</success> => Some(s.text)
      case _ => None
    }
  }

  object Challenge {
    def apply(data: String) = <challenge xmlns={ ns.Sasl }>
                                { data }
                              </challenge>

    def unapply(node: Node) = node match {
      case s @ <challenge>{ _* }</challenge> => Some(s.text)
      case _ => None
    }
  }

  object Response {
    def apply(data: String) = {
      <response xmlns={ ns.Sasl }>
        { data }
      </response>
    }
  }

  val xmpp = "xmpp"
  val saslAuth = this

  trait SASLMechanism extends CallbackHandler {

    protected var _userName: String = _
    protected var _password: Array[Char] = _
    protected var _hostName: String = _

    def userName = _userName

    def password = new String(_password)

    def hostName = _hostName

    val name: String
    protected lazy val mechanism = Array(name)
    protected lazy val saslProps = Map[String, String]()

    protected var saslClient: SaslClient = _

    def authenticate(username: String, host: String, pass: String) {
      _userName = username
      _password = pass.toArray
      _hostName = host
      saslClient = Sasl.createSaslClient(mechanism, username, xmpp, host, saslProps, this)
      authenticate
    }

    def authenticate(username: String, host: String) {
      saslClient = Sasl.createSaslClient(mechanism, username, xmpp, host, saslProps, this)
      authenticate
    }

    protected def authenticate() {
      var authText: String = ""
      try {
        if (saslClient.hasInitialResponse) {
          val resp = saslClient.evaluateChallenge(new Array[Byte](0))
          authText = Base64.encodeBytes(resp)
        }
      } catch {
        case e: SaslException => throw new SASLAuthenticationFailed(e)
      }
      connection.write(<auth mechanism={ name } xmlns={ ns.Sasl }>
                         { authText }
                       </auth>)
    }

    def onServerChallenge(challenge: String) {
      val response = if (challenge.isNotNull) {
        saslClient.evaluateChallenge(Base64.decode(challenge))
      } else {
        saslClient.evaluateChallenge(new Array[Byte](0))
      }

      val stanza = if (response == null) Response("") else Response(Base64.encodeBytes(response))
      connection.write(stanza)
    }

    def handle(callbacks: Array[Callback]) {
      callbacks foreach {
        case cb: NameCallback => {
          cb.setName(_userName)
        }
        case cb: PasswordCallback => {
          cb.setPassword(_password)
        }
        case cb: RealmCallback => {
          cb.setText(_hostName)
        }
        case cb: RealmChoiceCallback => {}
        case cb: Callback => throw new UnsupportedCallbackException(cb)
      }
    }

    private var _connection: ScapuletConnection = _

    def connection = _connection

    def connection_=(conn: ScapuletConnection) = {
      _connection = conn
      _connection
    }
  }

  object SASLMechanisms {

    class SASLPlainMechanism extends SASLMechanism {
      val name = "PLAIN"
    }

    class SASLCramMD5Mechanism extends SASLMechanism {
      val name = "CRAM-MD5"
    }

    class SASLDigestMD5Mechanism extends SASLMechanism {
      val name = "DIGEST-MD5"
    }

  }

}

