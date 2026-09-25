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
    private static StationPosition position(
            final String id, final String hafasPositionName, final String label, final Product product) {
        return new StationPosition(
                new Location(LocationType.STATION, id, Point.fromDouble(52.456, 13.321), null, id),
                hafasPositionName,
                label,
                product);
    }

    @Test
    public void matchesOnlyExplicitStationIdAliases() {
        final String aliases =
                "900062202,900000062202,de:11000:900062202,de:11000:900000062202";

        assertTrue(VbbStopPositions.matchesStationId(aliases, "900062202"));
        assertTrue(VbbStopPositions.matchesStationId(aliases, "900000062202"));
        assertTrue(VbbStopPositions.matchesStationId(aliases, "de:11000:900062202"));
        assertTrue(VbbStopPositions.matchesStationId(aliases, "de:11000:900000062202"));
        assertFalse(VbbStopPositions.matchesStationId(aliases, "900000062282"));
        assertFalse(VbbStopPositions.matchesStationId(aliases, "prefix:900062202"));
    }

    @Test
    public void resolvesExactPositionAndProduct() {
        final StationPosition bus = position("bus", "Pos. 1", "1", Product.BUS);
        final StationPosition suburban = position("s", "1", "1", Product.SUBURBAN_TRAIN);
        final StationPosition subway = position("u", "1", "1", Product.SUBWAY);
        final List<StationPosition> positions = List.of(bus, suburban, subway);

        assertSame(bus, VbbStopPositions.find(positions, new Position("Pos. 1"), Product.BUS));
        assertSame(suburban, VbbStopPositions.find(
                positions, new Position("1"), Product.SUBURBAN_TRAIN));
        assertSame(subway, VbbStopPositions.find(
                positions, new Position("1"), Product.SUBWAY));
    }

    @Test
    public void doesNotNormalizeOrGuessPosition() {
        final StationPosition bus = position("bus", "Pos. 8", "8", Product.BUS);
        final StationPosition subway = position("u", "1", "1", Product.SUBWAY);
        final List<StationPosition> positions = List.of(bus, subway);

        assertNull(VbbStopPositions.find(positions, new Position("8"), Product.BUS));
        assertNull(VbbStopPositions.find(positions, new Position("Gleis 1"), Product.SUBWAY));
        assertNull(VbbStopPositions.find(positions, new Position("1"), null));
    }

    @Test
    public void refusesAmbiguousExactMatch() {
        final StationPosition first = position("a", "1", "1", Product.SUBWAY);
        final StationPosition second = position("b", "1", "1", Product.SUBWAY);

        assertNull(VbbStopPositions.find(
                List.of(first, second), new Position("1"), Product.SUBWAY));
    }
}
