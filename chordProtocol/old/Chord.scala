import akka.actor._
import scala.math._ //for absolute value
import scala.collection.mutable.ListBuffer //for storing neighbor list : https://www.cs.helsinki.fi/u/wikla/OTS/Sisalto/examples/html/ch17.html
import scala.util._ //for random number

class fingerData {
  var nodeId
  ActorRef ref
}

class Node(id: Int) extends Actor {
  var m = 8
  var nodeId = id
  var successor,predecessor
  var fingerTable: Array[fingerData] = new Array[fingerData](m)

  def receive = {
    case findPredecessor (nid, nref) => 
      var pred=-1
      for(i<- 0 until m) {
        if(fingerTable(i).nodeId>nid) {
          pred=i-1
          break
        }
      }

      if (pred < 0) {
        nref ! isPredecessor (this,fingerTable(0).ref)
        fingerTable(0).ref ! isPredecessor (nref, null)
        successor = nref 
      }
      else fingerTable(pred).ref ! findPredecessor (nid, nref)

    case isPredecessor (nref,succ) => // PUT MYSELF AFTER NREF
      predecessor = nref
      if (succ != null)
        successor = succ

    


  }

}

case class isPredecessor (ref: ActorRef, succ: ActorRef)
case class findPredecessor (id: Int, ref: ActorRef)
case class nodeJoin(id: Int)
case class isSucessor()

object Chord extends App {
  val system = ActorSystem("Chord")

}
