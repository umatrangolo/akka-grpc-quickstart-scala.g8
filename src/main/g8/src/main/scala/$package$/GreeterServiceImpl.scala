package $package$

import $package$.{SayHelloRequest, SayHelloResponse}
import scala.concurrent.Future
import org.slf4j.LoggerFactory
import com.google.protobuf.timestamp.Timestamp
import java.time._
import akka.stream.scaladsl._
import akka.stream.Materializer
import akka.grpc.GrpcServiceException
import io.grpc.Status
import scala.concurrent.duration._

object GreeterServiceImpl {
  import $package$.SayHelloRequest._

  val logger = LoggerFactory.getLogger(this.getClass)

  def die() = throw new NullPointerException("bomb") // scalafix:ok

  def mkGreeting(locale: SupportedLocales, username: String) = locale match {
    case SupportedLocales.IT                                      => s"Ciao, \${username}"
    case SupportedLocales.EN                                      => s"Hello, \${username}"
    case SupportedLocales.CH if scala.util.Random.nextInt(10) > 6 => s"你好, \${username}"
    // Failure
    case _ => throw new NullPointerException("Ouch!")
  }
}

import GreeterServiceImpl._

class GreeterServiceImpl(val now: () => Instant)(implicit mat: Materializer) extends GreeterService {
  import mat.executionContext

  override def sayHello(in: SayHelloRequest): Future[SayHelloResponse] = Future.successful {
    import $package$.SayHelloRequest._
    logger.info(s"sayHello(\$in)")
    // Error
    if (in.username.trim.isEmpty)
      throw new GrpcServiceException(Status.INVALID_ARGUMENT.withDescription("Username must be non empty"))

    val greeting = in.locale match {
      case SupportedLocales.IT                                      => s"Ciao, \${in.username}"
      case SupportedLocales.EN                                      => s"Hello, \${in.username}"
      case SupportedLocales.CH if scala.util.Random.nextInt(10) > 6 => s"你好, \${in.username}"
      // Failure
      case _ => throw new NullPointerException("Ouch!")
    }

    SayHelloResponse(greetings = greeting, Some(Timestamp(now().getEpochSecond(), 0)))
  }

  override def keepSayingHello(in: SayHelloRequest): Source[SayHelloResponse, akka.NotUsed] = {
    import scala.language.postfixOps

    logger.info(s"keepSayingHello(\$in)")
    // Error
    if (in.username.trim.isEmpty)
      throw new GrpcServiceException(Status.INVALID_ARGUMENT.withDescription("Username must be non empty"))

    val ts = Timestamp(now().getEpochSecond(), 0)
    Source(
      List(
        SayHelloResponse(greetings = s"Ciao, \${in.username}", Some(ts)),
        SayHelloResponse(greetings = s"Hello, \${in.username}", Some(ts)),
        SayHelloResponse(greetings = s"你好, \${in.username}", Some(ts)),
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
