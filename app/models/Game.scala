package models

import play.api.libs.json.{JsValue, Json}

case class Vector(
  x: Float,
  y: Float
)

case class Bullet(
  id: Int,
  pos: Vector,
  angle: Float,
  nameShip: String
) {
  def toJson: JsValue = Json.obj(
    "id" -> id,
    "posX" -> pos.x,
    "posY" -> pos.y,
    "angle" -> angle,
    "nameShip" -> nameShip
  )
}

case class Player(
  name: String,
  pos: Vector,
  angle: Float,
  score: Int,
  color: String,
  life: Int
) {
  def toJson: JsValue = Json.obj(
    "name" -> name,
    "posX" -> pos.x,
    "posY" -> pos.y,
    "angle" -> angle,
    "score" -> score,
    "color" -> color,
    "life" -> life
  )
}