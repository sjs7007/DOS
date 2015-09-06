import java.security.MessageDigest


import akka.actor._
import akka.actor.ActorSystem
import scala.util


class Master extends Actor {



 def receive = {
    case "hello" => println("hello back at you")
    case _       => {
	println()
	context.stop(self)
	
	}
  }

  }

 object Main extends App {
  
	println ("Running: ")
	
	sha256("ankurbagchi")
	
	
	while (true) {
    val r = new scala.util.Random
    val sb = new StringBuilder
    for (i <- 1 to 10) {
      sb.append(r.nextPrintableChar)
    }
    sha256 ("ankurbagchi"+sb.toString)
  }

def sha256(base:String) : String = {
    
		//val system = ActorSystem("SenderSystem")
		
		//val senderActor = system.actorOf(Props[Master], name = "senderActor")
		
        val digest =  MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(base.getBytes("UTF-8"))
        var hexString = new StringBuffer()

		var i = 0
		
        for ( i <- 0 to (hash.length-1)) {
            var hex = Integer.toHexString(0xff & hash(i))
            if(hex.length() == 1) hexString.append('0')
            hexString.append(hex)
        }
		
		var s = hexString.toString();
		
		var difficulty = 6;
	
		var isDif = 1;
		
		for ( i <- 0 to (difficulty-1))
		{
		
		if (s.charAt(i) != '0')
			isDif = 0;
		
		}
		
		if (isDif == 1)
		println (base + " " + s)
		
		//senderActor ! hexString.toString()
		
        return hexString.toString()
		
		
    

}
	






}
