package gonitro

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.Http
import com.typesafe.config.ConfigFactory
import gonitro._
import scala.concurrent.{ ExecutionContext, Future }
import org.slf4j.LoggerFactory
import java.time.Instant
import io.grpc.protobuf.services._
import akka.grpc.scaladsl.{ ServiceHandler, ServerReflection }
import io.grpc.health.v1.{ Health, HealthHandler }

object GreeterServer {
  val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    val conf = ConfigFactory
      .parseString("akka.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.defaultApplication())
    val system = ActorSystem("GreeterServer", conf)
    implicit val ec: ExecutionContext = system.dispatcher
    new GreeterServer(system, () => Instant.now()).run().foreach(_ => logger.info("Server started at port 8080/8081"))
  }
}

class GreeterServer(system: ActorSystem, now: () => Instant) {
  def run(): Future[Http.ServerBinding] = {
    // Akka boot up code
    implicit val sys: ActorSystem = system

    // Create services
    val greeterService = GreeterServiceHandler.partial(new GreeterServiceImpl(now))
    val healthService = HealthHandler.partial(new HealthServiceImpl())
    val reflectionService = ServerReflection.partial(List(GreeterService, Health))

    // Create service handlers
    val service = ServiceHandler.concatOrNotFound(greeterService, healthService, reflectionService)

    Http().newServerAt("127.0.0.1", 9000).bind(service)
  }
}
