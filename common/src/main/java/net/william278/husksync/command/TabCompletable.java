/*
 * This file is part of HuskSync by William278. Do not redistribute!
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  All rights reserved.
 *
 *  This source code is provided as reference to licensed individuals that have purchased the HuskSync
 *  plugin once from any of the official sources it is provided. The availability of this code does
 *  not grant you the rights to modify, re-distribute, compile or redistribute this source code or
 *  "plugin" outside this intended purpose. This license does not cover libraries developed by third
 *  parties that are utilised in the plugin.
 */

package net.william278.husksync.command;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interface providing tab completions for a command
 */
public interface TabCompletable {

    /**
     * What should be returned when the player or console attempts to TAB-complete a command
     *
     * @param args Current command arguments
     * @return List of String arguments to offer TAB suggestions
     */
    List<String> onTabComplete(@NotNull String[] args);

}
