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
    @Id
    private String sub; // ID único de Cognito (sub claim)
    private String name;
    private String email;
    private List<String> role; // Lista de roles: STUDENT, TUTOR, etc.
    private String phoneNumber;
    
    // Campos adicionales del perfil (no relacionados con autenticación)
    private String idType;
    private String idNumber;

    // Perfil de estudiante
    private String educationLevel;

    // Perfil de tutor
    private String bio;
    private List<String> specializations;
    private List<String> credentials;
}
