package controllers

import java.net.InetAddress
import javax.inject._

import play.api.libs.streams.ActorFlow
import akka.actor.ActorSystem
import akka.actor._

import akka.stream.Materializer

import models._
import play.api.inject.ApplicationLifecycle
import play.api.libs.json._
import play.api.mvc._
import state._


// WEBSOCKET
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.Logger
import play.api.libs.ws._
import play.api.libs.concurrent.Promise
import play.api.Play.current
import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.stream.scaladsl._
import scala.util.parsing.json._
import play.api.libs.json._
// ---------

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Application @Inject()(lifecycle: ApplicationLifecycle, ws: WSClient)(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer) extends Controller {

  var gameList: Seq[Game] = Seq.empty[Game]
  var indexGame: Int = 0
  val host = InetAddress.getLocalHost.getHostAddress

  lifecycle.addStopHook(() => {
    for(game <- gameList) {
      game.state.stop()
    }
    Future.successful(())
  })

  def socketGameList() = WebSocket.accept[String, String] { request =>
    Flow[String].map {res =>
      val data: JsValue = Json.parse(res)
      val name = (data \ "name").as[String]
      gameList = gameList :+ new Game(indexGame,name,new StateGame,0,0)
      indexGame = indexGame +1
      (indexGame-1).toString()
    }
  }

  def socketGame(id: String) = WebSocket.accept[String, String] { request =>
    var game = gameList.apply(id.toInt)
    Flow[String].map {res =>
      var output = ""
      val data: JsValue = Json.parse(res)
      val action = (data \ "action").as[String]

      action match {
        case "addShip" => {
          var name = (data \ "name").as[String]
          var color = (data \ "color").as[String]
          var rnd = new scala.util.Random
          output = game.indexShip.toString()
          game.state.push(AddShip(new Ship(game.indexShip, name, new Vector(50+rnd.nextInt((750-50)+1),-(50+rnd.nextInt((550-50)+1))),0,10,color))).flatMap { _ =>
            game.indexShip += 1
            game.state.state.map(s => Ok(s.toJson))
          }
        }
        case "addBullet" => {
          var id = (data \ "id").as[String]
          output = game.indexBullet.toString()
          game.state.push(AddBullet(id.toInt,game.indexBullet)).flatMap { _ =>
            game.indexBullet += 1
            game.state.state.map(s => Ok(s.toJson))
          }
        }
        case "moveShip" => {
          var id = (data \ "id").as[String]
          var angle = (data \ "angle").as[String]
          game.state.push(MoveShip(id.toInt, angle.toFloat)).flatMap { _ =>
            game.state.state.map(s => Ok(s.toJson))
          }
        }
        case "clear" => {
          game.state.push(Clear()).flatMap { _ =>
            game.state.state.map(s => Ok(s.toJson))
          }
        }
        case _ => println("Json data unknown")
      }
      output
    }
  }

  def home = Action { implicit request =>
    val host = if(env.Env.isProd) request.host.split(":")(0) else request.host
    Ok(views.html.home(gameList, host))
  }

  def board(id: String) = Action { implicit request =>
    val host = if(env.Env.isProd) request.host.split(":")(0) else request.host
    Ok(views.html.board(id.toInt, host))
  }

  def mobileStart(id: String) = Action { implicit request =>
    val host = if(env.Env.isProd) request.host.split(":")(0) else request.host
    Ok(views.html.mobilestart(id.toInt, host))
  }

  def getState(id: String) = Action.async {
    gameList.apply(id.toInt).state.state.map(s => Ok(s.toJson))
  }

  def controller(id: String, idShip: String) = Action { implicit request =>
    val host = if(env.Env.isProd) request.host.split(":")(0) else request.host
    Ok(views.html.control(id.toInt, idShip.toInt, host))
  }

  def resultat(id: String, username: String, color: String) = Action { implicit request =>
    val host = if(env.Env.isProd) request.host.split(":")(0) else request.host
    Ok(views.html.resultat(id.toInt, username, color, host))
  }

  def source(id: String) = Action {
    Ok.chunked(
      gameList.apply(id.toInt).state.stream.map(_.toJson).map(e => Json.stringify(e)).map(data => s"data: $data\n\n")
    ).as("text/event-stream")
  }
}