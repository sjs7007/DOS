/**
 * Created by shinchan on 11/6/15.
 */

import spray.json._

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val personFormat = jsonFormat3(Person)

  case class Person(name: String, fistName: String, age: Long)
  //createUser
  case class User(Email:String, Name: String,Birthday: String,CurrentCity : String) {
    require(!Email.isEmpty, "Emails must not be empty.")
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
  case object CantAddSelf

  //user idenitifier, add keys here later
  case class UserID(Email:String)
  object UserID extends  DefaultJsonProtocol {
    implicit  val format = jsonFormat1(UserID.apply)
  }
  
   //wallwrite
  case class fbPost(fromEmail:String, toEmail:String, data:String)
  case object PostSuccess
  case object PostFail

  //page post
  case class pagePost(fromEmail:String,data:String)
  case object PostFailNotFollowing
  case object FollowSuccess
  case object FollowFail
  case object PageNotPresent
  case object AlreadyFollowingPage

  //upload
  case class Photo(Email:String, Caption: String, Image: Array[Byte])
  object Photo extends DefaultJsonProtocol {
    implicit  val format = jsonFormat3(Photo.apply)
  }

  //image upload
  case class AlbumMetaData(Email:String,Title: String)
  object AlbumMetaData extends  DefaultJsonProtocol {
    implicit val format = jsonFormat2(AlbumMetaData.apply)
  }
  case class AlbumCreated(Title: String)
  case object AlbumCreationFailed
  case class ImageMetaData(Title:String)
  object AlbumCreated extends DefaultJsonProtocol {
    implicit val format = jsonFormat1(ImageMetaData.apply)
  }


  //comments
  case class Comment(fromEmail:String, data: String)
  object Comment extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(Comment.apply)
  }
  //you need to wrap your format constructor with lazyFormat and supply an explicit type annotation:
  //implicit val fooFormat: JsonFormat[Foo] = lazyFormat(jsonFormat(Foo, "i", "foo"))
  //object Comment extends DefaultJsonProtocol {
   // implicit  val format : JsonFormat[Comment] = lazyFormat(jsonFormat(Comment,"commentID","comment"))
  //}


  object FriendRequest extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(FriendRequest.apply)
  }

  object fbPost extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(fbPost.apply)
  }

  object pagePost extends  DefaultJsonProtocol {
    implicit  val format = jsonFormat2(pagePost.apply)
  }

}

