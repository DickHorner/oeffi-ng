package de.schildbach.oeffi.util;

import android.annotation.SuppressLint;

import java.util.ArrayList;
import java.util.List;

import de.schildbach.pte.dto.VehicleInformation;

public class VehicleInformationRenderer {
    private final StringBuilder builder = new StringBuilder();

    final VehicleInformation vehicleInformation;

    public VehicleInformationRenderer(final VehicleInformation vehicleInformation) {
        this.vehicleInformation = vehicleInformation;
        render();
    }

    public String getHtml() {
        return builder.toString();
    }

    private static class Entry {
        double fromMeters;
        boolean platformEnd;
        boolean vehicleEnd;
        VehicleInformation.PlatformSection platformSection;
        VehicleInformation.VehicleData vehicleData;
    }

    private void render() {
        final boolean reverseOrientation = vehicleInformation.reverseOrientationAtPlatform;

        final VehicleInformation.PlatformSection platform = vehicleInformation.platform;
        final List<VehicleInformation.PlatformSection> platformSections = vehicleInformation.platformSections;
        final int numPlatformSections = platformSections == null ? 0 : platformSections.size();

        Entry entry;
        final List<Entry> list = new ArrayList<>();
        if (numPlatformSections == 0) {
            entry = new Entry();
            entry.fromMeters = reverseOrientation ? -platform.toMeters : platform.fromMeters;
            list.add(entry);

            entry = new Entry();
            entry.platformEnd = true;
            entry.fromMeters = reverseOrientation ? -platform.fromMeters : platform.toMeters;
            list.add(entry);
        } else if (reverseOrientation) {
            final double sectionToMeters = platformSections.get(numPlatformSections - 1).toMeters;
            final double platformToMeters = platform.toMeters;
            if (platformToMeters > sectionToMeters) {
                entry = new Entry();
                entry.fromMeters = -platformToMeters;
                list.add(entry);
            }

            for (int index = numPlatformSections - 1; index >= 0; --index) {
                final VehicleInformation.PlatformSection section = platformSections.get(index);

                entry = new Entry();
                entry.platformSection = section;
                entry.fromMeters = -section.toMeters;
                list.add(entry);
            }

            final double sectionFromMeters = platformSections.get(0).fromMeters;
            final double platformFromMeters = platform.fromMeters;
            if (sectionFromMeters > platformFromMeters) {
                entry = new Entry();
                entry.fromMeters = -sectionFromMeters;
                list.add(entry);
            }

            entry = new Entry();
            entry.platformEnd = true;
            entry.fromMeters = -platformFromMeters;
            list.add(entry);
        } else {
            final double sectionFromMeters = platformSections.get(0).fromMeters;
            final double platformFromMeters = platform.fromMeters;
            if (sectionFromMeters > platformFromMeters) {
                entry = new Entry();
                entry.fromMeters = platform.fromMeters;
                list.add(entry);
            }

            for (int index = 0; index < numPlatformSections; ++index) {
                final VehicleInformation.PlatformSection section = platformSections.get(index);

                entry = new Entry();
                entry.platformSection = section;
                entry.fromMeters = section.fromMeters;
                list.add(entry);
            }

            final double sectionToMeters = platformSections.get(numPlatformSections - 1).toMeters;
            final double platformToMeters = platform.toMeters;
            if (sectionToMeters < platformToMeters) {
                entry = new Entry();
                entry.fromMeters = sectionToMeters;
                list.add(entry);
            }

            entry = new Entry();
            entry.platformEnd = true;
            entry.fromMeters = platformToMeters;
            list.add(entry);
        }

        VehicleInformation.VehicleData lastVehicleData = null;
        final double reverseFactor = reverseOrientation ? -1 : 1;
        for (final VehicleInformation.VehicleGroup vehicleGroup : vehicleInformation.vehicleGroups) {
            for (final VehicleInformation.VehicleData vehicleData : vehicleGroup.vehicles) {
                lastVehicleData = vehicleData;
                entry = new Entry();
                entry.vehicleData = vehicleData;
                final VehicleInformation.PlatformSegment segment = vehicleData.platformSegment;
                entry.fromMeters = segment.fromMeters * reverseFactor;
                list.add(entry);
            }
        }
        entry = new Entry();
        entry.vehicleEnd = true;
        entry.fromMeters = lastVehicleData.platformSegment.toMeters * reverseFactor;
        list.add(entry);

        list.sort((e1, e2) -> {
            final double d = e1.fromMeters - e2.fromMeters;
            return d < 0 ? -1 : d > 0 ? 1 : 0;
        });

        builder.append("<html>");
        builder.append("<head>");
        builder.append("<style>");
        builder.append("table { border-collapse: collapse; }");
        builder.append("tr, td { border: 1px solid; }");
        builder.append("</style>");
        builder.append("</head>");
        builder.append("<body>");
        builder.append("<table>");

        int nextPlatformSectionIndex = findNext(list, -1, false);
        int nextVehicleIndex = findNext(list, -1, true);

        entry = list.get(0);
        if (nextPlatformSectionIndex == 0) {
            nextPlatformSectionIndex = findNext(list, 0, false);
            addTableRowStart();
            addTableData(null, 1);
            addTableDataMeters(entry.fromMeters);
            addTableDataVehicle(null, nextVehicleIndex + 1);
            addTableRowEnd();
            addTableRowStart();
            addTableDataPlatform(entry.platformSection, nextPlatformSectionIndex + 1);
            addTableRowEnd();
        } else {
            nextVehicleIndex = findNext(list, 0, true);
            addTableRowStart();
            addTableDataPlatform(null, nextPlatformSectionIndex + 1);
            addTableDataMeters(entry.fromMeters);
            addTableDataVehicle(null, 1);
            addTableRowEnd();
            addTableRowStart();
            addTableDataVehicle(entry.vehicleData, nextVehicleIndex + 1);
            addTableRowEnd();
        }

        for (int i = 1, listSize = list.size(); i < listSize; i++) {
            entry = list.get(i);
            if (i == nextPlatformSectionIndex) {
                nextPlatformSectionIndex = findNext(list, i, false);
                addTableRowStart();
                addTableDataMeters(entry.fromMeters);
                addTableRowEnd();
                addTableRowStart();
                addTableDataPlatform(entry.platformSection, nextPlatformSectionIndex - i + 2);
                addTableRowEnd();
            } else {
                nextVehicleIndex = findNext(list, i, true);
                addTableRowStart();
                addTableDataMeters(entry.fromMeters);
                addTableRowEnd();
                addTableRowStart();
                addTableDataVehicle(entry.vehicleData, nextVehicleIndex - i + 2);
                addTableRowEnd();
            }
        }

        builder.append("</table>");
        builder.append("</body>");
        builder.append("</html>");
    }

    private void addTableRowStart() {
        builder.append("<tr>");
    }

    private void addTableRowEnd() {
        builder.append("</tr>");
    }

    private void addTableDataStart(final int span) {
        if (span > 1) {
            builder.append("<td rowspan=\"");
            builder.append(span);
            builder.append("\">");
        } else {
            builder.append("<td>");
        }
    }

    private void addTableDataEnd() {
        builder.append("</td>");
    }

    private void addTableData(final String data, final int span) {
        addTableDataStart(span);
        if (data != null)
            builder.append(data);
        addTableDataEnd();
    }

    @SuppressLint("DefaultLocale")
    private void addTableDataMeters(final double meters) {
        addTableData(String.format("%.0f", Math.abs(meters)), 2);
    }

    private void addTableDataPlatform(final VehicleInformation.PlatformSection platformSection, final int span) {
        if (platformSection == null) {
            addTableData(null, span + 2);
            return;
        }
        addTableDataStart(span + 2);
        builder.append(platformSection.name);
        addTableDataEnd();
    }

    private void addTableDataVehicle(final VehicleInformation.VehicleData vehicleData, final int span) {
        if (vehicleData == null) {
            addTableData(null, span + 2);
            return;
        }
        addTableDataStart(span + 2);
        builder.append("vehicle ");
        builder.append(vehicleData.group.indexInFormation);
        builder.append("-");
        builder.append(vehicleData.indexInGroup);
        addTableDataEnd();
    }

    private static int findNext(final List<Entry> list, final int consumedStartIndex, final boolean vehicle) {
        for (int i = consumedStartIndex + 1, listSize = list.size(); i < listSize; i++) {
            final Entry e = list.get(i);
            final boolean isVehicle = e.vehicleData != null || e.vehicleEnd;
            if (vehicle == isVehicle)
                return i;
        }
        return list.size();
    }

}
