package auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

// Passwords are never stored, only a salted PBKDF2 hash of them.
// The random salt means two users with the same password still get different hashes,
// and the many iterations make guessing passwords from a stolen file very slow.
public final class PasswordHasher {

    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() { }

    public static String newSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String password, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt), ITERATIONS, KEY_BITS);
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            spec.clearPassword();
            return Base64.getEncoder().encodeToString(key);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 is not available", e);
        }
    }

    // MessageDigest.isEqual takes the same time whether the first or last byte differs,
    // so response timing can't leak how close a guess was.
    public static boolean matches(String password, String salt, String expectedHash) {
        byte[] actual = hash(password, salt).getBytes();
        return MessageDigest.isEqual(actual, expectedHash.getBytes());
    }
}
