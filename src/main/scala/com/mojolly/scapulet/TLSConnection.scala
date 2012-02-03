package com.mojolly.scapulet

import XMPPConstants._
import util.Implicits._
import java.io.{ File, FileInputStream, InputStream, IOException }
import java.security._
import akka.util.Logging
import java.security.cert.{ X509Certificate, CertificateException }
import javax.net.ssl.X509TrustManager
import Scapulet.TLSConfig
import java.util.Date

trait TLSStanza extends Logging {
  def startTLS = <starttls xmlns={TLS_NS} />

  def processTLS 
}

object TLSConnection extends TLSStanza with X509TrustManager {

  private var _truststore: KeyStore = null
  private val commonNameRegex = """(?i)(cn=)([^,]*)""".r
  private var _server: String = _
  
  def processTLS = {}

  private var _config: TLSConfig = _

  def config_=(conf: TLSConfig) = {
    _config = config
    _config
  }

  def config = _config

  def server = _server
  def server_=(s: String) = { 
    _server = s
    _server
  }
  
  def keystorePath = {
    if(config.keystorePath.isBlank) {
      _config = _config.copy(keystorePath = System.getProperty("javax.net.ssl.keyStore"))
    }
    config.keystorePath
  }

  def trustStorePath = {
    if(config.truststorePath.isBlank) {
      val sb = new StringBuilder
      sb append (System getProperty "java.home") append File.separator append "lib"
      sb append File.separator append "security" append File.separator append "cacerts"
      _config = _config.copy(truststorePath = sb.toString)
    }
    config.truststorePath
  }

  def truststore: KeyStore = {
    if (_truststore == null) {
      var in: InputStream = null
      try {
        _truststore = KeyStore.getInstance(config.truststoreType)
        in = new FileInputStream(trustStorePath)
        _truststore.load(in, config.truststorePassword.toArray)
      } catch {
        case e => { 
          log error (e, "An error occurred while getting the trust store")
          _config = _config.copy(verifyRootCA = false)
        }
      } finally {
        if(in != null) try { in.close } catch { case e: IOException => }
      }
    } 
    _truststore
  }

  def getAcceptedIssuers = Array[X509Certificate]()

  def checkClientTrusted(certificates: Array[X509Certificate], arg: String) {
  }

  def checkServerTrusted(certificates: Array[X509Certificate], arg: String) {
    if (certificates.length == 0) throw new CertificateException("We need at least one certificate to validate")

    val peerId: List[String] = (certificates.headOption map { peerIdentity _ }) getOrElse Nil

    if(config.verifyChain) {
      var lastPrincipal: Principal = null
      certificates.reverse sliding (2, 1) foreach { certs =>
        val curr = certs.last
        val subject = curr.getSubjectDN()
        if(lastPrincipal != null) {
          if (curr.getIssuerDN() == lastPrincipal) {
            try {
              val pubKey = certs.head.getPublicKey()
              curr verify pubKey
            } catch {
              case e: GeneralSecurityException => {
                throw new CertificateException("Signature verifcation failed for: " + peerId.mkString(", "))
              }
            }
          } else throw new CertificateException("Subject/issuer verification failed for: " + peerId.mkString(", "))
        }
        lastPrincipal = subject
      }
    }

    if(config.verifyRootCA) {
      var trusted = false
      try {
        trusted = truststore.getCertificateAlias(certificates.last) != null
        if( !trusted && certificates.length == 1 && config.allowSelfSignedCertificate) {
          log.warn("Accepting self-signed certificate from remote server: " + peerId.mkString(", "))
          trusted = true
        }
      } catch {
        case e: KeyStoreException =>
          log.error(e, "Couldn't validate the Root CA")
      }
      if(!trusted) throw new CertificateException("" + peerId.mkString(", "))
    }

    if(config.verifyMatchingDomain) {
      if(peerId.length > 0 && peerId.head.startsWith("*.")) {
        val peer = peerId.head.replace("*.", "")
        if(!server.endsWith(peer)) throw new CertificateException("Target verification failed for: " + peerId.mkString(","))
      } else if(!peerId.contains(server)) {
        throw new CertificateException("Target verification failed for: " + peerId.mkString(","))
      }
    }
    
    if(!config.allowExpiredCertificates) {
      val now = new Date
      certificates foreach { cert => 
        try {
          cert checkValidity now
        } catch {
          case e: GeneralSecurityException => {
            throw new CertificateException("certificate for " + server + " has expired")
          }
        }
      }
    }
  }

  def peerIdentity(cert: X509Certificate) = {
    val nm = cert.getSubjectDN.getName
    val name = commonNameRegex.findFirstMatchIn(nm) map { _.group(2) } getOrElse nm
    name :: Nil
  }
  
}
