package $package$

import akka.stream.scaladsl._
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import akka.stream.testkit.scaladsl._

import com.typesafe.config.ConfigFactory

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._
import com.google.protobuf.timestamp.Timestamp

class GreeterSpec extends AnyWordSpec with BeforeAndAfterAll with Matchers with ScalaFutures {
  implicit val patience: PatienceConfig = PatienceConfig(scaled(5.seconds), scaled(100.millis))

  // important to enable HTTP/2 in server ActorSystem's config
  val conf = ConfigFactory
    .parseString("akka.http.server.preview.enable-http2 = on")
    .withFallback(ConfigFactory.defaultApplication())

  val testKit = ActorTestKit(conf)

  val now = java.time.Instant.now()
  val ts  = Some(Timestamp(now.getEpochSecond(), 0))

  val serverSystem: ActorSystem[Nothing] = testKit.system
  val bound                              = new GreeterServer(serverSystem.classicSystem, () => now).run()

  // make sure server is bound before using client
  bound.futureValue

  implicit val clientSystem: ActorSystem[_] = ActorSystem(Behaviors.empty, "GreeterClient")

  val client =
    GreeterServiceClient(GrpcClientSettings.fromConfig("GreeterService"))

  override def afterAll(): Unit = {
    ActorTestKit.shutdown(clientSystem)
    testKit.shutdownTestKit()
  }

  import $package$.SayHelloRequest._

  "GreeterService" should {
    "reply to single request" in {
      val reply = client.sayHello(SayHelloRequest("Alice", SupportedLocales.EN))
      reply.futureValue should ===(SayHelloResponse("Hello, Alice", ts))
    }

    "reply with multiple responses for a single request" in {
      val reply = client.keepSayingHello(SayHelloRequest("Alice", SupportedLocales.EN))
      reply.runWith(TestSink[SayHelloResponse]())
        .request(3)
        .expectNextN(
          List(SayHelloResponse("Ciao, Alice", ts), SayHelloResponse("Hello, Alice", ts), SayHelloResponse("你好, Alice", ts))
        ).expectComplete()
    }

    "reply with a single response to multiple requests" in {
      val reply = client.sayHelloToEveryone(
        Source(List(SayHelloRequest("Oscar", SupportedLocales.EN), SayHelloRequest("Matteo", SupportedLocales.IT)))
      )
      reply.futureValue should ===(SayHelloResponse("Hello Oscar, Matteo, ", ts))
    }

    "reply with multiple responses to multiple requests" in {
      val reply = client.sayHelloForeachOne(
        Source(List(SayHelloRequest("Oscar", SupportedLocales.EN), SayHelloRequest("Matteo", SupportedLocales.IT)))
      )
      reply
        .runWith(TestSink[SayHelloResponse]())
        .request(3)
        .expectNextN(List(SayHelloResponse("Hello, Oscar", ts), SayHelloResponse("Ciao, Matteo", ts)))
        .expectComplete()
    }
  }
}
