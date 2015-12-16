import java.security._



object SHA256 extends App {
 // def main(args: Nothing) {
    val md: MessageDigest = MessageDigest.getInstance("SHA-256")
    val text: String = "This is some text"
    md.update(text.getBytes("UTF-8"))
    val digest: Array[Byte] = md.digest

        /*println(java.util.Arrays.equals("hell".getBytes,"hell".getBytes))


    println(java.util.Arrays.equals("hell".getBytes,"no".getBytes))

      println(Array.equals("hell".getBytes,"hell".getBytes))


    println(Array.equals("hell".getBytes,"no".getBytes))*/


  //}

 // }
}

