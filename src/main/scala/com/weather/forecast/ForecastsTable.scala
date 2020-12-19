package com.weather.forecast

import java.sql.Date

import com.App
import App.places
import akka.Done
import slick.driver.SQLiteDriver.api._
import slick.jdbc.meta.MTable
import slick.lifted.ProvenShape

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationInt}

final case class Forecast(date: Date,
                        minTemperature: Float,
                        maxTemperature: Float,
                        placeId: Long = 0,
                        id: Long = 0)

case class ForecastShort(date: Date,
                         minTemperature: Float,
                         maxTemperature: Float)

object ForecastShort {
  def from(f: Forecast): ForecastShort = {
    ForecastShort(
      date = f.date,
      minTemperature = f.minTemperature,
      maxTemperature = f.maxTemperature
    )
  }
}

class ForecastsTable(tag: Tag) extends Table[Forecast](tag, "Forecasts") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def placeId = column[Long]("placeId")
  def minTemperature = column[Float]("minTemperature")
  def maxTemperature = column[Float]("maxTemperature")
  def date = column[Date]("date")

  def * : ProvenShape[Forecast] = (date, minTemperature, maxTemperature, placeId, id) <> ((Forecast.apply _).tupled, Forecast.unapply)
  def forecast_fk = foreignKey("placeId", placeId, places)(_.id)
}

object ForecastsQueries{

  def setup()(implicit ec : ExecutionContext): Future[Any] = {
    val tables = Await.result(App.db.run(MTable.getTables), 1.seconds).toList
    tables.find(p => p.name.name.equals("Forecasts")) match {
      case Some(value) => Future.successful(Done) // Forecasts table already exists
      case None => App.db.run(DBIO.seq((App.forecasts.schema).create))
    }
  }

  def selectAll(): Seq[Forecast] = {
    val q1 = for (m <- App.forecasts) yield m
    Await.result(App.db.run(q1.result), Duration.Inf)
  }

  def insert(forecast: Forecast): Unit = {
    val insert = DBIO.seq(App.forecasts += forecast)
    App.db.run(insert)
  }

  def insertAll(forecasts: Seq[Forecast]): Unit = {
    val insert = DBIO.seq(App.forecasts ++= forecasts)
    App.db.run(insert)
  }

  def get(id: Long): Option[Forecast] = {
    Await.result(App.db.run(App.forecasts.filter(_.id === id).result.headOption), Duration.Inf)
  }

  def delete(id: Long): Unit = {
    App.db.run(App.forecasts.filter(_.id === id).delete)
  }

  def clearObsolete(current: Date, placeId: Long): Unit = {
    Await.result(App.db.run(App.forecasts.filter(_.placeId === placeId).filter(_.date <= current).delete), Duration.Inf)
  }
}
