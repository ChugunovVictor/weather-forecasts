package com

import java.sql.Date

import com.typesafe.config.ConfigFactory
import com.weather.forecast.{Forecast, ForecastShort, ForecastsTable}
import com.weather.place.{Place, PlacesTable}
import com.weather.schedule.Response
import slick.jdbc.JdbcBackend.Database
import slick.lifted.TableQuery
import spray.json.JsValue

import scala.collection.IterableOnce.iterableOnceExtensionMethods

//#json-formats
import spray.json.DefaultJsonProtocol._
import spray.json._

object JsonFormats {

  implicit object placeJsonFormat extends RootJsonFormat[Place] {
    def write(c: Place) = JsObject(
      "id" -> JsNumber(c.id),
      "title" -> JsString(c.title),
      "latitude" -> JsNumber(c.latitude),
      "longitude" -> JsNumber(c.longitude),
      "expectedMinTemperature" -> JsNumber(c.expectedMinTemperature),
      "expectedMaxTemperature" -> JsNumber(c.expectedMaxTemperature)
    )

    def read(value: JsValue) = {
      value.asJsObject.getFields("title", "latitude", "longitude", "expectedMinTemperature", "expectedMaxTemperature", "id") match {
        case Seq(JsString(title), JsNumber(latitude), JsNumber(longitude), JsNumber(expectedMinTemperature), JsNumber(expectedMaxTemperature), JsNumber(id)) =>
          new Place(title, latitude.doubleValue, longitude.doubleValue, expectedMinTemperature.floatValue, expectedMaxTemperature.floatValue, id.longValue)
        case Seq(JsString(title), JsNumber(latitude), JsNumber(longitude), JsNumber(expectedMinTemperature), JsNumber(expectedMaxTemperature)) =>
          new Place(title, latitude.doubleValue, longitude.doubleValue, expectedMinTemperature.floatValue, expectedMaxTemperature.floatValue)
        case _ => throw new DeserializationException("Forecast expected")
      }
    }
  }

  implicit val placesJsonFormat: RootJsonFormat[Seq[Place]] = new RootJsonFormat[Seq[Place]] {
    def write(p: Seq[Place]) = JsObject(
      "places" -> JsArray(p.map(_.toJson).toVector)
    )

    def read(value: JsValue) = {
      import spray.json._

      val places = value.asJsObject.getFields("targets").head match {
        case JsArray(targets) => targets.map(_.asJsObject.getFields("title", "latitude", "longitude",
          "expectedMaxTemperature", "expectedMinTemperature") match {
          case Seq(JsString(title), JsNumber(latitude), JsNumber(longitude), JsNumber(expectedMinTemperature), JsNumber(expectedMaxTemperature)) =>
              Some(Place(title, latitude.doubleValue, longitude.doubleValue, expectedMinTemperature.floatValue, expectedMaxTemperature.floatValue))
          case _ => None;
        })
        case _ => None;
      };
      places.toSeq.filterNot(_.isEmpty).map(_.get)
    }
  }

  implicit object forecastJsonFormat extends RootJsonFormat[Forecast] {
    def write(c: Forecast) = JsObject(
      "placeId" -> JsNumber(c.placeId),
      "date" -> JsNumber(c.date.getTime),
      "minTemperature" -> JsNumber(c.minTemperature),
      "maxTemperature" -> JsNumber(c.maxTemperature),
      "id" -> JsNumber(c.id)
    )

    def read(value: JsValue) = {
      value.asJsObject.getFields("placeId", "date", "minTemperature", "maxTemperature", "id") match {
        case Seq(JsNumber(placeId), JsNumber(date), JsNumber(minTemperature), JsNumber(maxTemperature), JsNumber(id)) =>
          new Forecast(new Date(date.longValue), minTemperature.floatValue, maxTemperature.floatValue, placeId.longValue, id.longValue)
        case Seq(JsNumber(placeId), JsNumber(date), JsNumber(minTemperature), JsNumber(maxTemperature)) =>
          new Forecast(new Date(date.longValue), minTemperature.floatValue, maxTemperature.floatValue, placeId.longValue)
        case Seq(JsNumber(date), JsNumber(minTemperature), JsNumber(maxTemperature)) =>
          new Forecast(new Date(date.longValue), minTemperature.floatValue, maxTemperature.floatValue)
        case _ => throw new DeserializationException("Forecast expected")
      }
    }
  }

  implicit object forecastsJsonFormat extends RootJsonFormat[Seq[Forecast]] {
    def write(p: Seq[Forecast]) = JsObject(
      "forecasts" -> JsArray(p.map(_.toJson).toVector)
    )

    def read(value: JsValue) = {
      import spray.json._

      val forecasts = value.asJsObject.getFields("daily").head match {
        case JsArray(daily) => daily.map(_.asJsObject.getFields("dt", "temp") match {
          case Seq(JsNumber(dt), JsObject(temp)) => {
            if (!(temp.get("min").isEmpty && temp.get("max").isEmpty))
              Some(Forecast(new Date(dt.longValue), temp.get("min").get.convertTo[Float], temp.get("max").get.convertTo[Float]))
            else
              None;
          }
          case _ => None;

        })
        case _ => None;
      };
      forecasts.toSeq.filterNot(_.isEmpty).map(_.get)
    }
  }
  
  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)


  implicit val forecastShortWriter = new JsonWriter[ForecastShort] {
    def write(c: ForecastShort) = JsObject(
      "date" -> JsString(new Date(c.date.getTime * 1000).toString),
      "minTemperature" -> JsNumber(c.minTemperature),
      "maxTemperature" -> JsNumber(c.maxTemperature),
    )
  }

  implicit val forecastsShortWriter = new JsonWriter[Seq[ForecastShort]] {
    def write(p: Seq[ForecastShort]) = JsObject(
      "forecasts" -> JsArray(p.map(_.toJson(forecastShortWriter)).toVector)
    )
  }

  implicit object responseJsonFormat extends RootJsonFormat[Response] {
    def write(r: Response) = JsObject(
      "place" -> JsString(r.place.title),
      "forecasts" -> r.forecasts.map(ForecastShort.from).toJson(forecastsShortWriter),
      "comment" -> JsString(r.calcText())
    )

    def read(value: JsValue) = ???
  }

  implicit val responsesJsonFormat: RootJsonFormat[Seq[Response]] = new RootJsonFormat[Seq[Response]] {
    def write(r: Seq[Response]) = JsObject(
      "places" -> JsArray(r.map(_.toJson).toVector)
    )

    def read(value: JsValue) = ???
  }
}

object App {
  var config = ConfigFactory.load("application.conf")
  var places = TableQuery[PlacesTable]
  var forecasts = TableQuery[ForecastsTable]
  var db: Database = Database.forConfig("jdbc", config)
}

final case class ActionPerformed(description: String)


//#json-formats
