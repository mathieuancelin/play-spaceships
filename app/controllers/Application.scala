package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import models._
import play.api.libs.json._
import state._

@Singleton
class Application @Inject()() extends Controller {

  val stateGame = new StateGame()

  def board = Action { implicit request =>
    Ok(views.html.board(stateGame.state.players))
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

  def getLeaderboard = Action {
    //val json = Json.toJson(stateGame.state.leaderboard.map(p => Json.obj("key" -> p)))
    val json = Json.obj("Leaderboard" -> stateGame.state.leaderboard.toSeq.sortBy(-_._2).toMap)
    Ok(json)
  }

  def controller(username: String) = Action { implicit request =>
    // If username exists else redirection
    Ok(views.html.control(username))
  }

  def source = Action {
    Ok.chunked(stateGame.events.map(_.toJson).map(e => Json.stringify(e)).map(data => s"data: $data\n\n")).as("text/event-stream")
  }

}