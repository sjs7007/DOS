import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import java.security._
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import javax.crypto.Cipher
import java.util._

import com.sun.org.apache.xml.internal.security.utils.Base64

object SHA256 extends App {
 // def main(args: Nothing) {
    val md: MessageDigest = MessageDigest.getInstance("SHA-256")
    val text: String = "This is some text"
    md.update(text.getBytes("UTF-8"))
    val digest: Array[Byte] = md.digest

        println(java.util.Arrays.equals("hell".getBytes,"hell".getBytes))


    println(java.util.Arrays.equals("hell".getBytes,"no".getBytes))

      println(Array.equals("hell".getBytes,"hell".getBytes))


    println(Array.equals("hell".getBytes,"no".getBytes))


  //}

 // }
}

