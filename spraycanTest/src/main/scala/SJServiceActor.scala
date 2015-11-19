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
  def httpReceive = runRoute(aSimpleRoute ~ anotherRoute ~ anotherThirdRoute ~ facebookStuff)

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

  var users = scala.collection.immutable.Vector[User]()


  def receive = handle orElse httpReceive

  // handles the api path, we could also define these in separate files
  // this path respons to get queries, and make a selection on the
  // media-type.
  val aSimpleRoute = {
    path("path1") {

      get {

        // Get the value of the content-header. Spray
        // provides multiple ways to do this.
        headerValue({
          case x@HttpHeaders.`Content-Type`(value) => Some(value)
          case default => None
        }) {
          // the header is passed in containing the content type
          // we match the header using a case statement, and depending
          // on the content type we return a specific object
          header => header match {

            // if we have this contentype we create a custom response
            case ContentType(MediaType("application/vnd.type.a"), _) => {
              respondWithMediaType(`application/json`) {
                complete {
                  Person("Bob", "Type A", System.currentTimeMillis());
                }
              }
            }

            // if we habe another content-type we return a different type.
            case ContentType(MediaType("application/vnd.type.b"), _) => {

              respondWithMediaType(`application/json`) {
                complete {
                  Person("Bob", "Type B", System.currentTimeMillis());
                }
              }
            }

            case ContentType(MediaType("application/vnd.type.testType"),_) => {
              respondWithMediaType(`application/json`) {
                complete {
                  Person("Baaab","Type test",900)
                }
              }
            }

            case ContentType(MediaType("application/vnd.type.killserver"),_) => {
              implicit val timeout = Timeout(0.seconds)
              httpListener.get ! Http.Unbind(timeout.duration)
              println(" killing server now")
              respondWithMediaType(`application/json`) {
                complete {
                  Person("Baaab","Type test",900)
                }
              }
            }

            // if content-types do not match, return an error code
            case default => {
              complete {
                HttpResponse(406);
              }
            }
          }
        }
      }
    }
  }

  // handles the other path, we could also define these in separate files
  // This is just a simple route to explain the concept
  val anotherRoute = {
    path("path2") {
      get {
        // respond with text/html.
        respondWithMediaType(`text/html`) {
          complete {
            // respond with a set of HTML elements
            <html>
              <body>
                <h1>Path 2</h1>
                <h2>some more random text</h2>
              </body>
            </html>
          }
        }
      }
    }
  }

  val anotherThirdRoute = {
    path("order" / IntNumber / IntNumber ) { (orderID,orderID2) =>
      (get | put) { ctx =>
        ctx.complete("Received " + ctx.request.method + " request for order " + orderID+"\n OrderID 2 is : "+orderID2)
      }
    }
  }

  val facebookStuff = {
    pathPrefix("createUser") {
      pathEnd {
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
    }

    pathPrefix("user"/IntNumber/"friends") {
      pathEnd {
        post {

        }
      }
    }
  }


  private def createResponder(requestContext: RequestContext) = {
    context.actorOf(Props(new Responder(requestContext)))
  }

  private def createUser(user: User) : Boolean = {
    val doesNotExist = !users.exists(_.Email == user.Email)
    if(doesNotExist) users = users :+ user
    doesNotExist
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
