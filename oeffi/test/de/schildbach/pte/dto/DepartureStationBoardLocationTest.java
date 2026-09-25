package de.schildbach.pte.dto;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class DepartureStationBoardLocationTest {
    private static final Line LINE = new Line(null, null, Product.BUS, "B");

    @Test
    public void oldConstructorLeavesStationBoardLocationEmpty() {
        final Departure departure = new Departure(
                false,
                PTDate.withSystemOffset(0),
                null,
                LINE,
                null,
                null,
                null,
                false,
                null,
                null,
                null);

        assertNull(departure.stationBoardLocation);
    }

    @Test
    public void newConstructorPreservesStationBoardLocation() {
        final Location stationBoardLocation = new Location(LocationType.STATION, "mast");

        final Departure departure = new Departure(
                false,
                PTDate.withSystemOffset(0),
                null,
                LINE,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                stationBoardLocation);

        assertSame(stationBoardLocation, departure.stationBoardLocation);
    }
}
