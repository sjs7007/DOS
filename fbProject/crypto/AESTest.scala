import javax.crypto._

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
