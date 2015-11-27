/**
 * Created by shinchan on 11/6/15.
 */
import spray.json.DefaultJsonProtocol

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val personFormat = jsonFormat3(Person)

  case class Person(name: String, fistName: String, age: Long)

  //createUser
  case class User(Email:String, Name: String,Birthday: String,CurrentCity : String) {
    require(!Email.isEmpty, "Email must not be empty.")
    require(!Name.isEmpty,"Name must not be empty.")
  }
  case class UserCreated(Email:String)
  case object UserAlreadyExists

  object User extends DefaultJsonProtocol {
    implicit val format = jsonFormat4(User.apply)
  }

  //createPage
  case class Page(adminEmail: String,Title : String,pageID : String)
  case class PageCreated(Title: String)
  case object PageCreationFailed

  object Page extends DefaultJsonProtocol {
    implicit  val format = jsonFormat3(Page.apply)
  }

  //sendFriendRequest
  case class FriendRequest(fromEmail:String, toEmail:String)
  case object AlreadyFriends
  case object UserNotPresent
  case object FriendRequestSent

  //user idenitifier, add keys here later
  case class UserID(Email:String)
  object UserID extends  DefaultJsonProtocol {
    implicit  val format = jsonFormat1(UserID.apply)
  }
  
   //wallwrite or pagewrite
  case class fbPost(fromEmail:String, toEmail:String, data:String,postID:String = "defaultID")
  case class pagePost(fromEmail:String,postID:String,data:String)
  case object PostSuccess
  case object PostFail
  
  object FriendRequest extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(FriendRequest.apply)
  }

  object fbPost extends DefaultJsonProtocol {
    implicit val format = jsonFormat4(fbPost.apply)
  }

  object pagePost extends  DefaultJsonProtocol {
    implicit  val format = jsonFormat3(pagePost.apply)
  }

}

