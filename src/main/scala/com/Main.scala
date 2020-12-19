package com

import akka.Done
import akka.actor.Props
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.weather.place.{Place, PlaceRegistry, PlacesQueries}
import com.weather.forecast.{ForecastRegistry, ForecastsQueries}
import com.weather.schedule.ScheduleActor
import com.weather.schedule.ScheduleActor.duration

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, DurationInt}
import scala.reflect.ClassTag.Any
import scala.util.{Failure, Success}

object Main {
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

    val setup = for {
      _ <- PlacesQueries.setup()
      _ <- Future.sequence(jsonToPlaceSequence.map(result => {
        PlacesQueries.upsert(result)
      }))
      _ <- ForecastsQueries.setup()
    } yield "Started"

    val schedule = akka.actor.ActorSystem("system")
    val scheduleActor = schedule.actorOf(Props(classOf[ScheduleActor]))
    schedule.scheduler.scheduleWithFixedDelay(
      Duration.Zero,
      duration(),
      scheduleActor,
      ScheduleActor.Tick)

    val futureBinding = Http().newServerAt("localhost", 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {
    val rootBehavior = Behaviors.setup[Nothing] {
      context =>
        val forecastRegistryActor = context.spawn(ForecastRegistry(), "ForecastRegistryActor")
        context.watch(forecastRegistryActor)
        val placeRegistryActor = context.spawn(PlaceRegistry(), "PlaceRegistryActor")
        context.watch(placeRegistryActor)

        val routes = new MainRoutes(placeRegistryActor, forecastRegistryActor)(context.system)
        startHttpServer(routes.mainRoutes)(context.system)

        Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "WeatherForecastServer")
    system.executionContext
  }

  def jsonToPlaceSequence(): Seq[Place] = {
    import spray.json._

    val file = scala.io.Source.fromFile(getClass.getResource("/targets-config.json").getFile()).getLines.mkString
    com.JsonFormats.placesJsonFormat.read(file.parseJson)
  }
}
