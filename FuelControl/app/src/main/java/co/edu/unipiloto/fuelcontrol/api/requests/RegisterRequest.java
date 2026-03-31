package co.edu.unipiloto.fuelcontrol.api.requests;

public class RegisterRequest {
    private String fullName;
    private String username;
    private String email;
    private String password;
    private String address;
    private String birthDate;
    private String role;
    private String gender;



    public RegisterRequest(String fullName, String username, String email, String password,
                           String address, String birthDate, String role, String gender) {
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.password = password;
        this.address = address;
        this.birthDate = birthDate;
        this.role = role;
        this.gender = gender;
    }
}