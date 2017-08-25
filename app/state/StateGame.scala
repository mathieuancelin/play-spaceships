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
    var now: Long = System.currentTimeMillis()
    var delta: Float = now-prevDate
    delta = delta / 1000f
    deltaTime = delta
    //prevDate = now
    //println(prevDate)
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
            game.copy(bullets = game.bullets :+ new Bullet(idB,ship.position,ship.angle,100,id))
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

    val stateF = if(!state.ships.isEmpty && !state.bullets.isEmpty) {
      var bullets: Seq[Bullet] = state.bullets
      var ships: Seq[Ship] = state.ships
      var nameScore : String = state.nameScore
      var bestScore : Int = state.bestScore
      for(bullet <- state.bullets) {
        //VERIFIER COLLISION TIR - BORD DU TERRAIN
        if(bullet.position.x > 800 || bullet.position.x < 0 || bullet.position.y < -600 || bullet.position.y > 0) {
          bullets = bullets.filter(_.id != bullet.id)
        }

        for(ship <- state.ships) {
          //VERIFIER COLLISION VAISSEAU - TIR
          if(bullet.position.x-5 < ship.position.x-20 + 40 &&
            bullet.position.x-5 + 10 > ship.position.x-20 &&
            bullet.position.y+5 < ship.position.y-20 + 40 &&
            bullet.position.y+5 + 10 > ship.position.y-20 &&
            bullet.idShip != ship.id) {
            //Supprime le tir
            bullets = bullets.filter(_.id != bullet.id)
            //Supprime le ship
            ships = ships.filter(_.id != ship.id)
            //Ajouter score
            ships.filter(_.id == bullet.idShip).headOption.map(p => {
              ships = ships.filter(_.id != bullet.idShip)
              ships = ships :+ new Ship(p.id, p.name, p.position, p.angle, p.speed, p.color, p.velocity,
                p.angularVelocity, p.drag, p.angularDrag, p.life, p.score + 1)
              if(p.score+1 > bestScore) {
                bestScore = p.score+1
                nameScore = p.name
              }
            })
            //Verifie la vie
            if(!(ship.life-1 <= 0)) {
              ships = ships :+ new Ship(ship.id,ship.name,ship.position, ship.angle,
              ship.speed,ship.color,ship.velocity,ship.angularVelocity,ship.drag,
              ship.angularDrag,ship.life-1,ship.score)
            }


          }
        }
      }
      state.copy(ships = ships, bullets = bullets, nameScore = nameScore, bestScore = bestScore)
    } else {
      state
    }

    ref.set(stateF)
    //Logger.info(s"action: $action => next state: $newState")
    stateF
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
