
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import scala.concurrent.Future
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import scala.collection.concurrent.Map
import akka.actor._
import java.util.concurrent.ConcurrentHashMap
import java.io.BufferedInputStream
import java.io.FileInputStream
import akka.routing.RoundRobinRouter
import scala.collection.mutable.ListBuffer
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.http._
import spray.json._
import HttpCharsets._
import MediaTypes._
import scala.util.Random._
import spray.http.HttpHeaders._
import spray.http.HttpMethods._
import akka.util.Timeout
import spray.json.DefaultJsonProtocol
import spray.httpx.SprayJsonSupport

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val personFormat = jsonFormat3(Person)

  case class Person(name: String, fistName: String, age: Long)

  //createUser
  case class User(Email:String, Name: String,Birthday: String,CurrentCity : String)
  case class UserCreated(Email:String)
  case object UserAlreadyExists

  object User extends DefaultJsonProtocol {
    implicit val format = jsonFormat4(User.apply)
  }

  //sendFriendRequest
  case class FriendRequest(fromEmail:String, toEmail:String)
  case object AlreadyFriends
  case object UserNotPresent
  case object FriendRequestSent
  
   //wallwrite
  case class Wallpost(fromEmail:String, toEmail:String, data:String)
  case object PostSuccess
  case object PostFail
  
    object FriendRequest extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(FriendRequest.apply)
  }
  
   

    object Wallpost extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(Wallpost.apply)
  }
}

case class Start()

object StartHere extends App {
              val system = ActorSystem("Client")

  val client = system.actorOf(Props[ClientStarter], name = "ClientStarter")  // the local actor
  client ! "start"
  
}

class ClientStarter extends Actor {
        import context._
        val noOfClients = 100000

              def receive = {
              case _ =>
        val client = context.actorOf(Props[Client].withRouter(RoundRobinRouter(noOfClients)))
        
        for (n <- 1 to noOfClients)
          client ! Start
          }

}

class Client extends Actor
{
  import MyJsonProtocol._
  implicit val personFormat = jsonFormat3(Person)
  import context._
  
  implicit val timeout: Timeout = 3.seconds

  var name = "pandu"
  var email = self.toString
  var bday = "tomallow"
  var city = "loaftown"
  
  var serverIP = "http://192.168.0.14:"
  var requestType = "createUser"

  var jsonString = User(email, name, bday, city).toJson
  
  def receive = {
    
  case Start => requestType = "createUser"
  
  }
  
  
  
  requestType match {
  
  case "createUser" => jsonString = User(email, name, bday, city).toJson
  val r = scala.util.Random
  var port = (5000 + r.nextInt(50)).toString
  for {
      response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + port + "/" + requestType),entity= HttpEntity(`application/json`, jsonString.toString)))
   }
   yield {
   println (response)
   }
  
  case "getFriendList" =>
  for {
  response <- IO(Http).ask(HttpRequest(GET, Uri(serverIP + "user/sjs7007/friends")))
  }
   yield {
   println (response)
   }
   
   case "sendpics" =>
   
   val bis = new BufferedInputStream(new FileInputStream("/var/tmp/modified-dog"))
  val bArray = Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray
  bis.close();
 
 
   for {
      response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + requestType), entity = HttpEntity(`image/jpeg`, bArray)))
}
   yield {
   println (response)
   }
   
  case "sendFriendRequest" => jsonString = FriendRequest(email, "jaadugar").toJson 
  
    for {
      response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + requestType),entity= HttpEntity(`application/json`, jsonString.toString)))
   }
   yield {
   println (response)
   }
  
  
  case "wallWrite" => jsonString = Wallpost(email, "jaadugar", "i am gay").toJson
  for {
      response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + requestType),entity= HttpEntity(`application/json`, jsonString.toString)))
   }
   yield {
   println (response)
   }
  }
  
   
   
}