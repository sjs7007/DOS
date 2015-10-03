import akka.actor._
import scala.math._ //for absolute value
import scala.collection.mutable.ListBuffer //for storing neighbor list : https://www.cs.helsinki.fi/u/wikla/OTS/Sisalto/examples/html/ch17.html
import scala.util._ //for random number

object Simulate extends App {
  
    val system = ActorSystem("Gossip")
		
    //val node1 = system.actorOf(Props(new Node(2)),name="node")
    //val node2 = system.actorOf(Props(new Node(3)),name="node2")

		println ("wtf")
		
		var n = 8
		var counter = 1
		var x = 0
		var y = 0
		
		// 3DCube
		
		var a = math.ceil(math.cbrt(n)).toInt
		var b = math.ceil(math.sqrt(n/a)).toInt
		var c = math.ceil(n/(a*b)).toInt + 1
		
		println (a, b, c)
		
		
		//var nodeList = new ListBuffer[ActorRef]
		
		a = n
		b = 1
		c = 1
		
		var nodeList = Array.ofDim[ActorRef](a,b,c)
		
		
		
		/*for (x <- 0 to (a-1))
			for (y <- 0 to (b-1))
				for (z <- 0 to (c-1)) {
					if (counter <= n) {
					println (counter)
					nodeList(x)(y)(z) = system.actorOf(Props(new Node(counter)))
					counter += 1
					}				
				}
		
		for (x <- 0 to (a-1))
			for (y <- 0 to (b-1))
				for (z <- 0 to (c-1)) {
				
				if (nodeList(x)(y)(z) != null) {
				
					if (z < (c-1) && nodeList(x)(y)(z+1) != null)
						nodeList(x)(y)(z) ! addNeighbor (nodeList(x)(y)(z+1))
						
					if (y < (b-1) && nodeList(x)(y+1)(z) != null)
						nodeList(x)(y)(z) ! addNeighbor (nodeList(x)(y+1)(z))
						
					if (x < (a-1) && nodeList(x+1)(y)(z) != null)
						nodeList(x)(y)(z) ! addNeighbor (nodeList(x+1)(y)(z))
					
					if (z > 0 && nodeList(x)(y)(z-1) != null)
						nodeList(x)(y)(z) ! addNeighbor (nodeList(x)(y)(z-1))
						
					if (y > 0 && nodeList(x)(y-1)(z) != null)
						nodeList(x)(y)(z) ! addNeighbor (nodeList(x)(y-1)(z))
						
					if (x > 0 && nodeList(x-1)(y)(z) != null)
						nodeList(x)(y)(z) ! addNeighbor (nodeList(x-1)(y)(z))
					}
				}
		
		
        
        
		
		for (x <- 0 to (a-1))
			for (y <- 0 to (b-1))
				for (z <- 0 to (c-1)) {
				
				if (nodeList(x)(y)(z) != null)
					nodeList (x)(y)(z) ! "printDetails"
				
				}
		
		*/
		// LINEBRO
		
		
		for (x <- 0 to (n-1)) {
		nodeList(x)(0)(0) = system.actorOf(Props(new Node(x+1)))

		if (x > 0) {
			println (x.toString+" "+(x-1).toString())
			nodeList(x)(0)(0) ! addNeighbor (nodeList(x-1)(0)(0))
			nodeList(x-1)(0)(0) ! addNeighbor (nodeList(x)(0)(0))
			}
		}
		
		
		// EVERYWHEREBRO
		/*
		
		for (x <- 0 to (n-1)) {
		nodeList += system.actorOf(Props(new Node(x)))
	
		for (y <- 0 to (x-1)) {
			nodeList(y) ! addNeighbor (nodeList(x))
			}
		}

		for (y <- 0 to (n-2)) {
			nodeList(n-1) ! addNeighbor (nodeList(y))
		}
	
	*/
	
  nodeList(0)(0)(0) ! StartPushSum
	
	
  
  //case class gossipMsg
  case class pushSumMsg(s: Double,w: Double,senderId: Int)
  case class StartGossip
  case class StartPushSum
  case class addNeighbor(x: ActorRef)
  case class gossipMsg(nodeId: Int)
  case class nodeGoingDown(nodeId: Int)

  class Node(id: Int) extends Actor {
    var nodeId = id //actor number
		println ("Initialized node with id " + id)
    var s : Double= id.toDouble
    var w : Double= 1
    var ratio : Double= s/w
    var gossipRecCount=0 // used for termination condition
    var ratioChange=0 //used for termination condition
    var rumorCount=0
    var change = new Array[Double](3)
    var neighborList = new ListBuffer[ActorRef]

    def shouldITerminatePushSum() = {
      var newRatio = s/w
      change(gossipRecCount%3) = math.abs(newRatio - ratio)
      ratio = newRatio
      gossipRecCount = gossipRecCount + 1

      if(gossipRecCount>=3) { //if difference is 
        var sumChange = change(0)+change(1)+change(2)
        if(sumChange<Math.pow(10,-10)) {
          //send message to all neighbors that this node is going down
          for(i<- 0 until neighborList.length) {
            neighborList(i) ! nodeGoingDown(nodeId)
          }

          // terminate the actor
          println("Node "+nodeId.toString()+" terminated.")
          context.stop(self)
        }
      }
    }

    def receive = {
      case pushSumMsg(s_r,w_r,senderId) =>
        println("Node "+nodeId.toString()+" received push sum message from node "+senderId.toString()+". ratio = "+ratio)
        s = s + s_r
        w = w + w_r
        shouldITerminatePushSum() //terminate if not much change in sum for 3 consec rounds
        s=s/2
        w=w/2
        ratio=s/w
        if(neighborList.length==0) {
          println("All neighboring nodes of"+nodeId+" are down. Not sending message.")
        }
        else {
          var receiver=Random.nextInt(neighborList.length)
          // println("Node "+nodeId.toString()+" forwarding push sum message to node "+receiver.toString()+". ratio = "+ratio)
          println("Node "+nodeId.toString()+" forwarding push sum message . ratio = "+ratio+"\n")
          neighborList(receiver) ! pushSumMsg(s,w,nodeId)
        }
        
      case StartPushSum =>
        s=s/2
        w=w/2
        rumorCount=rumorCount+1
        neighborList(Random.nextInt(neighborList.length)) ! pushSumMsg(s,w,nodeId)

      case addNeighbor(x) =>
        neighborList+= x

      case StartGossip =>
        neighborList(Random.nextInt(neighborList.length)) ! gossipMsg(nodeId)

      case nodeGoingDown(nodeId) =>
        println("Received message that node "+nodeId+" going down.")
        neighborList -= sender


      case gossipMsg(senderId) => 
        gossipRecCount = gossipRecCount + 1
        println("Node "+nodeId.toString()+" received gossip msg from node "+senderId.toString()+". Gossip Count="+gossipRecCount+".");
        if(gossipRecCount>=10) {
          println("Node "+nodeId.toString()+ " terminated.\n")
          context.stop(self)
        }
        else {
          if(neighborList.length==0) {
            println("All neighboring nodes of node "+nodeId+" are down. Not sending message.")
          }
          var receiver = Random.nextInt(neighborList.length)
          println("Node "+nodeId.toString()+" forwarding gossip message.\n")
          neighborList(receiver) ! gossipMsg(nodeId) 
        }
				
				
			case "printDetails" =>
				println (id)
				println (neighborList)
    }  
  }

}
