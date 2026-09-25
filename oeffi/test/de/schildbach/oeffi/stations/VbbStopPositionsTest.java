package de.schildbach.oeffi.stations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Position;
import de.schildbach.pte.dto.Product;

public class VbbStopPositionsTest {
    private static Location position(final String id, final String name) {
        return new Location(
                LocationType.STATION, id, Point.fromDouble(52.456, 13.321), null, name);
    }

    @Test
    public void matchesVbbStationIdVariants() {
        assertTrue(VbbStopPositions.matchesStationId("900062202", "900062202"));
        assertTrue(VbbStopPositions.matchesStationId("900062202", "900000062202"));
        assertTrue(VbbStopPositions.matchesStationId("900062202", "de:11000:900062202"));
        assertTrue(VbbStopPositions.matchesStationId("900062202", "de:11000:900000062202"));
        assertFalse(VbbStopPositions.matchesStationId("900062202", "900000062282"));
    }

    @Test
    public void resolvesSameNumberByProduct() {
        final Location bus = position("bus", "Bushalt Schloßstraße Pos. 1");
        final Location suburban = position("s", "S Bahnsteig Gleis 1");
        final Location subway = position("u", "U-Bahnsteig Gleis 1");
        final List<Location> positions = List.of(bus, suburban, subway);

        assertSame(bus, VbbStopPositions.find(positions, new Position("1"), Product.BUS));
        assertSame(suburban, VbbStopPositions.find(
                positions, new Position("1"), Product.SUBURBAN_TRAIN));
        assertSame(subway, VbbStopPositions.find(
                positions, new Position("Gleis 1"), Product.SUBWAY));
        assertNull(VbbStopPositions.find(positions, new Position("1"), null));
    }

    @Test
    public void resolvesUniquePositionWithoutProductSpecificName() {
        final Location position = position("bus", "Bushalt Steglitzer Kreisel Pos. 8");

        assertSame(position, VbbStopPositions.find(
                List.of(position), new Position("Pos. 8"), Product.BUS));
        assertNull(VbbStopPositions.find(
                List.of(position), new Position("7"), Product.BUS));
    }
}
