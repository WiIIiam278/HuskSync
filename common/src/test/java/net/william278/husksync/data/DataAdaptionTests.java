package net.william278.husksync.data;

import net.william278.husksync.player.DummyPlayer;
import net.william278.husksync.player.OnlineUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

/**
 * Tests for the data system {@link DataAdapter}
 */
public class DataAdaptionTests {

    @Test
    public void testJsonDataAdapter() {
        final OnlineUser dummyUser = DummyPlayer.create();
        final UserData dummyUserData = dummyUser.getUserData().join();
        final DataAdapter dataAdapter = new JsonDataAdapter();
        final byte[] data = dataAdapter.toBytes(dummyUserData);
        final UserData deserializedUserData = dataAdapter.fromBytes(data);

        boolean isEquals = deserializedUserData.getInventoryData().serializedItems
                                   .equals(dummyUserData.getInventoryData().serializedItems)
                           && deserializedUserData.getEnderChestData().serializedItems
                                   .equals(dummyUserData.getEnderChestData().serializedItems)
                           && deserializedUserData.getPotionEffectsData().serializedPotionEffects
                                   .equals(dummyUserData.getPotionEffectsData().serializedPotionEffects)
                           && deserializedUserData.getStatusData().health == dummyUserData.getStatusData().health
                           && deserializedUserData.getStatusData().hunger == dummyUserData.getStatusData().hunger
                           && deserializedUserData.getStatusData().saturation == dummyUserData.getStatusData().saturation
                           && deserializedUserData.getStatusData().saturationExhaustion == dummyUserData.getStatusData().saturationExhaustion
                           && deserializedUserData.getStatusData().selectedItemSlot == dummyUserData.getStatusData().selectedItemSlot
                           && deserializedUserData.getStatusData().totalExperience == dummyUserData.getStatusData().totalExperience
                           && deserializedUserData.getStatusData().maxHealth == dummyUserData.getStatusData().maxHealth
                           && deserializedUserData.getStatusData().healthScale == dummyUserData.getStatusData().healthScale;

        Assertions.assertTrue(isEquals);
    }

    @Test
    public void testJsonFormat() {
        final OnlineUser dummyUser = DummyPlayer.create();
        final UserData dummyUserData = dummyUser.getUserData().join();
        final DataAdapter dataAdapter = new JsonDataAdapter();
        final byte[] data = dataAdapter.toBytes(dummyUserData);
        final String json = new String(data, StandardCharsets.UTF_8);
        final String expectedJson = "{\"status\":{\"health\":20.0,\"max_health\":20.0,\"health_scale\":0.0,\"hunger\":20,\"saturation\":5.0,\"saturation_exhaustion\":5.0,\"selected_item_slot\":1,\"total_experience\":100,\"experience_level\":1,\"experience_progress\":1.0,\"game_mode\":\"SURVIVAL\",\"is_flying\":false},\"inventory\":{\"serialized_items\":\"\"},\"ender_chest\":{\"serialized_items\":\"\"},\"potion_effects\":{\"serialized_potion_effects\":\"\"},\"advancements\":[],\"statistics\":{\"untyped_statistics\":{},\"block_statistics\":{},\"item_statistics\":{},\"entity_statistics\":{}},\"location\":{\"world_name\":\"dummy_world\",\"world_uuid\":\"00000000-0000-0000-0000-000000000000\",\"world_environment\":\"NORMAL\",\"x\":0.0,\"y\":64.0,\"z\":0.0,\"yaw\":90.0,\"pitch\":180.0},\"persistent_data_container\":{\"persistent_data_map\":{}},\"minecraft_version\":\"1.19-beta123456\",\"format_version\":1}";
        Assertions.assertEquals(expectedJson, json);
    }

    @Test
    public void testCompressedDataAdapter() {
        final OnlineUser dummyUser = DummyPlayer.create();
        final UserData dummyUserData = dummyUser.getUserData().join();
        final DataAdapter dataAdapter = new CompressedDataAdapter();
        final byte[] data = dataAdapter.toBytes(dummyUserData);
        final UserData deserializedUserData = dataAdapter.fromBytes(data);

        boolean isEquals = deserializedUserData.getInventoryData().serializedItems
                                   .equals(dummyUserData.getInventoryData().serializedItems)
                           && deserializedUserData.getEnderChestData().serializedItems
                                   .equals(dummyUserData.getEnderChestData().serializedItems)
                           && deserializedUserData.getPotionEffectsData().serializedPotionEffects
                                   .equals(dummyUserData.getPotionEffectsData().serializedPotionEffects)
                           && deserializedUserData.getStatusData().health == dummyUserData.getStatusData().health
                           && deserializedUserData.getStatusData().hunger == dummyUserData.getStatusData().hunger
                           && deserializedUserData.getStatusData().saturation == dummyUserData.getStatusData().saturation
                           && deserializedUserData.getStatusData().saturationExhaustion == dummyUserData.getStatusData().saturationExhaustion
                           && deserializedUserData.getStatusData().selectedItemSlot == dummyUserData.getStatusData().selectedItemSlot
                           && deserializedUserData.getStatusData().totalExperience == dummyUserData.getStatusData().totalExperience
                           && deserializedUserData.getStatusData().maxHealth == dummyUserData.getStatusData().maxHealth
                           && deserializedUserData.getStatusData().healthScale == dummyUserData.getStatusData().healthScale;

        Assertions.assertTrue(isEquals);
    }

}
