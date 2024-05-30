package org.acme.vehiclerouting.domain.geo;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.acme.vehiclerouting.domain.Location;

public interface DrivingTimeCalculator {

    /**
     * Calculate the driving time between {@code from} and {@code to} in seconds.
     *
     * @param from starting location
     * @param to   target location
     * @return driving time in seconds
     */
    long calculateDrivingTime(Location from, Location to);

    /**
     * Bulk calculation of driving time.
     * Typically, much more scalable than
     * {@link #calculateDrivingTime(Location, Location)} iteratively.
     *
     * @param fromLocations never null
     * @param toLocations   never null
     * @return never null
     */
    default Map<Location, Map<Location, Long>> calculateBulkDrivingTime(
            Collection<Location> fromLocations,
            Collection<Location> toLocations) {
        return fromLocations.stream().collect(Collectors.toMap(
                Function.identity(),
                from -> toLocations.stream().collect(Collectors.toMap(
                        Function.identity(),
                        to -> calculateDrivingTime(from, to)))));
    }

    /**
     * Calculate driving time matrix for the given list of locations and assign
     * driving time maps accordingly.
     *
     * @param locations locations list
     * @param sheet3    driving time data as a List of Maps
     */
    default void initDrivingTimeMaps(Collection<Location> locations, List<Map<String, Object>> sheet3) {
        // Parse the List of Maps to create the driving time matrix
        Map<Integer, Map<Integer, Long>> timeMatrix = new HashMap<>();
        for (Map<String, Object> entry : sheet3) {
            int fromId = ((Number) entry.get("Time")).intValue();
            Map<Integer, Long> row = new HashMap<>();
            for (Map.Entry<String, Object> mapEntry : entry.entrySet()) {
                String key = mapEntry.getKey();
                if (!key.equals("Time")) {
                    int toId = Integer.parseInt(key);
                    long time = ((Number) mapEntry.getValue()).longValue();
                    row.put(toId, time);
                }
            }
            timeMatrix.put(fromId, row);
        }

        // Map locations by their ID for easier lookup, handling duplicates
        Map<Integer, Location> locationById = new HashMap<>();
        for (Location location : locations) {
            int locationId = location.getLocationId();
            if (locationById.containsKey(locationId)) {
                System.out.println("Duplicate location ID found: " + locationId + ". Ignoring duplicate.");
                continue; // Ignore duplicate location
            }
            locationById.put(locationId, location);
        }

        // Create the driving time matrix using the parsed data
        Map<Location, Map<Location, Long>> drivingTimeMatrix = locationById.values().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        from -> locationById.values().stream()
                                .collect(Collectors.toMap(
                                        Function.identity(),
                                        to -> timeMatrix.get(from.getLocationId()).get(to.getLocationId())))));

        // Assigning the calculated driving time maps to each location
        locationById.values().forEach(location -> location.setDrivingTimeSeconds(drivingTimeMatrix.get(location)));
        System.out.println(drivingTimeMatrix);
    }
}
