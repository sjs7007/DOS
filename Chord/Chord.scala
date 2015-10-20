import akka.actor._
import scala.math._ //for absolute value
import scala.collection.mutable.ListBuffer //for storing neighbor list : https://www.cs.helsinki.fi/u/wikla/OTS/Sisalto/examples/html/ch17.html
import scala.util._ //for random number

object Main extends App {
    val system = ActorSystem("Chord")
		
		var n = 100 // no of nodes
		
		var nodeList = Array.ofDim[ActorRef](n)
		
		var circleSize = 1
		var m = 0
		
		while (circleSize < n){
				circleSize *= 2
				m += 1
		}
		
		for (i <- 0 to (n-1))
			nodeList(i) = system.actorOf(Props(new Node(i+1)))
			
		var nextPointer = 0
			
		for (i <- 0 to (n-1)) {
			for (j <- 0 to (m-1)) {
				nodeList(i) ! addFinger(findNext(nodeList, (i + scala.math.pow(2,j).toInt)%circleSize))
			}
		}
}

def findNext (nodeList: Array, index: Int) : ActorRef = {
   
	 while (nodeList(index%nodeList.length) == null)
		index++
	 
   return nodeList(index%nodeList.length)
}

case class addFinger (actor: ActorRef)

class Node(id: Int) extends Actor {
    var nodeId = id //actor number
		var fingerList = new ListBuffer[ActorRef]()
		def receive = {
		case addFinger(actor) => fingerList += actor
		}
}