package models

import play.api.libs.json.{JsArray, JsValue, Json}
import state._

class Game(var id: Int,
           var name: String,
           var state: StateGame,
           var indexShip: Int,
           var indexBullet: Int) {

}

class Vector(var x:Float, var y:Float) {
  def +(that: Vector) = new Vector(this.x + that.x, this.y + that.y)
  def +(that: Int) = new Vector(this.x + that, this.y + that)
  def +(that: Long) = new Vector(this.x + that, this.y + that)
  def +(that: Float) = new Vector(this.x + that, this.y + that)
  def *(that: Int) = new Vector(this.x * that, this.y * that)
  def *(that: Float) = new Vector(this.x * that, this.y * that)
  def unary_- = new Vector(-this.x,-this.y)
  def magnitude() = Math.abs(Math.sqrt(this.x * this.x + this.y * this.y)).toFloat
  def normalized() = new Vector(this.x/this.magnitude(), this.y/this.magnitude())
  override def toString: String = x.toString + "," + y.toString()
}

class Ship (
  var id: Int,
  var name: String,
  var position: Vector,
  var angle: Float,
  var speed: Float,
  var color: String,
  var velocity: Vector = new Vector(0f,0f),
  var angularVelocity: Float = 0f,
  final var drag: Float = 1.0f,
  var angularDrag: Float = 0.1f,
  var life: Int = 3,
  var score: Int = 0
  ) {
  def cloned(): Ship = return new Ship(this.id,this.name,new Vector(this.position.x,this.position.y),
  this.angle,this.speed,this.color,new Vector(this.velocity.x,this.velocity.y),this.angularVelocity,
  this.drag,this.angularDrag,this.life,this.score)

  def toJson(): JsValue = Json.obj(
    "id" -> this.id,
    "name" -> this.name,
    "posX" -> this.position.x,
    "posY" -> this.position.y,
    "angle" -> this.angle,
    "speed" -> this.speed,
    "color" -> this.color,
    "velX" -> this.velocity.x,
    "velY" -> this.velocity.y,
    "angularVelocity" -> this.angularVelocity,
    "drag" -> this.drag,
    "angularDrag" -> this.angularDrag,
    "life" -> this.life,
    "score" -> this.score
  )

  def simulate(deltaTime: Float): Unit = {
    var pos = this.position + (this.velocity * this.speed * deltaTime)
    if(pos.x < 0) {
      pos.x = 800 - (0 - pos.x)
    } else if(pos.x > 800) {
      pos.x = 0 - (800 - pos.x)
    }
    if(pos.y > 0) {
      pos.y = -600 - (0 - pos.y)
    } else if(pos.y < -600) {
      pos.y = 0 - (-600 - pos.y)
    }
    this.position = pos

    this.velocity = this.velocity + this.getDragForce(this.velocity, this.drag) * deltaTime
    if(this.velocity.x < 0.1f && this.velocity.x > -0.1f) {
      this.velocity.x = 0f
    }
    if(this.velocity.y < 0.1f && this.velocity.y > -0.1f) {
      this.velocity.y = 0f
    }
    //println(this.velocity)
    //this.angle = this.angle + this.angularVelocity * deltaTime
    //this.angularVelocity = this.angularVelocity + this.getAngularDragForce(this.angularVelocity, this.angularDrag) * deltaTime
  }

  def getDragForce(velocity: Vector, drag: Float): Vector = {
    //var velMag = velocity.magnitude()
    //println(-velocity,drag,velMag,-velocity * drag * velMag)
    return -velocity * drag
  }

  def getAngularDragForce(angularVelocity: Float, angularDrag: Float): Float = {
    return -angularVelocity * Math.abs(angularVelocity) * angularDrag
  }

  def push(value: Vector): Unit = {
    this.velocity = this.velocity + value
  }

  def turnToward(newAngle: Float): Unit = {
    this.angle = (newAngle*180/Math.PI).toFloat
    //this.angularVelocity = this.angularVelocity + newAngle - this.angle
  }

  def move(newAngle: Float): Unit = {
    this.push(new Vector(Math.cos(newAngle).toFloat, Math.sin(newAngle).toFloat))
    this.turnToward(newAngle)
  }

  def teleport(newPosition : Vector): Unit = {
    this.position = newPosition
  }

}

class Bullet (
  var id: Int,
  var position: Vector,
  var angle: Float,
  var speed: Float,
  var idShip: Int
  ) {
  def cloned(): Bullet = return new Bullet(this.id,new Vector(this.position.x,this.position.y),
  this.angle,this.speed,this.idShip)

  def toJson(): JsValue = Json.obj(
    "id" -> this.id,
    "posX" -> this.position.x,
    "posY" -> this.position.y,
    "angle" -> this.angle,
    "speed" -> this.speed,
    "idShip" -> this.idShip
  )

  def simulate(deltaTime: Float): Unit = {
    this.position = this.position + (new Vector(Math.cos(this.angle*(Math.PI/180)).toFloat, Math.sin(this.angle*(Math.PI/180)).toFloat) * this.speed * deltaTime)
  }
}