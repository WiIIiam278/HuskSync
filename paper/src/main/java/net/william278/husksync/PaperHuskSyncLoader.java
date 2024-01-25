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

package net.william278.husksync;

import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurations;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import lombok.NoArgsConstructor;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@SuppressWarnings("UnstableApiUsage")
public class PaperHuskSyncLoader implements PluginLoader {

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        final MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolveLibraries(classpathBuilder).stream()
                .map(DefaultArtifact::new)
                .forEach(artifact -> resolver.addDependency(new Dependency(artifact, null)));
        resolver.addRepository(new RemoteRepository.Builder(
                "maven", "default", "https://repo.maven.apache.org/maven2/"
        ).build());

        classpathBuilder.addLibrary(resolver);
    }

    @NotNull
    private static List<String> resolveLibraries(@NotNull PluginClasspathBuilder classpathBuilder) {
        try (InputStream input = getLibraryListFile()) {
            return YamlConfigurations.read(
                    Objects.requireNonNull(input, "Failed to read libraries file"),
                    PaperLibraries.class
            ).libraries;
        } catch (Throwable e) {
            classpathBuilder.getContext().getLogger().error("Failed to resolve libraries", e);
        }
        return List.of();
    }

    @Nullable
    private static InputStream getLibraryListFile() {
        return PaperHuskSyncLoader.class.getClassLoader().getResourceAsStream("paper-libraries.yml");
    }

    @Configuration
    @NoArgsConstructor
    public static class PaperLibraries {

        private List<String> libraries;

    }

}