package controllers

import javax.inject._

import akka.stream.Materializer
import models._
import play.api.libs.json._
import play.api.mvc._
import state._

import scala.concurrent.ExecutionContext

@Singleton
class Application @Inject()()(implicit ec: ExecutionContext) extends Controller {

  val game = new StateGame()

  def board = Action { implicit request =>
    Ok(views.html.board(Seq.empty[Player]))
  }

  def mobileStart = Action { implicit request =>
    Ok(views.html.mobilestart())
  }

  def addNewPlayer(username: String) = Action.async {
    game.push(AddPlayer(Player(username, 1, 1))).flatMap { _ =>
      game.state.map(s => Ok(s.toJson))
    }
  }

  def dropPlayer(username: String) = Action.async {
    game.push(DropPlayer(username)).flatMap { _ =>
      game.state.map(s => Ok(s.toJson))
    }
  }

  def addPoint(username: String, point: String) = Action.async {
    println(point.toInt)
    game.push(AddPoint(username, point.toInt)).flatMap { _ =>
      game.state.map(s => Ok(s.toJson))
    }
  }

  def movePlayer(username: String, x: String, y: String) = Action.async {
    game.push(MovePlayer(username, x.toInt, y.toInt)).flatMap { _ =>
      game.state.map(s => Ok(s.toJson))
    }
  }

  def clearGame() = Action.async {
    game.push(ClearGame()).flatMap { _ =>
      game.state.map(s => Ok(s.toJson))
    }
  }

  def controller(username: String) = Action { implicit request =>
    Ok(views.html.control(username))
  }

  def source = Action {
    Ok.chunked(
      game.stream.map(_.toJson).map(e => Json.stringify(e)).map(data => s"data: $data\n\n")
    ).as("text/event-stream")
  }

}