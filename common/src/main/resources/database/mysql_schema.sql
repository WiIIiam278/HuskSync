# Set the storage engine
SET DEFAULT_STORAGE_ENGINE = INNODB;

# Enable foreign key constraints
SET FOREIGN_KEY_CHECKS = 1;

# Create the users table if it does not exist
CREATE TABLE IF NOT EXISTS `%users_table%`
(
    `uuid`     char(36)    NOT NULL UNIQUE,
    `username` varchar(16) NOT NULL,

    PRIMARY KEY (`uuid`)
) CHARACTER SET utf8
  COLLATE utf8_unicode_ci;

# Create the user data table if it does not exist
CREATE TABLE IF NOT EXISTS `%user_data_table%`
(
    `version_uuid` char(36)    NOT NULL UNIQUE,
    `player_uuid`  char(36)    NOT NULL,
    `timestamp`    datetime    NOT NULL,
    `save_cause`   varchar(32) NOT NULL,
    `pinned`       boolean     NOT NULL DEFAULT FALSE,
    `data`         longblob    NOT NULL,
    PRIMARY KEY (`version_uuid`, `player_uuid`),
    FOREIGN KEY (`player_uuid`) REFERENCES `%users_table%` (`uuid`) ON DELETE CASCADE
) CHARACTER SET utf8
  COLLATE utf8_unicode_ci;