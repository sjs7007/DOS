import akka.actor._
import scala.math._ //for absolute value
import scala.collection.mutable.ListBuffer //for storing neighbor list : https://www.cs.helsinki.fi/u/wikla/OTS/Sisalto/examples/html/ch17.html
import scala.util._ //for random number

object Simulate extends App {
  
    val system = ActorSystem("Gossip")
    val node1 = system.actorOf(Props[Node],name="node")
    val node2 = system.actorOf(Props[Node],name="node2")
  
  //node1.neighborList += node2
  //node2.neighborList += node1
 // node1 ! addNeighbor(node2)
 // node2 ! addNeighbor(node1)
  //node1 ! start 
  
  case class gossipMsg(s: Double,w: Double)
  case class Start
  case class addNeighbor(x: ActorRef)

  class Node(id: Double) extends Actor {
    var nodeId = id //actor number
    var s : Double= nodeId 
    var w : Double= 1
    var ratio : Double= s/w
    var gossipRecCount=0 // used for termination condition
    var ratioChange=0 //used for termination condition
    var rumorCount=0
    var change = new Array[Double](3)
    var neighborList = new ListBuffer[ActorRef]

    def shouldITerminate() = {
      var newRatio = s/w
      change(gossipRecCount%3) = math.abs(newRatio - ratio)
      ratio = newRatio
      gossipRecCount = gossipRecCount + 1

      if(gossipRecCount>=3) { //if difference is 
        var sumChange = change(0)+change(1)+change(2)
        if(sumChange<Math.pow(10,-10)) {
          // terminate the actor
          context.stop(self)
        }
      }
    }

    def receive = {
      case gossipMsg(s_r,w_r) =>
        println("Received gossip message. ratio = "+ratio)
        s = s + s_r
        w = w + w_r
        rumorCount = rumorCount+1
        shouldITerminate()

        //select neighbor and send gossip if rumorCount <= 10
        if(rumorCount<10) {
          s=s/2
          w=w/2
          ratio=s/w
          println("Forwarding gossip message. ratio = "+ratio)
          neighborList(Random.nextInt(neighborList.length)) ! gossipMsg(s,w)
        }

      case Start =>
        s=s/2
        w=w/2
        rumorCount=rumorCount+1
        neighborList(Random.nextInt(neighborList.length)) ! gossipMsg(s,w)

      case addNeighbor(x) =>
        neighborList+= x
    }  
  }

}
