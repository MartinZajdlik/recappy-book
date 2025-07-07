package cz.martinzajdlik.recappy_book.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserRegistrationDTO {

    @NotBlank(message = "Uživatelské jméno nesmí být prázdné.")
    private String username;

    @NotBlank(message = "Heslo nesmí být prázdné.")
    @Size(min = 4, message = "Heslo musí mít alespoň 6 znaků.")
    private String password;

    @NotBlank(message = "E-mail nesmí být prázdný.")
    @Pattern(
            regexp = "^[\\w-.]+@[\\w-]+\\.[a-zA-Z]{2,6}$",
            message = "E-mail nemá správný formát."
    )

    private String email;

    // Gettery a settery

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
