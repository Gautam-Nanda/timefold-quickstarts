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

public class VehicleRoutingConstraintProvider implements ConstraintProvider {

        public static final String VEHICLE_CAPACITY = "vehicleCapacity";
        public static final String SERVICE_FINISHED_AFTER_MAX_END_TIME = "serviceFinishedAfterMaxEndTime";
        public static final String MINIMIZE_TRAVEL_TIME = "minimizeTravelTime";

        @Override
        public Constraint[] defineConstraints(ConstraintFactory factory) {
                // System.out.println("HELLLLO");
                return new Constraint[] {
                                vehicleCapacity(factory),
                                serviceFinishedAfterMaxEndTime(factory),
                                minimizeTravelTime(factory)
                };
        }

        // ************************************************************************
        // Hard constraints
        // ************************************************************************

        protected Constraint vehicleCapacity(ConstraintFactory factory) {
                return factory.forEach(Vehicle.class)
                                .filter(vehicle -> vehicle.getTotalDemand() > vehicle.getCapacity())
                                .penalizeLong(HardSoftLongScore.ONE_HARD,
                                                vehicle -> vehicle.getTotalDemand() - vehicle.getCapacity())
                                .justifyWith((vehicle, score) -> new VehicleCapacityJustification(vehicle.getId(),
                                                vehicle.getTotalDemand(),
                                                vehicle.getCapacity()))
                                .asConstraint(VEHICLE_CAPACITY);
        }

        protected Constraint serviceFinishedAfterMaxEndTime(ConstraintFactory factory) {
                return factory.forEach(Visit.class)
                                .filter(Visit::isServiceFinishedAfterMaxEndTime)
                                .penalizeLong(HardSoftLongScore.ONE_HARD,
                                                Visit::getServiceFinishedDelayInMinutes)
                                .justifyWith((visit, score) -> new ServiceFinishedAfterMaxEndTimeJustification(
                                                visit.getId(),
                                                visit.getServiceFinishedDelayInMinutes()))
                                .asConstraint(SERVICE_FINISHED_AFTER_MAX_END_TIME);
        }

        // ************************************************************************
        // Soft constraints
        // ************************************************************************

        protected Constraint minimizeTravelTime(ConstraintFactory factory) {
                return factory.forEach(Vehicle.class)
                                .penalizeLong(HardSoftLongScore.ONE_SOFT,
                                                Vehicle::getTotalDrivingTimeSeconds)
                                .justifyWith((vehicle, score) -> new MinimizeTravelTimeJustification(vehicle.getId(),
                                                vehicle.getTotalDrivingTimeSeconds()))
                                .asConstraint(MINIMIZE_TRAVEL_TIME);
        }
}
