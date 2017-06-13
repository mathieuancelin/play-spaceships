package models

/**
  * Created by dsagnier on 13/06/17.
  */
class Game {
  val xMax = 800;
  val yMax = 600;
  val slotMax = 20;
  var scale = 1.0f;

  // Start game
  def start() = {

  }

  // Stop game
  def stop() = {

  }

  // Return horizontal size with scale
  def scaleX(): Int = {
    return (xMax.toFloat * scale).toInt;
  }

  // Return vertical size with scale
  def scaleY(): Int = {
    return (yMax.toFloat * scale).toInt;
  }

  // Create unit and catch a slot (max slot)
  def createUser( username: String ) = {

  }

  // Destroy unit and released a slot (max slot)
  def kill ( username: String ) = {

  }
}

case class Bullet(
 posX: Int,
 posY: Int
)

case class Player(
 name: String,
 posX: Int,
 posY: Int
)