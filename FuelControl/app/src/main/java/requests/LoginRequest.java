package requests;

public class LoginRequest {
    private String email;
    private String password;

    public LoginRequest(String correo, String password) {
        this.email = correo;
        this.password = password;
    }
}
