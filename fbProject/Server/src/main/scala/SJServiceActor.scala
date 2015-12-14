/**
 * Created by shinchan on 11/6/15.
 */

import java.io._
import java.security.spec.X509EncodedKeySpec
import java.security.{KeyFactory, MessageDigest, PublicKey}
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher

import MyJsonProtocol._
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.can.server.Stats
import spray.http.MediaTypes._
import spray.http.{HttpData, MultipartFormData}
import spray.httpx.SprayJsonSupport._
import spray.routing._

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
//import java.util

import au.com.bytecode.opencsv.CSVWriter

object commonVars {
  //list of all users : make it more efficient
  //var users = scala.collection.immutable.Vector[User]()
 // var users = new ConcurrentHashMap[String,User]()
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
  var comments = new ConcurrentHashMap[String,ListBuffer[Comment]]()

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
      log.debug("dis listener : "+httpListener.get.path.toString())
      println("bound")
      log.debug("can kill when I want to now")
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
    /*pathPrefix("upload") {
      pathEnd {
        post {
          log.debug("inhere")
          entity(as[Photo]) { pic => requestContext =>
            val responder = createResponder(requestContext)
            log.debug (pic.Image)
            responder ! UserCreated("abc")
          }
        }
      }
    } ~ */
    /*pathPrefix("clearAllStats") {
      get {
        respondWithMediaType(`application/json`) {
          complete {
            clearAllStats()
            "Stats cleared."
          }
        }
      }
    }~*/
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
    /*pathPrefix("resetStats") {
      get {
        respondWithMediaType(`application/json`) {
          complete {
            count=0
            "Count set to : "+count
          }
        }
      }
    }~*/
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
          /*implicit val timeout : Timeout= Timeout(5 seconds)
          var tmp ="default"
          context.actorSelection("/user/IO-HTTP/listener-0") ? Http.GetStats onSuccess {
            case x: Stats =>
              log.debug("idhar aa gaya yaaay")
              tmp = x.toString()
            case _ =>
              log.debug("future fail")
              tmp = "future fail"
          }*/
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
              case true => responder ! UserCreated(encUser.user.Email)
              case _ => responder ! UserAlreadyExists
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
              case "alreadyFriends" => responder ! AlreadyFriends

              case "userNotPresent" => responder ! UserNotPresent

              case "requestSent" => responder ! FriendRequestSent

              case "cantAddSelf" => responder ! CantAddSelf
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
        pathPrefix("albums") {
          pathEnd {
            get {
              respondWithMediaType(`application/json`) {
                complete {
                  //count=count+1
                  albumDirectory.get(userEmail).toString()
                }
              }
            }
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
                    respondWithMediaType(`image/jpeg`) {
                      complete {
                        //count=count+1
                        HttpData(new File("users/"+userEmail+"/"+albumID+"/"+imageID))
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
                      if (areFriendsOrSame(fromUser, userEmail)) {
                        userPosts.get(userEmail).keySet().toString()
                        //"dss"
                      }
                      else {
                        "Don't have rights to view posts."
                      }
                    }
                  }
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
                              userPosts.get(userEmail).toString()
                            }
                            else {
                              "Don't have right to view post with ID : " + postID
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
          writePost(wallpost) match {
            case "posted" => responder ! PostSuccess

            case "invalidPost" => responder ! PostFail

            case "notFriends" => responder ! PostFail
          }
        }
      }
    }
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
          log.debug("idhar aa gaya yaaay")
          log.debug(x.toString+"dsds")
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

  //create User
  //private def createUser(user: User) : Boolean = {
  private def createUser(encUser: EncryptedUser) : Boolean = {
    //val doesNotExist = !users.exists(_.Email == user.Email)
    log.debug("User creation request received on server.")
    var publicKey: PublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encUser.pubkey))

    //check if sign matches with hash of encUser.user

    //get hash
    val md: MessageDigest = MessageDigest.getInstance("SHA-256")
    md.update(serialize(encUser.user))
    val hashEncUser: Array[Byte] = md.digest


    //decrypt hash

    val cipher : Cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.DECRYPT_MODE,publicKey);
    val decryptedSign: Array[Byte] = cipher.doFinal(encUser.sign)

   // if(Array.equals(hashEncUser,decryptedSign)) {
     if(java.util.Arrays.equals(hashEncUser,decryptedSign)) {
      log.debug("User has the public key/private key pair.")
      val doesNotExist = !doesUserExist(encUser.user.Email)
      log.debug("Users : "+doesNotExist)
      if(doesNotExist) {
        //users = users :+ ufser
        users.put(encUser.user.Email,encUser)
        log.debug("Created User : "+encUser.user.Email)
        friendLists.put(encUser.user.Email,new ListBuffer())
        friendRequests.put(encUser.user.Email,new ListBuffer())
        userPosts.put(encUser.user.Email,new ConcurrentHashMap())
        albumDirectory.put(encUser.user.Email,new ConcurrentHashMap())
      }
      doesNotExist
    }
    else {
      val x : String = new String(decryptedSign,"UTF-8")
      val y : String = new String(hashEncUser,"UTF-8")
      val z : String = new String(decryptedSign,"UTF-8")
      /*log.debug("User : "+x)
      log.debug(">>"+y+"<< >>"+z+"<<")
      log.debug("true or not : "+(y.equals(z)))
      log.debug("true or not2 : "+Array.equals(hashEncUser,decryptedSign))*/
      log.debug("Hashes didn't match. User dont have correct public/private pair.")
      false
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
      return "requestSent"
    }
  }
  
  
  //private def writePost(p : fbPost) : String = {
  private def writePost(p : EncryptedPost) : String = {
   // p.encryptedAddresses

  /*  if(!doesUserExist(p.fromEmail)) {
      log.debug("From email doesn't exist. Can't post.")
      return "invalidPost"
    }
    else if(!doesUserExist(p.toEmail)) {
      log.debug("To email doesn't exist. Can't post")
      return "invalidPost"
    }
    if(areFriendsOrSame(p.fromEmail,p.toEmail)) {
        userPosts.get(p.toEmail).put(System.currentTimeMillis().toString(),p)
        nUserPosts = nUserPosts+1
      return "posted"
    }*/
      log.debug("Can't post because not friends.")
     return "notFriends"
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

    case FriendRequestSent =>
     //requestContext.complete(StatusCodes.Accepted)
      requestContext.complete("Friend request was successfully sent.")
     log.debug("Friend request was successfully sent.")
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
