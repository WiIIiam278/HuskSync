/*
 * This file is part of HuskSync, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.husksync.data;

import net.william278.husksync.DummyHuskSync;
import net.william278.husksync.player.DummyPlayer;
import net.william278.husksync.player.OnlineUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for the data system {@link DataAdapter}
 */
public class DataAdaptionTests {

    @Test
    public void testJsonDataAdapter() {
        final OnlineUser dummyUser = DummyPlayer.create();
        dummyUser.getUserData(new DummyHuskSync()).join().ifPresent(dummyUserData -> {
            final DataAdapter dataAdapter = new JsonDataAdapter();
            final byte[] data = dataAdapter.toBytes(dummyUserData);
            final UserData deserializedUserData = dataAdapter.fromBytes(data);

            // Assert all deserialized data is equal to the original data
            Assertions.assertTrue(dummyUserData.getInventory().isPresent());
            Assertions.assertTrue(deserializedUserData.getInventory().isPresent());
            Assertions.assertEquals(dummyUserData.getInventory().get().serializedItems, deserializedUserData.getInventory().get().serializedItems);
            Assertions.assertEquals(dummyUserData.getFormatVersion(), deserializedUserData.getFormatVersion());
        });
    }

    @Test
    public void testJsonFormat() {
        final OnlineUser dummyUser = DummyPlayer.create();
        final String expectedJson = "{\"status\":{\"health\":20.0,\"max_health\":20.0,\"health_scale\":0.0,\"hunger\":20,\"saturation\":5.0,\"saturation_exhaustion\":5.0,\"selected_item_slot\":1,\"total_experience\":100,\"experience_level\":1,\"experience_progress\":1.0,\"game_mode\":\"SURVIVAL\",\"is_flying\":false},\"inventory\":{\"serialized_items\":\"\"},\"ender_chest\":{\"serialized_items\":\"\"},\"potion_effects\":{\"serialized_potion_effects\":\"\"},\"advancements\":[],\"statistics\":{\"untyped_statistics\":{},\"block_statistics\":{},\"item_statistics\":{},\"entity_statistics\":{}},\"minecraft_version\":\"1.19\",\"format_version\":3}";
        AtomicReference<String> json = new AtomicReference<>();
        dummyUser.getUserData(new DummyHuskSync()).join().ifPresent(dummyUserData -> {
            final DataAdapter dataAdapter = new JsonDataAdapter();
            final byte[] data = dataAdapter.toBytes(dummyUserData);
            json.set(new String(data, StandardCharsets.UTF_8));
        });
        Assertions.assertEquals(expectedJson, json.get());
    }

    @Test
    public void testCompressedDataAdapter() {
        final OnlineUser dummyUser = DummyPlayer.create();
        dummyUser.getUserData(new DummyHuskSync()).join().ifPresent(dummyUserData -> {
            final DataAdapter dataAdapter = new CompressedDataAdapter();
            final byte[] data = dataAdapter.toBytes(dummyUserData);
            final UserData deserializedUserData = dataAdapter.fromBytes(data);

            // Assert all deserialized data is equal to the original data
            Assertions.assertTrue(dummyUserData.getInventory().isPresent());
            Assertions.assertTrue(deserializedUserData.getInventory().isPresent());
            Assertions.assertEquals(dummyUserData.getInventory().get().serializedItems, deserializedUserData.getInventory().get().serializedItems);
            Assertions.assertEquals(dummyUserData.getFormatVersion(), deserializedUserData.getFormatVersion());
        });
    }

    private String getTestSerializedPersistentDataContainer() {
        final HashMap<String, PersistentDataTag<?>> persistentDataTest = new HashMap<>();
        persistentDataTest.put("husksync:byte_test", new PersistentDataTag<>(PersistentDataTagType.BYTE, 0x01));
        persistentDataTest.put("husksync:double_test", new PersistentDataTag<>(PersistentDataTagType.DOUBLE, 2d));
        persistentDataTest.put("husksync:string_test", new PersistentDataTag<>(PersistentDataTagType.STRING, "test"));
        persistentDataTest.put("husksync:int_test", new PersistentDataTag<>(PersistentDataTagType.INTEGER, 3));
        persistentDataTest.put("husksync:long_test", new PersistentDataTag<>(PersistentDataTagType.LONG, 4L));
        persistentDataTest.put("husksync:float_test", new PersistentDataTag<>(PersistentDataTagType.FLOAT, 5f));
        persistentDataTest.put("husksync:short_test", new PersistentDataTag<>(PersistentDataTagType.SHORT, 6));
        final PersistentDataContainerData persistentDataContainerData = new PersistentDataContainerData(persistentDataTest);

        final DataAdapter dataAdapter = new JsonDataAdapter();
        UserData userData = new UserData();
        userData.persistentDataContainerData = persistentDataContainerData;
        return dataAdapter.toJson(userData, false);
    }

    @Test
    public void testPersistentDataContainerSerialization() {
        Assertions.assertEquals(getTestSerializedPersistentDataContainer(), "{\"persistent_data_container\":{\"persistent_data_map\":{\"husksync:int_test\":{\"type\":\"INTEGER\",\"value\":3},\"husksync:string_test\":{\"type\":\"STRING\",\"value\":\"test\"},\"husksync:long_test\":{\"type\":\"LONG\",\"value\":4},\"husksync:byte_test\":{\"type\":\"BYTE\",\"value\":1},\"husksync:short_test\":{\"type\":\"SHORT\",\"value\":6},\"husksync:double_test\":{\"type\":\"DOUBLE\",\"value\":2.0},\"husksync:float_test\":{\"type\":\"FLOAT\",\"value\":5.0}}},\"format_version\":3}");
    }


}
