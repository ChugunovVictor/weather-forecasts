package com

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.weather.forecast.ForecastRegistry._
import com.weather.forecast.{Forecast, ForecastRegistry}
import com.weather.place.PlaceRegistry._
import com.weather.place.{Place, PlaceRegistry}
import com.weather.schedule.{Response, ResponseQueries}

import scala.concurrent.Future

//#import-json-formats
//#place-routes-class
class MainRoutes(placeRegistry: ActorRef[PlaceRegistry.Command], forecastRegistry: ActorRef[ForecastRegistry.Command])(implicit val system: ActorSystem[_]) {

  //#place-routes-class

  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  //#import-json-formats

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("app.routes.ask-timeout"))

  def getPlaces(): Future[Seq[Place]] =
    placeRegistry.ask(GetPlaces)

  def getPlace(id: Long): Future[GetPlaceResponse] =
    placeRegistry.ask(GetPlace(id, _))

  def createPlace(place: Place): Future[ActionPerformed] = {
    placeRegistry.ask(CreatePlace(place, _))
  }

  def deletePlace(id: Long): Future[ActionPerformed] =
    placeRegistry.ask(DeletePlace(id, _))

  def getForecasts(): Future[Seq[Forecast]] =
    forecastRegistry.ask(GetForecasts)

  def getForecast(id: Long): Future[GetForecastResponse] =
    forecastRegistry.ask(GetForecast(id, _))

  def createForecast(result: Forecast): Future[ActionPerformed] = {
    forecastRegistry.ask(CreateForecast(result, _))
  }

  def deleteForecast(id: Long): Future[ActionPerformed] =
    forecastRegistry.ask(DeleteForecast(id, _))

  def getDailyForecast(): Future[Seq[Response]] =
    ResponseQueries.daily

  //#all-routes
  //#places-get-post
  //#places-get-delete
  val mainRoutes: Route = concat(
    pathPrefix("places") {
      concat(
        //#places-get-delete
        pathEnd {
          concat(
            get {
              complete(getPlaces())
            },
            post {
              entity(as[Place]) { place =>
                onSuccess(createPlace(place)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            })
        },
        //#places-get-delete
        //#places-get-post
        path(Segment) { id =>
          concat(
            get {
              //#retrieve-place-info
              rejectEmptyResponse {
                onSuccess(getPlace(id.toLong)) { response =>
                  complete(response.maybePlace)
                }
              }
              //#retrieve-place-info
            },
            delete {
              //#places-delete-logic
              onSuccess(deletePlace(id.toLong)) { performed =>
                complete((StatusCodes.OK, performed))
              }
              //#places-delete-logic
            })
        })
      //#places-get-delete
    },

    pathPrefix("forecasts") {
      concat(
        //#results-get-delete
        pathEnd {
          concat(
            get {
              complete(getForecasts())
            },
            post {
              entity(as[Forecast]) { result =>
                onSuccess(createForecast(result)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            })
        },
        //#results-get-delete
        //#results-get-post
        path(Segment) { id =>
          concat(
            get {
              //#retrieve-result-info
              rejectEmptyResponse {
                onSuccess(getForecast(id.toLong)) { response =>
                  complete(response.maybeForecast)
                }
              }
              //#retrieve-result-info
            },
            delete {
              //#results-delete-logic
              onSuccess(deleteForecast(id.toLong)) { performed =>
                complete((StatusCodes.OK, performed))
              }
              //#results-delete-logic
            })
        })
      //#results-get-delete
    },

    pathPrefix("example") {
      pathEnd {
        get {
          getFromResource("example.json")
        }
      }
    },

    pathPrefix("daily") {
      pathEnd {
        get {
          complete(getDailyForecast())
        }
      }
    }
  )
  //#all-routes
}
