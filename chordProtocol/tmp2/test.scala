import util.control.Breaks._

object adsjjs extends App {
  println("dsds")
  breakable {
  for(i<-1 to 10) {
    println(i)
    if(i==5) {
      break
    }
  }
  }
}
