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

package net.william278.husksync.command;

import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

public abstract class Node implements Executable {

    protected static final String PERMISSION_PREFIX = "husksync.command";

    protected final HuskSync plugin;
    private final String name;
    private final List<String> aliases;
    private boolean operatorCommand = false;

    protected Node(@NotNull String name, @NotNull List<String> aliases, @NotNull HuskSync plugin) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Command name cannot be blank");
        }
        this.name = name;
        this.aliases = aliases;
        this.plugin = plugin;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public List<String> getAliases() {
        return aliases;
    }

    @NotNull
    public String getPermission(@NotNull String... child) {
        final StringJoiner joiner = new StringJoiner(".")
                .add(PERMISSION_PREFIX)
                .add(getName());
        for (final String node : child) {
            joiner.add(node);
        }
        return joiner.toString().trim();
    }

    public boolean isOperatorCommand() {
        return operatorCommand;
    }

    public void setOperatorCommand(boolean operatorCommand) {
        this.operatorCommand = operatorCommand;
    }

    protected Optional<Integer> parseIntArg(@NotNull String[] args, int index) {
        try {
            if (args.length > index) {
                return Optional.of(Integer.parseInt(args[index]));
            }
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    protected Optional<Float> parseFloatArg(@NotNull String[] args, int index) {
        try {
            if (args.length > index) {
                return Optional.of(Float.parseFloat(args[index]));
            }
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    protected Optional<Double> parseCoordinateArg(@NotNull String[] args, int index, double relativeTo) {
        try {
            if (args.length > index) {
                final String arg = args[index];
                if (arg.startsWith("~")) {
                    final String coordinate = arg.substring(1);
                    if (coordinate.isBlank()) {
                        return Optional.of(relativeTo);
                    }
                    return Optional.of(relativeTo + Double.parseDouble(coordinate));
                }
                return Optional.of(Double.parseDouble(arg));
            }
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    protected Optional<String> parseStringArg(@NotNull String[] args, int index) {
        if (args.length > index) {
            return Optional.of(args[index]);
        }
        return Optional.empty();
    }

    protected Optional<String> parseGreedyArguments(@NotNull String[] args) {
        if (args.length > 1) {
            final StringJoiner sentence = new StringJoiner(" ");
            for (int i = 1; i < args.length; i++) {
                sentence.add(args[i]);
            }
            return Optional.of(sentence.toString().trim());
        }
        return Optional.empty();
    }

}