/*
 * Copyright the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package de.schildbach.oeffi.stations;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Product;

public final class StationPosition {
    public final Location location;
    public final String hafasPositionName;
    public final String label;
    public final Product product;

    public StationPosition(
            final Location location,
            final String hafasPositionName,
            final String label,
            final Product product) {
        this.location = location;
        this.hafasPositionName = hafasPositionName;
        this.label = label;
        this.product = product;
    }
}
