package com.weather.place

import akka.Done
import com.App
import slick.driver.SQLiteDriver.api._
import slick.jdbc.meta.MTable
import slick.lifted.ProvenShape

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.Success

final case class Place(title: String,
                       latitude: Double,
                       longitude: Double,
                       expectedMinTemperature: Float,
                       expectedMaxTemperature: Float,
                       id: Long = 0)

class PlacesTable(tag: Tag) extends Table[Place](tag, "Places") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def title = column[String]("title")

  def latitude = column[Double]("latitude")

  def longitude = column[Double]("longitude")

  def expectedMinTemperature = column[Float]("expectedMinTemperature")

  def expectedMaxTemperature = column[Float]("expectedMaxTemperature")

  def * : ProvenShape[Place] = (title, latitude, longitude, expectedMinTemperature, expectedMaxTemperature, id) <>
    ((Place.apply _).tupled, Place.unapply)
}

object PlacesQueries {

  def setup()(implicit ec : ExecutionContext): Future[Any] = {
    val tables = Await.result(App.db.run(MTable.getTables), 1.seconds).toList
    tables.find(p => p.name.name.equals("Places")) match {
      case Some(value) => Future.successful(Done) // Places table already exists
      case None => App.db.run(DBIO.seq((App.places.schema).create)).andThen {
        case Success(_) => Future.successful(Done)
      }
    }
  }

  def selectAll(): Seq[Place] = {
    val q1 = for (m <- App.places) yield m
    Await.result(App.db.run(q1.result), Duration.Inf) // TODO check out how akka works with futures properly
  }

  def insert(place: Place): Unit = {
    val insert = DBIO.seq(App.places += place)
    App.db.run(insert)
  }

  def insertAll(places: Seq[Place]): Unit = {
    val insert = DBIO.seq(App.places ++= places)
    App.db.run(insert)
  }

  def upsert(place: Place): Future[Any] = {
    val existed = getByTitle(place.title)
    val dbio = if (!existed.isEmpty)
      DBIO.seq(App.places.update(place.copy(id = existed.get.id)))
    else
      DBIO.seq(App.places += place)
    App.db.run(dbio)

    // App.db.run(App.places.insertOrUpdate(place))
  }

  def get(id: Long): Option[Place] = {
    Await.result(App.db.run(App.places.filter(_.id === id).result.headOption), Duration.Inf)
  }

  def getByTitle(title: String): Option[Place] = {
    Await.result(App.db.run(App.places.filter(_.title === title).result.headOption), Duration.Inf)
  }

  def delete(id: Long): Unit = {
    App.db.run(App.places.filter(_.id === id).delete)
  }


  // try {} finally db.close
}
