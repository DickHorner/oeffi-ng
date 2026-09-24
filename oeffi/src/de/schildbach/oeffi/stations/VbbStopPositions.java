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

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.Point;

final class VbbStopPositions {
    private static final Logger log = LoggerFactory.getLogger(VbbStopPositions.class);
    private static final String ASSET_FILENAME = "vbb-stop-positions.txt";

    private VbbStopPositions() {
    }

    static List<Location> load(final Context context, final String stationId) {
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
                if (fields.length != 5 || !stationId.equals(fields[0]))
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
}
