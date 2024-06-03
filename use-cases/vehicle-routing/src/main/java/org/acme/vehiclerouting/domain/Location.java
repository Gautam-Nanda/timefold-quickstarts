package org.acme.vehiclerouting.domain;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class Location {
    private int id;
    private double latitude;
    private double longitude;

    @JsonIgnore
    private Map<Location, Long> drivingTimeSeconds;

    @JsonCreator
    public Location( @JsonProperty("latitude") double latitude, @JsonProperty("longitude") double longitude, @JsonProperty("id") int id) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.id = id;
        this.drivingTimeSeconds = new HashMap<>();
    }

    public double getLatitude() {
        return latitude;
    }

    public int getLocationId() {
        return id;
    }

    public double getLongitude() {
        return longitude;
    }

    public Map<Location, Long> getDrivingTimeSeconds() {
        return drivingTimeSeconds;
    }

    /**
     * Set the driving time map (in seconds).
     *
     * @param drivingTimeSeconds a map containing driving time from here to other locations
     */
    public void setDrivingTimeSeconds(Map<Location, Long> drivingTimeSeconds) {
        this.drivingTimeSeconds = drivingTimeSeconds;
    }

    /**
     * Driving time to the given location in seconds.
     *
     * @param location other location
     * @return driving time in seconds
     */
    public long getDrivingTimeTo(Location location) {
        Long drivingTime = drivingTimeSeconds.get(location);
        if (drivingTime == null) {
            return 0; 
        }
        return drivingTime.longValue();
    }

    @Override
    public String toString() {
        return id + "," + latitude + "," + longitude;
    }

}
