package $package$

import akka.grpc.GrpcServiceException
import akka.stream.Materializer
import akka.stream.scaladsl._
import com.google.protobuf.timestamp.Timestamp
import $package$.SayHelloRequest
import $package$.SayHelloRequest._
import $package$.SayHelloResponse
import io.grpc.Status
import org.slf4j.LoggerFactory
import cats.implicits._

import java.time._
import scala.concurrent.Future
// import scala.concurrent.duration._
// import scala.language.postfixOps

// TODO: error as values
// TODO: more meaningful example endpoints
// TODO: better logging
object GreeterServiceImpl {
  val logger = LoggerFactory.getLogger(this.getClass)

  def mkGreeting(locale: SupportedLocales, username: String*): Option[String] = locale match {
    case SupportedLocales.IT => Some(s"Ciao \${username.mkString(",")}")
    case SupportedLocales.EN => Some(s"Hello \${username.mkString(",")}")
    case SupportedLocales.CH => Some(s"你好 \${username.mkString(",")}")
    case _                   => None
  }

  // With Akka gRPC errors are communicated upstream by throwing (eww)
  // an exception.
  // ref: https://doc.akka.io/docs/akka-grpc/current/server/details.html#status-codes
  def invalid(msg: String) = throw new GrpcServiceException(Status.INVALID_ARGUMENT.withDescription(msg))
}

import GreeterServiceImpl._

class GreeterServiceImpl(val now: () => Instant)(implicit mat: Materializer) extends GreeterService {
  import mat.executionContext

  override def sayHello(in: SayHelloRequest): Future[SayHelloResponse] = (for {
    n <- Either.cond(in.username.trim.nonEmpty, in.username, "Empty username")
    g <- mkGreeting(in.locale, n).toRight("Locale not supported")
    t = Some(Timestamp(now().getEpochSecond(), 0))
  } yield SayHelloResponse(g, t)).fold(
    err => invalid(err),
    shr => {
      logger.info(s"Saying: \$shr")
      Future.successful(shr)
    },
  )

  override def sayHellos(in: SayHelloRequest): Source[SayHelloResponse, akka.NotUsed] = (for {
    n  <- Either.cond(in.username.trim.nonEmpty, in.username, "Empty username")
    gs <- SayHelloRequest.SupportedLocales.values.map(l => mkGreeting(l, n).toRight("Locale not supported")).sequence
    t = Some(Timestamp(now().getEpochSecond(), 0))
  } yield gs.map(g => SayHelloResponse(g, t))).fold(
    err => invalid(err),
    shrs => {
      logger.info(s"Saying: \$shrs")
      Source(shrs)
    },
  )

  override def sayHelloToEveryone(in: Source[SayHelloRequest, akka.NotUsed]): Future[SayHelloResponse] = in
    .runFold(List.empty[String])((ns, n) => if (n.username.trim.isEmpty) invalid("Empty username") else n.username :: ns)
    .map { ns =>
      val greeting = mkGreeting(SupportedLocales.EN, ns: _*).get
      val t = Some(Timestamp(now().getEpochSecond(), 0))
      val shr = SayHelloResponse(greeting, t)
      logger.info(s"Saying: \$shr")
      shr
    }

  override def sayHelloForeachOne(in: Source[SayHelloRequest, akka.NotUsed]): Source[SayHelloResponse, akka.NotUsed] = in
    .map(r =>
      (for {
        n <- Either.cond(r.username.trim.nonEmpty, r.username, "Empty username")
        g <- mkGreeting(r.locale, n).toRight("Locale not supported")
        t = Some(Timestamp(now().getEpochSecond(), 0))
      } yield SayHelloResponse(g, t)).fold(err => invalid(err), shr => shr),
    )
}
