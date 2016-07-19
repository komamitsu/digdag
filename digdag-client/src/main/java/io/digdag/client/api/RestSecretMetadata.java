package io.digdag.client.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(visibility = Value.Style.ImplementationVisibility.PACKAGE)
@JsonSerialize(as = ImmutableRestSecretMetadata.class)
@JsonDeserialize(as = ImmutableRestSecretMetadata.class)
public interface RestSecretMetadata
{
//    int id();

    String key();

    static RestSecretMetadata of(String key)
    {
        return builder().key(key).build();
    }

    static Builder builder()
    {
        return ImmutableRestSecretMetadata.builder();
    }

//    Instant updatedAt();

    interface Builder
    {
//        Builder id(int id);

        Builder key(String key);

//        Builder updatedAt(Instant updatedAt);

        RestSecretMetadata build();
    }
}
