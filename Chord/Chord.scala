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
		
		
		
		system.actorOf(Props(new Node(counter, null)))
		//system.actorOf(Props(new Node(counter, ref)))
		
}


case class addFinger (actor: ActorRef)
case class request ()
case class gotTable (t: Table)

class Table(x: Int, act: ActorRef)
{
  var clientId = x
  var actor = act
	var fingerList = new ListBuffer[ActorRef]()
  var predecessor = new Table
}

class Node(id: Int, ref: ActorRef) extends Actor {

		var myTable = new Table (id, ref)
    
    if (ref != null)
      ref ! request()
    else {
      myTable.fingerList += myTable
      myTable.predecessor = myTable
    }
		
		def receive = {
      case addFinger(actor) => fingerList += actor
      case request () => sender ! gotTable(myTable)
      case gotTable (t) =>
		}
}