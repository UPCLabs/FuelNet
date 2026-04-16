package co.edu.unipiloto.fuelcontrol.api.requests;

import com.google.gson.annotations.SerializedName;

public class MeResponse {

    @SerializedName("id")
    private Integer id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("username")
    private String username;

    @SerializedName("address")
    private String address;

    @SerializedName("birthDate")
    private String birthDate;

    @SerializedName("gender")
    private String gender;

    @SerializedName("role")
    private String role;

    @SerializedName("stationId")
    private Integer stationId;


    public Integer getId() { return id; }

    public String getName() { return name; }

    public String getEmail() { return email; }

    public String getUsername() { return username; }

    public String getAddress() { return address; }

    public String getBirthDate() { return birthDate; }

    public String getGender() { return gender; }

    public String getRole() { return role; }

    public Integer getStationId() { return stationId; }
}