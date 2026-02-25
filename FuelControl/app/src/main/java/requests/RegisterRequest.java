package requests;

public class RegisterRequest {
    private String name;
    private String email;
    private String password;

    public RegisterRequest(String nombre, String correo, String password) {
        this.name = nombre;
        this.email = correo;
        this.password = password;
    }
}
