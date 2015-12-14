import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import javax.crypto.*;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;



//AES with padding and iv
public class AESTest {
  public static void main(String[] args) throws Exception {
    KeyGenerator KeyGen = KeyGenerator.getInstance("AES");
    KeyGen.init(128);

    SecretKey SecKey = KeyGen.generateKey();

    Cipher AesCipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");


    SecureRandom randomNumberGenerator  = new SecureRandom();
    byte bytes[] = new byte[16];
    randomNumberGenerator.nextBytes(bytes);

    byte[] byteText = "Your Plain Text Here".getBytes();

    IvParameterSpec initializationVector = new IvParameterSpec(bytes);


    AesCipher.init(Cipher.ENCRYPT_MODE, SecKey,initializationVector);
    byte[] encryptedData = AesCipher.doFinal(byteText);


    AesCipher.init(Cipher.DECRYPT_MODE, SecKey,initializationVector);
    byte[] decryptedData = AesCipher.doFinal(encryptedData);
    String temp = new String(decryptedData,"UTF-8");
    System.out.println("Decrytped Data : "+temp);
  }
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