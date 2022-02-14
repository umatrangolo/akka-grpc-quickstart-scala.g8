package $package$

import org.slf4j.LoggerFactory
import io.grpc.health.v1._
import scala.concurrent.Future
import akka.stream.scaladsl._

object HealthServiceImpl {
  val logger = LoggerFactory.getLogger(this.getClass)
}

// TODO: real healthcheck (re-using the impl from grpc-java)
class HealthServiceImpl extends Health {
  override def check(in: HealthCheckRequest): Future[HealthCheckResponse] =
    Future.successful(HealthCheckResponse(HealthCheckResponse.ServingStatus.SERVING))
  override def watch(in: HealthCheckRequest): Source[HealthCheckResponse, akka.NotUsed] = ???
}
