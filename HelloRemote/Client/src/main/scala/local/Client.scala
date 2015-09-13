package local
import common._
import java.security.MessageDigest
import scala.collection.mutable.ListBuffer
import scala.util
import akka.actor._
import akka.routing.RoundRobinRouter

object Local extends App {

  implicit val system = ActorSystem("LocalSystem")
  val client = system.actorOf(Props[Client], name = "Client")  // the local actor
}


class Client extends Actor {

  var start = System.nanoTime
  var nodeID = -1
  var lastWorkSize = 0
  var currentWorkSize = 0

	
  val remote = context.actorFor(("akka.tcp://MiningRemoteSystem@192.168.0.10:5150/user/Master"))
	
  remote ! ClientState(-1, 1)
  
  var minersComplete : Int = 0
  var totalCoinsFound : Int = 0
  var workCycles = 0
  var totalWorkDone : Double = 0
  
  var numberOfProcessors : Int = Runtime.getRuntime().availableProcessors()
  
  var listOfCoins = new ListBuffer[Bitcoin]()
  
  var Server : ActorRef = _

  def receive = {
    
    case AssignWork(diff, size) =>
        
        workCycles += 1
        minersComplete = 0
        currentWorkSize = size
        listOfCoins = new ListBuffer[Bitcoin]()
    
        if (size != lastWorkSize) {
          println("Client work unit size updated : " + size)
          lastWorkSize = size
          }
        
        val worker = context.actorOf(Props[Miner].withRouter(RoundRobinRouter(nrOfInstances=numberOfProcessors)))
        
        for (n <- 1 to numberOfProcessors)
          worker ! AssignWork (diff, size/(numberOfProcessors))
          
        Server = sender

    case BitcoinList (bitcoins) => 
    
      minersComplete += 1
      
      for (x <- 0 until bitcoins.length) 
        listOfCoins += bitcoins(x)
        
      if (minersComplete >= numberOfProcessors) {
        Server ! BitcoinList (listOfCoins)
        totalCoinsFound += listOfCoins.length
        totalWorkDone += currentWorkSize
        
        
        
        if (workCycles %5 == 0) {
                
        println("Total work units done by this node : " + totalWorkDone.toInt)
        println("Total time taken by this node      : " + (System.nanoTime - start) / 1e6 + "ms")
        }
        }
        
    case ClientState(id, a) => nodeID = id
  }
  
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


