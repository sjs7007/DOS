
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

  object User extends DefaultJsonProtocol {
    implicit val format = jsonFormat4(User.apply)
  }

   case class Photo(Email:String, Caption: String, Image: String)
    object Photo extends DefaultJsonProtocol {
    implicit  val format = jsonFormat3(Photo.apply)
  }
  
  //sendFriendRequest
  case class FriendRequest(fromEmail:String, toEmail:String)
  
  object FriendRequest extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(FriendRequest.apply)
  }
  
   //wallwrite
  case class Wallpost(fromEmail:String, toEmail:String, data:String)
  
    object Wallpost extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(Wallpost.apply)
  }
  
  case class CreatePage (adminEmail:String, Title:String, pageID:String)
  
  object CreatePage extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(CreatePage.apply)
  }
  
    case class PagePost(fromEmail:String, data:String)
  
    object PagePost extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(PagePost.apply)
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
        val noOfClients = 10
      
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
  var aggrandizementArray = Array ("best", ".godlike", "cool", "cutie", "lovely", ".coolguy", "saucepants", "thebest", "nice", "batman", "spoderman", "ossum", "secret", "", "", "", "", "")
  var emailArray = Array ("@gmail.com", "@hotmail.com", "@yahoo.com", "@aol.net", "@piratebay.se", "@orkut.in")
  
  var postPrefixArray = Array ("Hey!", "OMG!", "Dude!", "Yo.", "Sup.", "Ola coca cola!", "M8,")
  var postBodyArray = Array ("Let's go to the movies!", "Did you read that article?", "Made up your mind yet?", "Not really.", "What's wrong with maroon?", "Where did the soda go?", "It's a trap!")
  var postSuffixArray = Array ("Let me know soon!", "Like and subscribe!", "It was just clickbait, though.", "Would be pretty cool if Martians existed.", "See you!", "Later!", "Bye!", "TTYL!")
  
  var selfPostFirst = Array ("Excited about", "Feeling nervous about", "Worried about", "Happy about", "Content with", "Anxious about", "Very expensive", "At odds with")
  var selfPostSecond = Array ("breakfast", "candy", "Wal-Mart", "pop culture", "soda", "activities planned", "falling rocks", "death by turtle")
  var selfPostThird = Array ("this Halloween!", "today!", "tonight!", "on New Year's Eve!", "that day.", "that never happened.", "that doesn't exist.", "at Broadway", "with my husband.", "with my wife.")
  
  var pagePrefix = Array ("Society for the Protection of", "Down with", "Fans of", "Collector's Edition", "Pictures of", "Cookies and", "Gory Images of", "Sherlock Holmes and", "Still a better love story than", "Quiet admirers of", "News about")
  var pageSuffix = Array ("Elfish Welfare", "Dungeons and Dragons", "The Milky Way", "John Snow", "John Watson's left thumb", "a spider", "various assorted implements", "a walk in the park", "a shark eating a dolphin", "facebook pages")
  
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
  var email = name.substring(0, r.nextInt(5)+1).split(" ")(0) + aggrandizementArray(r.nextInt(aggrandizementArray.length)) + r.nextInt(3000).toString + emailArray(r.nextInt(emailArray.length))
  var bday = (r.nextInt(30)+1).toString + "/" + (r.nextInt(11)+1).toString + "/" + (r.nextInt(100) + 1915).toString
  var city = nameArray(r.nextInt(nameArray.length)) + townArray(r.nextInt(townArray.length))
  
  var socialFactor = 1 + r.nextInt (30)
  var loudFactor = 1 + r.nextInt (10)
  var lurkFactor = 1 + r.nextInt (70)
  var listOfFriends : Array[String] = new Array[String](1)
  var listOfPages = new ListBuffer[String]


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
    
  if (selectedFriend != null && selectedFriend.length() > 2) {
  println (name + " is adding as friend: " + selectedFriend)
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
   }
   
  case "sendFriendRequest" => jsonString = FriendRequest(email, allEmails(r.nextInt(allEmails.length))).toJson

    for {
      response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + requestType),entity= HttpEntity(`application/json`, jsonString.toString)))
   }
   yield {
   }
  
  
  case "wallWrite" => 
  
  // Special case: Write on a page, create a page if it hasn't been created
  
  if (r.nextInt(100) == loudFactor || (listOfPages.length > 0 && r.nextInt(100) > loudFactor)) {
  
  if (listOfPages.length == 0 || r.nextInt(100) == loudFactor) {
  val pageTitle = pagePrefix(r.nextInt(pagePrefix.length)) + " " + pageSuffix(r.nextInt(pageSuffix.length))
  val pageID = r.nextInt (10000).toString + r.nextInt (10000).toString
  
  for {
      response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + "createPage"),entity= HttpEntity(`application/json`, CreatePage(email, pageTitle, pageID).toJson.toString)))
   }
   yield {
   listOfPages += pageID
   }
  
  }
  val pageToWriteOn = listOfPages(r.nextInt(listOfPages.length))
  val myPost = postPrefixArray(r.nextInt(postPrefixArray.length)) + " " + postBodyArray(r.nextInt(postBodyArray.length)) + " " + postSuffixArray(r.nextInt(postSuffixArray.length))
  
  
  for {
      response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + "pages/" + pageToWriteOn + "/createPost"),entity= HttpEntity(`application/json`, PagePost(email, myPost).toJson.toString)))
   }
   yield {
   }
  
  }
  
  else {
  
  // Write on some walls
  
  var writeOnOwnWall = false
  
    if (socialFactor < loudFactor)
    {
      if (r.nextInt(100) < loudFactor || listOfFriends.length < 2)
        writeOnOwnWall = true
    }
    else if (r.nextInt(100) < socialFactor  || listOfFriends.length < 2)
      writeOnOwnWall = true
    
    var target = ""
    
    if (!writeOnOwnWall)
      target = listOfFriends(r.nextInt(listOfFriends.length))
    else target = email
    
    var textPost = ""
    
    if (writeOnOwnWall)
      textPost = selfPostFirst(r.nextInt(selfPostFirst.length)) + " " + selfPostSecond(r.nextInt(selfPostSecond.length)) + " " + selfPostThird(r.nextInt(selfPostThird.length))
    else textPost = postPrefixArray(r.nextInt(postPrefixArray.length)) + " " + postBodyArray(r.nextInt(postBodyArray.length)) + " " + postSuffixArray(r.nextInt(postSuffixArray.length))
  
  
  for {
      response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + requestType),entity= HttpEntity(`application/json`, Wallpost(email, target, textPost).toJson.toString)))
   }
   yield {
   }
   }
   
  }
  
  
  // Model next behaviour here
  dieRoller = r.nextInt (100)
  
  
  if (listOfFriends.length < 2)
    requestType = "getFriendList"
  else if (dieRoller < loudFactor)
    requestType = "wallWrite"
  else 
  if (dieRoller < socialFactor)
    requestType = "addNewFriend" 
  else requestType = "doNothing"
  
  
  
  }
  
  
  
  
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  