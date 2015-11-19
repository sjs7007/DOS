/**
 * Created by shinchan on 11/6/15.
 */
import spray.json.DefaultJsonProtocol

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val personFormat = jsonFormat3(Person)

  case class Person(name: String, fistName: String, age: Long)
  case class User(Email:String, Name: String,publickey: String)

  case class UserCreated(Email:String)
  case object UserAlreadyExists

  object User extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(User.apply)
  }
}

