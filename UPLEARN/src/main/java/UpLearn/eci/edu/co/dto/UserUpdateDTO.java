package UpLearn.eci.edu.co.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserUpdateDTO {
    private String name;
    private String email;
    private String phoneNumber;

    // Perfil de estudiante
    private String educationLevel;

    // Perfil de tutor
    private String bio;
    private List<String> specializations;
    private List<String> credentials;
}
