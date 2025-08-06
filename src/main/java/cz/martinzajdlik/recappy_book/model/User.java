package cz.martinzajdlik.recappy_book.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Uživatelské jméno nesmí být prázdné")
    @Column(unique = true)
    private String username;

    @NotBlank(message = "Heslo nesmí být prázdné")
    private String password;

    @NotBlank
    private String role; // např. "ADMIN" nebo "USER"

    @Column(unique = true)
    @NotBlank
    @Email
    private String email;


    public User() {
    }

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // Gettery a settery
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmail() {return email;}
    public void setEmail(String email) {this.email = email;}



}
