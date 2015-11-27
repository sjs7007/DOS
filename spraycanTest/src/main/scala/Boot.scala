/**
 * Created by shinchan on 11/6/15.
 */

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.util.Timeout
import spray.can.Http

import scala.concurrent.duration._

object Boot extends App {

  // create our actor system with the name smartjava
  implicit val system = ActorSystem("smartjava")
  val service = system.actorOf(Props[SJServiceActor], "sj-rest-service")

  // IO requires an implicit ActorSystem, and ? requires an implicit timeout
  // Bind HTTP to the specified service.
  implicit val timeout = Timeout(5.seconds)
  //IO(Http) ? Http.Bind(service, interface = "localhost", port = 8082)

  //IO(Http).tell(Http.Bind(service, interface = "0.0.0.0", port = 8087), sender = service)
  for(i <- 0 to 51) {
    IO(Http).tell(Http.Bind(service, interface = "0.0.0.0", port = 5000+i), sender = service)
  }

}

/*object Boot extends  App {
  println("test")
}*/

