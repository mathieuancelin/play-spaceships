package state

import java.util.concurrent.atomic.AtomicReference

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{BroadcastHub, Keep, RunnableGraph, Source, SourceQueueWithComplete}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import models.Player
import models.Vector
import models.Bullet
import play.api.Logger
import play.api.libs.json.{JsArray, JsValue, Json}

import scala.concurrent.Future
import scala.concurrent.duration._

// Class Action
sealed trait Action
case class AddPlayer(player: Player) extends Action
case class DropPlayer(username: String) extends Action
case class AddPoint(username: String, point: Int) extends Action
case class LostLife(username: String, point: Int) extends Action
case class AddBullet(bullet: Bullet) extends Action
case class DropBullet(id: Int) extends Action
case class MovePlayer(username: String, pos: Vector, angle: Float) extends Action
case class ModifScore(username: String, point: Int) extends Action
case class ClearGame() extends Action
case object TickEvent extends Action

// Class State
sealed trait State
case class GameState(players: Seq[Player] = Seq.empty[Player], bullets: Seq[Bullet] = Seq.empty[Bullet], nameScore: String = "", bestScore: Int = 0) extends State {
  def toJson: JsValue = Json.obj(
    "players" -> JsArray(players.map(_.toJson)),
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
  private var indexBullet = 0
  private var prevDate = System.currentTimeMillis()
  private val vitesse = 0.25

  def reducer[A <: Action](game: GameState, action: A): GameState = action match {
    case AddPlayer(p) => game.copy(players = game.players :+ p)
    case DropPlayer(u) => game.copy(players = game.players.filter(_.name != u))
    case AddPoint(u, p) =>
      game.players
          .filter(_.name == u)
          .headOption
          .map(player =>
            game.copy(players = game.players.filter(_.name != u) :+
                new Player(player.name, player.pos,player.angle,player.score+p,player.color, player.life))
          ).getOrElse(game);
    case LostLife(u,p) =>
        game.players
          .filter(_.name == u)
          .headOption
          .map(player =>
            game.copy(players = game.players.filter(_.name != u) :+
              new Player(player.name, player.pos,player.angle,player.score,player.color, player.life-p))
          ).getOrElse(game);
    case AddBullet(b) => game.copy(bullets = game.bullets :+ b)
    case DropBullet(id) => game.copy(bullets = game.bullets.filter(_.id != id))
    case MovePlayer(u,p,a) => {
      game.players
          .filter(_.name == u)
          .headOption
          .map(player => {
            var x = player.pos.x + p.x*2
            var y = player.pos.y + p.y*2
            if (x < -10) {
              x = 800
            } else if (x > 810) {
              x = 0
            }
            if (y > 10) {
              y = -600;
            } else if (y < -610) {
              y = 0
            }
              game.copy(players = game.players.filter(_.name != u) :+
                new Player(player.name, new Vector(x, y), a, player.score, player.color, player.life))
          })
        .getOrElse(game);
    }
    case ModifScore(u,p) => game.copy(nameScore = u,bestScore = p)
    case ClearGame() => game.copy(players = Seq.empty[Player], bullets = Seq.empty[Bullet])
    case _ => game
  }

  val source: Source[Action, SourceQueueWithComplete[Action]] = Source.queue[Action](50000, OverflowStrategy.dropTail)
  val eventsAndTicks: Source[GameState, SourceQueueWithComplete[Action]] = source.merge(Source.tick(0.second, 100.millis, NotUsed).map(_ => TickEvent)).scan(initialState) { (prevState, action) =>
    val newState = reducer(prevState, action)

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
          bullets = bullets :+ new Bullet(bullet.id,new Vector(x,y),bullet.angle,bullet.nameShip)
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
    finalState
  } statefulMapConcat { () =>

    import scala.collection.immutable.{ Iterable => Imuterable }

    var lastState: GameState = GameState()

    def processNextState(next: GameState): Imuterable[GameState] = {

      val nextGameState = if (next == lastState) {
        Imuterable.empty[GameState]
      } else {
        Imuterable(next)
      }
      lastState = next

      nextGameState
    }

    processNextState
  }

  val runnableGraph: RunnableGraph[(SourceQueueWithComplete[Action], Source[GameState, NotUsed])] =
    eventsAndTicks.toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)

  val (queue: SourceQueueWithComplete[Action], stateStream: Source[GameState, NotUsed]) = runnableGraph.run()

  def stream: Source[GameState, NotUsed] = stateStream

  def push[A <: Action](action: A): Future[Unit] = queue.offer(action).map(_ => ())

  def state: Future[GameState] = Future.successful(ref.get())
}
