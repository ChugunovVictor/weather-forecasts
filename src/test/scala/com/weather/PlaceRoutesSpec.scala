package com.weather

//#forecast-routes-spec
//#test-top

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.MainRoutes
import com.weather.forecast.{Forecast, ForecastRegistry}
import com.weather.place.{Place, PlaceRegistry}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

//#set-up
class PlaceRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
  //#test-top

  // the Akka HTTP route testkit does not yet support a typed actor system (https://github.com/akka/akka-http/issues/2036)
  // so we have to adapt for now
  lazy val testKit = ActorTestKit()

  implicit def typedSystem = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  // Here we need to implement all the abstract members of ForecastRoutes.
  // We use the real ForecastRegistryActor to test it while we hit the Routes,
  // but we could "mock" it by implementing it in-place or by using a TestProbe
  // created with testKit.createTestProbe()
  val forecastRegistry = testKit.spawn(ForecastRegistry())
  val placeRegistry = testKit.spawn(PlaceRegistry())
  lazy val routes = new MainRoutes(placeRegistry, forecastRegistry).mainRoutes

  // use the json formats to marshal and unmarshall objects in the test

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.JsonFormats._
  //#set-up

  //#actual-test
  "PlaceRoutes" should {
    "return no placess if no present (GET /places)" in {
      // note that there's no need for the host part in the uri:
      val request = HttpRequest(uri = "/places")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"forecasts":[]}""")
      }
    }
    //#actual-test

    //#testing-post
    "be able to add forecasts (POST /forecasts)" in {
      val places = Place("Chicago", 33.441792, -94.037689, 1, 0, 999)
      val placesEntity = Marshal(places).to[MessageEntity].futureValue // futureValue is from ScalaFutures

      // using the RequestBuilding DSL:
      val request = Post("/places").withEntity(placesEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and we know what message we're expecting back:
        entityAs[String] should ===("""{"description":"Place 999 created."}""")
      }
    }
    //#testing-post

    "be able to remove forecasts (DELETE /places)" in {
      // forecast the RequestBuilding DSL provided by ScalatestRouteSpec:
      val request = Delete(uri = "/places/999")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"description":"Forecast 999 deleted."}""")
      }
    }
    //#actual-test
  }
  //#actual-test

  //#set-up
}

//#set-up
//#places-routes-spec
