package io.digdag.core.database;

import org.junit.Test;

import java.util.Base64;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AESGCMSecretCryptoTest
{
    @Test
    public void testEncryptDecrypt()
            throws Exception
    {
        int keySizeBytes = 128 / 8;
        byte[] key = new byte[keySizeBytes];
        String keyBase64 = Base64.getEncoder().encodeToString(key);

        AESGCMSecretCrypto crypto = new AESGCMSecretCrypto(keyBase64);

        String secret = "Hello Secret World!";

        String encrypted = crypto.encryptSecret(secret);

        String decrypted = crypto.decryptSecret(encrypted);

        assertThat(decrypted, is(secret));
    }
}