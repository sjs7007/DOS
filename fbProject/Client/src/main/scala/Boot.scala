import java.security.{MessageDigest, KeyPairGenerator, KeyFactory, PublicKey, PrivateKey}
import javax.crypto.Cipher
import java.security.spec.{X509EncodedKeySpec, PKCS8EncodedKeySpec}


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
//import scala.util.Marshal



object MyJsonProtocol extends DefaultJsonProtocol {

  //createUser
  case class User(Email:String, Name: String,Birthday: String,CurrentCity : String,pubKey : Array[Byte]) {
    require(!Email.isEmpty, "Emails must not be empty.")
    require(!Name.isEmpty,"Name must not be empty.")
  }
  object User extends DefaultJsonProtocol {
    implicit val format = jsonFormat5(User.apply)
  }



  case class EncryptedUser(user: User,sign: String,pubkey: Array[Byte])

  object EncryptedUser extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(EncryptedUser.apply)
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

  var worldSize = 2000

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

  var allEmails = new ListBuffer[String]()

}

class Client extends Actor
{

  import UserVariables._
  import MyJsonProtocol._
  import context._

  val r = scala.util.Random
  implicit val timeout: Timeout = 3.seconds

  var baseIP = "http://localhost:"
  var requestType = "getFriendList"

  var name = nameArray(r.nextInt(nameArray.length)) + " " + nameArray(r.nextInt(nameArray.length))
  var email = name.substring(0, r.nextInt(5)+1).split(" ")(0) + aggrandizementArray(r.nextInt(aggrandizementArray.length)) + r.nextInt(3000).toString + emailArray(r.nextInt(emailArray.length))
  var bday = (r.nextInt(30)+1).toString + "/" + (r.nextInt(11)+1).toString + "/" + (r.nextInt(100) + 1915).toString
  var city = nameArray(r.nextInt(nameArray.length)) + townArray(r.nextInt(townArray.length))

  var socialFactor = 1 + r.nextInt (30)
  var loudFactor = 1 + r.nextInt (60)
  var lurkFactor = 1 + r.nextInt (50)
  var fluxRate = 1 + r.nextInt(5)

  var friendCap = (200*socialFactor)/15

  if (worldSize < 10000)
    friendCap = (friendCap*worldSize)/10000 + 2

  var listOfFriends : Array[String] = new Array[String](1)
  var listOfPages = new ListBuffer[String]
  var albumsCreated = 0

  // KEYS

  val kpg = KeyPairGenerator.getInstance("RSA")
  kpg.initialize(1024)
  val kp = kpg.genKeyPair
  val publicKey = kp.getPublic
  val privateKey = kp.getPrivate





  var port = (5000 + r.nextInt(50)).toString
  var jsonString = User(email, name, bday, city, Array(192, 168, 1, 1).map(_.toByte)).toJson

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
          var tickTime = (worldSize/50).toInt
          val tick = context.system.scheduler.schedule(2 millis, tickTime millis, self, "Continue") //UNCOMMENT
          //  val tick = context.system.scheduler.schedule(25 millis, 25 millis, self, "Continue") //UNCOMMENT
        }

    case "Continue" =>

      port = (5000 + r.nextInt(50)).toString
      serverIP = baseIP + port + "/"

      requestType match {

        case "doNothing" =>


          socialFactor += 5*fluxRate
          loudFactor += 5*fluxRate
          lurkFactor += 3*fluxRate

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

          lurkFactor -= fluxRate*fluxRate

        case "addNewFriend" =>

          // I want to add a random friend of friend

          val selectedFriend = listOfFriends(r.nextInt(listOfFriends.length))

          if (selectedFriend != null && selectedFriend.length() > 2 && listOfFriends.length < friendCap) {
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

          if (r.nextInt(100) > 50) {

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

                    }
                  listOfPages += thisPage
                }
              }
          }

          socialFactor -= fluxRate

        case "upload" =>

          val bis = new BufferedInputStream(new FileInputStream(imageBase(r.nextInt(imageBase.length))))

          val bArray = Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray

          bis.close();

          if (albumsCreated == 0 || (r.nextInt(100) == loudFactor)) {

            for {
              response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + "createAlbum"),entity= HttpEntity(`application/json`, CreateAlbum(email, albumTitles(r.nextInt(albumTitles.length))).toJson.toString))).mapTo[HttpResponse]
            }
              yield {
              }

            albumsCreated = albumsCreated + 1
          }


          if (albumsCreated > 0) {
            var picsToUpload = 1 + r.nextInt(4)
            var albumNumber = (r.nextInt(albumsCreated)+1).toString
            for (i <- 1 to picsToUpload) {
              for {
                response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + "users/" +email+"/albums/"+albumNumber+"/upload"),entity= HttpEntity(`application/json`, Photo(email, "1", bArray).toJson.toString)))
              }
                yield {
                }

            }
          }


        case "lurk" =>

          var viewPageOf = ""

          if (listOfFriends.length < 2 || r.nextInt(100) < 50)
            viewPageOf = email
          else viewPageOf = listOfFriends(r.nextInt(listOfFriends.length))

          for {
            response <- IO(Http).ask(HttpRequest(GET, Uri(serverIP + "users/" + viewPageOf + "/posts?Email="+email))).mapTo[HttpResponse]
          }
            yield {}

          lurkFactor -= fluxRate*fluxRate


        case "wallWrite" =>

          // Special case: Write on a page, create a page if it hasn't been created

          if (r.nextInt(100) > 95) {

            if (listOfPages.length < 3) {
              val pageTitle = pagePrefix(r.nextInt(pagePrefix.length)) + " " + pageSuffix(r.nextInt(pageSuffix.length))
              val pageID = r.nextInt (10000).toString + r.nextInt (10000).toString

              for {
                response <- IO(Http).ask(HttpRequest(POST, Uri(serverIP + "createPage"),entity= HttpEntity(`application/json`, CreatePage(email, pageTitle, pageID).toJson.toString)))
              }
                yield {
                }

              listOfPages += pageID

            }
            if (listOfPages.length > 0) {
              val pageToWriteOn = listOfPages(r.nextInt(listOfPages.length))
              val myPost = postPrefixArray(r.nextInt(postPrefixArray.length)) + " " + postBodyArray(r.nextInt(postBodyArray.length)) + " " + postSuffixArray(r.nextInt(postSuffixArray.length))


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

          loudFactor -= fluxRate*fluxRate

      }


      // Model next behaviour here


      if (listOfFriends.length < 2)
        requestType = "getFriendList"
      else if (r.nextInt(100) < loudFactor) {
        if (r.nextInt(100) > 15)
          requestType = "wallWrite"
        else requestType = "upload"
      }
      else if (r.nextInt(100) < socialFactor  && listOfFriends.length > 2)
        requestType = "addNewFriend"
      else if (r.nextInt(100) < lurkFactor)
        requestType = "lurk"
      else requestType = "doNothing"




  }

  def encryptRSA(a: Array[Byte], pubKey: Array[Byte]) : Array[Byte] = {

    var pKey: PublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(pubKey));


    val cipher: Cipher = Cipher.getInstance("RSA")
    cipher.init(Cipher.ENCRYPT_MODE, pKey)
    val cipherData: Array[Byte] = cipher.doFinal(a)

    return (cipherData)
  }

  def decryptRSA(a: Array[Byte], priKey: Array[Byte]) : Array[Byte] = {

    var pKey:PrivateKey  = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(priKey));

    val cipher: Cipher = Cipher.getInstance("RSA")
    Cipher.getInstance("RSA").init(Cipher.DECRYPT_MODE, pKey)
    val decryptedData: Array[Byte] = cipher.doFinal(a)

    return (decryptedData)
  }
}
  
  
  
  
  
  
  
  
  
  
  
  
  
