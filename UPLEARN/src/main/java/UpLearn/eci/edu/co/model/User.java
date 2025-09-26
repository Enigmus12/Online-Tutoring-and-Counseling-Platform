package UpLearn.eci.edu.co.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import org.springframework.data.annotation.Id;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String name;
    private String idType;
    private String idNumber;
    private String email;
    @Id
    private String userId;
    private String password;
    private String passwordConfirmation;
    private String phoneNumber;
    private String role; // STUDENT o TUTOR

    // Perfil de estudiante
    private String educationLevel;

    // Perfil de tutor
    private String bio;
    private List<String> specializations;
    private List<String> credentials;
}
