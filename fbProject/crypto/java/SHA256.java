import java.security.MessageDigest;

class SHA256 {
    public static void main(String args[]) {
       /// MessageDigest md = MessageDigest.getInstance("SHA-256");
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String text = "This is some text";

        md.update(text.getBytes("UTF-8")); // Change this to "UTF-16" if needed
        byte[] digest = md.digest();
    }




}

