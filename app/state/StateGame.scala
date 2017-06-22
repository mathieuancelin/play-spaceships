package state

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}
import models.Player

/**
  * Created by dsagnier on 21/06/17.
  */

// Class Action
sealed trait Action
case class addPlayer(player: Player) extends Action
case class dropPlayer(username: String) extends Action
case class addPoint(username: String, point: Int) extends Action
case class clearGame() extends Action

// Class State
sealed trait State
case class GameState(players: Seq[Player], leaderboard: scala.collection.mutable.Map[String, Int]) extends State


class StateGame {
  // Init
  private var _state = GameState(Seq.empty[Player], scala.collection.mutable.Map[String, Int]())
  private val src = Source.queue[Action](50000, OverflowStrategy.dropTail)
    .fold(_state) { (prevState, action) =>
      println(s"action: $action")
      val newState = reducer(prevState, action)
      println(s"next state: $newState")
      _state = newState
      newState
  }
  private val actorSystem   = ActorSystem("Redux-System")
  private val materializer  = ActorMaterializer.create(actorSystem)
  private implicit val ec   = actorSystem.dispatcher
  private val (_queue, _done) = src.toMat(Sink.ignore)(Keep.both).run()(materializer)
  _done.andThen {
    case _ => println("The End !")
  }

  // Function
  def reducer[A <: Action](game: GameState, action: A): GameState = action match {
    case addPlayer(p) => game.copy(players = game.players :+ p, leaderboard = game.leaderboard + (p.name -> 0) )
    case dropPlayer(u) => game.copy(players = game.players.filter(_.name != u), leaderboard = game.leaderboard - u)
    case clearGame() => game.copy(players = Seq.empty[Player], leaderboard = scala.collection.mutable.Map[String, Int]())
    case addPoint(u,p) => game/*.copy(leaderboard = game.leaderboard + (u -> game.leaderboard.get(u) + p))*/
    case _ => game
  }

  // Getter
  def queue = _queue
  def state = _state

}
