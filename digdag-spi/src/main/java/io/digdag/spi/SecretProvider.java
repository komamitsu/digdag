package io.digdag.spi;

import com.google.common.base.Optional;

public interface SecretProvider
{
    /**
     * Get a secret identified by a key.
     *
     * @param key A key identifing the secret to get.
     * @return A secret.
     * @throws SecretAccessDeniedException if access to the secret was not permitted.
     * @throws SecretNotFoundException if no matching secret was found.
     */
    default String getSecret(String key) {
        return getSecretOptional(key).or(() -> {
            throw new SecretNotFoundException(key);
        });
    }

    /**
     * Get a secret identified by a key.
     *
     * @param key A key identifing the secret to get.
     * @return {@link Optional#of(Object)} with a secret or {@link Optional#absent()} if no matching secret was found.
     * @throws SecretAccessDeniedException if access to the secret was not permitted.
     */
    Optional<String> getSecretOptional(String key);
}
