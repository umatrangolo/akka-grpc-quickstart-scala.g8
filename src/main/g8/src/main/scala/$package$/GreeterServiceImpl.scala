package $package$

import akka.grpc.GrpcServiceException
import akka.stream.Materializer
import akka.stream.scaladsl._
import com.google.protobuf.timestamp.Timestamp
import io.corecursive.coffeemaker.SayHelloRequest
import io.corecursive.coffeemaker.SayHelloResponse
import io.grpc.Status
import org.slf4j.LoggerFactory

import java.time._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

// TODO: error as values
// TODO: more meaningful example endpoints
// TODO: better logging
object GreeterServiceImpl {
  import io.corecursive.coffeemaker.SayHelloRequest._

  val logger = LoggerFactory.getLogger(this.getClass)

  def die() = throw new NullPointerException("bomb") // scalafix:ok

  def mkGreeting(locale: SupportedLocales, username: String) = locale match {
    case SupportedLocales.IT => s"Ciao, \${username}"
    case SupportedLocales.EN => s"Hello, \${username}"
    case SupportedLocales.CH => s"你好, \${username}"
    case _                   => die()  // Failure
  }
}

import GreeterServiceImpl._

class GreeterServiceImpl(val now: () => Instant)(implicit mat: Materializer) extends GreeterService {
  import mat.executionContext

  override def sayHello(in: SayHelloRequest): Future[SayHelloResponse] = Future.successful {
    logger.info(s"sayHello(\$in)")

    // Error
    if (in.username.trim.isEmpty)
      throw new GrpcServiceException(Status.INVALID_ARGUMENT.withDescription("Username must be non empty"))

    SayHelloResponse(greetings = mkGreeting(in.locale, in.username), Some(Timestamp(now().getEpochSecond(), 0)))
  }

  override def keepSayingHello(in: SayHelloRequest): Source[SayHelloResponse, akka.NotUsed] = {
    logger.info(s"keepSayingHello(\$in)")

    // Error
    if (in.username.trim.isEmpty)
      throw new GrpcServiceException(Status.INVALID_ARGUMENT.withDescription("Username must be non empty"))

    val ts = Timestamp(now().getEpochSecond(), 0)
    Source(
      List(
        SayHelloResponse(greetings = mkGreeting(SayHelloRequest.SupportedLocales.IT, in.username), Some(ts)),
        SayHelloResponse(greetings = mkGreeting(SayHelloRequest.SupportedLocales.EN, in.username), Some(ts)),
        SayHelloResponse(greetings = mkGreeting(SayHelloRequest.SupportedLocales.CH, in.username), Some(ts)),
      ),
    ).throttle(1, 1 seconds)
  }

  override def sayHelloToEveryone(in: Source[SayHelloRequest, akka.NotUsed]): Future[SayHelloResponse] = {
    logger.info(s"sayHelloToEveryone: \$in")

    in.runFold("Hello ")((ns, n) => ns + s"\${n.username}, ")
      .map(ns => SayHelloResponse(ns, Some(Timestamp(now().getEpochSecond(), 0))))
  }

  override def sayHelloForeachOne(in: Source[SayHelloRequest, akka.NotUsed]): Source[SayHelloResponse, akka.NotUsed] = {
    logger.info(s"sayHelloForeachOne: \$in")

    in.map(r => SayHelloResponse(mkGreeting(r.locale, r.username), Some(Timestamp(now().getEpochSecond(), 0))))
  }
}
