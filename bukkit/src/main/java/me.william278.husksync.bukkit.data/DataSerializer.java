package me.william278.husksync.bukkit.data;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

/**
 * Holds legacy data store methods for data storage
 */
@Deprecated
@SuppressWarnings("DeprecatedIsStillUsed")
public class DataSerializer {

    /**
     * A record used to store data for advancement synchronisation
     *
     * @deprecated Old format - Use {@link AdvancementRecordDate} instead
     */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    // Suppress deprecation warnings here (still used for backwards compatibility)
    public record AdvancementRecord(String advancementKey,
                                    ArrayList<String> awardedAdvancementCriteria) implements Serializable {
    }

    /**
     * A record used to store data for a player's statistics
     */
    public record StatisticData(HashMap<Statistic, Integer> untypedStatisticValues,
                                HashMap<Statistic, HashMap<Material, Integer>> blockStatisticValues,
                                HashMap<Statistic, HashMap<Material, Integer>> itemStatisticValues,
                                HashMap<Statistic, HashMap<EntityType, Integer>> entityStatisticValues) implements Serializable {
    }

    /**
     * A record used to store data for native advancement synchronisation, tracking advancement date progress
     */
    public record AdvancementRecordDate(String key, Map<String, Date> criteriaMap) implements Serializable {
        public AdvancementRecordDate(String key, List<String> criteriaList) {
            this(key, new HashMap<>() {{
                criteriaList.forEach(s -> put(s, Date.from(Instant.EPOCH)));
            }});
        }
    }

    /**
     * A record used to store data for a player's location
     */
    public record PlayerLocation(double x, double y, double z, float yaw, float pitch,
                                 String worldName, World.Environment environment) implements Serializable {
    }
}
