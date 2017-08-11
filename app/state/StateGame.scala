package state

import java.util.concurrent.atomic.AtomicReference

import akka.NotUsed
import akka.actor.{ActorSystem, Cancellable}
import akka.stream.scaladsl.{BroadcastHub, Keep, RunnableGraph, Source, SourceQueueWithComplete}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import models._
import play.api.Logger
import play.api.libs.json.{JsArray, JsValue, Json}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection._

// Class Action
sealed trait Action
case class AddShip(ship: Ship) extends Action
case class AddBullet(id: Int, idB: Int) extends Action
case class MoveShip(id: Int, angle: Float) extends Action
case class TeleportShip(id: Int, position: Vector) extends Action
case class Clear() extends Action
case object TickEvent extends Action

// Class State
sealed trait State
case class GameState(ships: Seq[Ship] = Seq.empty[Ship],bullets: Seq[Bullet] = Seq.empty[Bullet], nameScore: String = "", bestScore: Int = 0) extends State {

  private var prevDate: Long = System.currentTimeMillis()
  private var deltaTime: Float = 0

  def simulate() = {
    var now:Long = System.currentTimeMillis()
    var delta: Float = now-prevDate
    delta = delta / 1000f
    deltaTime = delta
    prevDate = now
    var shipsL: Seq[Ship] = Seq.empty[Ship]
    var bulletsL: Seq[Bullet] = Seq.empty[Bullet]
    for(ship <- ships) {
      var s = ship.cloned()
      s.simulate(deltaTime)
      shipsL = shipsL :+ s
    }
    for(bullet <- bullets) {
      var b = bullet.cloned()
      b.simulate(deltaTime)
      bulletsL = bulletsL :+ b
    }
    (shipsL,bulletsL)
  }

  def toJson: JsValue = Json.obj(
    "ships" -> JsArray(ships.map(_.toJson)),
    "bullets" -> JsArray(bullets.map(_.toJson)),
    "nameScore" -> nameScore,
    "bestScore" -> bestScore
  )
}

class StateGame() {

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer.create(actorSystem)
  implicit val ec = actorSystem.dispatcher

  private val initialState = GameState()
  private val ref = new AtomicReference[GameState](initialState)

   def stop() = {
     Logger.info("Cancelling queue and ticks")
     queue.complete()
     tickCancel.cancel()
   }

  def reducer[A <: Action](game: GameState, action: A): GameState = action match {
    case AddShip(ship) => game.copy(ships = game.ships :+ ship)
    case AddBullet(id,idB) => {
      game.ships.filter(_.id == id).headOption
          .map(ship => {
            game.copy(bullets = game.bullets :+ new Bullet(idB,ship.position,ship.angle,100,ship.name))
          })
          .getOrElse(game)
    }
    case MoveShip(id,angle) => {
      game.ships.filter(_.id == id).headOption
          .map(ship => {
            var s = ship.cloned()
            s.move(angle)
            game.copy(ships = game.ships.filter(_.id != id) :+ s)
          })
          .getOrElse(game)
    }
    case TeleportShip(id,position) => {
      game.ships.filter(_.id == id).headOption
        .map(ship => {
          var s = ship.cloned()
          s.teleport(position)
          game.copy(ships = game.ships.filter(_.id != id) :+ s)
        })
        .getOrElse(game)
    }
    case Clear() => game.copy(ships =  Seq.empty[Ship], bullets = Seq.empty[Bullet], nameScore = "", bestScore = 0)
    case _ => game
  }

  val tick: Source[Action, Cancellable] = Source.tick(0.second, 100.millis, NotUsed).map(_ => TickEvent)
  val source: Source[Action, SourceQueueWithComplete[Action]] = Source.queue[Action](50000, OverflowStrategy.dropTail)
  val eventsAndTicks: Source[GameState, (SourceQueueWithComplete[Action], Cancellable)] = source.mergeMat(tick)((a, b) => (a, b)).scan(initialState) { (prevState, action) =>

    val newState = reducer(prevState, action)
    val (ships,bullets) = newState.simulate()
    val state = new GameState(ships,bullets,newState.nameScore,newState.bestScore)
    //newState.simulate()
    ref.set(state)
    //println(state)
    state


    /*
    // CALCUL DEPLACEMENT DES TIRS
    var now = System.currentTimeMillis()
    var deltaTime = (now - prevDate) * vitesse.toFloat
    prevDate = now
    val bulletState = if(!newState.bullets.isEmpty) {
      var bullets: Seq[Bullet] = Seq.empty[Bullet]
      for(bullet <- newState.bullets) {
        var x = bullet.pos.x + Math.cos(bullet.angle.toDouble * Math.PI/180).toFloat*deltaTime
        var y = bullet.pos.y + Math.sin(bullet.angle.toDouble * Math.PI/180).toFloat*deltaTime
        if(x < 800 && x > 0 && y > -600 && y < 0) {
          bullets = bullets :+ new Bullet(bullet.id,new models.Vector(x,y),bullet.angle,bullet.nameShip)
        }
      }
      newState.copy(bullets = bullets)
    } else {
      newState
    }

    // CALCUL COLLISION AVEC VAISSEAU-TIR
    val finalState = if(!bulletState.players.isEmpty && ! bulletState.bullets.isEmpty) {
      var bullets : Seq[Bullet] = bulletState.bullets
      var players : Seq[Player] = bulletState.players
      for(bullet <- bulletState.bullets) {
        for(player <- bulletState.players) {
          if(bullet.pos.x-5 < player.pos.x-20 + 40 &&
              bullet.pos.x-5 + 10 > player.pos.x-20 &&
              bullet.pos.y+5 < player.pos.y-20 + 40 &&
              bullet.pos.y+5 + 10 > player.pos.y-20 &&
              bullet.nameShip != player.name) {
            bullets = bullets.filter(_.id != bullet.id)
            players = players.filter(_.name != player.name)
            var scoreUp = players.filter(_.name == bullet.nameShip)
            players = players.filter(_.name != bullet.nameShip)
            scoreUp.headOption.map(p =>
              players = players :+ new Player(p.name,p.pos,p.angle,p.score+1,p.color,p.life))
            if(!(player.life-1 <= 0)) {
              players = players :+ new Player(player.name,player.pos,player.angle,player.score,player.color,player.life-1)
            }
          }
        }
      }
      bulletState.copy(players = players, bullets = bullets)
    } else {
      bulletState
    }

    ref.set(finalState)
    //Logger.info(s"action: $action => next state: $newState")
    finalState*/
  } statefulMapConcat { () =>

    import scala.collection.immutable.{ Iterable => Imuterable }

    var lastState: GameState = GameState()

    def processNextState(next: GameState): Imuterable[GameState] = {

      val nextGameState = if (next == lastState) {
        Imuterable.empty[GameState]
      } else {
        Imuterable(next)
      }
      lastState = new GameState(next.ships,next.bullets,next.nameScore,next.bestScore)

      nextGameState
    }

    processNextState
  }

  val runnableGraph: RunnableGraph[((SourceQueueWithComplete[Action], Cancellable), Source[GameState, NotUsed])] =
    eventsAndTicks.toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)

  val ((queue: SourceQueueWithComplete[Action], tickCancel: Cancellable), stateStream: Source[GameState, NotUsed]) = runnableGraph.run()

  def stream: Source[GameState, NotUsed] = stateStream

  def push[A <: Action](action: A): Future[Unit] = queue.offer(action).map(_ => ())

  def state: Future[GameState] = Future.successful(ref.get())
}
