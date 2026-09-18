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
        builder.append("<style>\n");
        builder.append("table { border-collapse: collapse; margin-top: 2em; }\n");
        builder.append("td { }\n");
        builder.append("td.gap { }\n");
        builder.append("td.meters { text-align: center; vertical-align: top; }\n");
        builder.append("td.vehicle { padding: 3px; border-bottom: 1px solid; border-left: 3px solid; border-right: 3px solid; background-color: #f0f0f0; text-color: #000000; }\n");
        builder.append("td.vehicle.groupHead { border-top: 3px solid; }\n");
        builder.append("td.vehicle.groupTail { border-bottom: 3px solid; }\n");
        builder.append("td.section { padding: 5px; border: 3px solid; background-color: #2020ff; color: #ffffff; font-weight: bold; }\n");
        builder.append("div.metersvalue { position: relative; top: -0.8em; }\n");
        builder.append("</style>");
        builder.append("</head>");
        builder.append("<body>");
        builder.append("<table>");

        int nextPlatformSectionIndex = findNext(list, -1, false);
        int nextVehicleIndex = findNext(list, -1, true);

        entry = list.get(0);
        addTableRowStart();
        if (nextPlatformSectionIndex == 0) {
            nextPlatformSectionIndex = findNext(list, 0, false);
            addTableDataPlatform(entry.platformSection, nextPlatformSectionIndex);
            addTableDataMeters(entry.fromMeters);
            addTableDataVehicle(null, nextVehicleIndex);
        } else {
            nextVehicleIndex = findNext(list, 0, true);
            addTableDataPlatform(null, nextPlatformSectionIndex);
            addTableDataMeters(entry.fromMeters);
            addTableDataVehicle(entry.vehicleData, nextVehicleIndex);
        }
        addTableRowEnd();

        for (int i = 1, listSize = list.size(); i < listSize; i++) {
            entry = list.get(i);
            addTableRowStart();
            if (i == nextPlatformSectionIndex) {
                nextPlatformSectionIndex = findNext(list, i, false);
                addTableDataPlatform(entry.platformSection, nextPlatformSectionIndex - i);
                addTableDataMeters(entry.fromMeters);
            } else {
                nextVehicleIndex = findNext(list, i, true);
                addTableDataMeters(entry.fromMeters);
                addTableDataVehicle(entry.vehicleData, nextVehicleIndex - i);
            }
            addTableRowEnd();
        }

        builder.append("</table>");
        builder.append("</body>");
        builder.append("</html>");
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

    private void addTableRowStart() {
        builder.append("<tr>");
    }

    private void addTableRowEnd() {
        builder.append("</tr>");
    }

    private void addTableDataStart(final int span, final String... cssClasses) {
        builder.append("<td rowspan=\"");
        builder.append(span);
        if (cssClasses != null) {
            builder.append("\" class=\"");
            for (final String cssClass : cssClasses) {
                if (cssClass != null) {
                    builder.append(cssClass);
                    builder.append(" ");
                }
            }
        }
        builder.append("\">");
    }

    private void addTableDataEnd() {
        builder.append("</td>");
    }

    private void addTableData(final String data, final int span, final String... cssClass) {
        addTableDataStart(span, cssClass);
        if (data != null)
            builder.append(data);
        addTableDataEnd();
    }

    @SuppressLint("DefaultLocale")
    private void addTableDataMeters(final double meters) {
        addTableData(String.format("<div class=\"metersvalue\">%.0f</div>", Math.abs(meters)), 1, "meters");
    }

    private void addTableDataPlatform(final VehicleInformation.PlatformSection platformSection, final int span) {
        if (platformSection == null) {
            addTableData(null, span, "gap");
            return;
        }
        addTableDataStart(span, "section");
        builder.append(platformSection.name);
        addTableDataEnd();
    }

    private void addTableDataVehicle(final VehicleInformation.VehicleData vehicleData, final int span) {
        if (vehicleData == null) {
            addTableData(null, span, "gap");
            return;
        }
        addTableDataStart(span, "vehicle",
                vehicleData.indexInGroup == 0 ? "groupHead" : null,
                vehicleData.indexInGroup == vehicleData.group.vehicles.size() - 1 ? "groupTail" : null);
//        builder.append("vehicle ");
//        builder.append(vehicleData.group.indexInFormation);
//        builder.append("-");
//        builder.append(vehicleData.indexInGroup);
        if (vehicleData.wagonLabel != null) {
            builder.append("<div><b>");
            builder.append(vehicleData.wagonLabel);
            builder.append("</b></div>");
        }
        if (vehicleData.vehicleIdentification != null) {
            builder.append("<div><i>");
            builder.append(vehicleData.vehicleIdentification);
            builder.append("</i></div>");
        }
        if (vehicleData.bicycleSpaces != null) {
            builder.append("<div> ");
            // builder.append("&#x1F6B2;&#xFE0E;"); bicycle character with black rendering modifier -- doesn't work
            builder.append("<svg width=\"24\" height=\"24\" viewBox=\"0 -960 960 960\" fill=\"black\">" +
                    "<path d=\"M200-160q-85 0-142.5-57.5T0-360q0-85 58.5-142.5T200-560q77 0 129.5 46T396-400h26l-72-200h-30q-17 0-28.5-11.5T280-640q0-17 11.5-28.5T320-680h120q17 0 28.5 11.5T480-640q0 17-11.5 28.5T440-600h-4l14 40h192l-58-160h-64q-17 0-28.5-11.5T480-760q0-17 11.5-28.5T520-800h64q26 0 46.5 14t29.5 38l68 186h32q83 0 141.5 58.5T960-362q0 84-58 143t-142 59q-72 0-126.5-45T564-320H396q-14 69-68 114.5T200-160Zm0-80q41 0 70.5-22.5T312-320h-72q-17 0-28.5-11.5T200-360q0-17 11.5-28.5T240-400h72q-12-36-41.5-58T200-480q-51 0-85.5 34.5T80-360q0 50 34.5 85t85.5 35Zm308-160h56q5-23 13.5-43t22.5-37H478l30 80Zm252 160q51 0 85.5-35t34.5-85q0-51-34.5-85.5T760-480h-4l26 69q6 16-1 30.5T758-360q-16 6-31-1t-21-23l-24-68q-20 17-31 40t-11 52q0 50 34.5 85t85.5 35ZM196-360Zm564 0Z\"/>" +
                    "</svg>");
            builder.append(" ");
            builder.append(vehicleData.bicycleSpaces.available);
            builder.append(" / ");
            builder.append(vehicleData.bicycleSpaces.total);
            builder.append("</div>");
        }
//        vehicleData.firstClass;
//        vehicleData.economyClass;
//        vehicleData.infoZone;
//        vehicleData.valuedCustomer;
//        vehicleData.childrenSpace;
//        vehicleData.familyZone;
//        vehicleData.quietZone;
//        vehicleData.seatsForDisabled;
//        vehicleData.toiletForWheelChair;
//        vehicleData.airCondition;
//        vehicleData.wheelChairSpaces;
        addTableDataEnd();
    }
}
