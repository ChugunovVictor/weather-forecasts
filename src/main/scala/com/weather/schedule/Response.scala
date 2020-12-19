package com.weather.schedule

import java.sql.Date

import com.App
import com.weather.forecast.Forecast
import com.weather.place.Place
import slick.driver.SQLiteDriver.api._
import com.weather.schedule.ScheduleActor.system
import system.executionContext

import scala.concurrent.{ExecutionContext, Future}

final case class Response(place: Place,
                          forecasts: Seq[Forecast]) {

  def calcText(): String = {
    checkNextForecast("", 0)
  }

  def checkNextForecast(text: String, n: Int): String = {
    if (n == this.forecasts.size) return text
    val f = this.forecasts(n)
    val commentaryMin = if (this.place.expectedMinTemperature < f.minTemperature)
      s"At ${new Date(f.date.getTime * 1000)} will be colder than expected; " else ""
    val commentaryMax = if (this.place.expectedMaxTemperature > f.maxTemperature)
      s"At ${new Date(f.date.getTime * 1000)} will be hotter than expected; " else ""
    checkNextForecast(text + commentaryMin + commentaryMax, n + 1)
  }
}

object ResponseQueries {
  def daily(): Future[Seq[Response]] = {
    val result = for {
      (place, forecasts) <- App.places join App.forecasts on (_.id === _.placeId)
    } yield (place, forecasts)

    App.db.run(result.result).map(
      _.groupBy(_._1).map(m =>
        Response(
          place = m._1,
          forecasts = m._2.map(_._2)
        )
      ).toSeq
    )
  }
}
