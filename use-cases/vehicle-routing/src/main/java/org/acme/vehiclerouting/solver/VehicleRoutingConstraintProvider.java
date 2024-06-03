package org.acme.vehiclerouting.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.vehiclerouting.domain.Visit;
import org.acme.vehiclerouting.domain.Vehicle;
import org.acme.vehiclerouting.solver.justifications.MinimizeTravelTimeJustification;
import org.acme.vehiclerouting.solver.justifications.ServiceFinishedAfterMaxEndTimeJustification;
import org.acme.vehiclerouting.solver.justifications.VehicleCapacityJustification;
import org.acme.vehiclerouting.solver.justifications.VehicleCapacityJustification;

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class VehicleRoutingConstraintProvider implements ConstraintProvider {

        public static final String VEHICLE_CAPACITY = "vehicleCapacity";
        public static final String SERVICE_FINISHED_AFTER_MAX_END_TIME = "serviceFinishedAfterMaxEndTime";
        public static final String MINIMIZE_TRAVEL_TIME = "minimizeTravelTime";

        @Override
        public Constraint[] defineConstraints(ConstraintFactory factory) {
                String vc = "HARD", sfme = "HARD", mtt = "SOFT";
                try {
                        String currentDir = System.getProperty("user.dir");
                        String fileDir = currentDir + "\\src\\main\\java\\org\\acme\\vehiclerouting\\solver";
                        String content = new String(Files.readAllBytes(Paths.get(fileDir + "\\sheet4.txt")));

                        JSONParser parser = new JSONParser();
                        JSONArray jsonArray = (JSONArray) parser.parse(content);
                        for (Object obj : jsonArray) {
                                JSONObject jsonObject = (JSONObject) obj;
                                String type = (String) jsonObject.get("Type");
                                String constraint = (String) jsonObject.get("Constraint");

                                switch (constraint) {
                                        case "vehicleCapacity":
                                                vc = type;
                                                break;
                                        case "serviceFinishedAfterMaxEndTime":
                                                sfme = type;
                                                break;
                                        case "minimizeTravelTime":
                                                mtt = type;
                                                break;
                                }
                        }
                } catch (IOException | ParseException e) {
                        e.printStackTrace();
                }
                // System.out.println(vc + " " + sfme + " " + mtt);
                return new Constraint[] {
                                vehicleCapacity(factory, vc),
                                serviceFinishedAfterMaxEndTime(factory, sfme),
                                minimizeTravelTime(factory, mtt)
                };
        }

        // ************************************************************************
        // Hard constraints
        // ************************************************************************

        public Constraint vehicleCapacity(ConstraintFactory factory, String type) {
                System.out.println(type);
                System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                return factory.forEach(Vehicle.class)
                                .filter(vehicle -> vehicle.getTotalDemand() > vehicle.getCapacity())
                                .penalizeLong(type.equals("HARD") ? HardSoftLongScore.ONE_HARD
                                                : HardSoftLongScore.ONE_SOFT,
                                                vehicle -> vehicle.getTotalDemand() - vehicle.getCapacity())
                                .justifyWith((vehicle, score) -> new VehicleCapacityJustification(vehicle.getId(),
                                                vehicle.getTotalDemand(),
                                                vehicle.getCapacity()))
                                .asConstraint(VEHICLE_CAPACITY);
        }

        public Constraint serviceFinishedAfterMaxEndTime(ConstraintFactory factory, String type) {
                return factory.forEach(Visit.class)
                                .filter(Visit::isServiceFinishedAfterMaxEndTime)
                                .penalizeLong(type.equals("HARD") ? HardSoftLongScore.ONE_HARD
                                                : HardSoftLongScore.ONE_SOFT,
                                                Visit::getServiceFinishedDelayInMinutes)
                                .justifyWith((visit, score) -> new ServiceFinishedAfterMaxEndTimeJustification(
                                                visit.getId(),
                                                visit.getServiceFinishedDelayInMinutes()))
                                .asConstraint(SERVICE_FINISHED_AFTER_MAX_END_TIME);
        }

        // ************************************************************************
        // Soft constraints
        // ************************************************************************

        public Constraint minimizeTravelTime(ConstraintFactory factory, String type) {
                return factory.forEach(Vehicle.class)
                                .penalizeLong(type.equals("HARD") ? HardSoftLongScore.ONE_HARD
                                                : HardSoftLongScore.ONE_SOFT,
                                                Vehicle::getTotalDrivingTimeSeconds)
                                .justifyWith((vehicle, score) -> new MinimizeTravelTimeJustification(vehicle.getId(),
                                                vehicle.getTotalDrivingTimeSeconds()))
                                .asConstraint(MINIMIZE_TRAVEL_TIME);
        }
}
