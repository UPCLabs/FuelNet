package co.edu.unipiloto.fuelcontrol.api.requests;

public class LoginRequest {
    private String email;
    private String password;

    public LoginRequest(String correo, String password) {
        this.email = correo;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
