package io.digdag.core;

public interface SecretCrypto
{
    String encryptSecret(String plainText);

    String decryptSecret(String encryptedBase64);
}
