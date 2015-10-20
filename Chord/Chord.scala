import akka.actor._
import scala.math._ //for absolute value
import scala.collection.mutable.ListBuffer //for storing neighbor list : https://www.cs.helsinki.fi/u/wikla/OTS/Sisalto/examples/html/ch17.html
import scala.util._ //for random number

object Main extends App {
    val system = ActorSystem("Chord")
		
		var n = 100 // no of nodes
		
		var nodeList = Array.ofDim[ActorRef](n)
		
		var circleSize = 1073741824
		var m = 30
		
		var counter = 1
		
		
		
		system.actorOf(Props(new NodeProcess(counter, null)))
		//system.actorOf(Props(new NodeProcess(counter, ref)))
		
		
			/*
		for (i <- 0 to (n-1))
			nodeList(i) = system.actorOf(Props(new Node(i+1)))
		
		
		var nextPointer = 0
		
		for (i <- 0 to (n-1)) {
			for (j <- 0 to (m-1)) {
				nodeList(i) ! (findNext(nodeList, (i + scala.math.pow(2,j).toInt)%circleSize))
			}
		}
		*/
}


//case class addFinger (actor: ActorRef)

case class Node(x: Int, act: ActorRef)
{
  var clientId = x
  var actor = act
	var fingerList = new ListBuffer[ActorRef]()
}

class NodeProcess(id: Int, ref: Node) extends Actor {
    var nodeId = id //actor number
		
		
		if (ref != null)
			println (ref.nodeId)
		
		
		def receive = {
		case addFinger(actor) => fingerList += actor
		}
}