package $package$

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.scaladsl.ServerReflection
import akka.grpc.scaladsl.ServiceHandler
import akka.http.scaladsl.Http
import com.typesafe.config.ConfigFactory
import $package$._
import io.grpc.health.v1.Health
import io.grpc.health.v1.HealthHandler
import org.slf4j.LoggerFactory

import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success
import scala.util.Failure

// TODO: Audit
// TODO: config ?
// TODO: 12 Factors App ?
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

    Http().newServerAt("127.0.0.1", 9000)
      .bind(service)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))
  }
}
