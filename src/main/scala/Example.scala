import cats.data.State
import cats.effect.concurrent.{MVar, Ref}
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import net.andimiller.cats.temper._
import scalacache.serialization.circe._
import scalacache.caffeine._
import scalacache.CatsEffect.modes._
import scala.concurrent.duration._

import scala.util.control.NoStackTrace

object Example extends IOApp {
  def print(a: Any): IO[Unit] = IO { println(a) }

  implicit val log: Logger[IO] = Slf4jLogger.getLoggerFromName("main")

  override def run(args: List[String]): IO[ExitCode] = for {
    _ <- IO { println("hi") }
    cache = CaffeineCache[Boolean]
    set <- Ref.of[IO, Set[String]](Set.empty)
    f = {s: String =>
      set.modifyState {
        State { strings: Set[String] =>
          if (strings.contains(s)) {
            (strings, false)
          } else {
            (strings.incl(s), true)
          }
        }
      }.flatMap {
        case true => IO { true }
        case false =>  IO.raiseError(new Throwable("oh no") with NoStackTrace)
      }
    }.temperAs["foo"](cache, ttl = 5.seconds)
    _ <- f("hello").flatTap(print)
    _ <- f("hello").flatTap(print)
    _ <- IO.sleep(6.seconds)
    _ <- f("hello").flatTap(print)
  } yield ExitCode.Success
}
