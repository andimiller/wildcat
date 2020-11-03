package net.andimiller.cats

import cats.data.OptionT
import cats.implicits._
import cats.effect._
import io.chrisdavenport.log4cats.Logger
import scalacache.{Cache, Mode}
import scalacache.serialization.Codec

import scala.concurrent.duration._

package object temper {

  implicit class TemperOps[F[_]: Async, I, O](run: I => F[O]) {
    /**
     * Temper a kleisli by caching it's successful results, and using them on error
     */
    def temperAs[S <: String with Singleton: ValueOf](
                                                       cache: Cache[O],
                                                       ttl: FiniteDuration = 10.minutes
                                                     )(implicit c: Codec[O],
                                                       m: Mode[F],
                                                       log: Logger[F]
    ): I => F[O] =
      { i: I =>
        Async[F].suspend(run(i)).flatTap{ o =>
          cache.put[F](valueOf[S], i)(o, ttl.some)
        }.recoverWith { case t: Throwable =>
          OptionT(cache.get[F](valueOf[S], i))
            .semiflatMap(o =>
              log.warn(t)(s"Unable to call ${valueOf[S]} directly for argument $i, using cached result")
                .as(o)
            )
            .getOrElseF(
              log.warn(t)(s"Unable to call ${valueOf[S]} directly for argument $i, cache was empty") *>
                Async[F].raiseError(t)

            )
        }
      }

  }
}
