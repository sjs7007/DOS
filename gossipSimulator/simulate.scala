import akka.actor._
import scala.math._ //for absolute value

object Simulate extends App {
  
  case class gossipMsg(Double s,Double w)

  class Node(id: Int) extends Actor {
    var nodeId = id //actor number
    var s = nodeId 
    var w = 1
    var ratio = s/w
    var gossipRecCount=0 // used for termination condition
    var ratioChange=0 //used for termination condition
    var rumorCount=0

    def shouldITerminate() = {
      gossipRecCount = gossipRecCount + 1
      var newRatio = s/w
      ratioChange = ratioChange + math.abs(newRatio - ratio)
      ratio = newRatio

      if(gossipRecCount%3==0) { //if difference is 
        if(ratioChange<0.0001) {
          // terminate the actor
          context.stop(self)
        }
        else {
          ratioChange=0
        }
      }
    }

    def receive = {
      case gossipMsg(s_r,w_r) =>
        s = s + s_r
        w = w + w_r
        rumorCount = rumorCount+1
        shouldITerminate()

        //select neighbor and send gossip if rumorCount <= 10
        
    }
  }
}
