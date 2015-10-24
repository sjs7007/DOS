import akka.actor._
import scala.math._ //for absolute value
import scala.collection.mutable.ListBuffer //for storing neighbor list : https://www.cs.helsinki.fi/u/wikla/OTS/Sisalto/examples/html/ch17.html
import scala.util._ //for random number
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._


object Chord extends App {
  var system = ActorSystem("Chord")
  //var x : Node = new Node(2)
  var tmp:ActorRef = system.actorOf(Props(new Node(1))) 
  
  tmp ! startJoin(null)
  
  Thread.sleep(5)

  implicit var timeout = Timeout(1 seconds)

  var tmp2 = system.actorOf(Props(new Node(2))) 

  tmp2 ! startJoin(tmp)

}

case class  getId()
case class  getPredecessor()
case class  setPredecessor(ref: ActorRef)
case class  getSuccessor()
case class  setSuccessor(ref: ActorRef)
case class  findClosestPrecedingFinger (id: Int) 
case class  findSuccessorMsg(id: Int)

case class startJoin (ref: ActorRef)

class fingerData(id: Int, x: ActorRef) {
  var nodeId : Int = id
  var actorReference : ActorRef = x
}

class Node(id: Int) extends Actor {
  var m = 8
  var nodeId = id
  var source : ActorRef = null
  var successor : ActorRef = null
  var predecessor : ActorRef = null
  var fingerTable : Array[fingerData] = new Array[fingerData](m)
  for(i<- 0 until m)
      fingerTable(i) = new fingerData(-1,null)
  var actorReference : ActorRef = self
  
  //join(source)

  implicit var timeout = Timeout(5 seconds)

  def receive = {
    case getId() => 
      println("dsdds")
      sender ! nodeId

    case getPredecessor() =>
      sender ! predecessor

    case setPredecessor(nref: ActorRef) =>
      predecessor = nref


    case getSuccessor() =>
      sender ! successor

    case setSuccessor(nref: ActorRef) =>
      successor = nref


    case findClosestPrecedingFinger(nid) =>
      sender ! closestPrecedingFinger (nid)

    case findSuccessorMsg(nid) =>
      sender ! findSuccessor(nid)

    case startJoin(nref) => source = nref
                            join(source)
  }  

  //ask node n to find id's successor
  def findSuccessor(id: Int) : ActorRef = {
    var nDash : ActorRef = findPredecessor(id)
   // return nDash.fingerTable[0].node //0th node has successor
    var future = nDash ? getSuccessor() 
    var nDashSucessor = Await.result(future,timeout.duration).asInstanceOf[ActorRef] 

    var tmp = Await.result(nDashSucessor ? getId(),timeout.duration).asInstanceOf[Int]
    println("successor of "+id+" is : "+tmp)
    
    return nDashSucessor
  }

  //ask node n to find id's predecessor
  def findPredecessor(id: Int) : ActorRef = {
    println("here")
    var nDash : ActorRef = self
    println("here2")

    var nDashNodeId = id
    if(successor==null) {
      println("this is null bro")
    }
    var future = successor ? getId()
    var nDashSuccessorNodeId = Await.result(future,timeout.duration).asInstanceOf[Int] 

    if(!In(id,nDashNodeId,nDashSuccessorNodeId, false, true)) { //id not in (nDash,nDash.succ] 
      println("here3")

      future = nDash ? findClosestPrecedingFinger(id)
      nDash = Await.result(future,timeout.duration).asInstanceOf[ActorRef]

      future = nDash ? getId ()
      nDashNodeId = Await.result(future,timeout.duration).asInstanceOf[Int]

      future = nDash ? getSuccessor()
      var nDashSuccessor = Await.result(future,timeout.duration).asInstanceOf[ActorRef]

      future = nDashSuccessor ? getId()
      nDashSuccessorNodeId = Await.result(future,timeout.duration).asInstanceOf[Int] 
    }

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
  def join (nDash: ActorRef) {
    println("join1")
    if (nDash != null) {
      println("join2")
      initFingerTable(nDash)
      println ("FINGERTABLE:")
      println(fingerTable)
      updateOthers(nDash)
    }

    else 
    {
      for(i <- 0 to (m-1)) { // n is the only node in the networks
        println("join3")
        fingerTable(i).nodeId = nodeId
        fingerTable(i).actorReference = self
      }
      successor = self
      predecessor = self 
    }
   
  }
  
  // Init Finger Table
  def initFingerTable (nDash: ActorRef){

    fingerTable(0).nodeId = Await.result((nDash ? getId()),Timeout(1 seconds).duration).asInstanceOf[Int]
    
    fingerTable(0).actorReference = Await.result((nDash ? findSuccessor(fingerTable(0).nodeId)),timeout.duration).asInstanceOf[ActorRef] 

    predecessor = Await.result((fingerTable(0).actorReference ? getPredecessor()),timeout.duration).asInstanceOf[ActorRef] 
    
    successor = fingerTable(0).actorReference
    
    fingerTable(0).actorReference ! setPredecessor(self)
  
    for (i <- 0 to (m-2)) {
      if (In(id + scala.math.pow(2,i+1).toInt, id, fingerTable(i).nodeId, true, false)) {
        fingerTable(i+1).nodeId = fingerTable(i).nodeId
        fingerTable(i+1).actorReference = fingerTable(i).actorReference
        }
      else {
      //  var newNode = nDash.findSuccessor(fingerTable(i+1).nodeId)
        var newNode = Await.result(nDash ? findSuccessorMsg(fingerTable(i+1).nodeId),timeout.duration).asInstanceOf[ActorRef] 
     
        fingerTable(i+1).nodeId = Await.result((newNode ? getId),timeout.duration).asInstanceOf[Int]
        fingerTable(i+1).actorReference = newNode
      }
    }
  }
  
  // Update Others
  def updateOthers (nDash: ActorRef) {
  
  }

 
  //return closest finger preceding id
  def closestPrecedingFinger(id: Int) : ActorRef = {
    var i : Int =0
    for(i <- m-1 to 0 by -1) {
      if(fingerTable(i).nodeId>nodeId && fingerTable(i).nodeId < id) {
        return fingerTable(i).actorReference
      }
    }
    return self
  }
}
