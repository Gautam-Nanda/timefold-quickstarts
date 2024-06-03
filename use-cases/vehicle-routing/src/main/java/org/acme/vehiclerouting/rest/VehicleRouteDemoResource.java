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
import org.acme.vehiclerouting.solver.VehicleRoutingConstraintProvider;
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
import org.json.simple.parser.ParseException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

@Tag(name = "Demo data", description = "Timefold-provided demo vehicle routing data.")
@Path("demo-data")
public class VehicleRouteDemoResource {
        public JSONArray sheet1;
        public JSONArray sheet2;
        public JSONArray sheet3;
        public JSONArray sheet4;

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
                        this.sheet4 = (JSONArray) parser.parse(obj.get("3").toString());

                        // Write sheet4 data to a text file
                        String currentDir = System.getProperty("user.dir");
                        String fileDir = currentDir + "\\src\\main\\java\\org\\acme\\vehiclerouting\\solver";

                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileDir + "\\sheet4.txt"))) {
                                writer.write(this.sheet4.toJSONString());
                        } catch (IOException e) {
                                e.printStackTrace();
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        public enum DemoData {
                Delhi;

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
                        Location location = null;
                        LocalDateTime startTime = null;

                        // Iterate over sheet1 to find the same "Location" as "assignedDepot"
                        for (Object obj : sheet1) {
                                JSONObject depot = (JSONObject) obj;
                                int locationId = Integer.parseInt(depot.get("Location").toString());
                                if (locationId == assignedDepot) {
                                        double depotLatitude = Double.parseDouble(depot.get("Lat").toString());
                                        double depotLongitude = Double.parseDouble(depot.get("Long").toString());
                                        location = new Location(depotLatitude, depotLongitude, locationId);

                                        long readyTimeSeconds = Long.parseLong(depot.get("Ready Time").toString());
                                        startTime = tomorrowAt(LocalTime.ofSecondOfDay(readyTimeSeconds));

                                        break;
                                }
                        }

                        if (location == null || startTime == null) {
                                // Handle the case where the assigned depot or ready time is not found
                                System.err.println("Assigned depot or ready time not found in sheet1.");
                                continue; // Skip this vehicle
                        }

                        // Create the vehicle and add it to the list
                        Vehicle vehicle = new Vehicle(vehicleId, capacity, location, startTime);
                        vehicles.add(vehicle);
                }

                List<Visit> visits = new ArrayList<>();
                // List<Double> lat = new ArrayList<>();
                AtomicLong visitSequence = new AtomicLong();
                for (Object obj : sheet1) {
                        JSONObject visitObject = (JSONObject) obj;
                        if ("Customer".equals(visitObject.get("Type"))) {

                                String visitId = visitObject.get("Location").toString();
                                String namee = visitObject.get("Name").toString();
                                double latitude = Double.parseDouble(visitObject.get("Lat").toString());
                                double longitude = Double.parseDouble(visitObject.get("Long").toString());
                                int demandValue = Integer.parseInt(visitObject.get("Demand").toString());

                                // Convert "Ready Time" and "Due Time" from seconds to LocalDateTime
                                long readyTimeSeconds = Long.parseLong(visitObject.get("Ready Time").toString());
                                long dueTimeSeconds = Long.parseLong(visitObject.get("Due Time").toString());

                                LocalDateTime minStartTime = tomorrowAt(LocalTime.ofSecondOfDay(readyTimeSeconds));
                                LocalDateTime maxEndTime = tomorrowAt(LocalTime.ofSecondOfDay(dueTimeSeconds));
                                long serviceDuration = Long.parseLong(visitObject.get("serviceDuration").toString());
                                Duration duration = Duration.ofSeconds(serviceDuration);

                                int locationid = Integer.parseInt(visitObject.get("Location").toString());
                                // Create the visit and add it to the list
                                Visit visit = new Visit(
                                                visitId,
                                                namee,
                                                new Location(latitude, longitude, locationid),
                                                demandValue,
                                                minStartTime,
                                                maxEndTime,
                                                duration);
                                visits.add(visit);
                        }
                }

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
