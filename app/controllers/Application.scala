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

  val game = new StateGame()
  var indexBullet = 0
  var indexShip = 0
  val host = InetAddress.getLocalHost.getHostAddress

  lifecycle.addStopHook(() => {
    game.stop()
    Future.successful(())
  })


  def socket = WebSocket.accept[String, String] { request =>
    Flow[String].map {res =>
      var output = "";
      val data: JsValue = Json.parse(res)
      val action = (data \ "action").as[String]

      action match {
        case "addShip" => {
          println("Add Ship")
          var name = (data \ "name").as[String]
          var color = (data \ "color").as[String]
          var rnd = new scala.util.Random
          output = indexShip.toString()
          game.push(AddShip(new Ship(indexShip, name, new Vector(50+rnd.nextInt((750-50)+1),-(50+rnd.nextInt((550-50)+1))),0,20,color))).flatMap { _ =>
            indexShip += 1
            game.state.map(s => Ok(s.toJson))
          }
        }
        case "addBullet" => {
          println("Add Bullet")
          var id = (data \ "id").as[String]
          output = indexBullet.toString()
          game.push(AddBullet(id.toInt,indexBullet)).flatMap { _ =>
            indexBullet += 1
            game.state.map(s => Ok(s.toJson))
          }
        }
        case "moveShip" => {
          println("Move Ship")
          var id = (data \ "id").as[String]
          var angle = (data \ "angle").as[String]
          game.push(MoveShip(id.toInt, angle.toFloat)).flatMap { _ =>
            game.state.map(s => Ok(s.toJson))
          }
        }
        case "clear" => {
          println("clear")
          game.push(Clear()).flatMap { _ =>
            game.state.map(s => Ok(s.toJson))
          }
        }
        case _ => println("Json data unknown")
      }
      output
    }



    /*
    val in = Sink.foreach[String](res => {
      val data: JsValue = Json.parse(res)
      val action = (data \ "action").as[String]

      action match {
        case "addShip" => {
          println("Add Ship")
          var name = (data \ "name").as[String]
          var color = (data \ "color").as[String]
          var rnd = new scala.util.Random
          game.push(AddShip(new Ship(indexShip, name, new Vector(50+rnd.nextInt((750-50)+1),-(50+rnd.nextInt((550-50)+1))),0,20,color))).flatMap { _ =>
            indexShip += 1
            output = indexShip.toString()
            game.state.map(s => Ok(s.toJson))
          }
        }
        case "addBullet" => {
          println("Add Bullet")
          var id = (data \ "id").as[String]
          game.push(AddBullet(id.toInt,indexBullet)).flatMap { _ =>
            indexBullet += 1
            game.state.map(s => Ok(s.toJson))
          }
        }
        case "moveShip" => {
          println("Move Ship")
          var id = (data \ "id").as[String]
          var angle = (data \ "angle").as[String]
          game.push(MoveShip(id.toInt, angle.toFloat)).flatMap { _ =>
            game.state.map(s => Ok(s.toJson))
          }
        }
        case "clear" => {
          println("clear")
          game.push(Clear()).flatMap { _ =>
            game.state.map(s => Ok(s.toJson))
          }
        }
        case _ => println("Json data unknown")
      }
    })
    val out = Source.single(output).concat(Source.maybe)
    Flow.fromSinkAndSource(in, out)*/
  }




  def board = Action { implicit request =>
    Ok(views.html.board(request.host.split(":")(0)))
  }

  def mobileStart = Action { implicit request =>
    Ok(views.html.mobilestart(request.host.split(":")(0)))
  }

  def addShip(name: String, color: String) = Action.async {
    var rnd = new scala.util.Random
    game.push(AddShip(new Ship(indexShip, name, new Vector(50+rnd.nextInt((750-50)+1),-(50+rnd.nextInt((550-50)+1))),0,20,color))).flatMap { _ =>
      indexShip += 1
      game.state.map(s => Ok(s.toJson))
    }
  }

  def addBullet(id: String) = Action.async {
    game.push(AddBullet(id.toInt,indexBullet)).flatMap { _ =>
      indexBullet += 1
      game.state.map(s => Ok(s.toJson))
    }
  }

  def moveShip(id: String, angle: String) = Action.async {
    game.push(MoveShip(id.toInt, angle.toFloat)).flatMap { _ =>
      game.state.map(s => Ok(s.toJson))
    }
  }

  def clear() = Action.async {
    game.push(Clear()).flatMap { _ =>
      game.state.map(s => Ok(s.toJson))
    }
  }

  def getState() = Action.async {
    game.state.map(s => Ok(s.toJson))
  }

  def controller(id: String) = Action { implicit request =>
    Ok(views.html.control(id.toInt, request.host.split(":")(0)))
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