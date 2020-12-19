package com.weather.schedule

import akka.actor.Actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.App
import com.provider.{OpenWeatherMap, Provider}
import com.weather.forecast.ForecastsQueries
import com.weather.place.PlacesQueries

import scala.concurrent.duration.{DurationDouble, FiniteDuration}

class ScheduleActor extends Actor {

  import ScheduleActor._
  import system.executionContext

  def receive = {
    case Tick => {
      val p = provider()
      PlacesQueries.selectAll().foreach {
        place =>
          for {
            forecasts <- p.check(place)
          } yield {
            ForecastsQueries.clearObsolete(forecasts.maxBy(_.date).date, place.id)
            ForecastsQueries.insertAll(forecasts)
          }
      }
    }
  }
}

object ScheduleActor {
  val Tick = "tick"
  implicit val system = ActorSystem(Behaviors.empty, "SchedulerRequest")
  implicit val executionContext = system.executionContext

  def duration(): FiniteDuration = {
    val value = App.config.getDouble("frequency.value")
    val units = App.config.getString("frequency.units")

    units match {
      case "second" => value.second
      case "minute" => value.minute
      case "hour" => value.hour
      case "day" => value.day
      case _ => value.day
    }
  }

  def provider(): Provider = {
    App.config.getString("provider") match {
      // only one provider so far
      case _ => OpenWeatherMap
    }
  }
}
