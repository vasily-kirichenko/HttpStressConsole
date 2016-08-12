import java.util.UUID

import akka.actor.ActorSystem
import akka.http.javadsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.OutgoingConnection
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import spray.json._

import language.postfixOps
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait Message
final case class Person(name: String, age: Int) extends Message
final case class Envelope[A <: Message](payload: A, server: String, appId: String)

object Envelope {
  private val id = UUID.randomUUID()
  def apply[A <: Message](payload: A): Envelope[A] =
    Envelope(payload, java.net.InetAddress.getLocalHost.toString, id.toString)
}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit def envelopeFormat[A <: Message : JsonFormat] = jsonFormat3(Envelope.apply[A])
  implicit val personFormat = jsonFormat2(Person)
}

object Main extends JsonSupport {
  def main(args: Array[String]) = {
    if (args.length != 2)
      throw new Exception("Two arguments are required: <total number of requests> " +
                          "|<number of parallel requests>".stripMargin)

    val (totalRequests, threads) = (args(0).toInt, args(1).toInt)
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val ctx = system.dispatcher

    val req = HttpRequest(uri = "http://10.70.16.193:31010/person/kot")
    def asyncReq() = Http().singleRequest(req)
    //val connectionFlow = Http().newHostConnectionPool("10.70.16.193", 31010)
    //val connectionFlow: Flow[HttpRequest, HttpResponse, Future[OutgoingConnection]] = Http().outgoingConnection("10.70.16.193", 31010)
    //val req = HttpRequest(uri = "/person/kot")
    val res = Utils.timeAsync {
      Source
        .fromIterator(() => Iterator.from(1).take(totalRequests))
        .mapAsyncUnordered(threads)(_ => asyncReq() flatMap { r => Unmarshal(r.entity).to[Envelope[Person]] })
        .map(r => r.server)
        .runFold(Map[String, Int]())((m, server) => {
          val n = m get server match {
            case Some(x) => x + 1
            case None => 1
          }
          m + (server -> n)
        })
    }

    res.onComplete {
      case Success((m, elapsedMillis)) =>
        println(s"$elapsedMillis ms elapsed, ${totalRequests * 1000 / elapsedMillis} req/s")
        m foreach { case (server, n) => println(s"$server: $n") }
      case Failure(e) => println(e)
    }
  }
}
