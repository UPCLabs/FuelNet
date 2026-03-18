package co.edu.unipiloto.fuelcontrol.api.requests;

public class AuthResponse {
    private String token;
    private String role;

    public String getToken() {
        return token;
    }

    public String getRole() {
        return role;
    }
}