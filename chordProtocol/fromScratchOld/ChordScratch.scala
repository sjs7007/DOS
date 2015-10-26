/*
  Changes I made. 
  1. Put wherever break was there inside a breakable block to get rid of break exception.

  What I don't understand/Possible Issues
  1. makes for n+1 nodes??
  2. for n=0, i.e for 1 node, the finger table is not displayed.

*/


import akka.actor._
import scala.math._ //for absolute value
import scala.collection.mutable.ListBuffer //for storing neighbor list : https://www.cs.helsinki.fi/u/wikla/OTS/Sisalto/examples/html/ch17.html
import scala.util._ //for random number
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.control.Breaks._
import java.security.MessageDigest
import java.math.BigInteger

case class  getId()
case class join (ref: ActorRef)
case class findClosestPrecedingFinger (requestingActor: ActorRef, id: Int, depth: Int)
case class addKey (requestingActor: ActorRef, id: Int, depth: Int)

case class takePredAndJoin (fLine: fingerData)
case class takePredecessor(ref: fingerData) // overloaded: update self or send
case class takeTables () // used while joining, take the whole fingertable of your buddy
case class takeFinger (line: Int) // take a single finger, mostly used to get successor of successor, etc
case class updateTables (ref: ActorRef) // Send to nodes periodically to make them update tables

case class activeRequestState () // Activated when a node is ready to send and receive requests
case class requestData (from: fingerData, to: Int, hops: Int) // Will request data from nodes and keep hopping
case class gotData () // Will send node back to active request state after getting data

object Chord extends App {
  var system = ActorSystem("Chord") 
  var actorList : Array [ActorRef] = new Array[ActorRef](256)
  actorList (0) = system.actorOf(Props(new Node(1)))
  var n = 30 // CHANGE THIS FOR DIFFERING AMOUNT OF NODES
  
  for (i <- 1 to n) {
    actorList (i) = system.actorOf(Props(new Node(i+1))) 
    actorList (i) ! join (actorList (i-1))
    Thread.sleep (n*25) // SLEEP NEEDS TO BE DONE TO AVOID DEADLOCK
    }
    
  for (i <- 0 to n) {
        Thread.sleep (n*25)
        actorList(i) ! updateTables(null) // THIS IS INEFFICIENT, I WILL FIX IT SOMETIME IF THERE IS TIME BUT FOR NOW IT WORKS FINE
      }
  
  for (i <- 0 to n) {
        Thread.sleep (n*25)
        actorList(i) ! "printFingers"
      }

  Thread.sleep(500)

  actorList(0) ! addKey(actorList(0),3,0)
  Thread.sleep(500)
    actorList(0) ! addKey(actorList(0),100,0)
     actorList(1) ! addKey(actorList(0),450,0)

}

class fingerData(id: Int, x: ActorRef) {
  var nodeId : Int = id
  var actorReference : ActorRef = x
}

class Node (mID: Int) extends Actor {
  var m: Int = 8
  var myID : Int = 0
  if (mID != 1)
    myID = sha(self.path.toString(), m) // myID is the first m bits of SHA-1 of path
    //myID=mID
  else myID = 1 // I currently need a base node of ID 1 to start everything off
  
  println (mID + ": " + myID)
  var predecessor : fingerData = new fingerData (myID, self)
  var fingerTable : Array[fingerData] = new Array[fingerData](m)
  implicit val t = Timeout(5 seconds)
  
  for(i<- 0 to (m-1))
        fingerTable(i) = new fingerData(myID,self)
      
  def receive = {
  case getId () =>
    sender ! myID
    
  case takeFinger (i) =>
    var tempFinger = new fingerData (fingerTable(i).nodeId, fingerTable(i).actorReference)
    sender ! tempFinger
  
  case join (ref) => // Tell a node to join the network
        ref ! findClosestPrecedingFinger (self, myID, 0)
    
  case findClosestPrecedingFinger (from, id, depth) => // Find the closest preceding node to "from" - this is for JOIN
      var done : Boolean = false
      var maxFingerBelowId : fingerData = new fingerData (-1, null)
           
     var current : fingerData = new fingerData (myID, self)
     var succ : fingerData = fingerTable(0)
     
     if (myID != 1)
      while (!(current.nodeId < id && (succ.nodeId > id || succ.nodeId < current.nodeId))){
          current = new fingerData (succ.nodeId, succ.actorReference)
       
          if (succ.actorReference != self)
            succ = Await.result((succ.actorReference ? takeFinger(0)),t.duration).asInstanceOf[fingerData]
          else succ = fingerTable(0)
          }
    from ! takePredAndJoin (new fingerData (current.nodeId, current.actorReference))

  case addKey (from, id, depth) => // Find the closest preceding node to "from" - this is for JOIN
      var done : Boolean = false
      var maxFingerBelowId : fingerData = new fingerData (-1, null)
           
     var current : fingerData = new fingerData (myID, self)
     var succ : fingerData = fingerTable(0)
     
     if (myID != 1)
      while (!(current.nodeId < id && (succ.nodeId > id || succ.nodeId < current.nodeId))){
          current = new fingerData (succ.nodeId, succ.actorReference)
       
          if (succ.actorReference != self)
            succ = Await.result((succ.actorReference ? takeFinger(0)),t.duration).asInstanceOf[fingerData]
          else succ = fingerTable(0)
          }

       var insertPos : fingerData = fingerTable(0)

       if (current.actorReference != self)
        insertPos = Await.result(current.actorReference ? takeFinger(0),t.duration).asInstanceOf[fingerData]

        println("key "+id+" insert at location "+insertPos.nodeId)

  case takePredecessor (ref: fingerData) => // Overloaded: Take or give predecessor based on whether ref is null
    println ("Got takePredecessor at node "+ myID + " with ref: "+ref)
    if (ref != null) 
      predecessor = new fingerData (ref.nodeId, ref.actorReference)
    else sender ! (new fingerData (predecessor.nodeId, predecessor.actorReference))
    
  case takePredAndJoin (ref) => // A node who just joined gets its predecessor from this. Main work done here.
    println ("Received pred at node "+ myID + " to join after node " + ref.nodeId)
 
    predecessor = new fingerData (ref.nodeId, ref.actorReference)
    var tempTable = Await.result((ref.actorReference ? takeTables()),t.duration).asInstanceOf[Array[fingerData]]
    
    for (i <- 0 to (m-1))
      fingerTable (i) = new fingerData (tempTable(i).nodeId, tempTable(i).actorReference)
    fingerTable(0).actorReference ! takePredecessor (new fingerData (myID, self))
    predecessor.actorReference ! updateTables (self)
            
  case takeTables () => // send tables to a new node who joined as your successor
    sender ! fingerTable
    
  case updateTables (ref) => // First sent by a new node who joined as your successor, else sent to periodically update
    if (ref != null) {
      fingerTable(0).actorReference = ref
      fingerTable(0).nodeId = Await.result((ref ? getId()),t.duration).asInstanceOf[Int]
      }
      println ("Updating tables for " + myID)
      breakable {
        for (i <- 1 to (m-2)) {
        var succ : fingerData = fingerTable(i)
        var next : fingerData = null
        while (succ.nodeId < ((myID+math.pow(2,i).toInt)%math.pow(2,m).toInt)){
          if (succ.actorReference != self) 
            next = Await.result((succ.actorReference ? takeFinger(0)),t.duration).asInstanceOf[fingerData]
          else next = fingerTable(0)
          if (next.nodeId < succ.nodeId)
            break
          else succ = next
          }
          fingerTable(i) = succ
        }
      }
        
  case activeRequestState () =>
  
  /*
  Randomly send requests after random times
  */
  
  case requestData (from, to, hops) => 
  
  /*
  Check my whole list to see if anyone is the recipient (by 'to' as nodeId)
    If not, do: fingerTable(0).actorReference ! requestData (from, to, (hops+1))
  If someone has it, do: from ! gotData ()
  Update total hops across all messages
  */
  
  case gotData () => self ! activeRequestState ()
        
  case "printFingers" => 
    println ("Fingers of node "+ myID)
    for (i <- 0 to (m-1))
        println ("Pos " + (myID+math.pow(2,i).toInt)%math.pow(2,m).toInt + ": " + fingerTable(i).nodeId)
      println ("Predecessor is: " + predecessor.nodeId + "\n============")
  }
  
  def sha(tobehashed:String, m: Int) :Integer = {
    var message_digest= MessageDigest.getInstance("SHA-1")
    message_digest.update(tobehashed.getBytes())
    var hash_value=message_digest.digest()
    var hexstring= new StringBuffer()
    for ( i <- 0 to (hash_value.length-1)) {
        var hex = Integer.toHexString(0xff & hash_value(i))
        if(hex.length() == 1) hexstring.append('0')
        hexstring.append(hex)
    }
    var hex_string:String=hexstring.toString()
    var binary_string:String= new BigInteger(hex_string, 16).toString(2);
    binary_string=binary_string.substring(0,m-1)
    var hash_int :Integer= Integer.parseInt(binary_string,2)
    return hash_int
  }
}

 
/*

    
    case addKey (from, id, depth) => // Find the closest preceding node to "from" - this is for JOIN
      var done : Boolean = false
      var maxFingerBelowId : fingerData = new fingerData (-1, null)
           
     var current : fingerData = new fingerData (myID, self)
     var succ : fingerData = fingerTable(0)
     
     if (myID != 1)
      while (!(current.nodeId < id && (succ.nodeId > id || succ.nodeId < current.nodeId))){
          current = new fingerData (succ.nodeId, succ.actorReference)
       
          if (succ.actorReference != self)
            succ = Await.result((succ.actorReference ? takeFinger(0)),t.duration).asInstanceOf[fingerData]
          else succ = fingerTable(0)
          }
      
     var temp = current.actorReference
      var tempSuccessor= fingerTable(0).actorReference
     var insertPos = fingerTable(0).nodeId


   //  println(current.nodeId+" -<")
    if(temp!=self) {
      println("dssa")
      tempSuccessor = Await.result(temp ? takeFinger(0),t.duration).asInstanceOf[fingerData].actorReference
      var tmp2= insertPos
      insertPos = Await.result(temp? takeFinger(0),t.duration).asInstanceOf[fingerData].nodeId
      println(tmp2+"----=>"+insertPos)
    }
    

     println(id+" key will be inserted at node "+insertPos)

     */
