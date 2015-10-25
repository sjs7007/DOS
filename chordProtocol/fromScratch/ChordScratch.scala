import akka.actor._
import scala.math._ //for absolute value
import scala.collection.mutable.ListBuffer //for storing neighbor list : https://www.cs.helsinki.fi/u/wikla/OTS/Sisalto/examples/html/ch17.html
import scala.util._ //for random number
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.control.Breaks._

case class  getId()
case class join (ref: ActorRef)
case class findClosestPrecedingFinger (requestingActor: ActorRef, id: Int)
case class takePredAndJoin (fLine: fingerData)
case class takePredecessor(ref: fingerData) // update self or send
case class takeTables ()
case class takeFinger (line: Int)
case class updateTables (ref: ActorRef) // sent by your successor

object Chord extends App {
  var system = ActorSystem("Chord")

  var tmp:ActorRef = system.actorOf(Props(new Node(1)))
  
  tmp ! join(null)
  
  Thread.sleep (500)
  
  tmp ! "printFingers"
  
  Thread.sleep (500)
  
  var actorList : Array [ActorRef] = new Array[ActorRef](10)
  
  actorList (0) = system.actorOf(Props(new Node(1)))
  
  actorList (0) ! join (null)
  
 
  for (i <- 1 to 9) {
    actorList (i) = system.actorOf(Props(new Node(i))) 
    actorList (i) ! join (actorList (i-1))
    Thread.sleep (500)
    }
    
    Thread.sleep (500)

      for (i <- 1 to 9) {
        actorList(i) ! "printFingers"
        Thread.sleep (500)
      }
       
  
}

class fingerData(id: Int, x: ActorRef) {
  var nodeId : Int = id
  var actorReference : ActorRef = x
}

class Node (myID: Int) extends Actor {
  
  var m: Int = 8
  var predecessor : fingerData = new fingerData (-1, null)
  var fingerTable : Array[fingerData] = new Array[fingerData](m)
  implicit val t = Timeout(3 seconds)
      
  def receive = {
  
  case getId () =>
    sender ! myID
    
  case takeFinger (i) =>
    sender ! fingerTable(i)
  
  case join (ref) => // tell a node to join the network
  
    for(i<- 0 to (m-1))
        fingerTable(i) = new fingerData(myID,self)
    predecessor = new fingerData (myID, self)
      
    
      if (ref != null)
        ref ! findClosestPrecedingFinger (self, myID)
    
  case findClosestPrecedingFinger (from, id) => // find the closest preceding node to "from" - this is for JOIN
      for (i <- (m-1) to 0 by -1) {
      println ("Inside the loop for node " + myID)
        if (fingerTable(i).nodeId <= id) {
          if (fingerTable(i).actorReference != self)
            fingerTable(i).actorReference ! findClosestPrecedingFinger (from, id)
          else from ! takePredAndJoin (new fingerData (myID, self))
          break
          }
        else if (i == 0)
          from ! takePredAndJoin (new fingerData(myID, self))
      }
        
  case takePredecessor (ref: fingerData) => // if shouldUpdateSelf with this, or just reply with it
    println ("Got takePredecessor at node "+ myID + " with ref: "+ref)
    if (ref != null)
      predecessor = ref
    else sender ! predecessor
    
  case takePredAndJoin (ref) => // a node who just joined gets its predecessor from this.
    println ("Received pred at node "+ myID + " to join after node " + ref.nodeId)
 
    predecessor = new fingerData (ref.nodeId, ref.actorReference)
    fingerTable = Await.result((ref.actorReference ? takeTables()),t.duration).asInstanceOf[Array[fingerData]]
        
    fingerTable(0).actorReference ! takePredecessor (new fingerData (myID, self))
   
    predecessor.actorReference ! updateTables (self)
    
    for (i <- 1 to (m-2))
      for (j <- 1 to (m-2))
        if (((fingerTable(i).nodeId+math.pow(2,j).toInt)%math.pow(2,m).toInt) == myID)
          fingerTable(i).actorReference ! updateTables(null)
    
  case takeTables () => // send tables to a new node who joined as your successor
    sender ! fingerTable
    
  case updateTables (ref) => // sent by a new node who joined as your successor, decrease all fingers by 1
    if (ref != null) {
      fingerTable(0).actorReference = ref
      fingerTable(0).nodeId = Await.result((ref ? getId()),t.duration).asInstanceOf[Int]
      }
      println ("Updating tables for " + myID)
      
      for (i <- 1 to (m-2)) 
        if (((myID+math.pow(2,i).toInt)%math.pow(2,m).toInt) > fingerTable (i).nodeId)
          fingerTable(i) = fingerTable(i+1)
      
      println ("Updated fingers of node "+ myID)
    for (i <- 0 to (m-1))
      println ("Line " + i + ": " + fingerTable(i).nodeId)
    
  
  case "printFingers" => 
    println ("Fingers of node "+ myID)
    for (i <- 0 to (m-1))
      println ("Line " + i + ": " + fingerTable(i).nodeId)

  }
}