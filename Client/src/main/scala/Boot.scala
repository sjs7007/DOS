import java.security.{SecureRandom, MessageDigest, KeyPairGenerator, KeyFactory, PublicKey, PrivateKey, NoSuchAlgorithmException, InvalidKeyException}

import javax.crypto._
import javax.crypto.{IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException}
import javax.crypto.spec.SecretKeySpec
import java.nio.ByteBuffer
import java.security.spec.{X509EncodedKeySpec, PKCS8EncodedKeySpec}

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.binary.Base64;

import java.math.BigInteger
import javax.crypto.spec.DHParameterSpec

import javax.crypto.spec.IvParameterSpec

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

  case class EncryptedUser(user: User,sign: Array[Byte],pubkey: Array[Byte])

  object EncryptedUser extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(EncryptedUser.apply)
  }
  
  case class userPublicKey(Email:String,EncryptedPublicKey : Array[Byte])
  object userPublicKey extends DefaultJsonProtocol {
  implicit val format = jsonFormat2(userPublicKey.apply)
  }

  case class Photo(Email:String, Caption: String, Image: Array[Byte])
  object Photo extends DefaultJsonProtocol {
    implicit  val format = jsonFormat3(Photo.apply)
  }
  
  /*
    case class EncryptedPhoto(encryptedPhotoData: Array[Byte], encryptedAESKey: EncryptedSecretKey, signedHashedEncryptedPhotoData: Array[Byte], fromEmail: String, albumID: String, encryptedKeyMap: Array[Byte])

  object EncryptedPhoto extends DefaultJsonProtocol {
    implicit val format = jsonFormat6(EncryptedPhoto.apply)
  }
  */
  

  case class CreateAlbum (Email:String, Title:Array[Byte], encryptedKeysMap: Array[Byte])
  object CreateAlbum extends DefaultJsonProtocol {
    implicit  val format = jsonFormat3(CreateAlbum.apply)
  }
  
  
   case class AlbumMetaData(Email:String,Title: String,encryptedKeysMap: Array[Byte],initVector : Array[Byte],encryptedSignedHash : Array[Byte])
 object AlbumMetaData extends  DefaultJsonProtocol {
   implicit val format = jsonFormat5(AlbumMetaData.apply)
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
  
  case class keyClass(publicKeyList : Array[userPublicKey],encryptedSignedHash : Array[Byte])
    object keyClass extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(keyClass.apply)
  }

  
  case class encryptedAddress (address: String)
        object Addresses extends DefaultJsonProtocol {
    implicit val format = jsonFormat1(encryptedAddress.apply)
  }
  case class EncryptedSecretKey (AESKeyBytes: Array[Byte], initializationVectorBytes: Array[Byte])
  object EncryptedSecretKey extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(EncryptedSecretKey.apply)
  }

  
    case class EncryptedPost(encryptedPostData: Array[Byte], initVector: Array[Byte], signedHashedEncryptedPostData: Array[Byte], fromEmail: String, encryptedToEmail: Array[Byte], encryptedKeyMap: Array[Byte])

  object EncryptedPost extends DefaultJsonProtocol {
    implicit val format = jsonFormat6(EncryptedPost.apply)
  }
  
  
  case class EncryptedPostKeyPair (EncryptedPostData: Array[Byte], EncryptedKey: Array[Byte])
      object EncryptedPostKeyPair extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(EncryptedPostKeyPair.apply)
  }
  
    
  case class InitDH (Email: String, KeyBytes: Array[Byte])
  
  object InitDH extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(InitDH.apply)
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

  var worldSize = 1

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
  implicit val timeout: Timeout = 300.seconds

  var baseIP = "http://192.168.0.28:"
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

  val pubBytes = publicKey.getEncoded()
  val priBytes = privateKey.getEncoded()
  
    val KeyGen = KeyGenerator.getInstance("AES")
    KeyGen.init(128)
 

  var port = "8087"//(5000 + r.nextInt(50)).toString
  
  //var encUser = EncryptedUser(userObj, encryptRSA(sha256(serialize(userObj)), pubBytes), pubBytes)
  
  
  var serverIP = ""

  import context.dispatcher
var serverPublicKey: PublicKey = null


  def receive = {

    case Start =>

      //User behaviour definitions

      // Create user

      // EXCHANGE KEYS
      
    var userNo = 0
      
    while (userNo < 2) {
    
      var friendKeys = new ConcurrentHashMap[String,Array[Byte]]()

    Thread.sleep (2000)
    
    if (userNo == 0)
    email = "Bob"
    else email = "Dick"
    
      var userObj = User(email, name, bday, city, pubBytes)
      
  var encUser = EncryptedUser(userObj, encryptPrivateRSA(sha256(serialize(userObj)), priBytes), pubBytes)
  
    val skip1024ModulusBytes = Array(0xF4.toByte, 0x88.toByte, 0xFD.toByte, 0x58.toByte, 0x4E.toByte, 0x49.toByte, 0xDB.toByte, 0xCD.toByte, 0x20.toByte, 0xB4.toByte, 0x9D.toByte, 0xE4.toByte, 0x91.toByte, 0x07.toByte, 0x36.toByte, 0x6B.toByte, 0x33.toByte, 0x6C.toByte, 0x38.toByte, 0x0D.toByte, 0x45.toByte, 0x1D.toByte, 0x0F.toByte, 0x7C.toByte, 0x88.toByte, 0xB3.toByte, 0x1C.toByte, 0x7C.toByte, 0x5B.toByte, 0x2D.toByte, 0x8E.toByte, 0xF6.toByte, 0xF3.toByte, 0xC9.toByte, 0x23.toByte, 0xC0.toByte, 0x43.toByte, 0xF0.toByte, 0xA5.toByte, 0x5B.toByte, 0x18.toByte, 0x8D.toByte, 0x8E.toByte, 0xBB.toByte, 0x55.toByte, 0x8C.toByte, 0xB8.toByte, 0x5D.toByte, 0x38.toByte, 0xD3.toByte, 0x34.toByte, 0xFD.toByte, 0x7C.toByte, 0x17.toByte, 0x57.toByte, 0x43.toByte, 0xA3.toByte, 0x1D.toByte, 0x18.toByte, 0x6C.toByte, 0xDE.toByte, 0x33.toByte, 0x21.toByte, 0x2C.toByte, 0xB5.toByte, 0x2A.toByte, 0xFF.toByte, 0x3C.toByte, 0xE1.toByte, 0xB1.toByte, 0x29.toByte, 0x40.toByte, 0x18.toByte, 0x11.toByte, 0x8D.toByte, 0x7C.toByte, 0x84.toByte, 0xA7.toByte, 0x0A.toByte, 0x72.toByte, 0xD6.toByte, 0x86.toByte, 0xC4.toByte, 0x03.toByte, 0x19.toByte, 0xC8.toByte, 0x07.toByte, 0x29.toByte, 0x7A.toByte, 0xCA.toByte, 0x95.toByte, 0x0C.toByte, 0xD9.toByte, 0x96.toByte, 0x9F.toByte, 0xAB.toByte, 0xD0.toByte, 0x0A.toByte, 0x50.toByte, 0x9B.toByte, 0x02.toByte, 0x46.toByte, 0xD3.toByte, 0x08.toByte, 0x3D.toByte, 0x66.toByte, 0xA4.toByte, 0x5D.toByte, 0x41.toByte, 0x9F.toByte, 0x9C.toByte, 0x7C.toByte, 0xBD.toByte, 0x89.toByte, 0x4B.toByte, 0x22.toByte, 0x19.toByte, 0x26.toByte, 0xBA.toByte, 0xAB.toByte, 0xA2.toByte, 0x5E.toByte, 0xC3.toByte, 0x55.toByte, 0xE9.toByte, 0x2F.toByte, 0x78.toByte, 0xC7.toByte)
    val skip1024Modulus = new BigInteger(1, skip1024ModulusBytes)
    val skip1024Base = BigInteger.valueOf(2)
    val dhSkipParamSpec = new DHParameterSpec(skip1024Modulus, skip1024Base)
    
    var done = false
    
    
        
    println("ALICE: Generate DH keypair ...")
    val aliceKeyPairGen = KeyPairGenerator.getInstance("DH")
    aliceKeyPairGen.initialize(dhSkipParamSpec)
    val aliceKeyPair = aliceKeyPairGen.generateKeyPair()
    println("ALICE: Initialization ...")
    val aliceKeyAgree = KeyAgreement.getInstance("DH")
    aliceKeyAgree.init(aliceKeyPair.getPrivate)
    val alicePublicKeyBytes = aliceKeyPair.getPublic.getEncoded()
    
    var idh : InitDH = InitDH (email, alicePublicKeyBytes)
        
      for {
        response <- IO(Http).ask(HttpRequest(POST, Uri(baseIP + port + "/createSymmetricKey"),entity= HttpEntity(`application/json`, idh.toJson.toString))).mapTo[HttpResponse]
      }
        yield {
      
        var bobPublicKey: PublicKey = KeyFactory.getInstance("DH").generatePublic(new  X509EncodedKeySpec(BigInt(response.entity.asString, 16).toByteArray))

        aliceKeyAgree.doPhase(bobPublicKey, true)
      
                  val aliceSharedSecret = aliceKeyAgree.generateSecret()
                  
                  //println (toHexString(aliceSharedSecret))

          aliceKeyAgree.doPhase(bobPublicKey, true)
      
          val aliceAESKey : SecretKey = aliceKeyAgree.generateSecret("AES")
          
                      val randomNumberGenerator = new SecureRandom()
                      val bytes = Array.ofDim[Byte](16)
                      randomNumberGenerator.nextBytes(bytes)
                      val initVector = new IvParameterSpec(bytes)

                    for {
        response <- IO(Http).ask(HttpRequest(POST, Uri(baseIP + port + "/getPublicKey"),entity= HttpEntity(`application/json`, InitDH (email, initVector.getIV()).toJson.toString))).mapTo[HttpResponse]
      } yield {
      
          if (!response.entity.asString.equals("null")) {
         
              val AesCipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
              AesCipher.init(Cipher.DECRYPT_MODE, aliceAESKey, initVector)
              val decryptedKey : Array[Byte] = AesCipher.doFinal(BigInt(response.entity.asString, 16).toByteArray)

         serverPublicKey = KeyFactory.getInstance("RSA").generatePublic(new  X509EncodedKeySpec(decryptedKey))
          done = true
                            //println ("\n\n" + toHexString(serverPublicKey.getEncoded()))

        } else println ("NULL")
      
                  
      

      
      // END OF KEY EXCHANGE
      
      for {
        response <- IO(Http).ask(HttpRequest(POST, Uri(baseIP + port + "/createUser"),entity= HttpEntity(`application/json`, encUser.toJson.toString))).mapTo[HttpResponse]
      }
        yield {
          allEmails += email
          var tickTime = (worldSize/50).toInt
          //val tick = context.system.scheduler.schedule(2 millis, tickTime millis, self, "Continue") //UNCOMMENT
          
          
          // ADD FRIEND OR GET THEIR PUBLIC RSA KEY
          
        for {
          response <- IO(Http).ask(HttpRequest(POST, Uri(baseIP + port + "/sendFriendRequest"),entity= HttpEntity(`application/json`, FriendRequest(email, "Bob").toJson.toString))).mapTo[HttpResponse]
          }
        yield {
        
          if (!response.entity.asString.equals("null")) {
          
          try {
          friendKeys.put("Bob",KeyFactory.getInstance("RSA").generatePublic(new  X509EncodedKeySpec(BigInt(response.entity.asString, 16).toByteArray)).getEncoded())
           }
              catch {
              case x : Exception => println ("ERROR WHEN FRIEND REQUEST " + x.toString)
              }
              
        } else println ("NO SUCH USER MATE!")
          
          
          // POST TO SELF
          
              var textPost = selfPostFirst(r.nextInt(selfPostFirst.length)) + " " + selfPostSecond(r.nextInt(selfPostSecond.length)) + " " + selfPostThird(r.nextInt(selfPostThird.length))

              var target = email
              
            var post = "mera naam chin chin choo"
            
            // Create AES Key
            
              val AESKey = KeyGen.generateKey
              val AesCipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
              val randomNumberGenerator: SecureRandom = new SecureRandom
              val bytes: Array[Byte] = new Array[Byte](16)
              randomNumberGenerator.nextBytes(bytes)    
              val initializationVector: IvParameterSpec = new IvParameterSpec(bytes)
              // get bytes with initializationVector.getIV()
            val AESbytes = AESKey.getEncoded()
            
            println ("INITIALIZATION VECTOR TO HEX STRING: " + toHexString(initializationVector.getIV()))
            
            println ("ABOUT TO START POST!")
              
            // Get public keys of people I want to share it with
                                       
              var encTo :  encryptedAddress = encryptedAddress (target)
              
              var esk : EncryptedSecretKey =  EncryptedSecretKey(encryptRSA(AESbytes, pubBytes), encryptRSA(initializationVector.getIV(), pubBytes))
            
            
            val aesEncryptedWallpost = encryptAES(post.getBytes, AESbytes, initializationVector)

            val hashedSignedEncryptedPost = encryptPrivateRSA(sha256(encryptAES(serialize(post), AESbytes, initializationVector)), priBytes)

            val rsaEncryptedTo = encryptRSA(serialize(encTo), serverPublicKey.getEncoded())
            
            
            var encryptedAESKeys = new ConcurrentHashMap [String, Array[Byte]] ()
            
              try {

                  encryptedAESKeys.put("Bob", encryptRSA(AESbytes, friendKeys.get("Bob")))
                  
                  if (!encryptedAESKeys.containsKey(email)) {
                  encryptedAESKeys.put(email, encryptRSA(AESbytes, pubBytes))
                              var theString = toHexString (encryptedAESKeys.get(email))

                              println ("RSA HEXSTRING OF ENCRYPTED KEY OF " + email + "IS: " + theString)
            
                  }
              }
              catch {
              case x : Exception => println ("ERROR WHEN ENCRYPTING TO RSA! " + x.toString)
              }
            
            
            
            
            
            
            var encPost = EncryptedPost(aesEncryptedWallpost, initializationVector.getIV(), hashedSignedEncryptedPost, email, rsaEncryptedTo, serialize(encryptedAESKeys))
           
            for {

              response <- IO(Http).ask(HttpRequest(POST, Uri(baseIP + port + "/wallWrite"),entity= HttpEntity(`application/json`, encPost.toJson.toString)))
            }
              yield {
              
             

             
             println ("WALL WRITING DONE BRO")
          
          // END POST
          
          // Fetch posts
          
          var postIDs = new Array[String](10) 
          
          for {            
           response <- IO(Http).ask(HttpRequest(GET, Uri(baseIP + port + "/users/Dick/ids?Email=Dick"))).mapTo[HttpResponse]
          }
          yield {
          
             println ("FETCHING DONE BRO")
          println ("PAKAWANAN FINAL STRING: " + response.entity.asString)
          postIDs = response.entity.asString.substring(11,response.entity.asString.length-1).split(",").map(_.trim)
          println ("postIDs 0 is " + postIDs(0))

          var postToView = postIDs(0)
          println ("Gonna look at "+postToView)
          for {
            response <- IO(Http).ask(HttpRequest(GET, Uri(baseIP + port + "/users/Dick/posts/" +postToView.toString +"?Email=Dick" ))).mapTo[HttpResponse]

         }
          yield {
          postIDs = response.entity.asString.split(",")
          println ("GOT DA POST!: "+ new String (BigInt(postIDs(0), 16).toByteArray   ))
          


                              
          val encPostBytes = BigInt(postIDs(0), 16).toByteArray                                 
          
          val initVectorBytes = BigInt(postIDs(1), 16).toByteArray   
          var encKeyBytes = BigInt(postIDs(2), 16).toByteArray   
          
          
          var stringOfKey = new String(encKeyBytes)
          
          println ("THE HEX STRING OF RSA ENCRYPTED AES KEY IS:" + toHexString(encKeyBytes))          
          
          
          val ivKey = new IvParameterSpec(initVectorBytes)
         
          val postAESKey = decryptRSA(encKeyBytes, priBytes)
         println ("THE KEY IS: " + toHexString(postAESKey))
         
         
          
          
          val decryptedPost = decryptAES(encPostBytes, postAESKey, ivKey)
          
          
          
          println ("DECRYPTED POST IS: " + new String (decryptedPost))
          
          
          }

             }

          }
                   
           }         
          // End Post fetching
          
          // Image
          
          
          val bis = new BufferedInputStream(new FileInputStream(imageBase(r.nextInt(imageBase.length))))

          val bArray = Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray

          bis.close();
          
          
           val albumAESKey = KeyGen.generateKey
              val albumRNG: SecureRandom = new SecureRandom
              val albytes: Array[Byte] = new Array[Byte](16)
              albumRNG.nextBytes(bytes)    
              val albumIV: IvParameterSpec = new IvParameterSpec(albytes)
              // get bytes with initializationVector.getIV()
            val albumAESbytes = albumAESKey.getEncoded()
            
            
             var albumEncryptedAESKeys = new ConcurrentHashMap [String, Array[Byte]] ()
            
              try {

                  albumEncryptedAESKeys.put("Bob", encryptRSA(albumAESbytes, friendKeys.get("Bob")))
                  
                  if (!albumEncryptedAESKeys.containsKey(email)) {
                  albumEncryptedAESKeys.put(email, encryptRSA(albumAESbytes, pubBytes))
            
                  }
              }
              catch {
              case x : Exception => println ("ERROR WHEN ENCRYPTING TO RSA! " + x.toString)
              }
            
            
            
            
          
            val album = AlbumMetaData (email, "Al-Akir the Windlord", serialize(albumEncryptedAESKeys), albumIV.getIV(), encryptPrivateRSA(sha256(email+"Al-Akir the Windlord"), priBytes))



            for {
              response <- IO(Http).ask(HttpRequest(POST, Uri(baseIP + port + "/createAlbum"),entity= HttpEntity(`application/json`, album.toJson.toString))).mapTo[HttpResponse]
            }
              yield {
              println (response.entity.asString)
              }


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
        }
        
        
        }
                userNo = userNo + 1

        }
        
        
        

  
 
  
  def hexStringToByteArray(s: String): Array[Byte] = {
    val len = s.length
    val data = Array.ofDim[Byte](len / 2)
    var i = 0
    while (i < len) {
      data(i / 2) = ((java.lang.Character.digit(s.charAt(i), 16) << 4) + java.lang.Character.digit(s.charAt(i + 1), 16)).toByte
      i += 2
    }
    data
  }
  
  
 def toHexString(byteArr: Array[Byte]):String = {
   BigInt(byteArr).toString(16)
 }

  def byte2hex(b: Byte, buf: StringBuffer) {
    val hexChars = Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
    val high = ((b & 0xf0) >> 4)
    val low = (b & 0x0f)
    buf.append(hexChars(high))
    buf.append(hexChars(low))
  }
      


  }

  def encryptRSA(a: Array[Byte], pubKey: Array[Byte]) : Array[Byte] = {

  try {
    var pKey: PublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(pubKey));

    val cipher: Cipher = Cipher.getInstance("RSA")
    cipher.init(Cipher.ENCRYPT_MODE, pKey)
    val cipherData: Array[Byte] = cipher.doFinal(a)
    return (cipherData)
    }
    
  catch {
    case x: Exception => {
      System.out.println(x)
    }
    }
    
    return null
    
  }
  
    def encryptPrivateRSA(a: Array[Byte], priKey: Array[Byte]) : Array[Byte] = {

  try {
    var pKey: PrivateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(priKey));

    val cipher: Cipher = Cipher.getInstance("RSA")
    cipher.init(Cipher.ENCRYPT_MODE, pKey)
    val cipherData: Array[Byte] = cipher.doFinal(a)

    return (cipherData)
    }
  catch {
    case x: Exception => {
      System.out.println(x)
    }
    }
    return null
  }

  def decryptRSA(a: Array[Byte], priKey: Array[Byte]) : Array[Byte] = {
  
  try {
  
    var pKey:PrivateKey  = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(priKey));

        val cipher: Cipher = Cipher.getInstance("RSA")

    cipher.init(Cipher.DECRYPT_MODE, pKey)
    val decryptedData: Array[Byte] = cipher.doFinal(a)

    return (decryptedData)
  }
  catch {
    case x: Exception => {
      System.out.println(x)
    }
    }
    return null
  }
  
  def decryptPublicRSA(a: Array[Byte], pubKey: Array[Byte]) : Array[Byte] = {
  
  try {
      var pKey: PublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(pubKey));

        val cipher: Cipher = Cipher.getInstance("RSA")

    cipher.init(Cipher.DECRYPT_MODE, pKey)
    val decryptedData: Array[Byte] = cipher.doFinal(a)

    return (decryptedData)
  }
  catch {
    case x: Exception => {
      System.out.println(x)
    }
    }
    return null
  }
  
  
  def encryptAES(a: Array[Byte], AESbytes: Array[Byte], initializationVector: IvParameterSpec) : Array[Byte] = {
  
  
  try {
    val AESKey : SecretKeySpec  = new SecretKeySpec(AESbytes, "AES");

    val AesCipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")

    AesCipher.init(Cipher.ENCRYPT_MODE, AESKey, initializationVector)
    val encryptedData: Array[Byte] = AesCipher.doFinal(a)

    return (encryptedData)
    }
    
  catch {
    case x: Exception => {
      System.out.println(x)
    }
    }
    
    return (null)
  }
  
    def decryptAES(a: Array[Byte], AESbytes: Array[Byte], initializationVector: IvParameterSpec) : Array[Byte] = {
    
        val AESKey : SecretKeySpec  = new SecretKeySpec(AESbytes, "AES");

    
    val AesCipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")

  
  
      AesCipher.init(Cipher.DECRYPT_MODE, AESKey, initializationVector)
    val decryptedData: Array[Byte] = AesCipher.doFinal(a)


    return (decryptedData)
  }
  
  
    def serialize(obj: AnyRef): Array[Byte] = {
    val b = new ByteArrayOutputStream()
    val o = new ObjectOutputStream(b)
    o.writeObject(obj)
    val r = b.toByteArray()
    
    return (r)
  }
  
  def deserialize(bytes: Array[Byte]): AnyRef = {
    val b = new ByteArrayInputStream(bytes)
    val o = new ObjectInputStream(b)
    o.readObject()
  }

  
  def sha256(s: String) : Array[Byte] = {
    val a = MessageDigest.getInstance("SHA-256")
    a.update(s.getBytes)
    return (a.digest)
}

  def sha256(s: Array[Byte]) : Array[Byte] = {
    val a = MessageDigest.getInstance("SHA-256")
    a.update(s)
    return (a.digest)

}

}

