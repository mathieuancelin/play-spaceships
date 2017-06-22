package controllers

import play.api._
import play.api.mvc._
import models._
import play.api.libs.json._
import state._

class Application extends Controller {

  val stateGame = new StateGame()
  val queue = stateGame.queue

  def board = Action { implicit request =>
    Ok(views.html.board(stateGame.state.players))
  }

  def mobileStart = Action { implicit request =>
    Ok(views.html.mobilestart())
  }

  def addNewPlayer(username: String) = Action {
    queue.offer(addPlayer(Player(username,1,1)))
    Ok(JsArray(stateGame.state.players.map(p => Json.obj("name" -> p.name))))
  }

  def dropPlayer(username: String) = Action {
    queue.offer(dropPlayer(username))
    Ok(JsArray(stateGame.state.players.map(p => Json.obj("name" -> p.name))))
  }

  def clearGame() = Action {
    queue.offer(clearGame())
    Ok(JsArray(stateGame.state.players.map(p => Json.obj("name" -> p.name))))
  }

  def getPlayer = Action {
    val json = Json.toJson(stateGame.state.players.map(p => Json.obj("name" -> p.name)))
    Ok(json)
  }

  def getLeaderboard = Action {
    //val json = Json.toJson(stateGame.state.leaderboard.map(p => Json.obj("key" -> p)))
    val json = Json.obj("Leaderboard" -> stateGame.state.leaderboard.toSeq.sortBy(-_._2).toMap)
    Ok(json)
  }

  def controller(username: String) = Action { implicit request =>
    // If username exists else redirection
    Ok(views.html.control(username))
  }

}