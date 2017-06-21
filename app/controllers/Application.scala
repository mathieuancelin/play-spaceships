package controllers

import play.api._
import play.api.mvc._
import models._
import play.api.libs.json._

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}

class Application extends Controller {

  var game = new Game()

  /*
    AKKA STREAM
   */
  sealed trait Action
  case class addP(player: Player) extends Action
  case class supP(username: String) extends Action
  case class setV(value: Int) extends Action

  sealed trait State
  case class GameState(players: Seq[Player], test: Int) extends State

  var stateGame = GameState(Seq.empty[Player], 20)

  def reducer[A <: Action](game: GameState, action: A): GameState = action match {
    case addP(p) => game.copy(players = game.players :+ p)
    case supP(u) => game.copy(players = game.players.filter(_ != Player(u)))
    case setV(v) => game.copy(test = v)
    case _ => game
  }

  val state = Source.queue[Action](50000, OverflowStrategy.dropTail)
    .fold(stateGame) { (prevState, action) =>
      println(s"action: $action")
      val newState = reducer(prevState, action)
      println(s"next state: $newState")
      stateGame = newState
      newState
    }

  val actorSystem   = ActorSystem("Redux-System")
  val materializer  = ActorMaterializer.create(actorSystem)
  implicit val ec   = actorSystem.dispatcher
  val (queue, done) = state.toMat(Sink.ignore)(Keep.both).run()(materializer)
  done.andThen {
    case _ => println("The End !")
  }
  // END

  def board = Action { implicit request =>
    Ok(views.html.board(game,stateGame.players))
  }

  def mobileStart = Action { implicit request =>
    Ok(views.html.mobilestart())
  }

  def addNewPlayer(username: String) = Action {
    queue.offer(addP(Player(username)))
    Ok(JsArray(stateGame.players.map(p => Json.obj("name" -> p.name))))
  }

  def dropPlayer(username: String) = Action {
    queue.offer(supP(username))
    Ok(JsArray(stateGame.players.map(p => Json.obj("name" -> p.name))))
  }

  def getPlayer = Action {
    val json = Json.toJson(stateGame.players.map(p => Json.obj("name" -> p.name)))
    Ok(json)
  }

  def controller(username: String) = Action { implicit request =>
    // If username exists else redirection
    Ok(views.html.control(username))
  }

}