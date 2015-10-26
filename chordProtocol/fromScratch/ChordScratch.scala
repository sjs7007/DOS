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
import scala.collection.mutable.ListBuffer
import java.math.BigInteger
import scala.concurrent.duration.Duration
import akka.japi.Function
import java.util.concurrent.Callable
import scala.concurrent._ //Futures, Promises and goodies  
import scala.concurrent.ExecutionContext.Implicits.global //"Thread pool" to run futures 

object Chord extends App {

case class  getId()
case class join (ref: ActorRef)
case class findClosestPrecedingFinger (requestingActor: ActorRef, id: Int)
case class takePredAndJoin (fLine: fingerData)
case class takePredecessor(ref: fingerData) // overloaded: update self or send
case class addKey (id: Int, data: Int)
case class findKey (id: Int, hops: Int)
case class acceptKey (data: Int)
case class takeTables () // used while joining, take the whole fingertable of your buddy
case class takeFinger (line: Int) // take a single finger, mostly used to get successor of successor, etc
case class updateTables (ref: ActorRef) // Send to nodes periodically to make them update tables

case class activeRequestState () // Activated when a node is ready to send and receive requests
case class requestData (from: fingerData, to: Int, hops: Int) // Will request data from nodes and keep hopping
case class gotData () // Will send node back to active request state after getting data

  var system = ActorSystem("Chord") 
  var actorList : Array [ActorRef] = new Array[ActorRef](15000)
  var busy : Boolean = false
  actorList (0) = system.actorOf(Props(new Node()))
  actorList(0) ! join (null)
  var n = 500 // CHANGE THIS FOR DIFFERING AMOUNT OF NODES
  var m: Int = 19
  var globalHopCount: Int = 0
  
  for (i <- 1 to n) {
    busy = true
    actorList (i) = system.actorOf(Props(new Node())) 
    actorList (i) ! join (actorList (0))
    while (busy) {print ("")} 
    
    for (j <- 1 to i) {
    busy = true
        actorList(j) ! updateTables(null) // THIS IS INEFFICIENT, I WILL FIX IT SOMETIME IF THERE IS TIME BUT FOR NOW IT WORKS FINE
        while (busy) {print ("")}
      }
      println (i)
    }
    
    println ("DONE!")

    /*
    
  for (i <- 0 to (n-1)) {
        busy = true
        actorList(i) ! "printFingers"
        while (busy) {print ("")}
      }
      */
      for (i <- 1 to n) 
        actorList (0) ! addKey(sha(i.toString, m), i)


      Thread.sleep(500)
      busy = false

      for( i<-1 to n ) {
        busy = true
        actorList(0) ! findKey(sha(i.toString,m), 0)
         while (busy) {print ("")}
         Thread.sleep(5)
      }

      Thread.sleep(500)


      println("Total hop count is : "+globalHopCount)

class fingerData(id: Int, x: ActorRef) {
  var nodeId : Int = id
  var actorReference : ActorRef = x
}

class Node () extends Actor {
  
  var myID : Int = 0
  myID = sha(self.path.toString(), m) // myID is the first m bits of SHA-1 of path
  //val buf = new ListBuffer[Int]   
  var dataEntries = new ListBuffer[Int]
  var predecessor : fingerData = new fingerData (myID, self)
  var fingerTable : Array[fingerData] = new Array[fingerData](m)
  implicit val t = Timeout(5 seconds)
  
  for(i<- 0 to (m-1))
        fingerTable(i) = new fingerData(1,self)
      
  def receive = {
  case getId () =>
    sender ! myID
    
  case takeFinger (i) =>
    var tempFinger = new fingerData (fingerTable(i).nodeId, fingerTable(i).actorReference)
    sender ! tempFinger
  
  case join (ref) => // Tell a node to join the network
    
    if (ref != null)
        ref ! findClosestPrecedingFinger (self, myID)
    else myID = 1
     
  case findClosestPrecedingFinger (from, id) => // Find the closest preceding node to "from" - this is for JOIN
            
      var done : Boolean = false
      var maxFingerBelowId : fingerData = new fingerData (-1, null)
           
     var current : fingerData = new fingerData (myID, self)
     
     if (predecessor.actorReference == self)
        from ! takePredAndJoin (new fingerData (current.nodeId, current.actorReference))
     
     else if (fingerTable (0).nodeId > id || fingerTable(0).nodeId == 1)
      from ! takePredAndJoin (new fingerData (current.nodeId, current.actorReference))
      
     else {
       var done : Boolean = false
       for (i <- 0 to (m-2)) {
        if (fingerTable(i).nodeId < id && fingerTable (i+1).nodeId > id) {
          fingerTable(i).actorReference ! findClosestPrecedingFinger (from, id)
          done = true
        } 
      }
      if (!done) {
      var largest : Int = 0
      
      for (i <- 0 to (m-1))
        if (fingerTable(i).nodeId > largest)
          largest = i
      
        fingerTable(largest).actorReference ! findClosestPrecedingFinger (from, id)
        }
     }

  case acceptKey (id) => dataEntries += id
  println ("Node " + myID + " now has key " + id)

  case addKey (id, data) => 

  var done : Boolean = false
      var maxFingerBelowId : fingerData = new fingerData (-1, null)
           
     var current : fingerData = new fingerData (myID, self)

     
     if (predecessor.actorReference == self)
        fingerTable(0).actorReference ! acceptKey (id)
     
     else if (fingerTable (0).nodeId > id || fingerTable(0).nodeId == 1)
       fingerTable(0).actorReference ! acceptKey (id)
      
     else {
       var done : Boolean = false
       for (i <- 0 to (m-2)) {
        if (fingerTable(i).nodeId < id && fingerTable (i+1).nodeId > id) {
          fingerTable(i).actorReference ! addKey (id, data)
          done = true
        } 
      }
      if (!done) {
      var largest : Int = 0
      
      for (i <- 0 to (m-1))
        if (fingerTable(i).nodeId > largest)
          largest = i
      
        fingerTable(largest).actorReference ! addKey (id, data)
        }
     }


  case findKey (id, hops) => 

  var done : Boolean = false
      var maxFingerBelowId : fingerData = new fingerData (-1, null)
           
     var current : fingerData = new fingerData (myID, self)
     
     if (predecessor.actorReference == self) {
        println("Key "+id+" found at "+fingerTable(0).nodeId + " totap hops:" +globalHopCount)
        globalHopCount += hops
      }
     
     else if (fingerTable (0).nodeId > id || fingerTable(0).nodeId == 1) {
        println("Key "+id+" found at "+fingerTable(0).nodeId + " totap hops:" +globalHopCount)
        globalHopCount += hops
      }
     else {
       var done : Boolean = false
       for (i <- 0 to (m-2)) {
        if (fingerTable(i).nodeId < id && fingerTable (i+1).nodeId > id) {
          fingerTable(i).actorReference ! findKey (id, hops+1)
          done = true
        } 
      }
      if (!done) {
      var largest : Int = 0
      
      for (i <- 0 to (m-1))
        if (fingerTable(i).nodeId > largest)
          largest = i
      
        fingerTable(largest).actorReference ! findKey (id, hops+1)
        }
     }
     busy = false

    
  case takePredecessor (ref: fingerData) => // Overloaded: Take or give predecessor based on whether ref is null
    if (ref != null) 
      predecessor = new fingerData (ref.nodeId, ref.actorReference)
    else sender ! (new fingerData (predecessor.nodeId, predecessor.actorReference))
    
  case takePredAndJoin (ref) => // A node who just joined gets its predecessor from this. Main work done here.
    println ("Node "+ myID + " joining after node " + ref.nodeId)
 
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
        busy = false
        
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
      busy = false
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
