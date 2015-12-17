/**
 * Created by shinchan on 11/6/15.
 */

import java.io._
import java.math.BigInteger
import java.security._
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.util.concurrent.ConcurrentHashMap
import javax.crypto._
import javax.crypto.interfaces.DHPublicKey
import javax.crypto.spec.{DHParameterSpec, IvParameterSpec}

import MyJsonProtocol._
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.can.server.Stats
import spray.http.MediaTypes._
import spray.http.MultipartFormData
import spray.httpx.SprayJsonSupport._
import spray.routing._

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
//import java.util

import java.nio.file.{Files, Paths}

import au.com.bytecode.opencsv.CSVWriter;

object commonVars {
  //list of all users : make it more efficient
  //var users = scala.collection.immutable.Vector[User]()
 // var users = new ConcurrentHashMap[String,User]()

  var serverPublicKey : PublicKey = null
  var serverPrivateKey : PrivateKey = null

  try {
    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(1024)
    val kp = kpg.genKeyPair
    println("RSA Key Pairs generated for server.")
    serverPublicKey = kp.getPublic
    serverPrivateKey = kp.getPrivate
  }
  catch {
    case x: UnsupportedEncodingException => {
      System.out.println(x.toString)
    }
    case x: NoSuchAlgorithmException => {
      System.out.println(x.toString)
    }
    case x: NoSuchPaddingException => {
      System.out.println(x.toString)
    }
    case x: BadPaddingException => {
      System.out.println(x.toString)
    }
    case x: InvalidKeyException => {
      System.out.println(x.toString)
    }
    case x: IllegalBlockSizeException => {
      System.out.println(x.toString)
    }
  }



    var userSymmetricKey = new ConcurrentHashMap[String,SecretKey]()
    var userPublicKey = new ConcurrentHashMap[String,Array[Byte]]()

    var users = new ConcurrentHashMap[String,EncryptedUser]()
  //var friendLists = new HashMap[String,Set[String]] with MultiMap[String,String]


  //concurrent to allow simultaneous access : stores friends of each email
  var friendLists = new ConcurrentHashMap[String,ListBuffer[String]]()

  //store all friend requests in the buffer below.
  var friendRequests = new ConcurrentHashMap[String,ListBuffer[String]]()

  //store all posts in the buffer below
  //var userPosts = new ConcurrentHashMap[String,ConcurrentHashMap[String,fbPost]]()
  var userPosts = new ConcurrentHashMap[String,ConcurrentHashMap[String,EncryptedPost]]()

  //store all album meta data
  //map user email to a map of albums
  var albumDirectory = new ConcurrentHashMap[String,ConcurrentHashMap[String,AlbumMetaData]]()

  //store content image id of each image
  //map album id to a map of post ids
  // var albumContent = new ConcurrentHashMap[String,ConcurrentHashMap[String,ImageMetaData]]()
  var albumContent = new ConcurrentHashMap[String,ConcurrentHashMap[String,String]]()

  //directory of all pages
  //key : pageID, value : page info(adminEmail,title,pageID)
  var pageDirectory = new ConcurrentHashMap[String,Page]()
  //var pageDirectory = new scala.util.HashMap[String,Page]()

  //used to get random page
  var pageIDs = new ListBuffer[String]

  //store content of each page : key pageID value : list of posts on the page
  var pageContent = new ConcurrentHashMap[String,ConcurrentHashMap[String,pagePost]]()

  //store list of followers for each pageID. only people following page can comment
  var pageFollowers = new ConcurrentHashMap[String,ListBuffer[String]]()

  //map any userpostid/fbpost to list of comments
  //var comments = new ConcurrentHashMap[String,ListBuffer[Comment]]()

 // var count =0

  var stats = "empty"

  //var httpListenerBuffer = new ListBuffer[Option[ActorRef]]()
  var httpListenerBuffer = new ListBuffer[String]()

  var portStats = new ConcurrentHashMap[Int,Stats]()

  var summary = ""

  var nPagePosts = 0
  var nUserPosts = 0
  var nImageUploads = 0
  var nAlbums = 0
  var nFriendRequests = 0
}

// simple actor that handles the routes.
class SJServiceActor extends Actor with HttpService with ActorLogging {

  import commonVars._ // ExecutionContext for the futures and scheduler

  var httpListener : Option[ActorRef] = None
  // required as implicit value for the HttpService
  // included from SJService
  log.debug("Actor started")
  def actorRefFactory = context

  import context.dispatcher

  context.system.scheduler.schedule(10 seconds,5 seconds,self,"getStats")



  /*  def httpReceive: Receive = runRoute(...)
    def handle: Receive = ...

    def receive = handle orElse httpReceive */

  // we don't create a receive function ourselve, but use
  // the runRoute function from the HttpService to create
  // one for us, based on the supplied routes.
  def httpReceive = runRoute(facebookStuff)

  def handle : Receive = {
    case Http.Bound(_) =>
      httpListener = Some(sender)
    //  log.debug("dis listener : "+httpListener.get.path.toString())
      println("bound")
   //   log.debug("can kill when I want to now")
      httpListenerBuffer += httpListener.get.path.toString()
      //sender ! Http.Unbind(10 seconds)
    case Http.Unbound =>
      println("unbound")
      context.stop(self)

   //case getStats => getServerStats()
    case "getStats" =>
      updateAllStats()
     // println(getSummary())
  }


  def receive = handle orElse httpReceive


  val facebookStuff = {
    pathPrefix("createSymmetricKey") {
      post {
        //respondWithMediaType(`application/json`) {
          entity (as[InitDH]) {
            publicKeyBytes => requestContext =>
              val responder = createResponder(requestContext)
              //complete {
                log.debug("Received request for symmetric key generation from : "+publicKeyBytes.Email)
                val skip1024ModulusBytes = Array(0xF4.toByte, 0x88.toByte, 0xFD.toByte, 0x58.toByte, 0x4E.toByte, 0x49.toByte, 0xDB.toByte, 0xCD.toByte, 0x20.toByte, 0xB4.toByte, 0x9D.toByte, 0xE4.toByte, 0x91.toByte, 0x07.toByte, 0x36.toByte, 0x6B.toByte, 0x33.toByte, 0x6C.toByte, 0x38.toByte, 0x0D.toByte, 0x45.toByte, 0x1D.toByte, 0x0F.toByte, 0x7C.toByte, 0x88.toByte, 0xB3.toByte, 0x1C.toByte, 0x7C.toByte, 0x5B.toByte, 0x2D.toByte, 0x8E.toByte, 0xF6.toByte, 0xF3.toByte, 0xC9.toByte, 0x23.toByte, 0xC0.toByte, 0x43.toByte, 0xF0.toByte, 0xA5.toByte, 0x5B.toByte, 0x18.toByte, 0x8D.toByte, 0x8E.toByte, 0xBB.toByte, 0x55.toByte, 0x8C.toByte, 0xB8.toByte, 0x5D.toByte, 0x38.toByte, 0xD3.toByte, 0x34.toByte, 0xFD.toByte, 0x7C.toByte, 0x17.toByte, 0x57.toByte, 0x43.toByte, 0xA3.toByte, 0x1D.toByte, 0x18.toByte, 0x6C.toByte, 0xDE.toByte, 0x33.toByte, 0x21.toByte, 0x2C.toByte, 0xB5.toByte, 0x2A.toByte, 0xFF.toByte, 0x3C.toByte, 0xE1.toByte, 0xB1.toByte, 0x29.toByte, 0x40.toByte, 0x18.toByte, 0x11.toByte, 0x8D.toByte, 0x7C.toByte, 0x84.toByte, 0xA7.toByte, 0x0A.toByte, 0x72.toByte, 0xD6.toByte, 0x86.toByte, 0xC4.toByte, 0x03.toByte, 0x19.toByte, 0xC8.toByte, 0x07.toByte, 0x29.toByte, 0x7A.toByte, 0xCA.toByte, 0x95.toByte, 0x0C.toByte, 0xD9.toByte, 0x96.toByte, 0x9F.toByte, 0xAB.toByte, 0xD0.toByte, 0x0A.toByte, 0x50.toByte, 0x9B.toByte, 0x02.toByte, 0x46.toByte, 0xD3.toByte, 0x08.toByte, 0x3D.toByte, 0x66.toByte, 0xA4.toByte, 0x5D.toByte, 0x41.toByte, 0x9F.toByte, 0x9C.toByte, 0x7C.toByte, 0xBD.toByte, 0x89.toByte, 0x4B.toByte, 0x22.toByte, 0x19.toByte, 0x26.toByte, 0xBA.toByte, 0xAB.toByte, 0xA2.toByte, 0x5E.toByte, 0xC3.toByte, 0x55.toByte, 0xE9.toByte, 0x2F.toByte, 0x78.toByte, 0xC7.toByte)
                val skip1024Modulus = new BigInteger(1, skip1024ModulusBytes)
                val skip1024Base = BigInteger.valueOf(2)

                val alicePublicKeyBytes = publicKeyBytes.KeyBytes
                //This portion of code should be on other end : bob or server in algo
                //Bob will get the public key in bytes
                //convert to public key formate now
                val bobKeyFactory: KeyFactory = KeyFactory.getInstance("DH")
                var x509KeySpec: X509EncodedKeySpec = new X509EncodedKeySpec((alicePublicKeyBytes))
                val alicePublicKey: PublicKey = bobKeyFactory.generatePublic(x509KeySpec)

                /*
                * Bob gets the DH parameters associated with Alice's public key.
                * He must use the same parameters when he generates his own key
                * pair.
                */
                val dhParameterSpec: DHParameterSpec = (alicePublicKey.asInstanceOf[DHPublicKey]).getParams

                //Bob will use this above info to create his own DH key pair
                System.out.println("Bob : Generate DH Keypair....")
                val bobKeyPairGen: KeyPairGenerator = KeyPairGenerator.getInstance("DH")
                bobKeyPairGen.initialize(dhParameterSpec)
                val bobKeyPair: KeyPair = bobKeyPairGen.generateKeyPair

                // Bob creates and initializes his DH KeyAgreement object
                System.out.println("BOB: Initialization ...")
                val bobKeyAgree: KeyAgreement = KeyAgreement.getInstance("DH")
                bobKeyAgree.init(bobKeyPair.getPrivate)

                //Bob encodes his public key and sends to Alice
                val bobPublicKeyBytes: Array[Byte] = bobKeyPair.getPublic.getEncoded

               /*
               * Bob uses Alice's public key for the first (and only) phase
               * of his version of the DH
               * protocol.
               */
              System.out.println("BOB: Execute PHASE1 ...")
              bobKeyAgree.doPhase(alicePublicKey, true)

              val bobSharedSecret: Array[Byte] = bobKeyAgree.generateSecret()
              log.debug("Hex form : "+toHexString(bobSharedSecret))
              //userSharedSecret.put(publicKeyBytes.Email,bobSharedSecret)

              bobKeyAgree.doPhase(alicePublicKey, true)
              val bobAesKey: SecretKey = bobKeyAgree.generateSecret("AES")
              userSymmetricKey.put(publicKeyBytes.Email,bobAesKey)

              //requestContext.complete(bobPublicKeyBytes)
              requestContext.complete(byteToString(bobPublicKeyBytes))
             // }
          }
       // }
      }
    }~
    pathPrefix("getPublicKey") {
      post {
        entity(as[InitDH]) {
          initVectorBytes => requestContext =>
            val responder = createResponder(requestContext)
            //generate symmetric key based on shared secret corresponding to username in shared secret
            val AESCipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
            if(userSymmetricKey.containsKey(initVectorBytes.Email)) {
              //AESCipher.init(Cipher.ENCRYPT_MODE, bobAesKey, initVector)
              val AESKey : SecretKey = userSymmetricKey.get(initVectorBytes.Email)
              if(initVectorBytes.KeyBytes==null) {
                log.debug("null hai init")
              }
              if(serverPublicKey==null) {
                log.debug("Server public key is null.")
              }
              AESCipher.init(Cipher.ENCRYPT_MODE,AESKey,new IvParameterSpec(initVectorBytes.KeyBytes))
              val encryptedServerPublicKey: Array[Byte] = AESCipher.doFinal(serverPublicKey.getEncoded)
              log.debug("Hex form of public key sent : "+toHexString(serverPublicKey.getEncoded))
              requestContext.complete(byteToString(encryptedServerPublicKey))
              //requestContext.complete(byteToString(serverPublicKey.getEncoded))
            }
            else {
              requestContext.complete("null")
            }
        }
      }
    }~
    pathPrefix("summary") {
      get {
        respondWithMediaType(`application/json`) {
          complete {
            updateAllStats()
            getSummary()
          }
        }
      }
    }~
    pathPrefix("portStats") {
      get {
        parameters('port.as[Int]) {
          port =>
          respondWithMediaType(`application/json`) {
            complete {
              getPortStats(port)
            }
          }
        }
      }
    }~
    pathPrefix("allStats") {
      pathEnd {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              //"d"
              portStats.toString
            }
          }
        }
      }
    }~
    pathPrefix("userStats") {
      pathEnd {
        get {
          respondWithMediaType(`application/json`) {
            complete {
             // count = count+1
              users.size().toString()//+" "+count.toString
            }
          }
        }
      }
    } ~
    pathPrefix("userList") {
      pathEnd {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              //var A = new Map[String,User]()
              //count=count+1
              users.toString()
            }
          }
        }
      }
    } ~
    pathPrefix("pageDirectory") {
      pathEnd {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              //count=count+1
              pageDirectory.toString
            }
          }
        }
      }
    } ~
    //create stuff
    pathPrefix("createUser") {
      pathEnd {
        post {
          log.debug("inhere")
          //entity(as[User]) { user => requestContext =>
          entity (as[EncryptedUser]) { encUser => requestContext =>
            val responder = createResponder(requestContext)
            //count=count+1
            createUser(encUser) match {
              case "doesNotExist" => responder ! UserCreated(encUser.user.Email)
              case "alreadyExists" => responder ! UserAlreadyExists
              case "DigitalSignFailed" => responder ! DigitalSignFailed
            }
          }
        }
      }
    } ~
    pathPrefix("createPage") {
      pathEnd {
        post {
          entity(as[Page]) {
            page => requestContext =>
              val responder = createResponder(requestContext)
              //count=count+1
              createPage(page) match  {
                case true => responder ! PageCreated(page.Title)
                case _ => responder ! PageCreationFailed
              }
          }
        }
      }
    } ~
    pathPrefix("createAlbum") {
      pathEnd {
        post {
          entity(as[AlbumMetaData]) {
            albumMetaData => requestContext =>
              val responder = createResponder(requestContext)
              //count=count+1
              createAlbum(albumMetaData) match {
                case true => responder ! AlbumCreated(albumMetaData.Title)
                case _ => responder ! AlbumCreationFailed
              }
          }
        }
      }
    } ~
    pathPrefix("pages") {
      path("ids") {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              //count=count+1
              pageDirectory.keySet().toString
            }
          }
        }
      } ~
      path("random") {
        var temp = pageDirectory.keySet()
        var r = new scala.util.Random()
        get {

          respondWithMediaType(`application/json`) {
            complete {
              //count=count+1
              if(pageIDs.size ==0) {
                //"No pages created till now."
                log.debug("No pages created till now.")
                "noPagesExist"
              }
              else {
                pageIDs(r.nextInt(pageIDs.size))
              }
            }
          }
        }
      }~
      pathPrefix(Segment) {
        pageID =>
          if(!doesPageExist(pageID)) {
            respondWithMediaType(`application/json`) {
              complete {
                //count=count+1
                "Page doesn't exist."
              }
            }
          }
          else {
            pathEnd {
              get {
                respondWithMediaType(`application/json`) {
                  complete {
                    //count=count+1
                    pageContent.get(pageID).toString()
                  }
                }
              }
            } ~
              pathPrefix("createPost") {
                pathEnd {
                  post {
                    entity(as[pagePost]) {
                      postData => requestContext =>
                        val responder = createResponder(requestContext)
                        //count=count+1
                        createPagePost(postData, pageID) match {
                          case "posted" => responder ! PostSuccess
                          case "invalidPost" => responder ! PostFail
                          case "notFollowing" => responder ! PostFailNotFollowing
                        }
                    }
                  }
                }
              } ~
              pathPrefix("follow") {
                pathEnd {
                  post {
                    entity(as[UserID]) {
                      user => requestContext =>
                        val responder = createResponder(requestContext)
                        //count=count+1
                        addFollower(user.Email, pageID) match {
                          case "invalidUser" => responder ! UserNotPresent
                          //case "invalidPage" => responder ! PageNotPresent
                          case "alreadyFollower" => responder ! AlreadyFollowingPage
                          case "followSuccess" => responder ! FollowSuccess
                        }
                    }
                  }
                }
              } ~
              pathPrefix("followers") {
                pathEnd {
                  get {
                    respondWithMediaType(`application/json`) {
                      complete {
                        //count=count+1
                        pageFollowers.get(pageID).toString()
                      }
                    }
                  }
                }
              } ~
              pathPrefix("posts") {
                pathEnd {
                  get {
                    respondWithMediaType(`application/json`) {
                      complete {
                        //count=count+1
                        pageContent.get(pageID).toString()
                      }
                    }
                  }
                }
              }
          }
      }
    } ~
    pathPrefix("sendFriendRequest") {
      pathEnd {
        post {
          entity(as[FriendRequest]) { friendRequest => requestContext =>
            val responder = createResponder(requestContext)
            //count=count+1
            sendFriendRequest(friendRequest) match {
              case "alreadyFriends" => 
               val publicKeybytes = byteToString(userPublicKey.get((friendRequest.toEmail)))
                responder ! FriendRequestSent(publicKeybytes)

              case "userNotPresent" => 
              // val publicKeybytes = byteToString(userPublicKey.get((friendRequest.toEmail)))
                responder ! FriendRequestSent(byteToString(serverPublicKey.getEncoded()))
               // responder ! FriendRequestSent("null")

              case "requestSent" =>
                val publicKeybytes = byteToString(userPublicKey.get((friendRequest.toEmail)))
                responder ! FriendRequestSent(publicKeybytes)

              case "cantAddSelf" => 
               val publicKeybytes = byteToString(userPublicKey.get((friendRequest.toEmail)))
                responder ! FriendRequestSent(publicKeybytes)
            }
          }
        }
      }
    } ~
    pathPrefix("users" / Segment) {
      userEmail =>
      if(!doesUserExist(userEmail)) {
        log.debug("User doesn't exist.")
        respondWithMediaType(`application/json`) {
          complete {
            //count=count+1
            "User : "+userEmail+" doesn't exist."
          }
        }
      }
      else {
        //returns list of albums to which user has access to
        pathPrefix("albums") {
          pathEnd {
            //parameters('Email.as[String]) {
            //  fromUser =>
              get {
                parameters('Email.as[String]) {
                  fromUser =>
                    respondWithMediaType(`application/json`) {
                      complete {
                        val albumIdsReturn : ListBuffer[String] = new ListBuffer()
                        //count=count+1
                        //albumDirectory.get(userEmail).toString()
                        //albumDirectory.get(userEmail).
                        val albumIdsList= albumDirectory.get(userEmail).keySet().toArray(new Array[String](albumDirectory.get(userEmail).size()))
                        for(i<-0 until albumIdsList.length) {
                          log.debug("inside for loop for albums")
                          if(albumIdsList(i)!=null) {
                            val tempEncryptedImagesKeyListBytes = albumDirectory.get(userEmail).get(albumIdsList(i)).encryptedKeysMap
                            var tempImagesKeyList = new ConcurrentHashMap[String,Array[Byte]]
                            tempImagesKeyList = deserialize(tempEncryptedImagesKeyListBytes).asInstanceOf[ConcurrentHashMap[String,Array[Byte]]]
                            println(tempImagesKeyList.toString)
                            if(tempImagesKeyList.containsKey(fromUser)) {
                              albumIdsReturn += albumIdsList(i)
                            }
                          }
                          else {
                            log.debug("Null found in albumIdsList("+i+").")
                          }

                        }
                        //"pappu"
                        albumIdsReturn.toString()
                      }
                    }
                }

              }
            //}
          } ~
          pathPrefix(Segment) { //returns a list of ids of the images
            albumID =>
              if(doesAlbumExist(albumID,userEmail)) {
                pathEnd {
                  get {
                    respondWithMediaType(`application/json`) {
                      complete {
                        //count=count+1
                        albumContent.get(userEmail+albumID).toString()
                      }
                    }
                  }
                } ~
                path(Segment) {
                  imageID =>
                  get {
                    /*respondWithMediaType(`image/jpeg`) {
                      complete {
                        //count=count+1
                        HttpData(new File("users/"+userEmail+"/"+albumID+"/"+imageID))
                      }
                    }*/
                    respondWithMediaType(`application/json`) {
                      complete {
                        val encryptedImgBytes = Files.readAllBytes(Paths.get("users/"+userEmail+"/"+albumID+"/"+imageID))
                        //val encryptedImgString = byteToString(encryptedImgBytes)

                        "dss"
                      }
                    }
                  }
                }~
                path("upload") {
                  post {
                    entity(as[Photo]) {
                      thisPhoto => requestContext =>
                       /* var imageID = "zz"//("z").toString()
                      //albumContent.get(Caption).put(imageID,"ds")
                      var ftmp = new File("ff.jpeg")
                        val output = new FileOutputStream(ftmp)
                        output.write(thisPhoto.Image)
                        output.close()*/
                        var imageID = (albumContent.get(userEmail+albumID).size()+1).toString()
                        albumContent.get(userEmail+albumID).put(imageID,thisPhoto.Caption)
                        var ftmp = new File("users/"+userEmail+"/"+albumID+"/"+imageID)
                        val output = new FileOutputStream(ftmp)
                        //formData.fields.foreach(f => output.write(f.entity.data.toByteArray ) )
                        output.write(thisPhoto.Image)
                        output.close()
                        //count=count+1
                        nImageUploads = nImageUploads+1
                        complete("done, file in: " + ftmp.getName())

                    }
                  }
                }~
                path("uploadCurl") {
                  post {
                    entity(as[MultipartFormData]) {
                      formData => {
                        //val ftmp = File.createTempFile("upload", ".tmp", new File("/tmp"))
                        //var imageID = System.currentTimeMillis().toString
                        var imageID = (albumContent.get(userEmail+albumID).size()+1).toString()
                        //var imageID = (albumContent.get(albumID).size()).toString()
                        albumContent.get(userEmail+albumID).put(imageID,"ds")
                        var ftmp = new File("users/"+userEmail+"/"+albumID+"/"+imageID)
                        val output = new FileOutputStream(ftmp)
                        formData.fields.foreach(f => output.write(f.entity.data.toByteArray ) )
                        output.close()
                        //count=count+1
                        nImageUploads = nImageUploads+1
                        complete("done, file in: " + ftmp.getName())
                      }
                    }
                  }
                }
              }
              else {
                respondWithMediaType(`application/json`) {
                  complete {
                    //count=count+1
                    "Album with id : "+albumID+" doesn't exist."
                  }
                }
              }
          }
        } ~
        path("friends") {
          get {
            respondWithMediaType(`application/json`) {
              complete {
                //count=count+1
                friendLists.get(userEmail).toString()
              }
            }
          }
        } ~
        path("profile") {
          get {
            respondWithMediaType(`application/json`) {
              complete {
                //count=count+1
                users.get(userEmail)
              }
            }
          }
        } ~
        path("ids") {
          /*get {
            respondWithMediaType(`application/json`) {
              complete {
                userPosts.get(userEmail).keySet().toString()
              }
            }
          }*/
          get {
            parameters('Email.as[String]) {
              fromUser =>
                respondWithMediaType(`application/json`) {
                  complete {
                    //count=count+1
                    log.debug("\n\n\n\n\n\n\ngot request for getting list of post ids")
                    log.debug("new1")
                    log.debug("\n\nfromUser : "+fromUser+"\n To User : "+userEmail+"\n\n")
                    if(!doesUserExist(fromUser)) {
                      fromUser+" does not exist."
                    }
                    else {
                      if(!doesUserExist(userEmail)) {
                        userEmail+" does not exist."
                      }
                      else {
                          if (areFriendsOrSame(fromUser, userEmail)) {
                          log.debug("inside if")
                          //userPosts.get(userEmail).keySet().toString()
                          //fetch only those post ids which this user has access to

                          //for username userEmail, get the list of postids first
                          // String[] strings = map.keySet().toArray(new String[map.size()]);

                          //var strings: Array[Nothing] = map.keySet.toArray(new Array[Nothing](map.size))

                          val postIdsList= userPosts.get(userEmail).keySet().toArray(new Array[String](userPosts.size()))
                          log.debug((postIdsList==null).toString)
                          log.debug("PostIdsList : "+postIdsList+" "+postIdsList.length)
                          log.debug("-->" + postIdsList(0)+"--->"+postIdsList(1))
                          val postIdsReturn : ListBuffer[String] = new ListBuffer()

                          //for each post id, get the encrypted post,get the list of people who have access to it
                          //the list itself is encrypted using server's public key
                          //decrypt it and then check if fromUser is there in the list or not
                          //if present, add the post id to list of postids to be returned
                          for(i<- 0 until postIdsList.length) {
                            log.debug("just inside for loop")
                            if(postIdsList(i)!=null) 
                            {
                              val tempEncryptedPostKeyList = userPosts.get(userEmail).get(postIdsList(i)).encryptedKeyMap
                              log.debug("for loop2")
                              log.debug("tempEncryptedPostKeyList : "+(tempEncryptedPostKeyList==null).toString)
                              log.debug("for loop3")
                              //now decrypt this using server's private key
                              //val tempPostKeyListBytes = decryptRSA(tempEncryptedPostKeyList,serverPrivateKey.getEncoded())
                              val tempPostKeyListBytes = tempEncryptedPostKeyList
                              log.debug("for loop4")
                              //now create a list from this bytes and return
                              var tempPostKeyList = new ConcurrentHashMap[String, Array[Byte]]
                             // try {
                              tempPostKeyList = deserialize(tempPostKeyListBytes).asInstanceOf[ConcurrentHashMap[String,Array[Byte]]]
                              println(tempPostKeyList.toString)
                           // }
                            /*catch {

                              case e: Exception => print ("YOOOOOOOOOOOOOOOOOOO" + e.toString)
                            }*/
                              log.debug("for loop5")
                              if(tempPostKeyList.containsKey(fromUser)) {
                                postIdsReturn += postIdsList(i)
                              }
                              log.debug("for loop6")
                            }
                            else
                            {
                              log.debug("Null found in postIdsList("+i+").")
                            }
                          }
                          log.debug("returning post ids")
                          postIdsReturn.toString()
                        }
                        else {
                           log.debug(" don't have rights to view post list.")
                          "Don't have rights to view post list."
                        }
                      }
                    }
                  
                  }

                }
            }
          }
        }~
        pathPrefix("posts") {
          pathEnd {
            get {
              parameters('Email.as[String]) {
                fromUser =>
                  respondWithMediaType(`application/json`) {
                    complete {
                      //count=count+1
                      if (areFriendsOrSame(fromUser, userEmail)) {
                        userPosts.get(userEmail).toString()
                        //"dss"
                      }
                      else {
                        "Don't have rights to view posts."
                      }
                    }
                  }
                //}
              }
            }
          } ~
          pathPrefix(Segment) {
            postID =>
              if (!userPosts.get(userEmail).containsKey(postID)) {
                respondWithMediaType(`application/json`) {
                  complete {
                    //count=count+1
                    "Post with : " + postID + " doesn't exist."
                  }
                }
              }
              else {
                  get {
                    parameters('Email.as[String]) {
                      fromUser =>
                        respondWithMediaType(`application/json`) {
                          complete {
                            //count=count+1
                            if (areFriendsOrSame(fromUser, userEmail)) {
                              //userPosts.get(userEmail).get(postID).toString()
                              //Post,initVector,encryptedKey


                              val tempEncryptedPostKeyList = userPosts.get(userEmail).get(postID).encryptedKeyMap
                              /*log.debug("for loop2")
                              log.debug("tempEncryptedPostKeyList : "+(tempEncryptedPostKeyList==null).toString)
                              log.debug("for loop3")
                              //now decrypt this using server's private key*/
                              //val tempPostKeyListBytes = decryptRSA(tempEncryptedPostKeyList,serverPrivateKey.getEncoded())
                              val tempPostKeyListBytes = tempEncryptedPostKeyList
                              //log.debug("for loop4")
                              //now create a list from this bytes and return
                              var tempPostKeyList = new ConcurrentHashMap[String, Array[Byte]]
                              // try {
                              tempPostKeyList = deserialize(tempPostKeyListBytes).asInstanceOf[ConcurrentHashMap[String,Array[Byte]]]
                              //println(tempPostKeyList.toString)
                              // }
                              /*catch {

                                case e: Exception => print ("YOOOOOOOOOOOOOOOOOOO" + e.toString)
                              }*/
                             // log.debug("for loop5")

                            if(tempPostKeyList.containsKey(fromUser)) {
                              val postContentReturn : ListBuffer[String] = new ListBuffer()
                              /*postContentReturn += byteToString(userPosts.get(userEmail).get(postID).encryptedPostData)
                              postContentReturn += byteToString(userPosts.get(userEmail).get(postID).initVector)
                              postContentReturn += byteToString(tempPostKeyList.get(fromUser))
                              postContentReturn.toString()*/
                              val x = byteToString(userPosts.get(userEmail).get(postID).encryptedPostData)
                              val y= byteToString(userPosts.get(userEmail).get(postID).initVector)
                              val z= byteToString(tempPostKeyList.get(fromUser))
                              x+","+y+","+z
                            }
                            else {
                              log.debug("Don't have right to view post with ID : " + postID)
                              "Don't have right to view post with ID : " + postID
                            }
                          }
                          else {
                              log.debug("Not friends with user so can't view.")
                              "Not friends with user so can't view."
                          }
                          }
                        }
                    }
                  }
               // }
              }

            }
          }
        }
      } ~
    path("wallWrite") {
      post {
        //entity(as[fbPost]) { wallpost => requestContext =>
        entity(as[EncryptedPost]) { wallpost => requestContext =>
          val responder = createResponder(requestContext)
          //count=count+1
          log.debug("\n\n\n\n\nWALL WRIIIIIIIIIIIITE")
          writePost(wallpost) match {
            case "posted" => responder ! PostSuccess

            case "invalidPost" => responder ! PostFail

            case "notFriends" => responder ! PostFail

            case "toEmailDecryptFailed" => responder ! PostFail
          }
        }
      }
    }
  }

  def byteToString(byteArr: Array[Byte]):String = {
    BigInt(byteArr).toString(16)
  }

  private def getSummary() : String = {
    var sec : Long = 0
    var totReq : Long = 0
    for(port <-0 until httpListenerBuffer.length) {
      if(portStats.containsKey(port)) {
        totReq=portStats.get(port).totalRequests+totReq
      }
    }
    sec = portStats.get(0).uptime.toSeconds.toLong
    var summary = "Time : "+sec+"\n Total Requests : "+totReq
    if(sec>0) {
      summary = summary + "\nRequests per second(Server uptime/number of Requests): "+(totReq.toDouble/sec)
    }
    var other = "\nNumber of users : "+users.size()+"\nNumber of Pages : "+pageDirectory.size()+"\nNumber of Page Posts : "+nPagePosts+"\nNumber of wall posts : "+nUserPosts
    other = other + "\nNumber of albums : "+nAlbums+"\nNumber of Image Uploads : "+nImageUploads+"\nFriend Requests"+nFriendRequests
    summary = summary + other


      val out = new BufferedWriter(new FileWriter("test.csv",true));
      val writer = new CSVWriter(out);
      val CSVSchema=Array("Time","Requests","Requests/Sec","nUsers","nPages","nPagePosts","nUserPosts","nAlbums","nImageUploads")

      //val employee1= Array("piyush","23","computerscience")

    //  val employee2= Array("neel","24","computerscience")
        if(sec<10) {
          writer.writeNext(CSVSchema)
        }

//      val employee3= Array("aayush","27","computerscience")
        val csv1 = Array(sec.toString,totReq.toString,(totReq.toDouble/sec).toString,users.size().toString,pageDirectory.size().toString,nPagePosts.toString,nUserPosts.toString,nAlbums.toString,nImageUploads.toString)
        /*for (i<- 0 until employee1.length) {
          employee1(i)=employee1(i).toString()
        }*/

    //var listOfRecords= List(employee1)

      writer.writeNext(csv1)
   // writer.
      out.close()

    summary
  }

  private  def clearAllStats() : Unit = {
    for(port <-0 until httpListenerBuffer.length) {
      httpListenerBuffer(port) ! Http.ClearStats
    }
  }

  private def updateAllStats() : Unit = {
    implicit val timeout : Timeout= Timeout(5 seconds)
    //var stat = new Stats(0,0,0,0,0,0,0,0)
    var sec : Long = 0
    var totReq : Long= 0
    for(port<- 0 until httpListenerBuffer.length) {
      context.actorSelection(httpListenerBuffer(port)) ? Http.GetStats onSuccess {
        case x: Stats =>
         // log.debug("idhar aa gaya yaaay")
         // log.debug(x.toString+"dsds")
          /*tmp = x.toString
          log.debug("this is tmps : "+tmp)
          println(tmp)
          tmp*/
          portStats.put(port,x)
        case _ =>
          log.debug("future fail")
          //tmp = "future
      }
     // log.debug("returning : "+tmp)
    }
  }

  private def getPortStats(port:Int) : String = {
    implicit val timeout : Timeout= Timeout(5 seconds)
    var tmp ="defau1"
    log.debug("acotr : "+httpListenerBuffer(port))
    context.actorSelection(httpListenerBuffer(port)) ? Http.GetStats onSuccess {
      case x: Stats =>
        log.debug("idhar aa gaya yaaay")
        log.debug(x.toString+"dsds")
        tmp = x.toString
        log.debug("this is tmps : "+tmp)
        println(tmp)
        tmp
      case _ =>
        log.debug("future fail")
        tmp = "future fail"
    }
    log.debug("returning : "+tmp)
    return tmp
  }

/*
  private def getServerStats(): String =
  {
      /*implicit val timeout : Timeout= Timeout(5 seconds)
      var tmp ="default2221"
    //Wcontext.actorS
      context.actorSelection("/user/IO-HTTP/listener-0") ? Http.GetStats onSuccess {
        case x: Stats =>
          log.debug("idhar aa gaya yaaay")
          log.debug(x.toString+"dsds")
          tmp = x.toString
          log.debug("this is tmps : "+tmp)
          println(tmp)
          tmp
        case _ =>
          log.debug("future fail")
          tmp = "future fail"
      }
    log.debug("returning : "+tmp)
      return tmp*/
    updateAllStats()
    portStats.toString()
  }*/

  private def createResponder(requestContext: RequestContext) = {
    context.actorOf(Props(new Responder(requestContext)))
  }

  //create album
  private def createAlbum(albumMetaData : AlbumMetaData) : Boolean = {
    if(doesUserExist(albumMetaData.Email)) {
      //check if the digital signature is matching before creating album
      val rawData = albumMetaData.Email+albumMetaData.Title
      val md: MessageDigest = MessageDigest.getInstance("SHA-256")
      md.update(rawData.getBytes("UTF-8"))
      val hashRawData: Array[Byte] = md.digest

      //decrypt the signed hash
      val compareTo = decryptPublicRSA(rawData.getBytes(),userPublicKey.get(albumMetaData.Email))

      if(!java.util.Arrays.equals(hashRawData,compareTo)) {
        return false
      }
      else {
        log.debug("HASHES MATCHED FOR ALBUM CREATION.")
      }

      //var albumID = System.currentTimeMillis().toString()
      var albumID = (albumDirectory.get(albumMetaData.Email).size()+1).toString()
      albumContent.put(albumMetaData.Email+albumID,new ConcurrentHashMap())
      albumDirectory.get(albumMetaData.Email).put(albumID,albumMetaData)
      //create a folder userid/albumid/ to store images
      var dir = new File("users/"+albumMetaData.Email+"/"+albumID)
      var ret = dir.mkdirs()
     // log.debug(dir.mkdir().toString)
      nAlbums=nAlbums+1

      return ret
    }
    return false
  }

  //does album exist
  private def doesAlbumExist(albumID : String,userEmail:String) : Boolean = {
    return albumDirectory.get(userEmail).containsKey(albumID)
  }


  def serialize(obj: AnyRef): Array[Byte] = {
    val b = new ByteArrayOutputStream()
    val o = new ObjectOutputStream(b)
    o.writeObject(obj)
    val r = b.toByteArray()
    return (r)
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
      case x: UnsupportedEncodingException => {
        System.out.println(x.toString)
      }
      case x: NoSuchAlgorithmException => {
        System.out.println(x.toString)
      }
      case x: NoSuchPaddingException => {
        System.out.println(x.toString)
      }
      case x: BadPaddingException => {
        System.out.println(x.toString)
      }
      case x: InvalidKeyException => {
        System.out.println(x.toString)
      }
      case x: IllegalBlockSizeException => {
        System.out.println(x.toString)
      }
    }

    return null
  }

  def toHexString(block: Array[Byte]): String = {
    val buf = new StringBuffer()
    val len = block.length
    for (i <- 0 until len) {
      byte2hex(block(i), buf)
      if (i < len - 1) {
        buf.append(":")
      }
    }
    buf.toString
  }

  def byte2hex(b: Byte, buf: StringBuffer) {
    val hexChars = Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
    val high = ((b & 0xf0) >> 4)
    val low = (b & 0x0f)
    buf.append(hexChars(high))
    buf.append(hexChars(low))
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
      case x: UnsupportedEncodingException => {
        System.out.println(x.toString)
      }
      case x: NoSuchAlgorithmException => {
        System.out.println(x.toString)
      }
      case x: NoSuchPaddingException => {
        System.out.println(x.toString)
      }
      case x: BadPaddingException => {
        System.out.println(x.toString)
      }
      case x: InvalidKeyException => {
        System.out.println(x.toString)
      }
      case x: IllegalBlockSizeException => {
        System.out.println(x.toString)
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
      case x: UnsupportedEncodingException => {
        System.out.println(x.toString)
      }
      case x: NoSuchAlgorithmException => {
        System.out.println(x.toString)
      }
      case x: NoSuchPaddingException => {
        System.out.println(x.toString)
      }
      case x: BadPaddingException => {
        System.out.println(x.toString)
      }
      case x: InvalidKeyException => {
        System.out.println(x.toString)
      }
      case x: IllegalBlockSizeException => {
        System.out.println(x.toString)
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
      case x: UnsupportedEncodingException => {
        System.out.println(x.toString)
      }
      case x: NoSuchAlgorithmException => {
        System.out.println(x.toString)
      }
      case x: NoSuchPaddingException => {
        System.out.println(x.toString)
      }
      case x: BadPaddingException => {
        System.out.println(x.toString)
      }
      case x: InvalidKeyException => {
        System.out.println(x.toString)
      }
      case x: IllegalBlockSizeException => {
        System.out.println(x.toString)
      }
    }
    return null
  }

  //create User
  //private def createUser(user: User) : Boolean = {
  private def createUser(encUser: EncryptedUser) : String = {
    //val doesNotExist = !users.exists(_.Email == user.Email)
    log.debug("User creation request received on server.")
    //var publicKey: PublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encUser.pubkey))

    //check if sign matches with hash of encUser.user

    //get hash
    val md: MessageDigest = MessageDigest.getInstance("SHA-256")
    md.update(serialize(encUser.user))
    val hashEncUser: Array[Byte] = md.digest


    //decrypt hash

    val cipher : Cipher = Cipher.getInstance("RSA");
    //cipher.init(Cipher.DECRYPT_MODE,publicKey);
    //val decryptedSign: Array[Byte] = cipher.doFinal(encUser.sign)
    val decryptedSign : Array[Byte] = decryptPublicRSA(encUser.sign,encUser.pubkey)

   // if(Array.equals(hashEncUser,decryptedSign)) {
     if(!(decryptedSign==null) && (java.util.Arrays.equals(hashEncUser,decryptedSign))) {
      log.debug("Digital Signature Successful.")
      val doesNotExist = !doesUserExist(encUser.user.Email)
      //log.debug("Users : "+doesNotExist)
      if(doesNotExist) {
        //users = users :+ ufser
        users.put(encUser.user.Email,encUser)
       // log.debug("Created User : "+encUser.user.Email)

        //decrypt user's public key and store
        userPublicKey.put(encUser.user.Email,encUser.pubkey)


        friendLists.put(encUser.user.Email,new ListBuffer())
        friendRequests.put(encUser.user.Email,new ListBuffer())
        userPosts.put(encUser.user.Email,new ConcurrentHashMap())
        albumDirectory.put(encUser.user.Email,new ConcurrentHashMap())
        return "doesNotExist"
      }
      else {
        return "alreadyExists"
      }

    }
    else {
      log.debug("Digital Signature failed. User doesnt have correct public/private pair.")
      return "DigitalSignFailed"
    }

  }

  //create Page
  private def createPage(page: Page) : Boolean = {
    val didSucceed = true
    if(pageDirectory.containsKey(page.pageID)) {
      return false
    }
    if(doesUserExist(page.adminEmail)) {
      pageContent.put(page.pageID,new ConcurrentHashMap())
      pageDirectory.put(page.pageID,page)
      pageFollowers.put(page.pageID,new ListBuffer())
      addFollower(page.adminEmail, page.pageID)
      pageIDs += page.pageID
      return true
    }
    return false
  }

  //sendFriendRequest
  private def sendFriendRequest(req : FriendRequest) : String = {
    nFriendRequests = nFriendRequests+1
    if(friendLists.containsKey(req.fromEmail)) {
      if(friendLists.get(req.fromEmail).contains(req.toEmail)) {
        return "alreadyFriends"
      }
      else if(req.fromEmail==req.toEmail){
        return "cantAddSelf"
      }
    }
    if(!friendLists.containsKey(req.toEmail) || !friendLists.containsKey(req.fromEmail)) {
      return "userNotPresent"
    }
    else {
      //store the request somewhere
      friendRequests.get(req.toEmail) += req.fromEmail
      //tmp
      friendLists.get(req.toEmail) += req.fromEmail
      friendLists.get(req.fromEmail) += req.toEmail
      //tmp
      //return public key in string form
      return "requestSent"

    }
  }

  def deserialize(bytes: Array[Byte]): AnyRef = {
    val b = new ByteArrayInputStream(bytes)
    val o = new ObjectInputStream(b)
    o.readObject()
  }
  
  //private def writePost(p : fbPost) : String = {
  private def writePost(p : EncryptedPost) : String = {
    //first decrypt the addresses using public key
    log.debug("\n\nWall write post request received from : "+p.fromEmail)

    //get public key of user p.fromEmail if exists
    if(!doesUserExist(p.fromEmail)) {
      log.debug("From email doesn't exist. Can't post.")
      return "invalidPost"
    }
    val publicKey : Array[Byte] = users.get(p.fromEmail).pubkey
    //val toEmailBytes : Array[Byte] = decryptPublicRSA(p.encryptedToEmail,publicKey)
    val toEmailBytes : Array[Byte] = decryptRSA(p.encryptedToEmail,serverPrivateKey.getEncoded())
    if(toEmailBytes==null) {
      log.debug("toEmail decryption failed.")
      return "toEmailDecryptFailed"
    }
    else {
      //val toEmail : String = new String(toEmailBytes,"UTF-8")
      val toEmailCase: encryptedAddress = deserialize(toEmailBytes).asInstanceOf[encryptedAddress]
      val toEmail : String = toEmailCase.address

      log.debug("decrypted to email : "+toEmail)

      if(!doesUserExist(toEmail)) {
        log.debug("To email doesn't exist. Can't post")
        return "invalidPost"
      }
      //if()
      //check for hash match, if not return invalid post
      val md: MessageDigest = MessageDigest.getInstance("SHA-256")
      md.update(p.encryptedPostData)
      val hashEncryptedPostData : Array[Byte] = md.digest
      val compareTo = decryptPublicRSA(p.signedHashedEncryptedPostData,userPublicKey.get(p.fromEmail))
      if(!java.util.Arrays.equals(hashEncryptedPostData,compareTo)) {
        log.debug("Fail because of hash not matchign in post.")
        return "invalidPost"
      }
      log.debug("Hashes matched. yay.")


      if(areFriendsOrSame(p.fromEmail,toEmail)) {
        userPosts.get(toEmail).put(System.currentTimeMillis().toString(),p)
        nUserPosts = nUserPosts+1
        return "posted"
      }
      log.debug("Can't post because not friends.")
      return "notFriends"
    }
  }

  private def createPagePost(post: pagePost,pageID : String) : String = {
    if(!doesUserExist(post.fromEmail)) {
      log.debug("From email doesn't exist. Can't post.")
      return "invalidPost"
    }
    else if(!isFollower(post.fromEmail,pageID)) {
      log.debug("Can't post because not following page.")
      return "notFollowing"
    }
    nPagePosts = nPagePosts+1
    pageContent.get(pageID).put(System.currentTimeMillis().toString(),post)
    return "posted"
  }

  private  def addFollower(userEmail : String, pageID : String) : String = {
    log.debug(userEmail+" ^ "+pageID)
    if(!doesUserExist(userEmail)) {
      return "invalidUser"
    }
    /*else if(!doesPageExist(pageID)) {
      return "invalidPage"
    }*/
    else if(isFollower(userEmail,pageID)) {
      log.debug(userEmail+" ^ "+pageID)
      return "alreadyFollower"
    }
    else {
      pageFollowers.get(pageID) += userEmail
      return "followSuccess"
    }
  }

  private def areFriendsOrSame(fromEmail: String,toEmail: String): Boolean = {
   return (fromEmail==toEmail || friendLists.get(fromEmail).contains(toEmail))
  }

  private def doesUserExist(Email: String): Boolean = {
    return users.containsKey(Email)
  }

  private  def isFollower(userEmail: String,pageID: String) : Boolean = {
    log.debug(userEmail+" "+pageID)
    return (pageFollowers.get(pageID).contains(userEmail))
  }

  private  def doesPageExist(pageID : String): Boolean = {
    return (pageDirectory.containsKey(pageID))
  }
}


class Responder(requestContext: RequestContext) extends Actor with ActorLogging {
  import MyJsonProtocol._
 // val log = Logging(context.system, this)
  def receive = {
    case UserCreated(email) =>
     // requestContext.complete(StatusCodes.Created)
      requestContext.complete("User created.")
      log.debug("User Created with Email : "+email)
      ////count=count+1
      killYourself

    case UserAlreadyExists =>
      //requestContext.complete(StatusCodes.Conflict)
      requestContext.complete("User with email id already exists.")
      log.debug("User already created with same email.")
      killYourself

    case DigitalSignFailed =>
      requestContext.complete("Digital Signature Failed.")
      log.debug("Digital Signature Failed.")
      killYourself

    case AlreadyFriends =>
      requestContext.complete("Already friends with the user.")
      log.debug("Already friends with the user.")
      killYourself

    case UserNotPresent =>
     //requestContext.complete(StatusCodes.PreconditionFailed)
     //log.debug("Can't send friend request to user not present in the system.")
      requestContext.complete("User not present in the system.")
      killYourself

    case PageNotPresent =>
      requestContext.complete("Page not present un the system.")
      killYourself

    case AlreadyFollowingPage =>
      requestContext.complete("Already following the page.")
      killYourself

    case FriendRequestSent(toFriendPublicKey) =>
     //requestContext.complete(StatusCodes.Accepted)
   //   requestContext.complete("Friend request was successfully sent.")
     requestContext.complete(toFriendPublicKey)
      log.debug("Public key was successfully sent.")
     killYourself

    case PostSuccess =>
     //requestContext.complete(StatusCodes.Accepted)
     requestContext.complete("Post successful.")
      log.debug("Post accepted.")
      killYourself

    case PostFail =>
     requestContext.complete("Post failed.")
     killYourself

    case PageCreated(page) =>
     requestContext.complete("Page creation succesful.")
      log.debug("Page "+page+"created.")
     killYourself

    case PageCreationFailed =>
     requestContext.complete("Page creation failed.")
     killYourself

    case PostFailNotFollowing =>
     requestContext.complete("Post failed because not following the page.")
     killYourself

    case FollowSuccess =>
     requestContext.complete("Successfully added as follower.")
     killYourself

    case FollowFail =>
     requestContext.complete("Already following page or user doesn't exist.")
     killYourself

    case AlbumCreated(title) =>
     requestContext.complete("Album : "+ title+" created.")
     killYourself

    case AlbumCreationFailed =>
     requestContext.complete("Album creation failed.")
     killYourself

    case CantAddSelf =>
     requestContext.complete("Can't add self as friend.")
     killYourself
  }

  private def killYourself = self ! PoisonPill
}
