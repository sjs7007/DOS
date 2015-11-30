
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
import java.io.{File, FileOutputStream}
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
import java.io._

object MyJsonProtocol extends DefaultJsonProtocol {

  //createUser
  case class User(Email:String, Name: String,Birthday: String,CurrentCity : String)

  object User extends DefaultJsonProtocol {
    implicit val format = jsonFormat4(User.apply)
  }

   case class Photo(Email:String, Caption: String, Image: Array[Byte])
    object Photo extends DefaultJsonProtocol {
    implicit  val format = jsonFormat3(Photo.apply)
  }
  
  case class CreateAlbum (Email:String, Title:String)
    object CreateAlbum extends DefaultJsonProtocol {
      implicit  val format = jsonFormat2(CreateAlbum.apply)
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
  
  
  case class FollowPage (Email:String)
  
  object FollowPage extends DefaultJsonProtocol {
    implicit val format = jsonFormat1(FollowPage.apply)
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
        import UserVariables._
      
        
       def receive ={
        case Begin =>{
         for (n <- 1 to worldSize) {
          val client= context.actorOf(Props[Client])
          client ! Start
          }
          }
        }
       

}

object UserVariables {

  var worldSize = 100
/*
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
  
  var albumTitles = Array ("Holiday in '07", "Random pics", "Me IRL", "Oscar Wilde")
  var imageBase = Array ("dog1.jpg", "dogeTiny.jpg", "serveriyanan.jpg")
  */
  
  var allEmails = new ListBuffer[String]()

}

class Client extends Actor
{

  import UserVariables._
  import MyJsonProtocol._
  import context._
  
    val r = scala.util.Random
  implicit val timeout: Timeout = 300.seconds

  var baseIP = "http://192.168.0.14:"
  var requestType = "getFriendList"

  var name = "a"
  var email = (r.nextInt(9999) + r.nextInt(9999) + r.nextInt(9999)).toString
  var bday = "a"
  var city = "a"
  
  var doThis = 0
 
  
  var listOfFriends : Array[String] = new Array[String](1)
  var listOfPages = new ListBuffer[String]
  var albumsCreated = 0


  var port = (5000 + r.nextInt(50)).toString
    var jsonString = User(email, name, bday, city).toJson

  var serverIP = ""
   
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
   val tick = context.system.scheduler.schedule(100 millis, 2 millis, self, "Continue") //UNCOMMENT
   }

   case "Continue" => 

  serverIP = baseIP + port + "/"
  
  doThis match {
    
   
  case 0 =>
  
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
  
  case 1 => 
  
  // I want to add a random friend of friend
  
  val selectedFriend = listOfFriends(r.nextInt(listOfFriends.length))
    
  if (selectedFriend != null && selectedFriend.length() > 2) {
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
   
   
   for {
  response <- IO(Http).ask(HttpRequest(GET, Uri(serverIP + "pages/random"))).mapTo[HttpResponse]  
  }
   yield {
    var thisPage = response.entity.asString
   if (thisPage != "noPagesExist" && !(listOfPages contains thisPage)) {
    
    for {
      response2 <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + "pages/" + thisPage + "/follow"),entity= HttpEntity(`application/json`, FollowPage(email).toJson.toString))).mapTo[HttpResponse]
   }
   yield {
    listOfPages += thisPage
    }
    }
   }
   
   
   case 2 =>
   /*
       val bis = new BufferedInputStream(new FileInputStream("dog1.jpg"))
      
      val bArray = Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray
      
      bis.close();

      if (albumsCreated == 0) {
      
      for {
      response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + "createAlbum"),entity= HttpEntity(`application/json`, CreateAlbum(email, "dsfsdf").toJson.toString))).mapTo[HttpResponse]
      }
   yield {
   albumsCreated = albumsCreated + 1
    }
      }
      
      
       
  if (albumsCreated > 0) {
  var picsToUpload = 1
  var albumNumber = 1
  for (i <- 1 to picsToUpload) {
  for {
      response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + "users/" +email+"/albums/"+albumNumber+"/upload"),entity= HttpEntity(`application/json`, Photo(email, "1", bArray).toJson.toString)))
   }
   yield {
    }
    
    }
    }
*/
  
  case 3 =>
  
  var viewPageOf = ""
  
  if (listOfFriends.length < 2)
    viewPageOf = email
  else viewPageOf = listOfFriends(0)
  
  for {
  response <- IO(Http).ask(HttpRequest(GET, Uri(serverIP + "users/" + viewPageOf + "/posts?Email="+email))).mapTo[HttpResponse]  
  }
   yield {}
   
  case 4 => 
  
  // Special case: Write on a page, create a page if it hasn't been created
  
  if (r.nextInt(100) == 100 || (listOfPages.length > 0)) {
  
  if (listOfPages.length == 0) {
  val pageTitle = "Title"
  val pageID = r.nextInt (10000).toString + r.nextInt (10000).toString
  
  for {
      response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + "createPage"),entity= HttpEntity(`application/json`, CreatePage(email, pageTitle, pageID).toJson.toString)))
   }
   yield {
   listOfPages += pageID
   }
  
  }
  if (listOfPages.length > 0) {
  val pageToWriteOn = listOfPages(0)
  val myPost = "posting on epic thread"
  
  for {
      response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + "pages/" + pageToWriteOn + "/createPost"),entity= HttpEntity(`application/json`, PagePost(email, myPost).toJson.toString)))
   }
   yield {
   }
   }
  
  }
  
  else {
  
  // Write on some walls
  
  var writeOnOwnWall = false
  
      if (listOfFriends.length < 2)
        writeOnOwnWall = true
    
    
    var target = ""
    
    if (!writeOnOwnWall)
      target = listOfFriends(r.nextInt(listOfFriends.length))
    else target = email
    
    var textPost = ""
    
      textPost = "hurrr durrr"
  
  for {
      response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + requestType),entity= HttpEntity(`application/json`, Wallpost(email, target, textPost).toJson.toString)))
   }
   yield {
   }
   }
   
   
  }
  
  
  // Model next behaviour here
  
 doThis = (doThis + 1) % 5
  
  
  
  }
  
  
  
  
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  