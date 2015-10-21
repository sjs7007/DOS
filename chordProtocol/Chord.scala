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
  ActorRef myReference

  //ask node n to find id's successor
  def findSuccessor(id: Int) : Node = {
    Node nDash = findPredecessor(id)
   // return nDash.fingerTable[0].node //0th node has successor
    return nDash.successor
  }

  //ask node n to find id's predecessor
  def findPredecessor(id: Int) : Node = {
    Node nDash = n 
    while(notIn(id,nDash,nDash.successor)) { //id not in (nDash,nDash.succ] 
      nDash = nDash.closestPrecedingFinger(id) 
    }
    return nDash
  }

  //notIn
  def notIn(x:Int, lower:Int,higher:Int) : Boolean {
    if(x>lower && x <=higher) {
      return false
    }
    return true
  }
}

object Chord extends App {
  val system = ActorSystem("Chord")

}
