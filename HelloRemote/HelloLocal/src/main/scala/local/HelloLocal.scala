package local
import common._
import java.security.MessageDigest
import scala.collection.mutable.ListBuffer
import scala.util

import akka.actor._

object Local extends App {

  implicit val system = ActorSystem("LocalSystem")
  val localActor = system.actorOf(Props[LocalActor], name = "LocalActor")  // the local actor
		
  //localActor ! AssignWork (5, 500000000)								// start the action

}


class LocalActor extends Actor {

	val remote = context.actorFor(("akka.tcp://MiningRemoteSystem@127.0.0.1:5150/user/Master"))
	
	remote ! "nodeUp"

  var counter = 0
	var difficulty = 0
	var base = "ankurbagchi"

  def receive = {
		
		case AssignWork(diff, size) =>
		
				difficulty = diff
        println("LocalActor received AssignWork: " + size)
				
				var bitcoins = new ListBuffer[Bitcoin]()

				
        for (counter <- 1 to size) {
				
            //sender ! "Hello back to you"
							
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
							println (toFind + "\t" + s)
						}
        }

				//sender ! ReturnedBitcoins(bitcoins)				
				println ("Terminated")
				
  }
	
}



