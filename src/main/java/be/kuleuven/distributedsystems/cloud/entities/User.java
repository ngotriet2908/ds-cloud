package be.kuleuven.distributedsystems.cloud.entities;

public class User {

    private String email;
    private String role;
    private String uid;

    public User(String email, String role, String uid) {
        this.email = email;
        this.role = role;
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getUid() {return uid;}

    public boolean isManager() {
        return this.role != null && this.role.equals("manager");
    }
}
