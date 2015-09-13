package remote

import akka.actor._ 
import scala.collection.mutable.ListBuffer
import common._

sealed trait ServerMessage
case class assignWork(workSize: Int, nZeros: Int) extends ServerMessage
case class receiveBitcoin(bitcoin: String) extends ServerMessage

var workerRef //store ref to worker of server

class clientData(x: Int)
{
  var clientId = x
}

/*class bitcoin(x: String, y: String)
{
  input = x
  hash = y
}*/



class Master extends Actor {
  //private isMining = false
  private var clientList = new ListBuffer[clientData]()
  private var (nNodes,threshold) = (0,1)
  private var bitCoinList = new ListBuffer[bitcoin]()
  val (workSize,nZeros,threshold)=(10,3,3)
  
  def receive = {
     case "nodeUp" =>
       //add to list of clients
      nNodes += 1
      clientList += new clientData(nNodes)
      sender ! assignWork(workSize,nZeros)
      
     case BitcoinList(bitcoins) => 
      //println(s"Server received new bitcoin : '$bitcoin'")
      // store in list and print
      for(i<-0 until bitcoin.length) {
        println(bitcoin(i).input+" : "+bitcoin(i).hash)
        bitcoinList += new bitcoin(bitcoin(i).input,bitcoin.hash)
      }


      /* if sender is master and threshold fails, dont start again, send stop message */
     if(sender eq workerRef && nNodes >= threshold) {
        sender ! "STOP!"
     }
      
     //else send more work
     sender ! AssignWork(difficulty,workSize)
    
  }
}

/*class Worker extends Actor {
  def receive = {
    case "STOP" => 
      println("Stopping worker on server.")
      context.stop(self)

    case "START" => //no worksize for master actor 
      sender ! findBitcoin(workSize,nZeros)
  }
}*/

object HelloRemote extends App {
  val system = ActorSystem("MiningRemoteSystem") //to use actor boiler plate
  val remoteActor = system.actorOf(Props[RemoteActor], name="RemoteActor") //create actor of type RemoteActor
  remoteActor ! "The RemoteActor is alive" //send string to remote actor ie self
}


