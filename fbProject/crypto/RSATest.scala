import java.io.UnsupportedEncodingException
import java.security.{NoSuchAlgorithmException, KeyPairGenerator,InvalidKeyException}
import javax.crypto.{IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, Cipher}

object RSATest extends App {
    try {
      val kpg = KeyPairGenerator.getInstance("RSA")
      kpg.initialize(1024)
      val kp = kpg.genKeyPair

      val publicKey = kp.getPublic
      val privateKey = kp.getPrivate



      val cipher: Cipher = Cipher.getInstance("RSA")
      cipher.init(Cipher.ENCRYPT_MODE, publicKey)
      val input: Array[Byte] = "test".getBytes
      val cipherData: Array[Byte] = cipher.doFinal(input)
      var temp: String = new String(input, "UTF-8")
      //System.out.println("Input : " + temp)
      //temp = new String(cipherData, "UTF-8")
      //System.out.println("Encrypted Data : " + temp)
      cipher.init(Cipher.DECRYPT_MODE,publicKey)
      val decryptedData: Array[Byte] = cipher.doFinal(cipherData)
      temp = new String(decryptedData, "UTF-8")
      System.out.println("Decrytped Data : " + temp)
    }
    catch {
      case x: UnsupportedEncodingException => {
        System.out.println(x.toString)
      }
      case x: NoSuchAlgorithmException => {
        System.out.println(x.toString)
      }
      case x: NoSuchPaddingException => {
        System.out.println(x.toString)
      }
      case x: BadPaddingException => {
        System.out.println(x.toString)
      }
      case x: InvalidKeyException => {
        System.out.println(x.toString)
      }
      case x: IllegalBlockSizeException => {
        System.out.println(x.toString)
      }
    }
}

