/*
 * Copyright the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package de.schildbach.oeffi.stations;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Position;
import de.schildbach.pte.dto.Product;

final class VbbStopPositions {
    private static final Logger log = LoggerFactory.getLogger(VbbStopPositions.class);
    private static final String ASSET_FILENAME = "vbb-stop-positions.txt";

    private VbbStopPositions() {
    }

    static List<Location> load(final Context context, final Location station) {
        final List<Location> positions = new ArrayList<>();

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open(ASSET_FILENAME), StandardCharsets.UTF_8))) {
            while (true) {
                final String line = reader.readLine();
                if (line == null)
                    break;
                if (line.isEmpty() || line.charAt(0) == '#')
                    continue;

                final String[] fields = line.split("\\|", -1);
                if (fields.length != 5 || !matchesStationId(station, fields[0]))
                    continue;

                try {
                    final Point coord = Point.fromDouble(
                            Double.parseDouble(fields[2]), Double.parseDouble(fields[3]));
                    positions.add(new Location(
                            LocationType.STATION, fields[1], coord, null, fields[4]));
                } catch (final NumberFormatException x) {
                    log.warn("Could not parse VBB stop position: {}", line, x);
                }
            }
        } catch (final IOException x) {
            log.warn("Could not read asset {}", ASSET_FILENAME, x);
        }

        return positions;
    }

    private static boolean matchesStationId(final Location station, final String parentId) {
        return matchesStationId(parentId, station.id)
                || matchesStationId(parentId, station.identityId)
                || matchesStationId(parentId, station.displayId);
    }

    static boolean matchesStationId(final String parentId, final @Nullable String stationId) {
        if (stationId == null)
            return false;

        return normalizeStationId(parentId).equals(normalizeStationId(stationId));
    }

    private static String normalizeStationId(final String stationId) {
        String id = stationId;
        for (final String part : stationId.split(":")) {
            if (part.length() >= 9 && part.length() <= 12 && part.chars().allMatch(Character::isDigit)) {
                id = part;
                break;
            }
        }

        if (id.length() == 12 && "000".equals(id.substring(3, 6)))
            return id.substring(0, 3) + id.substring(6);

        return id;
    }

    static @Nullable Location find(
            final List<Location> positions, final Position position, final @Nullable Product product) {
        final String positionKey = normalizePositionKey(position.toString());
        Location typedMatch = null;
        int typedMatches = 0;
        Location anyMatch = null;
        int anyMatches = 0;

        for (final Location location : positions) {
            if (location.name == null)
                continue;
            final String locationPositionKey = extractPositionKey(location.name);
            if (!positionKey.equals(locationPositionKey))
                continue;

            anyMatch = location;
            anyMatches++;
            if (matchesProduct(location.name, product)) {
                typedMatch = location;
                typedMatches++;
            }
        }

        if (typedMatches == 1)
            return typedMatch;
        if (anyMatches == 1)
            return anyMatch;
        return null;
    }

    private static String extractPositionKey(final String description) {
        final String lower = description.toLowerCase(Locale.ROOT);
        final String[] labels = { "gleis ", "pos. ", "pos ", "position " };

        int index = -1;
        int labelLength = 0;
        for (final String label : labels) {
            final int labelIndex = lower.lastIndexOf(label);
            if (labelIndex > index) {
                index = labelIndex;
                labelLength = label.length();
            }
        }

        if (index < 0)
            return "";
        return normalizePositionKey(description.substring(index + labelLength));
    }

    private static String normalizePositionKey(final String position) {
        String normalized = position.trim().toLowerCase(Locale.ROOT);
        for (final String prefix : new String[] { "gleis", "position", "pos.", "pos" }) {
            if (normalized.startsWith(prefix)) {
                normalized = normalized.substring(prefix.length()).trim();
                break;
            }
        }
        return normalized.replaceAll("\\s+", "");
    }

    private static boolean matchesProduct(final String description, final @Nullable Product product) {
        if (product == null)
            return true;

        final String lower = description.toLowerCase(Locale.ROOT);
        if (product == Product.BUS)
            return lower.contains("bushalt") || lower.contains("ersatzhalt");
        if (product == Product.SUBWAY)
            return lower.contains("u-bahnsteig") || lower.contains("u bahnsteig");
        if (product == Product.SUBURBAN_TRAIN)
            return lower.contains("s-bahnsteig") || lower.contains("s bahnsteig");
        if (product == Product.TRAM)
            return lower.contains("tram") || lower.contains("straßenbahn");
        return true;
    }
}
