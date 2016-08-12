import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Utils {
  def time[A](name: String) (f: => Unit) = {
    val s = System.nanoTime
    f
    println(s"$name: ${((System.nanoTime - s) / 1e6).toLong} elapsed.")
  }

  def timeAsync[A](f: => Future[A])(implicit ctx: ExecutionContext): Future[(A, Long)] = {
    val s = System.nanoTime
    f map { x => (x, ((System.nanoTime - s) / 1e6).toLong) }
  }
}