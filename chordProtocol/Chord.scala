import akka.actor._
import scala.math._ //for absolute value
import scala.collection.mutable.ListBuffer //for storing neighbor list : https://www.cs.helsinki.fi/u/wikla/OTS/Sisalto/examples/html/ch17.html
import scala.util._ //for random number

object Chord extends App {
  val system = ActorSystem("Chord")
  //var x : Node = new Node(2)
  val tmp = system.actorOf(Props(new Node(1,null))) 

  val tmp2 = system.actorOf(Props(new Node(2,tmp))) 

  tmp ! getPredecessor(2)
  
 // val tmp3 = system.actorOf(Props(new Node(3,1))) 
  
}

class fingerData(id: Int, x: ActorRef, y: Node) {
  var nodeId : Int = id
  var actorReference : ActorRef = x
  var nodeReference : Node = y
}

case class getPredecessor(id: Int)

class Node(id: Int , source: ActorRef) extends Actor {
  var m = 8
  var nodeId = id
  var successor : Node = null
  var predecessor : Node = null
  var fingerTable : Array[fingerData] = new Array[fingerData](m)
  var actorReference : ActorRef = null
  if(source==null) {
    /*successor = this
    predecessor = this
    for(i<- 0 until m) {
      fingerTable(i) = new fingerData(this.nodeId,this.actorReference,this)
    }*/
    initFingerTable(source)

  }


  //ask node n to find id's successor
  def findSuccessor(id: Int) : Node = {
    val nDash : Node = findPredecessor(id)
   // return nDash.fingerTable[0].node //0th node has successor
    return nDash.successor
  }

  //ask node n to find id's predecessor
  def findPredecessor(id: Int) : Node = {
    println("here")
    var nDash : Node = this 
    println("here2")
    println(nDash.nodeId)
    println(nDash.successor.nodeId)
    if(!In(id,nDash.nodeId,nDash.successor.nodeId, false, true)) { //id not in (nDash,nDash.succ] 
      println("here3")
      nDash = nDash.closestPrecedingFinger(id) 
    }
    println("predecessor is "+nDash.nodeId)
    return nDash
  }

    //In
  def In(x:Int, lower:Int,higher:Int, includeLower: Boolean, includeUpper: Boolean) : Boolean = {
  
  var a = 0
  var b = 0
  
  if (includeLower)
    a = 1
    
   if (includeUpper)
    b = 1
  
    if(lower < (x+a) && (x-b) < higher)
      return true
    return false
  }
  
  // Join
  def join (nDash: Node) {
    if (nDash != null) {
      initFingerTable(nDash)
      updateOthers(nDash)
    }
    else for(i <- 0 to (m-1)) { // n is the only node in the network
      fingerTable(i).nodeId = id
      fingerTable(i).actorReference = this.actorReference
      fingerTable(i).nodeReference = this
    }
  }
  
  // Init Finger Table
  def initFingerTable (nDash: Node){
    fingerTable(0).nodeReference = nDash.successor
    fingerTable(0).actorReference = nDash.successor.actorReference
    predecessor = nDash.successor.predecessor
    successor.predecessor = this
  
    for (i <- 0 to (m-2)) {
      if (In(id + scala.math.pow(2,i+1).toInt, id, fingerTable(i).nodeId, true, false)) {
        fingerTable(i+1).nodeId = fingerTable(i).nodeId
        fingerTable(i+1).actorReference = fingerTable(i).actorReference
        fingerTable(i+1).nodeReference =  fingerTable(i).nodeReference
        }
      else {
        var newNode = nDash.findSuccessor(fingerTable(i+1).nodeId)
        fingerTable(i+1).nodeId = newNode.nodeId
        fingerTable(i+1).actorReference = newNode.actorReference
        fingerTable(i+1).nodeReference = newNode
      }
    }
  }
  
  // Update Others
  def updateOthers (nDash: Node) {
  
  }

 
  //return closest finger preceding id
  def closestPrecedingFinger(id: Int) : Node = {
    var i : Int =0
    for(i <- m-1 to 0 by -1) {
      if(fingerTable(i).nodeId>nodeId && fingerTable(i).nodeId < id) {
        return fingerTable(i).nodeReference
      }
    }
    println("here4")
    return this
  }
  
  

  def receive = {
    case "hello" => 
      println("ds")
      closestPrecedingFinger(2)

    case getPredecessor(id: Int) =>
      findPredecessor(id)

  }  
}
