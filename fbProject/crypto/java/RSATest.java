import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.lang.Override;
import java.lang.String;
import java.lang.System;
import java.security.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

class RSATest {
    public static void main(String args[]) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024); //length of key
            KeyPair kp = kpg.genKeyPair(); //gen pub private
            Key publicKey = kp.getPublic();
            Key privateKey = kp.getPrivate();

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE,publicKey);

            byte[] input = "test".getBytes();
            byte [] cipherData = cipher.doFinal(input);
            String temp = new String(input,"UTF-8");
            System.out.println("Input : "+temp);
            temp = new String(cipherData,"UTF-8");
            System.out.println("Encrypted Data : " + temp);

            cipher.init(Cipher.DECRYPT_MODE,privateKey);
            byte [] decryptedData = cipher.doFinal(cipherData);
            temp = new String(decryptedData,"UTF-8");
            System.out.println("Decrytped Data : "+temp);
        }
        catch(UnsupportedEncodingException x) {
            System.out.println(x.toString());
        }
        catch(NoSuchAlgorithmException x) {
            System.out.println(x.toString());
        }
        catch(NoSuchPaddingException x) {
            System.out.println(x.toString());
        }
        catch(BadPaddingException x) {
            System.out.println(x.toString());
        }
        catch(InvalidKeyException x) {
            System.out.println(x.toString());
        }
        catch(IllegalBlockSizeException x) {
            System.out.println(x.toString());
        }
    }
}
