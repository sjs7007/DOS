import akka.actor._
import scala.math._ //for absolute value
import scala.collection.mutable.ListBuffer //for storing neighbor list : https://www.cs.helsinki.fi/u/wikla/OTS/Sisalto/examples/html/ch17.html
import scala.util._ //for random number

object Chord extends App {
  val system = ActorSystem("Chord")
  //var x : Node = new Node(2)
  val tmp = system.actorOf(Props(new Node(2)))  
  //println(x.nodeId) 
  tmp ! "hello"
}

class fingerData {
  var nodeId = -1
  var actorRefence : ActorRef = null
  var nodeReference : Node = null
}

class Node(id: Int) extends Actor {
  var m = 8
  var nodeId = id
  var successor : Node = null
  var predecessor : Node = null
  var fingerTable : Array[fingerData] = new Array[fingerData](m)
  var actorReference : ActorRef = null

  //ask node n to find id's successor
  def findSuccessor(id: Int) : Node = {
    val nDash : Node = findPredecessor(id)
   // return nDash.fingerTable[0].node //0th node has successor
    return nDash.successor
  }

  //ask node n to find id's predecessor
  def findPredecessor(id: Int) : Node = {
    var nDash : Node = this 
    while(notIn(id,nDash.nodeId,nDash.successor.nodeId)) { //id not in (nDash,nDash.succ] 
      nDash = nDash.closestPrecedingFinger(id) 
    }
    return nDash
  }

  //notIn
  def notIn(x:Int, lower:Int,higher:Int) : Boolean = {
    if(x>lower && x <=higher) {
      return false
    }
    return true
  }

  //return closest finger preceding id
  def closestPrecedingFinger(id: Int) : Node = {
    var i : Int =0
    for(i <- m-1 to 0 by -1) {
      if(fingerTable(i).nodeId>nodeId && fingerTable(i).nodeId < id) {
        return fingerTable(i).nodeReference
      }
    }
    return this
  }

  def receive = {
    case "hello" => 
      println("ds")
      //closestPrecedingFinger(2)
  }
}
