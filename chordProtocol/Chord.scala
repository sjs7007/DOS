import akka.actor._
import scala.math._ //for absolute value
import scala.collection.mutable.ListBuffer //for storing neighbor list : https://www.cs.helsinki.fi/u/wikla/OTS/Sisalto/examples/html/ch17.html
import scala.util._ //for random number

class Node(id: Int) extends Actor {
  var m = 8
  var nodeId = id
  var fingerTable = Array.ofDim[int](m,2)
  var successor,predecessor

  int closest_Preceding_Finger(id) {
    for(i <- m-1 to 0 by -1) {
      if(fingerTable[i].node in (n,id)) {
        return fingerTable[i].node
      }
    }
  }

}

case class nodeJoin(id: Int)
case class isSucessor()

object Chord extends App {
  val system = ActorSystem("Chord")

}
