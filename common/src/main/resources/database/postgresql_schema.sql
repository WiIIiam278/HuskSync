-- Create the users table if it does not exist
CREATE TABLE IF NOT EXISTS "%users_table%"
(
    uuid     uuid        NOT NULL UNIQUE,
    username varchar(16) NOT NULL,

    PRIMARY KEY (uuid)
);

-- Create the user data table if it does not exist
CREATE TABLE IF NOT EXISTS "%user_data_table%"
(
    version_uuid uuid        NOT NULL UNIQUE,
    player_uuid  uuid        NOT NULL,
    timestamp    timestamp   NOT NULL,
    save_cause   varchar(32) NOT NULL,
    pinned       boolean     NOT NULL DEFAULT FALSE,
    data         bytea       NOT NULL,

    PRIMARY KEY (version_uuid, player_uuid),
    FOREIGN KEY (player_uuid) REFERENCES "%users_table%" (uuid) ON DELETE CASCADE
);

-- Create the map data table if it does not exist
CREATE TABLE IF NOT EXISTS "%map_data_table%"
(
    world_uuid uuid  NOT NULL,
    map_id     int   NOT NULL,
    data       bytea NOT NULL,
    PRIMARY KEY (world_uuid, map_id)
);

-- Create the map ids table if it does not exist
CREATE TABLE IF NOT EXISTS "%map_ids_table%"
(
    from_world_uuid uuid NOT NULL,
    from_id         int  NOT NULL,
    to_world_uuid   uuid NOT NULL,
    to_id           int  NOT NULL,
    PRIMARY KEY (from_world_uuid, from_id, to_world_uuid),
    FOREIGN KEY (from_world_uuid, from_id) REFERENCES "%map_data_table%" (world_uuid, map_id) ON DELETE CASCADE
);
