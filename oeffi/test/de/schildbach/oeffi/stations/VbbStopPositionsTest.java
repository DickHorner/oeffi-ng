package de.schildbach.oeffi.stations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.Point;
import de.schildbach.pte.dto.Position;
import de.schildbach.pte.dto.Product;

public class VbbStopPositionsTest {
    private static Location position(final String id, final String name) {
        final Product product = VbbStopPositions.markerProduct(name);
        return new Location(
                LocationType.STATION,
                id,
                id,
                VbbStopPositions.extractPositionLabel(name),
                Point.fromDouble(52.456, 13.321),
                null,
                name,
                product != null ? Collections.singleton(product) : null,
                "de",
                null);
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
    public void extractsCompactMarkerLabels() {
        assertEquals("5", VbbStopPositions.extractPositionLabel(
                "Bushalt Albrechtstraße vor Kuhligkshofstr. Pos. 5"));
        assertEquals("2", VbbStopPositions.extractPositionLabel(
                "U-Bahnsteig Gleis 2"));
        assertNull(VbbStopPositions.extractPositionLabel(
                "Bushalt Steglitzer Kreisel"));
    }

    @Test
    public void classifiesMarkerProducts() {
        assertEquals(Product.BUS, VbbStopPositions.markerProduct(
                "Bushalt Schloßstraße Pos. 1"));
        assertEquals(Product.SUBWAY, VbbStopPositions.markerProduct(
                "U-Bahnsteig Gleis 1"));
        assertEquals(Product.SUBURBAN_TRAIN, VbbStopPositions.markerProduct(
                "S Bahnsteig Gleis 1"));
        assertEquals(Product.TRAM, VbbStopPositions.markerProduct(
                "Tramsteig Pos. 2"));
        assertEquals(Product.REGIONAL_TRAIN, VbbStopPositions.markerProduct(
                "Bahnsteig Gleis 4"));
        assertNull(VbbStopPositions.markerProduct(
                "Zugang Rathaus Steglitz"));
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
