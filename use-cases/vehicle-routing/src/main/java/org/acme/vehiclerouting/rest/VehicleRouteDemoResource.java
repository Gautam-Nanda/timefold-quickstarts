package org.acme.vehiclerouting.rest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
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

                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        private static final String[] FIRST_NAMES = { "Amy", "Beth", "Chad", "Dan", "Elsa", "Flo", "Gus", "Hugo", "Ivy",
                        "Jay" };
        private static final String[] LAST_NAMES = { "Cole", "Fox", "Green", "Jones", "King", "Li", "Poe", "Rye",
                        "Smith", "Watt" };
        private static final int[] SERVICE_DURATION_MINUTES = { 10, 20, 30, 40 };
        private static final LocalTime MORNING_WINDOW_START = LocalTime.of(8, 0);
        private static final LocalTime MORNING_WINDOW_END = LocalTime.of(12, 0);
        private static final LocalTime AFTERNOON_WINDOW_START = LocalTime.of(13, 0);
        private static final LocalTime AFTERNOON_WINDOW_END = LocalTime.of(18, 0);

        public enum DemoData {
                PHILADELPHIA(0, 55, 6, LocalTime.of(7, 30), 1, 50, 15, 30,
                                new Location(39.7656099067391, -76.83782328143754),
                                new Location(40.77636644354855, -74.9300739430771)),
                HARTFORT(1, 50, 6, LocalTime.of(7, 30), 1, 3, 20, 30,
                                new Location(41.48366520850297, -73.15901689943055),
                                new Location(41.99512052869307, -72.25114548877427)),
                DELHI(3, 50, 6, LocalTime.of(7, 30), 1, 3, 20, 30,
                                new Location(28.474849, 77.058861), new Location(28.496776, 77.101406)),
                FIRENZE(2, 77, 6, LocalTime.of(7, 30), 1, 2, 20, 40,
                                new Location(43.751466, 11.177210), new Location(43.809291, 11.290195));

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
                                                "Number of visitCount (%s) must be greater than zero."
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

                // Extract latitudes and longitudes from sheet1
                List<Double> latitudes = (List<Double>) sheet1.stream()
                                .filter(obj -> "Customer".equals(((JSONObject) obj).get("Type"))) // Filter by Type =
                                                                                                  // Customer
                                .map(obj -> Double.parseDouble(((JSONObject) obj).get("Lat").toString()))
                                .collect(Collectors.toList());

                List<Double> longitudes = (List<Double>) sheet1.stream()
                                .filter(obj -> "Customer".equals(((JSONObject) obj).get("Type"))) // Filter by Type =
                                                                                                  // Customer
                                .map(obj -> Double.parseDouble(((JSONObject) obj).get("Long").toString()))
                                .collect(Collectors.toList());

                // Ensure that there are enough coordinates for visits and vehicles
                if (latitudes.size() < demoData.visitCount + demoData.vehicleCount
                                || longitudes.size() < demoData.visitCount + demoData.vehicleCount) {
                        throw new IllegalStateException("Not enough coordinates provided in the uploaded data.");
                }

                PrimitiveIterator.OfInt demand = sheet1.stream()
                                .filter(obj -> "Customer".equals(((JSONObject) obj).get("Type"))) // Filter by Type =
                                                                                                  // Customer
                                .mapToInt(obj -> Integer.parseInt(((JSONObject) obj).get("Demand").toString())) // Extract
                                                                                                                // demand
                                                                                                                // values
                                .iterator();

                PrimitiveIterator.OfInt vehicleCapacity = new Random(demoData.seed)
                                .ints(demoData.minVehicleCapacity, demoData.maxVehicleCapacity + 1).iterator();

                AtomicLong vehicleSequence = new AtomicLong();
                Supplier<Vehicle> vehicleSupplier = () -> new Vehicle(
                                String.valueOf(vehicleSequence.incrementAndGet()),
                                vehicleCapacity.nextInt(),
                                new Location(latitudes.remove(0), longitudes.remove(0)),
                                tomorrowAt(demoData.vehicleStartTime));

                List<Vehicle> vehicles = Stream.generate(vehicleSupplier)
                                .limit(demoData.vehicleCount)
                                .collect(Collectors.toList());

                Supplier<String> nameSupplier = () -> {
                        Function<String[], String> randomStringSelector = strings -> strings[new Random(demoData.seed)
                                        .nextInt(strings.length)];
                        String firstName = randomStringSelector.apply(FIRST_NAMES);
                        String lastName = randomStringSelector.apply(LAST_NAMES);
                        return firstName + " " + lastName;
                };

                AtomicLong visitSequence = new AtomicLong();
                Supplier<Visit> visitSupplier = () -> {
                        boolean morningTimeWindow = new Random(demoData.seed).nextBoolean();

                        LocalDateTime minStartTime = morningTimeWindow ? tomorrowAt(MORNING_WINDOW_START)
                                        : tomorrowAt(AFTERNOON_WINDOW_START);
                        LocalDateTime maxEndTime = morningTimeWindow ? tomorrowAt(MORNING_WINDOW_END)
                                        : tomorrowAt(AFTERNOON_WINDOW_END);
                        int serviceDurationMinutes = SERVICE_DURATION_MINUTES[new Random(demoData.seed)
                                        .nextInt(SERVICE_DURATION_MINUTES.length)];
                        return new Visit(
                                        String.valueOf(visitSequence.incrementAndGet()),
                                        nameSupplier.get(),
                                        new Location(latitudes.remove(0), longitudes.remove(0)),
                                        demand.nextInt(),
                                        minStartTime,
                                        maxEndTime,
                                        Duration.ofMinutes(serviceDurationMinutes));
                };

                List<Visit> visits = Stream.generate(visitSupplier)
                                .limit(demoData.visitCount)
                                .collect(Collectors.toList());

                return new VehicleRoutePlan(name, demoData.southWestCorner, demoData.northEastCorner,
                                tomorrowAt(demoData.vehicleStartTime), tomorrowAt(LocalTime.MIDNIGHT).plusDays(1L),
                                vehicles, visits);
        }

        private static LocalDateTime tomorrowAt(LocalTime time) {
                return LocalDateTime.of(LocalDate.now().plusDays(1L), time);
        }
}
