# Create the players table if it does not exist
CREATE TABLE IF NOT EXISTS `%players_table%`
(
    `uuid`     char(36)    NOT NULL UNIQUE,
    `username` varchar(16) NOT NULL,

    PRIMARY KEY (`uuid`)
);

# Create the player data table if it does not exist
CREATE TABLE IF NOT EXISTS `%data_table%`
(
    `version_uuid` char(36) NOT NULL,
    `player_uuid`  char(36) NOT NULL,
    `timestamp`    datetime NOT NULL,
    `data`         json     NOT NULL,

    PRIMARY KEY (`version_uuid`),
    FOREIGN KEY (`player_uuid`) REFERENCES `%players_table%` (`uuid`) ON DELETE CASCADE
);