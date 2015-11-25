/**
 * Created by shinchan on 11/6/15.
 */

import MyJsonProtocol._
import akka.actor._
import akka.util.Timeout
import spray.can.Http
import spray.http.MediaTypes._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.routing._

import scala.concurrent.duration._
import collection.mutable.{ HashMap, MultiMap, Set } //http://www.scala-lang.org/api/2.11.5/index.html#scala.collection.mutable.MultiMap
import java.util.concurrent.ConcurrentHashMap
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
  var users = scala.collection.immutable.Vector[User]()
  //var friendLists = new HashMap[String,Set[String]] with MultiMap[String,String]

  //concurrent to allow simultaneous access : stores friends of each email
  var friendLists = new ConcurrentHashMap[String,ListBuffer[String]]()
  
    var posts = new ConcurrentHashMap[String,ListBuffer[Wallpost]]()


  //store all friend requests in the buffer below. Ba

  def receive = handle orElse httpReceive


  val facebookStuff = {
    path("createUser") {
      post {
        entity(as[User]) { user => requestContext =>
          val responder = createResponder(requestContext)
          createUser(user) match {
            case true => responder ! UserCreated(user.Email)
            case _ => UserAlreadyExists
          }
        }
      }
    }
    
    

    path("sendFriendRequest") {
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
    
    
    
  }


  private def createResponder(requestContext: RequestContext) = {
    context.actorOf(Props(new Responder(requestContext)))
  }

  //create User
  private def createUser(user: User) : Boolean = {
    val doesNotExist = !users.exists(_.Email == user.Email)
    if(doesNotExist) users = users :+ user
    doesNotExist
  }

  //sendFriendRequest
  private def sendFriendRequest(req : FriendRequest) : String = {
    if(friendLists.isDefinedAt(req.fromEmail)) {
      if(friendLists.get(req.fromEmail).contains(req.toEmail)) {
        return "alreadyFriends"
      }
    }

    else if(!friendLists.isDefinedAt(req.toEmail)) {
      return "userNotPresent"
    }
    else {
      //store the request somewhere
      return "requestSent"
    }
  }
  
    //writepost
  private def writePost(p : wallpost) : String = {
    if(friendLists.get(p.fromEmail).contains(p.toEmail)) {
        /*INSERT POST INTO THAT WALL*/
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

    case SendFriendRequest()
  }

  private def killYourself = self ! PoisonPill
}
