package controllers

import java.net.InetAddress
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
  var index = 0
  val host = InetAddress.getLocalHost.getHostAddress


  def board = Action { implicit request =>
    Ok(views.html.board(request.host.split(":")(0)))
  }

  def mobileStart = Action { implicit request =>
    Ok(views.html.mobilestart(request.host.split(":")(0)))
  }

  def addNewPlayer(username: String, color: String) = Action.async {
    var rnd = new scala.util.Random
    game.push(AddPlayer(Player(username, Vector(50+rnd.nextInt((750-50)+1),-(50+rnd.nextInt((550-50)+1))), 0, 0, "#"+color,3))).flatMap { _ =>
      game.state.map(s => Ok(s.toJson))
    }
  }

  def dropPlayer(username: String) = Action.async {
    game.push(DropPlayer(username)).flatMap { _ =>
      game.state.map(s => Ok(s.toJson))
    }
  }

  def addPoint(username: String, point: String) = Action.async {
    game.push(AddPoint(username, point.toInt)).flatMap { _ =>
      game.state.map(s => Ok(s.toJson))
    }
  }

  def lostLife(username: String, point: String) = Action.async {
    game.push(LostLife(username, point.toInt)).flatMap { _ =>
      game.state.map(s => Ok(s.toJson))
    }
  }

  def addBullet(x: String, y: String, angle: String, nameShip: String) = Action.async {
    game.push(AddBullet(Bullet(index,Vector(x.toFloat,y.toFloat),angle.toFloat,nameShip))).flatMap { _ =>
      index = index + 1;
      game.state.map(s => Ok(s.toJson))
    }
  }

  def dropBullet(id: String) = Action.async {
    game.push(DropBullet(id.toInt)).flatMap { _ =>
      game.state.map(s => Ok(s.toJson))
    }
  }

  def movePlayer(username: String, x: String, y: String, a: String) = Action.async {
    game.push(MovePlayer(username, Vector(x.toFloat, y.toFloat),a.toFloat)).flatMap { _ =>
      game.state.map(s => Ok(s.toJson))
    }
  }

  def clearGame() = Action.async {
    game.push(ClearGame()).flatMap { _ =>
      game.state.map(s => Ok(s.toJson))
    }
  }

  def getState() = Action.async {
    game.state.map(s => Ok(s.toJson))
  }

  def controller(username: String) = Action { implicit request =>
    Ok(views.html.control(username, request.host.split(":")(0)))
  }

  def resultat(username: String, color: String) = Action { implicit request =>
    Ok(views.html.resultat(username,color, request.host.split(":")(0)))
  }

  def source = Action {
    Ok.chunked(
      game.stream.map(_.toJson).map(e => Json.stringify(e)).map(data => s"data: $data\n\n")
    ).as("text/event-stream")
  }
}