package net.andimiller

import cats.data.{Kleisli, OptionT}
import cats.implicits._
import cats.effect._
import io.chrisdavenport.log4cats.Logger
import scalacache.{Cache, Mode}
import scalacache.serialization.Codec

import scala.concurrent.duration._

package object wildcat {

  def resilienceCache[F[_]: Async, I, O, S <: String with Singleton: ValueOf](
                                                                             run: I => F[O]
                                                                           )(cache: Cache[O],
                                                                             ttl: FiniteDuration)(
                                                                           implicit c: Codec[O],
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

  object syntax {
    implicit class WildcatFuncOps[F[_]: Async, I, O](run: I => F[O]) {
      def resilienceCacheNamed[S <: String with Singleton: ValueOf](cache: Cache[O], ttl: FiniteDuration)(
                                                                 implicit c: Codec[O], m: Mode[F], log: Logger[F]
      ): I => F[O] =
        wildcat.resilienceCache[F, I, O, S](run)(cache, ttl)
    }

    implicit class WildcatKleisliOps[F[_]: Async, I, O](k: Kleisli[F, I, O]) {
      def resilienceCacheNamed[S <: String with Singleton: ValueOf](cache: Cache[O], ttl: FiniteDuration)(
                                                                 implicit c: Codec[O], m: Mode[F], log: Logger[F]
      ): Kleisli[F, I, O] = Kleisli(
        wildcat.resilienceCache[F, I, O, S](k.run)(cache, ttl)
      )
    }
  }

}


