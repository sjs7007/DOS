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
case class takePredAndJoin (fLine: fingerData)
case class takePredecessor(ref: fingerData) // update self or send
case class takeTables ()
case class takeFinger (line: Int)
case class updateTables (ref: ActorRef) // sent by your successor

object Chord extends App {
  var system = ActorSystem("Chord") 
  var actorList : Array [ActorRef] = new Array[ActorRef](256)
  actorList (0) = system.actorOf(Props(new Node(1)))
  var n = 4
  
  for (i <- 1 to n) {
    actorList (i) = system.actorOf(Props(new Node(14*i+1))) 
    actorList (i) ! join (actorList (i-1))
    Thread.sleep (500)
    }
    
  for (i <- 0 to n) {
        Thread.sleep (500)
        actorList(i) ! updateTables(null)
      }
  
  for (i <- 0 to n) {
        Thread.sleep (500)
        actorList(i) ! "printFingers"
      }
    Thread.sleep (50)

}

class fingerData(id: Int, x: ActorRef) {
  var nodeId : Int = id
  var actorReference : ActorRef = x
}

class Node (mID: Int) extends Actor {
  var m: Int = 8
  var myID : Int = 0
  if (mID != 1)
    myID = sha(self.path.toString(), m)
  else myID = 1
  
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
  
  case join (ref) => // tell a node to join the network
      
      if (ref != null)
        ref ! findClosestPrecedingFinger (self, myID, 0)
    
  case findClosestPrecedingFinger (from, id, depth) => // find the closest preceding node to "from" - this is for JOIN
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
       
  case takePredecessor (ref: fingerData) => // if not null set to this, or just reply with it
    println ("Got takePredecessor at node "+ myID + " with ref: "+ref)
    if (ref != null) 
      predecessor = new fingerData (ref.nodeId, ref.actorReference)
    else sender ! (new fingerData (predecessor.nodeId, predecessor.actorReference))
    
  case takePredAndJoin (ref) => // a node who just joined gets its predecessor from this.
    println ("Received pred at node "+ myID + " to join after node " + ref.nodeId)
 
    predecessor = new fingerData (ref.nodeId, ref.actorReference)
    var tempTable = Await.result((ref.actorReference ? takeTables()),t.duration).asInstanceOf[Array[fingerData]]
    
    for (i <- 0 to (m-1))
      fingerTable (i) = new fingerData (tempTable(i).nodeId, tempTable(i).actorReference)
    
    fingerTable(0).actorReference ! takePredecessor (new fingerData (myID, self))
   
    predecessor.actorReference ! updateTables (self)
            
  case takeTables () => // send tables to a new node who joined as your successor
    sender ! fingerTable
    
  case updateTables (ref) => // sent by a new node who joined as your successor
    if (ref != null) {
      fingerTable(0).actorReference = ref
      fingerTable(0).nodeId = Await.result((ref ? getId()),t.duration).asInstanceOf[Int]
      }
      println ("Updating tables for " + myID)
 
      for (i <- 1 to (m-2)) {
      
        var succ : fingerData = fingerTable(i)
        var next : fingerData = null
      
        while (succ.nodeId < ((myID+math.pow(2,i).toInt)%math.pow(2,m).toInt)){
          if (succ.actorReference != self) {
          
            next = Await.result((succ.actorReference ? takeFinger(0)),t.duration).asInstanceOf[fingerData]
            }
          else next = fingerTable(0)
          
          if (next.nodeId < succ.nodeId)
            break
          else succ = next
          }
          fingerTable(i) = succ
        }
        
  case "printFingers" => 
    println ("Fingers of node "+ myID)
    for (i <- 0 to (m-1))
      if (fingerTable(i) != null)
        println ("Pos " + (myID+math.pow(2,i).toInt)%math.pow(2,m).toInt + ": " + fingerTable(i).nodeId)
      else println ("Line " + i + " is null")
      
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

 