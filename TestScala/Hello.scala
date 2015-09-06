import java.security.MessageDigest


import akka.actor._
import akka.actor.ActorSystem


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
  
	println ("Hi")
	
	sha256("ankurbagchi")
	

def sha256(base:String) : String = {
    
		val system = ActorSystem("SenderSystem")
		
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
		
		
		//senderActor ! hexString.toString()
		
        return hexString.toString()
		
		
    

}
	






}