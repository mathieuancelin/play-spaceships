package controllers

import play.api._
import play.api.mvc._
import models._
import scala.collection.mutable.ListBuffer

class Application extends Controller {

  val game = new Game()
  var players = new ListBuffer[Player]

  def board = Action { implicit request =>
    game.start()
    Ok(views.html.board(game))
  }

  def controller = Action { implicit request =>
    players+=new Player("test",1,1)
    players+=new Player("test2",1,1)
    Ok(views.html.control(players.toList))
  }

}