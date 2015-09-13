package remote

import akka.actor._ 
import scala.collection.mutable.ListBuffer
import common._

class clientData(x: Int, act: ActorRef, ws: Int)
{
  var clientId = x
	var actor = act
	var workSize = ws
	var workRatio : Double = 0
	var lastTimeStamp : Double = 0
}

class Master extends Actor {

  private var clientList = new ListBuffer[clientData]()
  private var nNodes = 0
  private var bitCoinList = new ListBuffer[Bitcoin]()
  var (workSize,difficulty,threshold)=(100000,5,3)
  
  def receive = {
    
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
			
			
			if (node >= 0) {
			
			/* This next part dynamically modifies work size per node to maximize efficiency.
				 Efficiency is calculated according to units of work done per unit time. 
				 Work sent to a node is started at 1000 units of work and increased or 
				 decreased according to the work ratio (work done per unit time). 
				 
				 First we determine which node requires work, then based on the last cycle,
				 we calculate the new work ratio. */
			
			if (((clientList(node).workSize)/(System.nanoTime-clientList(node).lastTimeStamp))*0.95 > clientList(node).workRatio)  {
				clientList(node).workRatio = (clientList(node).workSize/(System.nanoTime-clientList(node).lastTimeStamp))
				clientList(node).workSize = (clientList(node).workSize*1.25).toInt
				}
			else if (((clientList(node).workSize)/(System.nanoTime-clientList(node).lastTimeStamp)) < clientList(node).workRatio*0.95)  {
				clientList(node).workRatio = (clientList(node).workSize.toDouble/(System.nanoTime-clientList(node).lastTimeStamp))
				clientList(node).workSize = (clientList(node).workSize*0.8).toInt
				
				if (clientList(node).workSize < 100)
					clientList(node).workSize = 100
	
			}
			clientList(node).lastTimeStamp = System.nanoTime
			}
			
		
			
     case BitcoinList(bitcoins) => 
      //println(s"Server received new bitcoin : '$bitcoin'")
      // store in list and print
      for(i<-0 until bitcoins.length) {
        println(bitcoins(i).input+" : "+bitcoins(i).hash)
        bitCoinList += new Bitcoin(bitcoins(i).input,bitcoins(i).hash)
      }
			
			
			
			var node = -1
			
			for(i<-0 until clientList.length)
				if (clientList(i).actor == sender)
					node = i
					
			sender ! AssignWork(difficulty,clientList(node).workSize)
			
			
			
    
  }
}

object HelloRemote extends App {
  val system = ActorSystem("MiningRemoteSystem") //to use actor boiler plate
  val remoteActor = system.actorOf(Props[Master], name="Master") //create actor of type Master
  remoteActor ! "Server active. Please activate remote nodes." //send string to remote actor ie self
  
}


