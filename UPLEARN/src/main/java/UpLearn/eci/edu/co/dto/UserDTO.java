package UpLearn.eci.edu.co.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private String name;
    private String idType;
    private String idNumber;
    private String email;
    private String userId;
    private String password;
    private String passwordConfirmation;
    private String phoneNumber;
    private String role; // STUDENT o TUTOR

    // Campos específicos de STUDENT
    private String educationLevel;

    // Campos específicos de TUTOR
    private String bio;
    private List<String> specializations;
    private List<String> credentials;
}

