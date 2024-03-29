/**
 * Created by shinchan on 11/6/15.
 */

import java.util.concurrent.ConcurrentHashMap

import MyJsonProtocol._
import akka.actor._
import spray.can.Http
import spray.http.MediaTypes._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.routing._

import scala.collection.mutable.ListBuffer
//import java.util


// simple actor that handles the routes.
class SJServiceActor extends Actor with HttpService with ActorLogging {


  var httpListener : Option[ActorRef] = None
  // required as implicit value for the HttpService
  // included from SJService
  log.debug("Actor started")
  def actorRefFactory = context


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
      println("bound")
      log.debug("can kill when I want to now")
      //sender ! Http.Unbind(10 seconds)
    case Http.Unbound =>
      println("unbound")
      context.stop(self)
  }

  //list of all users : make it more efficient
  //var users = scala.collection.immutable.Vector[User]()
  //var users = new ConcurrentHashMap[String,User]()
  var users:Map[String,User] = Map()
  //var friendLists = new HashMap[String,Set[String]] with MultiMap[String,String]

  //concurrent to allow simultaneous access : stores friends of each email
  var friendLists = new ConcurrentHashMap[String,ListBuffer[String]]()
  
  //store all friend requests in the buffer below.
  var friendRequests = new ConcurrentHashMap[String,ListBuffer[String]]()
  
  //store all posts in the buffer below
  var userPosts = new ConcurrentHashMap[String,ListBuffer[fbPost]]()

  //directory of all pages
  //key : pageID, value : page info(adminEmail,title,pageID)
  //var pageDirectory = new ConcurrentHashMap[String,Page]()
  //var pageDirectory = new scala.util.HashMap[String,Page]()
   var pageDirectory:Map[String,Page] = Map()


  //store content of each page : key pageID value : list of posts on the page
  var pageContent = new ConcurrentHashMap[String,ListBuffer[pagePost]]()

  //store list of followers for each pageID. only people following page can comment
  var pageFollowers = new ConcurrentHashMap[String,ListBuffer[String]]()

  def receive = handle orElse httpReceive


  val facebookStuff = {
    pathPrefix("upload") {
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
    } ~
    pathPrefix("userStats") {
      pathEnd {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              users.size.toString()
            }
          }
        }
      }
    } ~
    pathPrefix("users") {
      pathEnd {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              //var A = new Map[String,User]()
              users
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
              //pageDirectory.toString()
              pageDirectory
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
          entity(as[User]) { user => requestContext =>
            val responder = createResponder(requestContext)
            createUser(user) match {
              case true => responder ! UserCreated(user.Email)
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
              createPage(page) match  {
                case true => responder ! PageCreated(page.Title)
                case _ => responder ! PageCreationFailed
              }
          }
        }
      }
    } ~
    pathPrefix("pages"/Segment) {
      pageID =>
      if(!doesPageExist(pageID)) {
        respondWithMediaType(`application/json`) {
          complete {
            "Page doesn't exist."
          }
        }
      }
      pathEnd {
        get {
          respondWithMediaType(`application/json`) {
            complete {
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
                createPagePost(postData,pageID) match {
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
                addFollower(user.Email,pageID) match {
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
                pageFollowers.toString()
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
                pageContent.get(pageID).toString()
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
            sendFriendRequest(friendRequest) match {
              case "alreadyFriends" => responder ! AlreadyFriends

              case "userNotPresent" => responder ! UserNotPresent

              case "requestSent" => responder ! FriendRequestSent
            }
          }
        }
      }
    } ~
    pathPrefix("user" / Segment) {
      userEmail =>
      if(!users.contains(userEmail)) {
        respondWithMediaType(`application/json`) {
          complete {
            "User : "+userEmail+" doesn't exist."
          }
        }
      }
      path("friends") {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              friendLists.get(userEmail).toString()
            }
          }
        }
      } ~
      path("profile") {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              users.get(userEmail)
            }
          }
        }
      } ~
      pathPrefix("posts") {
        pathEnd {
          get {
            parameters('Email.as[String]) { //(name,color) =>
              fromUser =>
                respondWithMediaType(`application/json`) {
                  complete {
                    if(areFriendsOrSame(fromUser,userEmail)) {
                      userPosts.get(userEmail).toString()
                    }
                    else {
                      "Don't have rights to view posts."
                    }
                  }
                }
            }
          }
        }~
        pathPrefix(Segment) {
          postID =>
          if(!userPosts.containsKey(postID)) {
            respondWithMediaType(`application/json`) {
              complete {
                "Post with : "+postID+" doesn't exist."
              }
            }
          }
          path("post") {
            get {
              parameters('Email.as[String]) {
                fromUser =>
                respondWithMediaType(`application/json`) {
                  complete {
                    if(areFriendsOrSame(fromUser,userEmail)) {
                      userPosts.get(userEmail).toString()
                    }
                    else {
                      "Don't have right to view post with ID : " + postID
                    }
                  }
                }
              }
            }
          }
        }
      }
    } ~
    path("wallWrite") {
      post {
        entity(as[fbPost]) { wallpost => requestContext =>
          val responder = createResponder(requestContext)
          writePost(wallpost) match {
            case "posted" => responder ! PostSuccess

            case "invalidPost" => responder ! PostFail

            case "notFriends" => responder ! PostFail
          }
        }
      }
    }
  }

  private def createResponder(requestContext: RequestContext) = {
    context.actorOf(Props(new Responder(requestContext)))
  }

  //create User
  private def createUser(user: User) : Boolean = {
    //val doesNotExist = !users.exists(_.Email == user.Email)
    val doesNotExist = !doesUserExist(user.Email)
    log.debug("Users : "+doesNotExist)
    if(doesNotExist) {
      //users = users :+ ufser
      //users.put(user.Email,user)
      users += (user.Email -> user)
      friendLists.put(user.Email,new ListBuffer())
      friendRequests.put(user.Email,new ListBuffer())
      userPosts.put(user.Email,new ListBuffer())
    }
    doesNotExist
  }

  //create Page
  private def createPage(page: Page) : Boolean = {
    val didSucceed = true
    //if(pageDirectory.containsKey(page.pageID)) {
    if(pageDirectory.contains(page.pageID)) {
      return false
    }
    if(doesUserExist(page.adminEmail)) {
      pageContent.put(page.pageID,new ListBuffer())
      //pageDirectory.put(page.pageID,page)
      pageDirectory += (page.pageID -> page)
      pageFollowers.put(page.pageID,new ListBuffer())
      return true
    }
    return false
  }

  //sendFriendRequest
  private def sendFriendRequest(req : FriendRequest) : String = {
    if(friendLists.containsKey(req.fromEmail)) {
      if(friendLists.get(req.fromEmail).contains(req.toEmail)) {
        return "alreadyFriends"
      }
    }
    if(!friendLists.containsKey(req.toEmail)) {
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
  
  
  private def writePost(p : fbPost) : String = {
  if(!doesUserExist(p.fromEmail)) {
    log.debug("From email doesn't exist. Can't post.")
    return "invalidPost"
  }
  else if(!doesUserExist(p.toEmail)) {
    log.debug("To email doesn't exist. Can't post")
    return "invalidPost"
  }
  if(areFriendsOrSame(p.fromEmail,p.toEmail)) {
      userPosts.get(p.toEmail) += p
    return "posted"
  }
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
    pageContent.get(pageID) += post
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
    return users.contains(Email)
  }

  private  def isFollower(userEmail: String,pageID: String) : Boolean = {
    log.debug(userEmail+" "+pageID)
    return (pageFollowers.get(pageID).contains(userEmail))
  }

  private  def doesPageExist(pageID : String): Boolean = {
    //return (pageDirectory.containsKey(pageID))
    return  (pageDirectory.contains(pageID))
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
      killYourself

    case UserAlreadyExists =>
      //requestContext.complete(StatusCodes.Conflict)
      requestContext.complete("User with email id already exists.")
      log.debug("User already created with same email.")
      killYourself

    case AlreadyFriends =>
      requestContext.complete(StatusCodes.Conflict)
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
     requestContext.complete(StatusCodes.Accepted)
     log.debug("Friend request was successfully ent.")
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
  }

  private def killYourself = self ! PoisonPill
}
