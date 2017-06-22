package state

import akka.NotUsed
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{BroadcastHub, Keep, RunnableGraph, Source, SourceQueueWithComplete}
import models.Player
import play.api.libs.json.{JsArray, JsValue, Json}
import scala.concurrent.duration._

// Class Action
sealed trait Action
case class AddPlayer(player: Player) extends Action
case class DropPlayer(username: String) extends Action
case class AddPoint(username: String, point: Int) extends Action
case class ClearGame() extends Action
case object TickEvent extends Action

// Class State
sealed trait State
case class GameState(players: Seq[Player] = Seq.empty[Player], leaderboard: Map[String, Int] = Map.empty[String, Int]) extends State {
  def toJson: JsValue = Json.obj(
    "players" -> JsArray(players.map(_.toJson)),
    "leaderboard" -> leaderboard
  )
}

class StateGame {

  def reducer[A <: Action](game: GameState, action: A): GameState = action match {
    case AddPlayer(p) => game.copy(players = game.players :+ p, leaderboard = game.leaderboard + (p.name -> 0) )
    case DropPlayer(u) => game.copy(players = game.players.filter(_.name != u), leaderboard = game.leaderboard - u)
    case ClearGame() => game.copy(players = Seq.empty[Player], leaderboard = Map.empty[String, Int])
    case AddPoint(u, p) => game.leaderboard.get(u) match {
      case Some(score) => {
        val newLeaderBoard = game.leaderboard + (u -> (score + p))
        game.copy(leaderboard = newLeaderBoard)
      }
      case None => game
    }
    case _ => game
  }

  private val source: Source[GameState, SourceQueueWithComplete[Action]] = Source.queue[Action](50000, OverflowStrategy.dropTail)
    .fold(GameState()) { (prevState, action) =>
      println(s"action: $action")
      val newState = reducer(prevState, action)
      println(s"next state: $newState")
      newState
  }

  private val eventsAndTicks = source.merge(Source.tick(0.second, 1.second, NotUsed).map(_ => TickEvent))

  private val (queue, runnableGraph) = eventsAndTicks.toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)

  val events: Source[GameState, NotUsed] = runnableGraph.run()

  def push[A <: Action](action: A): Unit = {
    queue.offer(action)
  }
}
