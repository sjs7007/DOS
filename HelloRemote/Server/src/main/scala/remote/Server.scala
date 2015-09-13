package remote

import akka.actor._ 
import scala.collection.mutable.ListBuffer
import common._
import akka.routing.RoundRobinRouter
import java.security.MessageDigest

class clientData(x: Int, act: ActorRef, ws: Int)
{
  var clientId = x
	var actor = act
	var workSize = ws
	var workRatio : Double = 0
	var lastTimeStamp : Double = 0
}

case class Difficulty (n: Int)

class Master extends Actor {

  private var clientList = new ListBuffer[clientData]()
  private var nNodes = 0
  private var bitCoinList = new ListBuffer[Bitcoin]()
  var (workSize,difficulty,threshold)=(100000,5,3)
	var numberOfProcessors = Runtime.getRuntime().availableProcessors()
	val worker = context.actorOf(Props[Miner].withRouter(RoundRobinRouter(nrOfInstances=numberOfProcessors)))
  
	for (n <- 1 to numberOfProcessors)
					worker ! AssignWork (difficulty, workSize/(numberOfProcessors))
	
  def receive = {
	    
		case Difficulty (n) => difficulty = n
			
    case ClientState(node, n) =>

			n match {
				case 1 => // New client
				println("Contacted by node #" + (nNodes+1))
				clientList += new clientData(nNodes, sender, workSize)
				sender ! ClientState (nNodes, 1)
				nNodes += 1
				sender ! AssignWork(difficulty,workSize)
				case 0 =>
			}
			
     case BitcoinList(bitcoins) => 
			
			// Print received bitcoins
		 
      for(i<-0 until bitcoins.length) {
        println(bitcoins(i).input+" : "+bitcoins(i).hash)
        bitCoinList += new Bitcoin(bitcoins(i).input,bitcoins(i).hash)
      }
			
			var node = -1
			
			for(i<-0 until clientList.length)
				if (clientList(i).actor == sender)
					node = i
					
			if (node > -1) {
			
			// Assign work to Client's Miners
						
			/* This next part dynamically modifies work size per node to maximize efficiency.
				 Efficiency is calculated according to units of work done per unit time. 
				 Work sent to a node is started at 100000 units of work and increased or 
				 decreased according to the work ratio (work done per unit time). 
				 
				 First we determine which node requires work, then based on the last cycle,
				 we calculate the new work ratio. */
			
			if (((clientList(node).workSize)/(System.nanoTime-clientList(node).lastTimeStamp))*0.97 > clientList(node).workRatio)  {
				clientList(node).workRatio = (clientList(node).workSize/(System.nanoTime-clientList(node).lastTimeStamp))
				clientList(node).workSize = (clientList(node).workSize*1.25).toInt
				}
			else if (((clientList(node).workSize)/(System.nanoTime-clientList(node).lastTimeStamp)) < clientList(node).workRatio*0.97){
				clientList(node).workRatio = (clientList(node).workSize.toDouble/(System.nanoTime-clientList(node).lastTimeStamp))
				clientList(node).workSize = (clientList(node).workSize*0.8).toInt
				
				if (clientList(node).workSize < 100)
					clientList(node).workSize = 100
	
			}
			clientList(node).lastTimeStamp = System.nanoTime
			
			
				sender ! AssignWork(difficulty,clientList(node).workSize)
				
				}
			else {
			
				// Assign work to Server's Miners
			
				for (n <- 1 to numberOfProcessors)
					sender ! AssignWork (difficulty, workSize/(numberOfProcessors))
			
			}
			
  }
}

object ServerStarter extends App {
  val system = ActorSystem("MiningRemoteSystem") //to use actor boiler plate
  val remoteActor = system.actorOf(Props[Master], name="Master") //create actor of type Master
  remoteActor ! Difficulty(args(0).toInt)
}

class Miner extends Actor {

	var counter = 0
	var difficulty = 0
	var base = "ankurbagchi"
	
	def receive = {
		
		case AssignWork(diff, size) =>
		
				difficulty = diff
				
				var bitcoins = new ListBuffer[Bitcoin]()
	
				for (counter <- 1 to size) {
				
							var r = new scala.util.Random
							var sb = new StringBuilder
							for (i <- 1 to 15) {
								sb.append(r.nextPrintableChar)
							}
							
						var toFind = base + sb.toString
		
						var digest =  MessageDigest.getInstance("SHA-256")
						var hash = digest.digest(toFind.getBytes("UTF-8"))
						var hexString = new StringBuffer()

						var i = 0
		
						for ( i <- 0 to (hash.length-1)) {
							var hex = Integer.toHexString(0xff & hash(i))
							if(hex.length() == 1) hexString.append('0')
							hexString.append(hex)
						}
		
						var s = hexString.toString();
						
						var isDif = 1;
		
						for ( i <- 0 to (difficulty-1))
						{
							if (s.charAt(i) != '0')
							isDif = 0;
						}
		
						if (isDif == 1) {
							bitcoins += Bitcoin(toFind, s)
							//println (toFind + "\t" + s)
						}
        }
				sender ! BitcoinList(bitcoins)				
				}
} 
