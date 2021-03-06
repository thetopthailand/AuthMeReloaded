package fr.xephi.authme.security.crypts;

import at.favre.lib.crypto.bcrypt.BCrypt;
import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.util.RandomStringUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Wraps a {@link BCrypt.Hasher} instance and provides methods suitable for use in AuthMe.
 */
public class BCryptHasher {

    /** Number of bytes in a BCrypt salt (not encoded). */
    public static final int BYTES_IN_SALT = 16;
    /** Number of characters of the salt in its radix64-encoded form. */
    public static final int SALT_LENGTH_ENCODED = 22;

    private final BCrypt.Hasher hasher;
    private final int costFactor;

    /**
     * Constructor.
     *
     * @param version the BCrypt version the instance should generate
     * @param costFactor the log2 cost factor to use
     */
    public BCryptHasher(BCrypt.Version version, int costFactor) {
        this.hasher = BCrypt.with(version);
        this.costFactor = costFactor;
    }

    public HashedPassword hash(String password) {
        byte[] hash = hasher.hash(costFactor, password.getBytes(UTF_8));
        return new HashedPassword(new String(hash, UTF_8));
    }

    public String hashWithRawSalt(String password, byte[] rawSalt) {
        byte[] hash = hasher.hash(costFactor, rawSalt, password.getBytes(UTF_8));
        return new String(hash, UTF_8);
    }

    /**
     * Verifies that the given password is correct for the provided BCrypt hash.
     *
     * @param password the password to check with
     * @param hash the hash to check against
     * @return true if the password matches the hash, false otherwise
     */
    public static boolean comparePassword(String password, String hash) {
        if (HashUtils.isValidBcryptHash(hash)) {
            BCrypt.Result result = BCrypt.verifyer().verify(password.getBytes(UTF_8), hash.getBytes(UTF_8));
            return result.verified;
        }
        return false;
    }

    /**
     * Generates a salt for usage in BCrypt. The returned salt is not yet encoded.
     * <p>
     * Internally, the BCrypt library in {@link BCrypt.Hasher#hash(int, byte[])} uses the following:
     * {@code Bytes.random(16, secureRandom).encodeUtf8();}
     * <p>
     * Because our {@link EncryptionMethod} interface works with {@code String} types we need to make sure that the
     * generated bytes in the salt are suitable for conversion into a String, such that calling String#getBytes will
     * yield the same number of bytes again. Thus, we are forced to limit the range of characters we use. Ideally we'd
     * only have to pass the salt in its encoded form so that we could make use of the entire "spectrum" of values,
     * which proves difficult to achieve with the underlying BCrypt library. However, the salt needs to be generated
     * manually only for testing purposes; production code should always hash passwords using
     * {@link EncryptionMethod#computeHash(String, String)}, which internally may represent salts in more suitable
     * formats.
     *
     * @return the salt for a BCrypt hash
     */
    public static String generateSalt() {
        return RandomStringUtils.generateLowerUpper(BYTES_IN_SALT);
    }
}
