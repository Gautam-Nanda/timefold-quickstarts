package org.acme.vehiclerouting.rest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.acme.vehiclerouting.domain.Location;
import org.acme.vehiclerouting.domain.Vehicle;
import org.acme.vehiclerouting.domain.VehicleRoutePlan;
import org.acme.vehiclerouting.domain.Visit;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@Tag(name = "Demo data", description = "Timefold-provided demo vehicle routing data.")
@Path("demo-data")
public class VehicleRouteDemoResource {
        public JSONArray sheet1;
        public JSONArray sheet2;
        public JSONArray sheet3;

        @Operation(summary = "Receive array of JSON data.")
        @POST
        @Path("/upload")
        @Consumes(MediaType.APPLICATION_JSON)
        public void receiveData(String data) {
                JSONParser parser = new JSONParser();
                try {
                        JSONObject obj = (JSONObject) parser.parse(data);
                        this.sheet1 = (JSONArray) parser.parse(obj.get("0").toString());
                        this.sheet2 = (JSONArray) parser.parse(obj.get("1").toString());
                        this.sheet3 = (JSONArray) parser.parse(obj.get("2").toString());
                        // System.out.println(sheet3);

                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        private static final String[] FIRST_NAMES = { "Amy", "Beth", "Chad", "Dan", "Elsa", "Flo", "Gus", "Hugo", "Ivy",
                        "Jay" };
        private static final String[] LAST_NAMES = { "Cole", "Fox", "Green", "Jones", "King", "Li", "Poe", "Rye",
                        "Smith", "Watt" };
        private static final int[] SERVICE_DURATION_MINUTES = { 10, 20, 35, 40 };
        private static final LocalTime MORNING_WINDOW_START = LocalTime.of(8, 0);
        private static final LocalTime MORNING_WINDOW_END = LocalTime.of(12, 0);
        private static final LocalTime AFTERNOON_WINDOW_START = LocalTime.of(13, 0);
        private static final LocalTime AFTERNOON_WINDOW_END = LocalTime.of(18, 0);

        public enum DemoData {
                PHILADELPHIA(0, 55, 6, LocalTime.of(7, 30), 1, 50, 15, 30,
                                new Location(39.7656099067391, -76.83782328143754, -2),
                                new Location(40.77636644354855, -74.9300739430771, -3));

                private long seed;
                private int visitCount;
                private int vehicleCount;
                private LocalTime vehicleStartTime;
                private int minDemand;
                private int maxDemand;
                private int minVehicleCapacity;
                private int maxVehicleCapacity;
                private Location southWestCorner;
                private Location northEastCorner;

                DemoData(long seed, int visitCount, int vehicleCount, LocalTime vehicleStartTime,
                                int minDemand, int maxDemand, int minVehicleCapacity, int maxVehicleCapacity,
                                Location southWestCorner, Location northEastCorner) {
                        if (minDemand < 1) {
                                throw new IllegalStateException(
                                                "minDemand (%s) must be greater than zero.".formatted(minDemand));
                        }

                        if (maxDemand < 1) {
                                throw new IllegalStateException(
                                                "maxDemand (%s) must be greater than zero.".formatted(maxDemand));
                        }
                        if (minDemand >= maxDemand) {
                                throw new IllegalStateException("maxDemand (%s) must be greater than minDemand (%s)."
                                                .formatted(maxDemand, minDemand));
                        }
                        if (minVehicleCapacity < 1) {
                                throw new IllegalStateException(
                                                "Number of minVehicleCapacity (%s) must be greater than zero."
                                                                .formatted(minVehicleCapacity));
                        }
                        if (maxVehicleCapacity < 1) {
                                throw new IllegalStateException(
                                                "Number of maxVehicleCapacity (%s) must be greater than zero."
                                                                .formatted(maxVehicleCapacity));
                        }
                        if (minVehicleCapacity >= maxVehicleCapacity) {
                                throw new IllegalStateException(
                                                "maxVehicleCapacity (%s) must be greater than minVehicleCapacity (%s)."
                                                                .formatted(maxVehicleCapacity, minVehicleCapacity));
                        }
                        if (visitCount < 1) {
                                throw new IllegalStateException(
                                                "Number of visitCount (%cs) must be greater than zero."
                                                                .formatted(visitCount));
                        }
                        if (vehicleCount < 1) {
                                throw new IllegalStateException(
                                                "Number of vehicleCount (%s) must be greater than zero."
                                                                .formatted(vehicleCount));
                        }
                        if (northEastCorner.getLatitude() <= southWestCorner.getLatitude()) {
                                throw new IllegalStateException(
                                                "northEastCorner.getLatitude (%s) must be greater than southWestCorner.getLatitude(%s)."
                                                                .formatted(northEastCorner.getLatitude(),
                                                                                southWestCorner.getLatitude()));
                        }
                        if (northEastCorner.getLongitude() <= southWestCorner.getLongitude()) {
                                throw new IllegalStateException(
                                                "northEastCorner.getLongitude (%s) must be greater than southWestCorner.getLongitude(%s)."
                                                                .formatted(northEastCorner.getLongitude(),
                                                                                southWestCorner.getLongitude()));
                        }

                        this.seed = seed;
                        this.visitCount = visitCount;
                        this.vehicleCount = vehicleCount;
                        this.vehicleStartTime = vehicleStartTime;
                        this.minDemand = minDemand;
                        this.maxDemand = maxDemand;
                        this.minVehicleCapacity = minVehicleCapacity;
                        this.maxVehicleCapacity = maxVehicleCapacity;
                        this.southWestCorner = southWestCorner;
                        this.northEastCorner = northEastCorner;
                }
        }

        @APIResponses(value = {
                        @APIResponse(responseCode = "200", description = "List of demo data represented as IDs.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DemoData.class, type = SchemaType.ARRAY))) })
        @Operation(summary = "List demo data.")
        @GET
        public DemoData[] list() {
                return DemoData.values();
        }

        @APIResponses(value = {
                        @APIResponse(responseCode = "200", description = "Unsolved demo route plan.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VehicleRoutePlan.class))) })
        @Operation(summary = "Find an unsolved demo route plan by ID.")
        @GET
        @Path("/{demoDataId}")
        public Response generate(
                        @Parameter(description = "Unique identifier of the demo data.", required = true) @PathParam("demoDataId") DemoData demoData) {
                if (sheet1 == null) {
                        return Response.status(Response.Status.BAD_REQUEST)
                                        .entity("No data received. Please upload data first.").build();
                }

                VehicleRoutePlan plan = build(demoData);
                return Response.ok(plan).build();
        }

        public VehicleRoutePlan build(DemoData demoData) {
                String name = "demo";

                List<Vehicle> vehicles = new ArrayList<>();

                for (int i = 0; i < sheet2.size(); i++) {
                        JSONObject vehicleObject = (JSONObject) sheet2.get(i);
                        String vehicleId = vehicleObject.get("Vehicles").toString();
                        int assignedDepot = Integer.parseInt(vehicleObject.get("AssignedDepot").toString());
                        int capacity = Integer.parseInt(vehicleObject.get("Capacity").toString());
                        long readyTimeSeconds = Long.parseLong(vehicleObject.get("VehicleStartTime").toString());
                        LocalDateTime startTime = tomorrowAt(LocalTime.ofSecondOfDay(readyTimeSeconds));

                        Location location;
                        if (assignedDepot == 0) {
                                // Find the depot in sheet1 where Type=Depot
                                JSONObject depot = (JSONObject) sheet1.stream()
                                                .filter(obj -> "Depot".equals(((JSONObject) obj).get("Type")))
                                                .findFirst()
                                                .orElse(null);

                                if (depot != null) {
                                        double depotLatitude = Double.parseDouble(depot.get("Lat").toString());
                                        double depotLongitude = Double.parseDouble(depot.get("Long").toString());
                                        int locationid = Integer.parseInt(depot.get("Location").toString());
                                        location = new Location(depotLatitude, depotLongitude, locationid);
                                } else {
                                        // Handle the case where no depot is found
                                        System.err.println("No depot found in sheet1.");
                                        continue; // Skip this vehicle
                                }
                        } else {
                                // Find the assigned depot in sheet1
                                JSONObject depot = (JSONObject) sheet1.stream()
                                                .filter(obj -> "Depot".equals(((JSONObject) obj).get("Type")) &&
                                                                Integer.parseInt(((JSONObject) obj).get("Depot")
                                                                                .toString()) == assignedDepot)
                                                .findFirst()
                                                .orElse(null);

                                if (depot != null) {
                                        double depotLatitude = Double.parseDouble(depot.get("Lat").toString());
                                        double depotLongitude = Double.parseDouble(depot.get("Long").toString());
                                        int locationid = Integer.parseInt(depot.get("Location").toString());
                                        location = new Location(depotLatitude, depotLongitude, locationid);
                                } else {
                                        // Handle the case where the assigned depot is not found
                                        System.err.println("Assigned depot not found in sheet1.");
                                        continue; // Skip this vehicle
                                }
                        }

                        // Create the vehicle and add it to the list
                        Vehicle vehicle = new Vehicle(vehicleId, capacity, location,
                                        startTime);
                        vehicles.add(vehicle);
                }

                List<Visit> visits = new ArrayList<>();
                // List<Double> lat = new ArrayList<>();
                AtomicLong visitSequence = new AtomicLong();
                for (Object obj : sheet1) {
                        JSONObject visitObject = (JSONObject) obj;
                        if ("Customer".equals(visitObject.get("Type"))) {

                                String visitId = visitObject.get("Location").toString();
                                String namee = visitObject.get("Type").toString()
                                                + visitObject.get("Location").toString();
                                double latitude = Double.parseDouble(visitObject.get("Lat").toString());
                                double longitude = Double.parseDouble(visitObject.get("Long").toString());
                                int demandValue = Integer.parseInt(visitObject.get("Demand").toString());

                                // Convert "Ready Time" and "Due Time" from seconds to LocalDateTime
                                long readyTimeSeconds = Long.parseLong(visitObject.get("Ready Time").toString());
                                long dueTimeSeconds = Long.parseLong(visitObject.get("Due Time").toString());

                                LocalDateTime minStartTime = tomorrowAt(LocalTime.ofSecondOfDay(readyTimeSeconds));
                                LocalDateTime maxEndTime = tomorrowAt(LocalTime.ofSecondOfDay(dueTimeSeconds));

                                // Calculate the duration between minStartTime and maxEndTime
                                Duration serviceDuration = Duration.between(minStartTime, maxEndTime);
                                // lat.add(latitude);
                                int locationid = Integer.parseInt(visitObject.get("Location").toString());
                                // Create the visit and add it to the list
                                Visit visit = new Visit(
                                                visitId,
                                                namee,
                                                new Location(latitude, longitude, locationid),
                                                demandValue,
                                                minStartTime,
                                                maxEndTime,
                                                serviceDuration);
                                visits.add(visit);
                        }
                }
                // System.out.println(lat);
                // use sheet1 row 0 to find start time and end time
                long readyTimeSeconds = Long.parseLong(((JSONObject) sheet1.get(0)).get("Ready Time").toString());
                long dueTimeSeconds = Long.parseLong(((JSONObject) sheet1.get(0)).get("Due Time").toString());
                LocalDateTime minStartTime = tomorrowAt(LocalTime.ofSecondOfDay(readyTimeSeconds));
                LocalDateTime maxEndTime = tomorrowAt(LocalTime.ofSecondOfDay(dueTimeSeconds));
                List<Map<String, Object>> list = new ArrayList<>();

                for (Object obj : sheet3) {
                        JSONObject jsonObject = (JSONObject) obj;
                        Map<String, Object> map = new HashMap<>(jsonObject);
                        list.add(map);
                }
                return new VehicleRoutePlan(name, VehicleRouteDemoResource.findSouthWestCorner(sheet1),
                                VehicleRouteDemoResource.findNorthEastCorner(sheet1),
                                minStartTime, maxEndTime,
                                vehicles, visits, list);
        }

        private static LocalDateTime tomorrowAt(LocalTime time) {
                return LocalDateTime.of(LocalDate.now().plusDays(1L), time);
        }

        public static Location findSouthWestCorner(JSONArray sheet1) {
                double minLat = Double.MAX_VALUE;
                double minLong = Double.MAX_VALUE;

                for (Object obj : sheet1) {
                        JSONObject jsonObject = (JSONObject) obj;
                        double lat = Double.parseDouble(jsonObject.get("Lat").toString());
                        double lon = Double.parseDouble(jsonObject.get("Long").toString());

                        if (lat < minLat && lon < minLong) {
                                minLat = lat;
                                minLong = lon;
                        }
                }

                return new Location(minLat, minLong, -1);
        }

        public static Location findNorthEastCorner(JSONArray sheet1) {
                double maxLat = Double.MIN_VALUE;
                double maxLong = Double.MIN_VALUE;

                for (Object obj : sheet1) {
                        JSONObject jsonObject = (JSONObject) obj;
                        double lat = Double.parseDouble(jsonObject.get("Lat").toString());
                        double lon = Double.parseDouble(jsonObject.get("Long").toString());

                        if (lat > maxLat && lon > maxLong) {
                                maxLat = lat;
                                maxLong = lon;
                        }
                }

                return new Location(maxLat, maxLong, 0);
        }
}
