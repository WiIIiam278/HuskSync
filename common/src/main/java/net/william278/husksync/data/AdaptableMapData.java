package net.william278.husksync.data;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import net.william278.husksync.adapter.Adaptable;
import net.william278.mapdataapi.MapData;

import java.io.IOException;

public class AdaptableMapData implements Adaptable {
    @SerializedName("data")
    private final byte[] data;

    public AdaptableMapData(byte @NotNull [] data) {
        this.data = data;
    }

    public AdaptableMapData(MapData data) {
        this(data.toBytes());
    }

    public MapData getData(int dataVersion) throws IOException {
        return MapData.fromByteArray(dataVersion, data);
    }
}
