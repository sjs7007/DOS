
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import scala.concurrent.Future
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import scala.collection.concurrent.Map
import akka.actor._
import spray.httpx.unmarshalling._
import spray.httpx.unmarshalling.FromResponseUnmarshaller
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
import scala.io.Source

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

   case class Photo(Email:String, Caption: String, Image: String)
    object Photo extends DefaultJsonProtocol {
    implicit  val format = jsonFormat3(Photo.apply)
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
        val noOfClients = 100

              def receive = {
              case _ =>
        val client = context.actorOf(Props[Client].withRouter(RoundRobinRouter(noOfClients)))
        
        for (n <- 1 to noOfClients)
          client ! Start
          }

}

object UserVariables {
  var nameArray = Array("Zara", "Nuha", "Ayan", "Pandu", "John", "Bobby", "Maya", "Krillin", "Picasso", "Goku", "Tyrael", "Mufasa", "Don Corleone", "Uther", "Arthas", "Billy")
  var townArray = Array ("sville", " Town", " Republic", " City", "pur", "derabad")
  var emailArray = Array ("@gmail.com", "@hotmail.com", "@yahoo.com", "@aol.com", "@piratebay.se")
}

class Client extends Actor
{
  import UserVariables._
  import MyJsonProtocol._
  implicit val personFormat = jsonFormat3(Person)
  import context._
  
  val r = scala.util.Random
  
  implicit val timeout: Timeout = 3.seconds

  // User variables
  
  var name = nameArray(r.nextInt(nameArray.length)) + " " + nameArray(r.nextInt(nameArray.length))
  var email = name.substring(0, r.nextInt(5)) + r.nextInt(500).toString + nameArray(r.nextInt(nameArray.length)) + emailArray(r.nextInt(emailArray.length))
  var bday = (r.nextInt(30)+1).toString + "/" + (r.nextInt(11)+1).toString + "/" + (r.nextInt(100) + 1915).toString
  var city = nameArray(r.nextInt(nameArray.length)) + townArray(r.nextInt(townArray.length))
  
  var serverIP = "http://192.168.0.21:"
  var requestType = "createUser"

  var jsonString = User(email, name, bday, city).toJson
  
  //User behaviour definitions
  
  var socialFactor = 10 + r.nextInt (90)
  var loudFactor = 10 + r.nextInt (90)
  var lurkFactor = 10 + r.nextInt (90)
  var active = true
  
  //Create User
  
  for {
      response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + port + "/" + "createUser"),entity= HttpEntity(`application/json`, jsonString.toString))).mapTo[HttpResponse]
   }
   yield {
   }

  //Constant requesting:
   
  while (active) {
 
  var port = (5000 + r.nextInt(50)).toString
  
  serverIP = serverIP + port + "/"
  
  requestType match {
  
  case "createUser" => 
  
   case "users" =>
  for {
  response <- IO(Http).ask(HttpRequest(GET, Uri(serverIP + requestType))).mapTo[HttpResponse]
  }
   yield {
   println (response.entity.asString.parseJson)
   }
  
  case "getFriendList" =>
  for {
  response <- IO(Http).ask(HttpRequest(GET, Uri(serverIP + "user/sjs7007/friends"))).mapTo[HttpResponse]
  }
   yield {
   println (response.entity.asString)
   }
   
   case "upload" =>
   
   val bis = new BufferedInputStream(new FileInputStream("dog.jpeg"))
  
  val bArray = Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray
  println (bArray)
  var z = new String (bArray)
  
  var photoStuff = Photo(email, "loleshwar", z).toJson
  bis.close();
 
 println (photoStuff.toString)
   for {
   response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + requestType), entity = HttpEntity(`application/json`, photoStuff.toString)))
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
  
  
  // Model next behaviour here
  
  
  
  
  }
  
  
  
  
  
  def receive = {
    
  case Start => 
  
  }
   
   
}
