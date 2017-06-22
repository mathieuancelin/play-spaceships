package controllers

import javax.inject._

import akka.stream.Materializer
import models._
import play.api.libs.json._
import play.api.mvc._
import state._

@Singleton
class Application @Inject()()(implicit materializer: Materializer) extends Controller {

  val stateGame = new StateGame()

  def board = Action { implicit request =>
    Ok(views.html.board(Seq.empty[Player]))
  }

  def mobileStart = Action { implicit request =>
    Ok(views.html.mobilestart())
  }

  def addNewPlayer(username: String) = Action {
    stateGame.push(AddPlayer(Player(username,1,1)))
    Ok("")
  }

  def dropPlayer(username: String) = Action {
    stateGame.push(DropPlayer(username))
    Ok("")
  }

  def clearGame() = Action {
    stateGame.push(ClearGame())
    Ok("")
  }

  def controller(username: String) = Action { implicit request =>
    // If username exists else redirection
    Ok(views.html.control(username))
  }

  def source = Action {
    Ok.chunked(
      stateGame.stream.map(_.toJson).map(e => Json.stringify(e)).map(data => s"data: $data\n\n")
    ).as("text/event-stream")
  }

}