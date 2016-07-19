package io.digdag.core.database;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import io.digdag.core.SecretCrypto;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.crypto.Cipher.ENCRYPT_MODE;

public class AESGCMSecretCrypto implements SecretCrypto
{
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey sharedSecret;

    private static final int AES_KEY_SIZE = 128;
    private static final int GCM_NONCE_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private static final int TERM = 1;
    private static final int VERSION = 1;

    private static final int MAX_PLAINTEXT_LENGTH = 1024;

    private static final int RECORD_SIZE = 2048;
    private static final int LENGTH_SIZE = 4;
    private static final int TEXT_SIZE = RECORD_SIZE - LENGTH_SIZE;
    private static final int TERM_SIZE = 4;
    private static final int VERSION_SIZE = 4;
    private static final int OPAQUE_SIZE = TERM_SIZE + VERSION_SIZE + GCM_NONCE_LENGTH + RECORD_SIZE + GCM_TAG_LENGTH;

    public AESGCMSecretCrypto(String sharedSecretBase64)
    {
        byte[] sharedSecretRaw = Base64.getDecoder().decode(sharedSecretBase64);
        Preconditions.checkArgument(sharedSecretRaw.length * 8 == AES_KEY_SIZE);
        this.sharedSecret = new SecretKeySpec(sharedSecretRaw, "AES");
    }

    @Override
    public String encryptSecret(String plainText)
    {
        if (plainText.length() > MAX_PLAINTEXT_LENGTH) {
            throw new IllegalArgumentException("Too long text");
        }

        byte[] nonce = generateNonce();

        Cipher cipher = cipher(ENCRYPT_MODE, sharedSecret, nonce);

        byte[] plainTextBytes = plainText.getBytes(UTF_8);

        if (plainTextBytes.length > MAX_PLAINTEXT_LENGTH) {
            throw new IllegalArgumentException("Too long text");
        }

        byte[] recordBytes = new byte[RECORD_SIZE];
        ByteBuffer recordBuffer = ByteBuffer.wrap(recordBytes);
        recordBuffer.putInt(plainTextBytes.length);
        recordBuffer.put(plainTextBytes);

        byte[] cipherText;
        try {
            cipherText = cipher.doFinal(recordBytes);
        }
        catch (IllegalBlockSizeException | BadPaddingException e) {
            throw Throwables.propagate(e);
        }

        assert cipherText.length == RECORD_SIZE + GCM_TAG_LENGTH;

        byte[] opaque = new byte[OPAQUE_SIZE];
        ByteBuffer output = ByteBuffer.wrap(opaque);

        output.putInt(TERM);
        output.putInt(VERSION);
        output.put(nonce);
        output.put(cipherText);

        assert output.remaining() == 0;

        return Base64.getEncoder().encodeToString(opaque);
    }

    @Override
    public String decryptSecret(String encryptedBase64)
    {
        byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);
        Preconditions.checkArgument(encrypted.length == OPAQUE_SIZE, "Bad size");

        ByteBuffer buffer = ByteBuffer.wrap(encrypted);
        int term = buffer.getInt();
        int version = buffer.getInt();

        if (term != TERM) {
            throw new IllegalArgumentException("Bad term");
        }

        if (version != VERSION) {
            throw new IllegalArgumentException("Bad version");
        }

        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        buffer.get(nonce);

        Cipher cipher = cipher(Cipher.DECRYPT_MODE, sharedSecret, nonce);

        byte[] recordBytes;
        try {
            recordBytes = cipher.doFinal(encrypted, buffer.position(), buffer.remaining());
        }
        catch (IllegalBlockSizeException | BadPaddingException e) {
            throw Throwables.propagate(e);
        }

        assert recordBytes.length == RECORD_SIZE;

        ByteBuffer decryptedBuffer = ByteBuffer.wrap(recordBytes);
        int length = decryptedBuffer.getInt();
        if (length < -1 || length > MAX_PLAINTEXT_LENGTH) {
            throw new IllegalArgumentException("Bad length");
        }

        decryptedBuffer.limit(decryptedBuffer.position() + length);

        return UTF_8.decode(decryptedBuffer).toString();
    }

    private Cipher cipher(int encryptMode, SecretKey sharedSecret, byte[] nonce)
    {
        Cipher result;
        try {
            result = Cipher.getInstance("AES/GCM/NoPadding");
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException e1) {
            throw Throwables.propagate(e1);
        }
        Cipher cipher = result;

        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);
        try {
            cipher.init(encryptMode, sharedSecret, spec);
        }
        catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw Throwables.propagate(e);
        }
        return cipher;
    }

    private byte[] generateNonce()
    {
        // The nonce need not be random, just unique. It is simply convenient to rely on
        // SecureRandom (/dev/urandom) to give us a value which is very likely to (although
        // of course not guaranteed) be unique.
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        SECURE_RANDOM.nextBytes(nonce);
        return nonce;
    }
}
