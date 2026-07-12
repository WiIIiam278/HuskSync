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

package net.william278.husksync.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class PaginatedList {

    private final List<String> items;
    private final int itemsPerPage;
    private final String headerFormat;
    private final String footerFormat;
    private final String previousButtonFormat;
    private final String nextButtonFormat;
    private final String pageJumpersFormat;
    private final String pageJumperPageFormat;
    private final String pageJumperCurrentPageFormat;
    private final String pageJumperSeparator;
    private final String pageJumperGroupSeparator;
    private final String command;

    private PaginatedList(@NotNull Builder builder) {
        this.items = builder.items;
        this.itemsPerPage = builder.itemsPerPage;
        this.headerFormat = builder.headerFormat;
        this.footerFormat = builder.footerFormat;
        this.previousButtonFormat = builder.previousButtonFormat;
        this.nextButtonFormat = builder.nextButtonFormat;
        this.pageJumpersFormat = builder.pageJumpersFormat;
        this.pageJumperPageFormat = builder.pageJumperPageFormat;
        this.pageJumperCurrentPageFormat = builder.pageJumperCurrentPageFormat;
        this.pageJumperSeparator = builder.pageJumperSeparator;
        this.pageJumperGroupSeparator = builder.pageJumperGroupSeparator;
        this.command = builder.command;
    }

    public int getTotalPages() {
        return (int) Math.ceil((double) items.size() / itemsPerPage);
    }

    public Component getNearestValidPage(int page) {
        final int totalPages = getTotalPages();
        final int clampedPage = Math.max(1, Math.min(page, Math.max(1, totalPages)));

        final MiniMessage mm = MiniMessage.miniMessage();
        final List<Component> lines = new ArrayList<>();

        if (headerFormat != null && !headerFormat.isEmpty()) {
            lines.add(mm.deserialize(headerFormat
                    .replace("%current_page%", Integer.toString(clampedPage))
                    .replace("%total_pages%", Integer.toString(totalPages))
                    .replace("%total_items%", Integer.toString(items.size()))));
        }

        final int startIndex = (clampedPage - 1) * itemsPerPage;
        final int endIndex = Math.min(startIndex + itemsPerPage, items.size());
        IntStream.range(startIndex, endIndex)
                .mapToObj(i -> mm.deserialize(items.get(i)))
                .forEach(lines::add);

        if (footerFormat != null && !footerFormat.isEmpty()) {
            final String previousButton = previousButtonFormat != null && clampedPage > 1
                    ? mm.serialize(mm.deserialize(previousButtonFormat
                    .replace("%previous_page_index%", Integer.toString(clampedPage - 1))
                    .replace("%command%", command)))
                    : "";
            final String nextButton = nextButtonFormat != null && clampedPage < totalPages
                    ? mm.serialize(mm.deserialize(nextButtonFormat
                    .replace("%next_page_index%", Integer.toString(clampedPage + 1))
                    .replace("%command%", command)))
                    : "";
            final String pageJumpers = buildPageJumpers(mm, clampedPage, totalPages);

            lines.add(mm.deserialize(footerFormat
                    .replace("%previous_page_button%", previousButton)
                    .replace("%next_page_button%", nextButton)
                    .replace("%page_jumpers%", pageJumpers)
                    .replace("%current_page%", Integer.toString(clampedPage))
                    .replace("%total_pages%", Integer.toString(totalPages))));
        }

        return Component.join(JoinConfiguration.newlines(), lines);
    }

    @NotNull
    private String buildPageJumpers(@NotNull MiniMessage mm, int currentPage, int totalPages) {
        if (pageJumpersFormat == null || pageJumperPageFormat == null) {
            return "";
        }

        final List<String> jumpButtons = new ArrayList<>();
        final int groupSize = 10;
        final int currentGroup = (currentPage - 1) / groupSize;
        final int startPage = currentGroup * groupSize + 1;
        final int endPage = Math.min(startPage + groupSize - 1, totalPages);

        for (int i = startPage; i <= endPage; i++) {
            final String format = (i == currentPage) ? pageJumperCurrentPageFormat : pageJumperPageFormat;
            final String rendered = mm.serialize(mm.deserialize(format
                    .replace("%target_page_index%", Integer.toString(i))
                    .replace("%command%", command)
                    .replace("%current_page%", Integer.toString(i))));
            jumpButtons.add(rendered);
        }

        final String joined = String.join(
                pageJumperSeparator != null ? pageJumperSeparator : " ",
                jumpButtons
        );

        return mm.serialize(mm.deserialize(pageJumpersFormat
                .replace("%page_jump_buttons%", joined)));
    }

    public static class Builder {
        private List<String> items = List.of();
        private int itemsPerPage = 10;
        private String headerFormat;
        private String footerFormat;
        private String previousButtonFormat;
        private String nextButtonFormat;
        private String pageJumpersFormat;
        private String pageJumperPageFormat;
        private String pageJumperCurrentPageFormat;
        private String pageJumperSeparator;
        private String pageJumperGroupSeparator;
        private String command;

        @NotNull
        public Builder setItems(@NotNull List<String> items) {
            this.items = items;
            return this;
        }

        @NotNull
        public Builder setItemsPerPage(int itemsPerPage) {
            this.itemsPerPage = itemsPerPage;
            return this;
        }

        @NotNull
        public Builder setHeaderFormat(@NotNull String headerFormat) {
            this.headerFormat = headerFormat;
            return this;
        }

        @NotNull
        public Builder setFooterFormat(@NotNull String footerFormat) {
            this.footerFormat = footerFormat;
            return this;
        }

        @NotNull
        public Builder setPreviousButtonFormat(@NotNull String format) {
            this.previousButtonFormat = format;
            return this;
        }

        @NotNull
        public Builder setNextButtonFormat(@NotNull String format) {
            this.nextButtonFormat = format;
            return this;
        }

        @NotNull
        public Builder setPageJumpersFormat(@NotNull String format) {
            this.pageJumpersFormat = format;
            return this;
        }

        @NotNull
        public Builder setPageJumperPageFormat(@NotNull String format) {
            this.pageJumperPageFormat = format;
            return this;
        }

        @NotNull
        public Builder setPageJumperCurrentPageFormat(@NotNull String format) {
            this.pageJumperCurrentPageFormat = format;
            return this;
        }

        @NotNull
        public Builder setPageJumperPageSeparator(@NotNull String separator) {
            this.pageJumperSeparator = separator;
            return this;
        }

        @NotNull
        public Builder setPageJumperGroupSeparator(@NotNull String separator) {
            this.pageJumperGroupSeparator = separator;
            return this;
        }

        @NotNull
        public Builder setCommand(@NotNull String command) {
            this.command = command;
            return this;
        }

        @NotNull
        public PaginatedList build() {
            return new PaginatedList(this);
        }
    }
}
