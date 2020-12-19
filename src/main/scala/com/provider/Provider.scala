package com.provider

import com.weather.forecast.Forecast
import com.weather.place.Place

import scala.concurrent.{Future}

trait Provider {
  def check(place: Place): Future[Seq[Forecast]];
}
