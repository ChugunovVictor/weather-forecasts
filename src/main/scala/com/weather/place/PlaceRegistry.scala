package com.weather.place

import com.ActionPerformed

//#place-registry-actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object PlaceRegistry {

  // actor protocol
  sealed trait Command

  final case class GetPlaces(replyTo: ActorRef[Seq[Place]]) extends Command

  final case class CreatePlace(place: Place, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetPlace(id: Long, replyTo: ActorRef[GetPlaceResponse]) extends Command

  final case class DeletePlace(id: Long, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetPlaceResponse(maybePlace: Option[Place])

  def apply(): Behavior[Command] = registry()

  private def registry(): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetPlaces(replyTo) =>
        replyTo ! PlacesQueries.selectAll()
        Behaviors.same
      case CreatePlace(place, replyTo) =>
        replyTo ! ActionPerformed(s"Place ${place.title} created.")
        PlacesQueries.insert(place)
        Behaviors.same
      case GetPlace(id, replyTo) =>
        replyTo ! GetPlaceResponse(PlacesQueries.get(id.toLong))
        Behaviors.same
      case DeletePlace(id, replyTo) =>
        replyTo ! ActionPerformed(s"Place $id deleted.")
        PlacesQueries.delete(id)
        Behaviors.same
    }
}

//#place-registry-actor
