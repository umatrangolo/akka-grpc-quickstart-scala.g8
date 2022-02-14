package $package$

import akka.actor.ActorSystem
import akka.grpc.scaladsl.{ServiceHandler, ServerReflection}
import akka.http.scaladsl.Http
import com.typesafe.config.ConfigFactory
import io.grpc.health.v1.{Health, HealthHandler}
import java.time.Instant
import org.slf4j.LoggerFactory
import scala.concurrent.{ExecutionContext, Future}

import $package$._

object GreeterServer {
  val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    val conf = ConfigFactory
      .parseString("akka.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.defaultApplication())
    val system                        = ActorSystem("GreeterServer", conf)
    implicit val ec: ExecutionContext = system.dispatcher
    new GreeterServer(system, () => Instant.now()).run().foreach(_ => logger.info("Server started at port 9000"))
  }
}

class GreeterServer(system: ActorSystem, now: () => Instant) {
  def run(): Future[Http.ServerBinding] = {
    // Akka boot up code
    implicit val sys: ActorSystem = system

    // Create services
    val greeterService    = GreeterServiceHandler.partial(new GreeterServiceImpl(now))
    val healthService     = HealthHandler.partial(new HealthServiceImpl())
    val reflectionService = ServerReflection.partial(List(GreeterService, Health))

    // Create service handlers
    val service = ServiceHandler.concatOrNotFound(greeterService, healthService, reflectionService)

    Http().newServerAt("127.0.0.1", 9000).bind(service)
  }
}
