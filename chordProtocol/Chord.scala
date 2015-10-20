import akka.actor._
import scala.math._ //for absolute value
import scala.collection.mutable.ListBuffer //for storing neighbor list : https://www.cs.helsinki.fi/u/wikla/OTS/Sisalto/examples/html/ch17.html
import scala.util._ //for random number

object Chord extends App {
  val system = ActorSystem("Chord")

  class Node(id: Int) extends Actor {
    var m = 8
    var fingerTable = Array.ofDim[int](m,2)
    var 
  }
}
