import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import concurrent.duration._
import language.postfixOps
import scala.concurrent._

object Main {
  def main(args: Array[String]) = {
    if (args.length != 2)
      throw new Exception("Two arguments are required: <total number of requests> " +
                          "|<number of parallel requests>".stripMargin)

    val (totalRequests, threads) = (args(0).toInt, args(1).toInt)
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    def asyncReq() = Http().singleRequest(HttpRequest(uri = "http://10.70.16.195:31000/person/kot"))

    Source
      .fromIterator(() => Iterator.from(1).take(totalRequests))
      .mapAsync(threads)(_ => asyncReq())
      .to(Sink.ignore)
      .run()

    //.runForeach(r => println(r))

    println("Done")
  }
}
