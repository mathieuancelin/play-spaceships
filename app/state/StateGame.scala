package state

import java.util.concurrent.atomic.AtomicReference

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{BroadcastHub, Keep, RunnableGraph, Source, SourceQueueWithComplete}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import models.Player
import play.api.Logger
import play.api.libs.json.{JsArray, JsValue, Json}

import scala.concurrent.Future
import scala.concurrent.duration._

// Class Action
sealed trait Action
case class AddPlayer(player: Player) extends Action
case class DropPlayer(username: String) extends Action
case class AddPoint(username: String, point: Int) extends Action
case class MovePlayer(username: String, x: Int, y: Int) extends Action
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

class StateGame() {

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer.create(actorSystem)
  implicit val ec = actorSystem.dispatcher

  private val initialState = GameState()
  private val ref = new AtomicReference[GameState](initialState)

  def reducer[A <: Action](game: GameState, action: A): GameState = action match {
    case AddPlayer(p) => game.copy(players = game.players :+ p, leaderboard = game.leaderboard + (p.name -> 0) )
    case DropPlayer(u) => game.copy(players = game.players.filter(_.name != u), leaderboard = game.leaderboard - u)
    case AddPoint(u, p) => game.leaderboard.get(u) match {
      case Some(score) => {
        val newLeaderBoard = game.leaderboard + (u -> (score + p))
        game.copy(leaderboard = newLeaderBoard)
      }
      case None => game
    }
    case MovePlayer(u,x,y) => {
      
      /*
      * METTRE A JOUR LE PLAYER PLUTOT QUE DELETE ET RECREATE
      * */
      game.players
        .filter(_.name == u)
        .headOption
        .map(player =>
          game.copy(players = game.players.filter(_.name != u) :+
            new Player(player.name,player.posX+x,player.posY+y))
        )
        .getOrElse(game);
/*      if(!game.players.filter(_.name == u).isEmpty) {
        game.copy(players = game.players.filter(_.name != u) :+
          new Player(player.apply(0).name,player.apply(0).posX+x,player.apply(0).posY+y))
      } else {
        game
      }*/
    }
    case ClearGame() => game.copy(players = Seq.empty[Player], leaderboard = Map.empty[String, Int])
    case _ => game
  }

  val source: Source[Action, SourceQueueWithComplete[Action]] = Source.queue[Action](50000, OverflowStrategy.dropTail)
  val eventsAndTicks: Source[GameState, SourceQueueWithComplete[Action]] = source.merge(Source.tick(0.second, 1.second, NotUsed).map(_ => TickEvent)).scan(initialState) { (prevState, action) =>
    val newState = reducer(prevState, action)
    ref.set(newState)
    //Logger.info(s"action: $action => next state: $newState")
    newState
  }

  val runnableGraph: RunnableGraph[(SourceQueueWithComplete[Action], Source[GameState, NotUsed])] =
    eventsAndTicks.toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)

  val (queue: SourceQueueWithComplete[Action], stateStream: Source[GameState, NotUsed]) = runnableGraph.run()

  def stream: Source[GameState, NotUsed] = stateStream

  def push[A <: Action](action: A): Future[Unit] = queue.offer(action).map(_ => ())

  def state: Future[GameState] = Future.successful(ref.get())
}
