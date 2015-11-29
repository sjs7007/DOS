
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
  
  case class UserMap (Email:String, Person:User)
   object UserMap extends DefaultJsonProtocol {
    implicit  val format = jsonFormat2(UserMap.apply)
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
case class Continue()

object StartHere extends App {
              val system = ActorSystem("Client")

  val client = system.actorOf(Props[ClientStarter], name = "ClientStarter")  // the local actor
  client ! Begin
  
}

case class Begin()

class ClientStarter extends Actor {
        import context._
        val noOfClients = 50
      
         //.withRouter(RoundRobinRouter(noOfClients)))
        
       def receive ={
        case Begin =>{
         for (n <- 1 to noOfClients) {
          val client= context.actorOf(Props[Client])
          client ! Start
          }
          }
        }
       

}

object UserVariables {
  var nameArray = Array("Junior", "Clooney", "Brigadier", "Zara", "Nuha", "Ayan", "Pandu", "John", "Bobby", "Maya", "Krillin", "Picasso", "Goku", "Tyrael", "Mufasa", "Don-Corleone", "Uther", "Arthas", "Billy")
  var townArray = Array ("sville", " Town", " Republic", " City", "pur", "derabad")
  var emailArray = Array ("@gmail.com", "@hotmail.com", "@yahoo.com", "@aol.com", "@piratebay.se")
  
  var allEmails = new ListBuffer[String]()

}

class Client extends Actor
{

  import UserVariables._
  import MyJsonProtocol._
  import context._
  
    val r = scala.util.Random
  implicit val timeout: Timeout = 300.seconds

  var baseIP = "http://192.168.0.21:"
  var requestType = "getFriendList"

  var name = nameArray(r.nextInt(nameArray.length)) + " " + nameArray(r.nextInt(nameArray.length))
  var email = name.substring(0, r.nextInt(2)+1) + r.nextInt(500).toString + nameArray(r.nextInt(nameArray.length)) + emailArray(r.nextInt(emailArray.length))
  var bday = (r.nextInt(30)+1).toString + "/" + (r.nextInt(11)+1).toString + "/" + (r.nextInt(100) + 1915).toString
  var city = nameArray(r.nextInt(nameArray.length)) + townArray(r.nextInt(townArray.length))
  
  var socialFactor = 50 + r.nextInt (70)
  var loudFactor = 1 + r.nextInt (70)
  var lurkFactor = 1 + r.nextInt (70)
  var listOfFriends : Array[String] = new Array[String](1)

  var port =  (5000 + r.nextInt(50)).toString
  
    var jsonString = User(email, name, bday, city).toJson

  var serverIP = ""
   var dieRoller = 0
   
     import context.dispatcher

   
    def receive = {
    
  case Start =>
  
  
  
    //User behaviour definitions
  
  
  
  
  // Create user
  
  for {
      response <- IO(Http).ask(HttpRequest(POST, Uri(baseIP + port + "/createUser"),entity= HttpEntity(`application/json`, jsonString.toString))).mapTo[HttpResponse]
   }
   yield {
   allEmails += email
     val tick = context.system.scheduler.schedule(100 millis, 200 millis, self, "Continue")

   }
  
  
    println (name + " created.")

    
    
    
   
case "Continue" =>

 port =  (5000 + r.nextInt(50)).toString
 
  serverIP = baseIP + port + "/"
  
  requestType match {
    
   case "doNothing" =>
  
  case "getFriendList" =>
  
  
  // Update my friend list and add a random friend if I have none
  
  for {
  response <- IO(Http).ask(HttpRequest(GET, Uri(serverIP + "users/" + email + "/friends"))).mapTo[HttpResponse]  
  }
   yield {
   var myFriends = response.entity.asString
   listOfFriends = myFriends.substring(11,myFriends.length-1).split(",").map(_.trim)
   
   var randEmail = allEmails(r.nextInt(allEmails.length))
   
   if (listOfFriends.length < 2 && randEmail != email && !(listOfFriends contains randEmail)) { 
   for {
      response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + "sendFriendRequest"),entity= HttpEntity(`application/json`, FriendRequest(email, randEmail).toJson.toString)))
   }
   yield {}
   }
  }
  
  case "addNewFriend" => 
  
  // I want to add a random friend of friend
  
  val selectedFriend = listOfFriends(r.nextInt(listOfFriends.length))
    
  if (selectedFriend != null && !(listOfFriends contains selectedFriend)) {
  println (name + "is adding as friend: " + selectedFriend)
  for {
      response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + "sendFriendRequest"),entity= HttpEntity(`application/json`, (FriendRequest(email, selectedFriend).toJson.toString)))).mapTo[HttpResponse]
   }
   yield {
      var theirFriends = response.entity.asString
      var listOfTheirFriends = theirFriends.substring(11,theirFriends.length-1).split(",").map(_.trim)
      
      if (listOfTheirFriends.length > 1) {
      var friendToAdd = listOfTheirFriends(r.nextInt(listOfTheirFriends.length))
      
      while (listOfTheirFriends.length > 1 && friendToAdd == email)
      friendToAdd = listOfTheirFriends(r.nextInt(listOfTheirFriends.length))
      
      println (name + " is adding " + friendToAdd)
      
      if (friendToAdd != email && !(listOfFriends contains friendToAdd)) {
      for {
      response2 <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + "sendFriendRequest"),entity= HttpEntity(`application/json`, FriendRequest(email,friendToAdd).toJson.toString))).mapTo[HttpResponse]
   }
   yield {
   for {
  response <- IO(Http).ask(HttpRequest(GET, Uri(serverIP + "users/" + email + "/friends"))).mapTo[HttpResponse]  
  }
   yield {
   var myFriends = response.entity.asString
   listOfFriends = myFriends.substring(11,myFriends.length-1).split(",").map(_.trim)
   }
   
   }
   }
      
      }
   }
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
   
  case "sendFriendRequest" => jsonString = FriendRequest(email, allEmails(r.nextInt(allEmails.length))).toJson

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
  dieRoller = r.nextInt (100)
  
  /*if (dieRoller < loudFactor)
    requestType = "wallWrite"
  else*/
  
  if (dieRoller < socialFactor)
    requestType = "addNewFriend" 
  else requestType = "doNothing"
  
  //self ! Continue
  
  
  }
  
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  