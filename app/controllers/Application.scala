package controllers

import play.api._
import play.api.mvc._
import models._
import play.api.libs.json._

class Application extends Controller {

  val game = new Game()
  var players = Seq.empty[Player]

  def board = Action { implicit request =>
    game.start()
    Ok(views.html.board(game,players))
  }

  def mobileStart = Action { implicit request =>
    Ok(views.html.mobilestart())
  }

  def addNewPlayer(username: String) = Action {
    players = players :+ Player(username,1,1)
    Ok(JsArray(players.map(p => Json.obj("name" -> p.name))))
  }

  def getPlayer = Action {
    val json = Json.toJson(players.map(p => Json.obj("name" -> p.name)))
    Ok(json)
  }

  def controller(username: String) = Action { implicit request =>
    Ok(views.html.control(username))
  }

}