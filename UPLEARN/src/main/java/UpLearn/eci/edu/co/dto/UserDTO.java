package UpLearn.eci.edu.co.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private String sub; // ID de Cognito
    private String name;
    private String email;
    private String role; // STUDENT o TUTOR
    private String phoneNumber;
    
    // Campos adicionales del perfil
    private String idType;
    private String idNumber;

    // Campos específicos de STUDENT
    private String educationLevel;

    // Campos específicos de TUTOR
    private String bio;
    private List<String> specializations;
    private List<String> credentials;
}

