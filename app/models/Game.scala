package models

import play.api.libs.json.{JsValue, Json}

case class Vector(
  x: Float,
  y: Float
)

case class Bullet(
  pos: Vector,
  angle: Float
) {
  def toJson: JsValue = Json.obj(
    "posX" -> pos.x,
    "posY" -> pos.y,
    "angle" -> angle
  )
}

case class Player(
  name: String,
  pos: Vector,
  angle: Float,
  score: Int,
  color: String
) {
  def toJson: JsValue = Json.obj(
    "name" -> name,
    "posX" -> pos.x,
    "posY" -> pos.y,
    "angle" -> angle,
    "score" -> score,
    "color" -> color
  )
}