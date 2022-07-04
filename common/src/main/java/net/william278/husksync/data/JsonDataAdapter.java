package net.william278.husksync.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public class JsonDataAdapter implements DataAdapter {

    @Override
    public byte[] toBytes(@NotNull UserData data) throws DataAdaptionException {
        return new GsonBuilder().create().toJson(data).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public @NotNull UserData fromBytes(byte[] data) throws DataAdaptionException {
        try {
            return new GsonBuilder().create().fromJson(new String(data, StandardCharsets.UTF_8), UserData.class);
        } catch (JsonSyntaxException e) {
            throw new DataAdaptionException("Failed to parse JSON data", e);
        }
    }
}
