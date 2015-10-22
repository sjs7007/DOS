import akka.actor._
import scala.math._ //for absolute value
import scala.collection.mutable.ListBuffer //for storing neighbor list : https://www.cs.helsinki.fi/u/wikla/OTS/Sisalto/examples/html/ch17.html
import scala.util._ //for random number
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._


object Chord extends App {
  val system = ActorSystem("Chord")
  //var x : Node = new Node(2)
  val tmp = system.actorOf(Props(new Node(1,null))) 

  val tmp2 = system.actorOf(Props(new Node(2,tmp))) 

  //tmp ! getPredecessor()(2)
  
  val tmp3 = system.actorOf(Props(new Node(3,tmp2))) 
  
}

case class  getId()
case class  getPredecessor()
case class  setPredecessor(ref: ActorRef)
case class  getSuccessor()
case class  setSuccessor(ref: ActorRef)
case class  findClosestPrecedingFinger (id: Int) 
case class  findSuccessorMsg(id: Int)

class fingerData(id: Int, x: ActorRef) {
  var nodeId : Int = id
  var actorReference : ActorRef = x
}

class Node(id: Int , source: ActorRef) extends Actor {
  var m = 8
  var nodeId = id
  var successor : ActorRef = null
  var predecessor : ActorRef = null
  var fingerTable : Array[fingerData] = new Array[fingerData](m)
  for(i<- 0 until m)
      fingerTable(i) = new fingerData(-1,null)
  var actorReference : ActorRef = null
  if(source==null) {
    /*successor = self
    predecessor = self
    for(i<- 0 until m) {
      fingerTable(i) = new fingerData(this.nodeId,self,this)
    }*/
    //initFingerTable(source)
    join(source)
  }

  implicit val timeout = Timeout(1 seconds)


  def receive = {
    case getId() => 
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



  }  

  //ask node n to find id's successor
  def findSuccessor(id: Int) : ActorRef = {
    val nDash : ActorRef = findPredecessor(id)
   // return nDash.fingerTable[0].node //0th node has successor
    val future = nDash ? getSuccessor() 
    val nDashSucessor = Await.result(future,timeout.duration).asInstanceOf[ActorRef] 
    return nDashSucessor
  }

  //ask node n to find id's predecessor
  def findPredecessor(id: Int) : ActorRef = {
    println("here")
    var nDash : ActorRef = self
    println("here2")
   // println(nDash.nodeId)
   // println(nDash.successor.nodeId)
    var nDashNodeId = id
    var future = successor ? getId()
    var nDashSuccessorNodeId = Await.result(future,timeout.duration).asInstanceOf[Int] 

    if(!In(id,nDashNodeId,nDashSuccessorNodeId, false, true)) { //id not in (nDash,nDash.succ] 
      println("here3")
  //    nDash = nDash.closestPrecedingFinger(id) 
      future = nDash ? findClosestPrecedingFinger(id)
      nDash = Await.result(future,timeout.duration).asInstanceOf[ActorRef]

      future = nDash ? getId ()
      nDashNodeId = Await.result(future,timeout.duration).asInstanceOf[Int]

      future = nDash ? getSuccessor()
      var nDashSuccessor = Await.result(future,timeout.duration).asInstanceOf[ActorRef]

      future = nDashSuccessor ? getId()
      nDashSuccessorNodeId = Await.result(future,timeout.duration).asInstanceOf[Int] 
    }
//    println("predecessor is "+nDash.nodeId)
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
    if (nDash != null) {
      initFingerTable(nDash)
      updateOthers(nDash)
    }






    else for(i <- 0 to (m-1)) { // n is the only node in the network













      fingerTable(i).nodeId = nodeId
      fingerTable(i).actorReference = self
    }
  }
  
  // Init Finger Table
  def initFingerTable (nDash: ActorRef){

    fingerTable(0).nodeId = Await.result((nDash ? getId()),timeout.duration).asInstanceOf[Int]
    fingerTable(0).actorReference = Await.result((nDash ? getSuccessor()),timeout.duration).asInstanceOf[ActorRef] 
    predecessor = Await.result((fingerTable(0).actorReference ? getPredecessor()),timeout.duration).asInstanceOf[ActorRef] 

    fingerTable(0).actorReference ! setPredecessor(self)
  
    for (i <- 0 to (m-2)) {
      if (In(id + scala.math.pow(2,i+1).toInt, id, fingerTable(i).nodeId, true, false)) {
        fingerTable(i+1).nodeId = fingerTable(i).nodeId
        fingerTable(i+1).actorReference = fingerTable(i).actorReference
        }
      else {
      //  var newNode = nDash.findSuccessor(fingerTable(i+1).nodeId)
        var newNode = Await.result(nDash ? findSuccessorMsg(fingerTable(i+1).nodeId),timeout.duration).asInstanceOf[ActorRef] 
        //fingerTable(i+1).nodeId = newNode.nodeId
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
