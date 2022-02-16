package $package$

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.scaladsl.ServerReflection
import akka.grpc.scaladsl.ServiceHandler
import akka.http.scaladsl.Http
import akka.http.scaladsl.HttpsConnectionContext
import akka.http.scaladsl.ConnectionContext
import akka.pki.pem.DERPrivateKeyLoader
import akka.pki.pem.PEMDecoder
import com.typesafe.config.ConfigFactory
import $package$._
import io.grpc.health.v1.Health
import io.grpc.health.v1.HealthHandler
import org.slf4j.LoggerFactory

import java.time.Instant
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success
import scala.util.Failure
import scala.io.Source

// TODO: Audit
// TODO: config ?
// TODO: 12 Factors App ?
// TODO: whatever happens shutdown the actor sys or it will hang
object GreeterServer {
  val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    val conf = ConfigFactory
      .parseString("akka.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.defaultApplication())
    val system                        = ActorSystem[Nothing](Behaviors.empty, "GreeterServer", conf)
    implicit val ec: ExecutionContext = system.executionContext

    val bound = new GreeterServer(system, () => Instant.now()).run()
    bound.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        logger.info("gRPC server bound to {}:{}", address.getHostString, address.getPort)
      case Failure(ex) =>
        logger.error("Failed to bind gRPC endpoint, terminating system", ex)
        system.terminate()
    }
  }
}

class GreeterServer(system: ActorSystem[_], now: () => Instant) {
  def run(): Future[Http.ServerBinding] = {
    // Akka boot up code
    implicit val sys = system
    implicit val ec  = system.executionContext

    // Create services
    val greeterService    = GreeterServiceHandler.partial(new GreeterServiceImpl(now))
    val healthService     = HealthHandler.partial(new HealthServiceImpl())
    val reflectionService = ServerReflection.partial(List(GreeterService, Health))

    // Create service handlers
    val service = ServiceHandler
      .concatOrNotFound(greeterService, healthService, reflectionService)

    Http()
      .newServerAt("127.0.0.1", 9000)
      .enableHttps(serverHttpContext)
      .bind(service)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))
  }

  def serverHttpContext: HttpsConnectionContext = {
    // This loads our private key from a file and decodes it following
    // the PEM standard as defined in
    // https://datatracker.ietf.org/doc/html/rfc7468
    val pemKey = PEMDecoder.decode(Source.fromResource("certs/server1.key").mkString)

    // Converting the PEM defined private key into a java PrivateKey
    // (https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/security/PrivateKey.html#java.security.PrivateKey)
    val privateKey = DERPrivateKeyLoader.load(pemKey)

    // Load and parse our X.509 certificate from the fs
    val certFactory = CertificateFactory.getInstance("X.509")
    val certificate = certFactory.generateCertificate(classOf[GreeterServer].getResourceAsStream("/certs/server1.pem"))

    // Create a key store that will encode all its content using the
    // PKCS#12 archive file format as defined in
    // https://datatracker.ietf.org/doc/html/rfc7292
    val keyStore = KeyStore.getInstance("PKCS12")

    keyStore.load(null) // We don't need to load the key store (null as param)
    keyStore.setKeyEntry(
      "private",
      privateKey,
      new Array[Char](0), // passwordless
      Array[Certificate](certificate),
    )

    // Building the key manager needed by the java SSL machinery. It
    // will use the keys we stored in our key store.
    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(keyStore, null)

    // Finally building the Secure Socker Layer (SSL) and initializing
    // it with our private keys, certs and a random source.
    val context = SSLContext.getInstance("TLS")
    context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom)

    ConnectionContext.httpsServer(context)
  }
}
