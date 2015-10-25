import akka.actor._
import scala.math._ //for absolute value
import scala.collection.mutable.ListBuffer //for storing neighbor list : https://www.cs.helsinki.fi/u/wikla/OTS/Sisalto/examples/html/ch17.html
import scala.util._ //for random number
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import util.control.Breaks._


case class  getId()
case class  getPredecessor()
case class  setPredecessor(ref: ActorRef)
case class  getSuccessor()
case class  setSuccessor(ref: ActorRef)
case class  findClosestPrecedingFinger (id: Int)
case class  findSuccessorMsg(id: Int)
case class startJoin (ref: ActorRef)
case class updateFingerTableMsg (row: Int, fData: fingerData)

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


class fingerData(id: Int, x: ActorRef) {
  var nodeId : Int = id
  var actorReference : ActorRef = x
}

class Node(id: Int) extends Actor {
  var m = 8
  var nodeId = id
  var source : ActorRef = null
  var successor : ActorRef = self
  var predecessor : ActorRef = self
  var fingerTable : Array[fingerData] = new Array[fingerData](m)
  for(i<- 0 until m)
      fingerTable(i) = new fingerData(-1,null)
  var actorReference : ActorRef = self
  
  implicit var timeout = Timeout(5 seconds)

  def receive = {
    case getId() => 
      println("Got getID request message at: " + nodeId)
      sender ! nodeId

    case getPredecessor() =>
      println("Got getPredecessor request message at: " + nodeId)
      sender ! predecessor

    case setPredecessor(nref: ActorRef) =>
      println("Got setPredecessor message at: " + nodeId)
      predecessor = nref


    case getSuccessor() =>
      println("Got getSuccessor request message at: " + nodeId)
      sender ! successor

    case setSuccessor(nref: ActorRef) =>
      println("Got setSuccessor message at: " + nodeId)
      successor = nref


    case findClosestPrecedingFinger(nid) =>
      println("Got findClosestPrecedingFinger message at: " + nodeId)
      sender ! closestPrecedingFinger (nid)

    case findSuccessorMsg(nid) =>
      println("Got findSuccessorMsg request message at: " + nodeId)
      sender ! findSuccessor(nid)
      
    case updateFingerTableMsg (i, fData) => updateFingerTable (i, fData)

    case startJoin(nref) => source = nref
                            join(source)
  }  

  //ask node n to find id's successor
  def findSuccessor(nid: Int) : ActorRef = {
  
       println ("ds0000")


  println ("Running findSuccessor from node: " + nodeId + " for nid: " + nid)
  
    var nDash : ActorRef = findPredecessor(nid)
   // return nDash.fingerTable[0].node //0th node has successor
       println ("ds1111")


 /*   var future = nDash ? getSuccessor() 
    var nDashSucessor = Await.result(future,timeout.duration).asInstanceOf[ActorRef] 
*/  var nDashSucessor = callFutureActor(self,nDash,-1,"getSuccessor")

   /* var tmp = Await.result(nDashSucessor ? getId(),timeout.duration).asInstanceOf[Int]
    */
    var tmp = callFutureInt(self,nDashSucessor,"getId")
    println ("Running findSuccessor from node: " + nodeId + " for nid: " + nid + "\nResult is: " + nDashSucessor)
    println ("ds2222")
    return nDashSucessor
  }

  //ask node n to find id's predecessor
  def findPredecessor(nid: Int) : ActorRef = {
    var nDash : ActorRef = self

    var nDashNodeId = nid
    println("inside find predecessor")
    println ("Asking successor ID from " + id)
    /*var future = successor ? getId()
    var nDashSuccessorNodeId = Await.result(future,timeout.duration).asInstanceOf[Int] 
    #1 replacement
    */

    var nDashSuccessorNodeId = callFutureInt(self,successor,"getId")
    println("still inside find predecessor")
    
    var count : Int =0
    while(!In(nid,nDashNodeId,nDashSuccessorNodeId, false, true)) { //id not in (nDash,nDash.succ] 
    count=count+1
    println(nid+"..."+nDashNodeId+"...."+nDashSuccessorNodeId)
    if(count>4)
      break
   // println ("IN THE WHILE YO " + id)
    
      nDash = callFutureActor(self,nDash,nid,"findClosestPrecedingFinger")

      nDashNodeId = callFutureInt(self,nDash,"getId")
      println("nDash : "+nDashNodeId)

      var nDashSuccessor = callFutureActor(self,nDash,0,"getSuccessor")

      nDashSuccessorNodeId = callFutureInt(self,nDashSuccessor,"getId")
    }

    println ("still Running findPredecessor from node: " + nodeId + " for nid: " + nid + "\nResult is: " + nDash)

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

  println ("Running JOIN from node: " + nodeId + " to join using actor: " + nDash)


  if (nDash != null) {
  
  println ("Running JOIN from node: " + nodeId + " to join using actor: " + nDash)

      initFingerTable(nDash)

      updateOthers(nDash)
    }

    else 
    {
    
    println ("Running JOIN from node: " + nodeId + ". nDash is null.")
    
      for(i <- 0 to (m-1)) { // n is the only node in the networks
        fingerTable(i).nodeId = nodeId
        fingerTable(i).actorReference = self
      }
      successor = self
      predecessor = self 

       for (i <- 0 to (m-1)) 
         println (fingerTable(i).nodeId + " " + fingerTable(i).actorReference)

    
  

    }
   
  }
  
  // Init Finger Table
  def initFingerTable (nDash: ActorRef){
  
    println ("Running initFingerTable from node: " + nodeId + " using actor: " + nDash)

    /* #7
    fingerTable(0).nodeId = Await.result((nDash ? getId()),Timeout(1 seconds).duration).asInstanceOf[Int]
    
    fingerTable(0).actorReference = Await.result((nDash ? findSuccessorMsg((nodeId+1)%m)),timeout.duration).asInstanceOf[ActorRef] 

    predecessor = Await.result((fingerTable(0).actorReference ? getPredecessor()),timeout.duration).asInstanceOf[ActorRef] 
    */

    fingerTable(0).nodeId = callFutureInt(self,nDash,"getId")
    fingerTable(0).actorReference = callFutureActor(self,nDash,(nodeId+1)%m,"findSuccessorMsg")
    predecessor = callFutureActor(self,fingerTable(0).actorReference,-1,"getPredecessor")

    successor = fingerTable(0).actorReference
    

    fingerTable(0).actorReference ! setPredecessor(self)
  
    for (i <- 0 to (m-2)) {
      if (In(id + scala.math.pow(2,i+1).toInt, id, fingerTable(i).nodeId, true, false)) {
        fingerTable(i+1).nodeId = fingerTable(i).nodeId
        fingerTable(i+1).actorReference = fingerTable(i).actorReference
        }
      else {
      //  var newNode = nDash.findSuccessor(fingerTable(i+1).nodeId)
     //   var newNode = Await.result(nDash ? findSuccessorMsg(fingerTable(i+1).nodeId),timeout.duration).asInstanceOf[ActorRef] 
          var newNode = callFutureActor(self,nDash,fingerTable(i+1).nodeId,"findSuccessorMsg")

       // fingerTable(i+1).nodeId = Await.result((newNode ? getId),timeout.duration).asInstanceOf[Int]
        fingerTable(i+1).nodeId = callFutureInt(self,newNode,"getId")
        fingerTable(i+1).actorReference = newNode
      }
    }
    
     for (i <- 0 to (m-1)) 
  println (fingerTable(i).nodeId + " " + fingerTable(i).actorReference)

    
  }
  
  // Update Others
  def updateOthers (nDash: ActorRef) {
  
  var pre : ActorRef = null
  
  for (i <- 0 to (m-1)) {
    pre = findPredecessor(nodeId-scala.math.pow(2, i).toInt)
    pre ! updateFingerTableMsg(i, new fingerData(nodeId, self))
  }
 
 for (i <- 0 to (m-1)) 
  println (fingerTable(i).nodeId + " " + fingerTable(i).actorReference)
 
  
  }
  
    //if s is ith finger of n, update n's finger table with s
  def updateFingerTable (i: Int, s: fingerData) {
    //  def In(x:Int, lower:Int,higher:Int, includeLower: Boolean, includeUpper: Boolean) : Boolean = {
    val iThFingerId = Await.result(fingerTable(i).actorReference ? getId(),timeout.duration).asInstanceOf[Int]
   // val sId = s.nodeId
    println("sId : "+s.nodeId+" iThFingerId : "+iThFingerId)
    if(In(s.nodeId,nodeId,iThFingerId,true,false)) {
      fingerTable(i) = s
      predecessor ! updateFingerTable(i,s)
    }
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

  def callFutureInt(from: ActorRef,to: ActorRef,msg: String) : Int = {
    var tmp : Int = -1
    if(from==to) {
      if(msg=="getId") {
        tmp = nodeId
      }
    }
    else {
      if(msg=="getId") {
        implicit val timeout = Timeout(1 seconds)
        tmp = Await.result(to ? getId(),timeout.duration).asInstanceOf[Int]
      }
    }
    return tmp
  }

  def callFutureActor(from : ActorRef,to : ActorRef,nodeId: Int,msg: String) : ActorRef = {
    var tmp : ActorRef = null
    if(from==to) {
      if(msg=="getSuccessor") {
        tmp = successor
      }
      else if(msg=="findClosestPrecedingFinger") {
        tmp = closestPrecedingFinger(nodeId)
      }
      else if(msg=="getPredecessor") {
        tmp = predecessor
      }
      else if(msg=="findSuccessor") {
        tmp =  findSuccessor(nodeId)
      }
    }

    else {
      implicit val timeout = Timeout(3 seconds)
      if(msg=="getSuccessor") {
          tmp = Await.result(to ? getSuccessor(),timeout.duration).asInstanceOf[ActorRef]
        }
        else if(msg=="findClosestPrecedingFinger") {
          tmp = Await.result(to ? findClosestPrecedingFinger(nodeId),timeout.duration).asInstanceOf[ActorRef]
        }
        else if(msg=="getPredecessor") {
          tmp = Await.result(to ? getPredecessor(),timeout.duration).asInstanceOf[ActorRef]
        }
        else if(msg=="findSuccessorMsg") {
          tmp = Await.result(to ? findSuccessorMsg(nodeId),timeout.duration).asInstanceOf[ActorRef]
        }
    }
    return tmp
  }
}