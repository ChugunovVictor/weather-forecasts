package com.weather.forecast

//#result-registry-actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.ActionPerformed
import com.weather.forecast.{Forecast, ForecastsQueries}

object ForecastRegistry {
  // actor protocol
  sealed trait Command
  final case class GetForecasts(replyTo: ActorRef[Seq[Forecast]]) extends Command
  final case class CreateForecast(result: Forecast, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetForecast(id: Long, replyTo: ActorRef[GetForecastResponse]) extends Command
  final case class DeleteForecast(id: Long, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetForecastResponse(maybeForecast: Option[Forecast])

  def apply(): Behavior[Command] = registry()

  private def registry(): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetForecasts(replyTo) =>
        replyTo ! ForecastsQueries.selectAll()
        Behaviors.same
      case CreateForecast(result, replyTo) =>
        replyTo ! ActionPerformed(s"Forecast ${result.id} created.")
        ForecastsQueries.insert(result)
        Behaviors.same
      case GetForecast(id, replyTo) =>
        replyTo ! GetForecastResponse(ForecastsQueries.get(id.toLong))
        Behaviors.same
      case DeleteForecast(id, replyTo) =>
        replyTo ! ActionPerformed(s"Forecast $id deleted.")
        ForecastsQueries.delete(id)
        Behaviors.same
    }
}
//#result-registry-actor
