
import java.io.UnsupportedEncodingException
import java.security.{InvalidKeyException, NoSuchAlgorithmException, SecureRandom}
import javax.crypto.{Cipher, _}
import javax.crypto.spec.IvParameterSpec

object AESTest extends App {
 // def main(args: Array[Nothing]) {
  try {
    val KeyGen = KeyGenerator.getInstance("AES")
    KeyGen.init(128)
    val SecKey = KeyGen.generateKey
    val AesCipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
    val randomNumberGenerator: SecureRandom = new SecureRandom
    val bytes: Array[Byte] = new Array[Byte](16)
    randomNumberGenerator.nextBytes(bytes)
    val byteText: Array[Byte] = "Your Plain Text Here".getBytes
    val initializationVector: IvParameterSpec = new IvParameterSpec(bytes)
    AesCipher.init(Cipher.ENCRYPT_MODE, SecKey, initializationVector)
    val encryptedData: Array[Byte] = AesCipher.doFinal(byteText)
    AesCipher.init(Cipher.DECRYPT_MODE, SecKey, initializationVector)
    val decryptedData: Array[Byte] = AesCipher.doFinal(encryptedData)
    val temp: String = new String(decryptedData, "UTF-8")
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

//  }
}

/*AES without IV
public class AESTest {
  public static void main(String[] args) throws Exception {
    //String FileName = "encryptedtext.txt";
    //String FileName2 = "decryptedtext.txt";

    KeyGenerator KeyGen = KeyGenerator.getInstance("AES");
    KeyGen.init(128);

    SecretKey SecKey = KeyGen.generateKey();

    Cipher AesCipher = Cipher.getInstance("AES");


    byte[] byteText = "Your Plain Text Here".getBytes();

    AesCipher.init(Cipher.ENCRYPT_MODE, SecKey);
    byte[] encryptedData = AesCipher.doFinal(byteText);

    //Files.write(Paths.get(FileName), byteCipherText);


   // byte[] cipherText = Files.readAllBytes(Paths.get(FileName));

    AesCipher.init(Cipher.DECRYPT_MODE, SecKey);
    byte[] decryptedData = AesCipher.doFinal(encryptedData);
    String temp = new String(decryptedData,"UTF-8");
    System.out.println("Decrytped Data : "+temp);
  }
}
*/

/* without iv
object AESTest extends App {
  //def main(args: Array[Nothing]) {
  val KeyGen = KeyGenerator.getInstance("AES")
  KeyGen.init(128)
  val SecKey = KeyGen.generateKey
  val AesCipher = Cipher.getInstance("AES")
  val byteText: Array[Byte] = "Your Plain Text Here".getBytes
  AesCipher.init(Cipher.ENCRYPT_MODE, SecKey)
  val encryptedData: Array[Byte] = AesCipher.doFinal(byteText)
  AesCipher.init(Cipher.DECRYPT_MODE, SecKey)
  val decryptedData: Array[Byte] = AesCipher.doFinal(encryptedData)
  val temp: String = new String(decryptedData, "UTF-8")
  System.out.println("Decrytped Data : " + temp)
  //}
}

*/
