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

    static List<StationPosition> load(final Context context, final Location station) {
        final List<StationPosition> positions = new ArrayList<>();

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open(ASSET_FILENAME), StandardCharsets.UTF_8))) {
            while (true) {
                final String line = reader.readLine();
                if (line == null)
                    break;
                if (line.isEmpty() || line.charAt(0) == '#')
                    continue;

                final String[] fields = line.split("\\|", -1);
                if (fields.length != 8 || !matchesStationIds(station, fields[0]))
                    continue;

                try {
                    final Product product = Product.fromCode(fields[4].charAt(0));
                    final Point coord = Point.fromDouble(
                            Double.parseDouble(fields[2]), Double.parseDouble(fields[3]));
                    final Location location = new Location(
                            LocationType.STATION, fields[1], coord, null, fields[7]);
                    positions.add(new StationPosition(location, fields[5], fields[6], product));
                } catch (final IllegalArgumentException x) {
                    log.warn("Could not parse VBB stop position: {}", line, x);
                }
            }
        } catch (final IOException x) {
            log.warn("Could not read asset {}", ASSET_FILENAME, x);
        }

        return positions;
    }

    private static boolean matchesStationIds(final Location station, final String stationIds) {
        return matchesStationId(stationIds, station.id)
                || matchesStationId(stationIds, station.identityId)
                || matchesStationId(stationIds, station.displayId);
    }

    static boolean matchesStationId(final String stationIds, final @Nullable String stationId) {
        if (stationId == null)
            return false;

        for (final String candidate : stationIds.split(","))
            if (candidate.equals(stationId))
                return true;
        return false;
    }

    static @Nullable StationPosition find(
            final List<StationPosition> positions, final Position position, final @Nullable Product product) {
        if (product == null)
            return null;

        StationPosition match = null;
        for (final StationPosition stationPosition : positions) {
            if (stationPosition.product != product || !stationPosition.hafasPositionName.equals(position.name))
                continue;
            if (match != null)
                return null;
            match = stationPosition;
        }
        return match;
    }

}
