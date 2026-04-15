package co.edu.unipiloto.fuelcontrol.api.requests;

import com.google.gson.annotations.SerializedName;

public class MeResponse {
    @SerializedName("station")
    private String station;

    public Integer getStation() {
        if (station == null || station.equalsIgnoreCase("null")) {
            return null;
        }
        return Integer.parseInt(station);
    }


}
