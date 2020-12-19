package com.provider

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.App
import com.JsonFormats.forecastsJsonFormat
import com.weather.forecast.Forecast
import com.weather.place.Place
import com.weather.schedule.ScheduleActor.{system}
import spray.json._

import scala.concurrent.{Future}
import system.executionContext

object OpenWeatherMap extends Provider {
  override def check(place: Place): Future[Seq[Forecast]] = {
    val template = App.config.getString("openweathermap.url").replace("${appid}", App.config.getString("openweathermap.appid"))

    val filledTemplate = template.replace("${latitude}", place.latitude.toString).replace("${longitude}", place.longitude.toString)
    for {
      response <- Http().singleRequest(HttpRequest(uri = filledTemplate))
      forecasts <- responseAsForecastSequence(response.entity.httpEntity).map(_.map(_.copy(placeId = place.id)))
    } yield forecasts
  }

  def responseAsForecastSequence(entity: HttpEntity): Future[scala.Seq[Forecast]] = {
    // Unmarshal(entity).to[scala.Seq[Result]]
    responseAsString(entity).map(_.parseJson).map(forecastsJsonFormat.read)
  }

  def responseAsString(entity: HttpEntity): Future[String] = Unmarshal(entity).to[String]

}
