package local

import akka.actor._

object Local extends App {

  implicit val system = ActorSystem("LocalSystem")
  val localActor = system.actorOf(Props[LocalActor], name = "LocalActor")  // the local actor
  localActor ! "START"                                                     // start the action

}

class LocalActor extends Actor {

	val remote = context.actorFor("akka.tcp://HelloRemoteSystem@192.168.0.10:5150/user/RemoteActor")

  var counter = 0

  def receive = {
    case "START" =>
        remote ! "Hello from the LocalActor at SJ"
    case msg: String =>
        println(s"LocalActor received message: '$msg'")
        if (counter < 5) {
            sender ! "Hello back to you"
            counter += 1
        }
  }
}
