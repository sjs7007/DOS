/**
 * Created by shinchan on 11/6/15.
 */

import java.util.concurrent.ConcurrentHashMap

import MyJsonProtocol._
import akka.actor._
import spray.can.Http
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.routing._
import MediaTypes._

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
  var users = new ConcurrentHashMap[String,User]()
  //var friendLists = new HashMap[String,Set[String]] with MultiMap[String,String]

  //concurrent to allow simultaneous access : stores friends of each email
  var friendLists = new ConcurrentHashMap[String,ListBuffer[String]]()
  
  //store all friend requests in the buffer below.
  var friendRequests = new ConcurrentHashMap[String,ListBuffer[String]]()
  
  //store all posts in the buffer below
  var allPosts = new ConcurrentHashMap[String,ListBuffer[Wallpost]]()

  def receive = handle orElse httpReceive


  val facebookStuff = {
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
      pathPrefix("user" / PathElement) {
        userEmail =>
          path("friends") {
            get {
              respondWithMediaType(`application/json`) {
                complete {
                  //userEmail
                  //"tesloop"
                  users.get(userEmail)
                }
              }
            }
          }
      } ~
      path("wallWrite") {
        post {
          entity(as[Wallpost]) { wallpost => requestContext =>
            val responder = createResponder(requestContext)
            writePost(wallpost) match {
              case "posted" => responder ! PostSuccess

              case "cannotPost" => responder ! PostFail
            }
          }
        }
      }
    /*pathPrefix("user"/String) {
     userEmail =>
     path("friends"){
        get {
          requestContext =>
          {
            val responder = createResponder(requestContext)
            getFriends(userEmail) match {
              case true => responder
              case _ =>
            }
          }
        }
     }
   } ~*/
  }

  private def createResponder(requestContext: RequestContext) = {
    context.actorOf(Props(new Responder(requestContext)))
  }

  //create User
  private def createUser(user: User) : Boolean = {
    //val doesNotExist = !users.exists(_.Email == user.Email)
    val doesNotExist = users.containsKey(user.Email)
    if(doesNotExist) {
      //users = users :+ user
      users.put(user.Email,user)
      friendLists.put(user.Email,new ListBuffer())
      friendRequests.put(user.Email,new ListBuffer())
    }
    doesNotExist
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
      return "requestSent"
    }

  }
  
  
    private def writePost(p : Wallpost) : String = {
    if(friendLists.get(p.from) == friendLists.get(p.to) ||  friendLists.get(p.from).contains(p.to)) {
        allPosts.get(p.to) += p
      return "posted"
    }

    else return "cannotPost"

  }
  
}


class Responder(requestContext: RequestContext) extends Actor with ActorLogging {
  import MyJsonProtocol._
 // val log = Logging(context.system, this)
  def receive = {
    case UserCreated(email) =>
      requestContext.complete(StatusCodes.Created)
      log.debug("User Created with Email : "+email)
      killYourself

    case UserAlreadyExists =>
      requestContext.complete(StatusCodes.Conflict)
      killYourself

    case AlreadyFriends =>
      requestContext.complete(StatusCodes.Conflict)
      log.debug("Already friends with the user.")
      killYourself

    case UserNotPresent =>
     requestContext.complete(StatusCodes.PreconditionFailed)
     log.debug("Can't send friend request to user not present in the system.")

    case FriendRequestSent =>
     requestContext.complete(StatusCodes.Accepted)
     log.debug("Friend request was successfully sent.")

  }

  private def killYourself = self ! PoisonPill
}
