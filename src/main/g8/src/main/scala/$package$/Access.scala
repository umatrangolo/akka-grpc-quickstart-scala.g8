package $package$

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.RouteResult.Rejected
import akka.http.scaladsl.server._
import net.logstash.logback.argument.StructuredArguments._
import org.slf4j.Logger

import scala.concurrent.duration._

// \$COVERAGE-OFF\$: not worth it

// This is a stripped down version of the access logger found in the
// Akka HTTP temaplte: intercepting req/resp at the HTTP level is
// problematic cause (i) is an abstraction leakage (IMHO), (ii) does
// not offer all the needed info and (iii) most of the machinery is
// hidden (e.g. all calls are a POST). For the moment is the best I
// could do.
//
// Note that you will always see a 200 OK even if the response was a
// Bad Request given that everything gets tunnelled through an always
// successful HTTP response.
object Access {
  private object domain {
    case class ReqLogEntry(
      path: String,
      remote_ip: Option[String] = None,
      remote_country: Option[String] = None,
    )

    case class RespLogEntry(
      status: String,
      code: Int,
    )
  }

  import domain._

  private def headerValueOrNone(req: HttpRequest, name: String) =
    req.headers.find(_.lowercaseName == name.toLowerCase()).map(_.value)

  // Best effort to figure out the remote caller's IP. We start by looking
  // at the CloudFlare header to then proceed with the more or less
  // standard ones.
  // TODO: Not sure if these are still valid in a gRPC world.
  private def extractRemoteIP(req: HttpRequest): Option[String] =
    List(
      headerValueOrNone(req, "cf-connecting-ip"),
      headerValueOrNone(req, "X-Forwarded-For"),
      headerValueOrNone(req, "RemoteAddress"),
      headerValueOrNone(req, "X-Real-IP"),
    ).flatten.headOption

  // If we are behind CloudFlare we will be also getting the
  // originating country.
  private def extractCountry(req: HttpRequest): Option[String] = headerValueOrNone(req, "Cf-Ipcountry")

  private def asLog(req: HttpRequest): ReqLogEntry =
    ReqLogEntry(
      path = req.uri.path.toString,
      remote_ip = extractRemoteIP(req),
      remote_country = extractCountry(req),
    )

  private def asLog(resp: HttpResponse): RespLogEntry = RespLogEntry(status = resp.status.value, code = resp.status.intValue)

  private def akkaResponseTimeLoggingFunction(requestTimestamp: Long, req: HttpRequest, res: RouteResult) = res match {
    case Complete(resp) =>
      val responseTimestamp: Long = System.nanoTime()
      val elapsedTime: Long =
        (FiniteDuration(responseTimestamp, NANOSECONDS) - FiniteDuration(requestTimestamp, NANOSECONDS)).toMillis // convert to ms
      (
        s"""\${req.uri.path} \${resp.status} \$elapsedTime""",
        Array(
          value("elapsed_time", elapsedTime),
          value("request", asLog(req)),
          value("response", asLog(resp)),
        ),
      )
    case Rejected(reason) =>
      (
        s"""Rejected Reason: \${reason.mkString(",")}""",
        Array(
          value("request", asLog(req)),
        ),
      )
  }

  def logTimedRequestResponse(logger: Logger) = extractRequestContext.flatMap { ctx =>
    val requestTimestamp = System.nanoTime()
    mapRouteResult { resp =>
      import akka.http.scaladsl.model.Uri.Path
      // Ignore the reflection calls to avoid too much noise. These
      // are just gRPC clients trying to figure out if the call is
      // supported by the server using API introspection.
      if (!ctx.request.uri.path.startsWith(Path("/grpc.reflection.v1alpha.ServerReflection"))) {
        val (msg, args) = akkaResponseTimeLoggingFunction(requestTimestamp, ctx.request, resp)
        logger.info(msg, args.asInstanceOf[Array[Object]]: _*)
      }

      resp
    }
  }
}
// \$COVERAGE-ON\$
