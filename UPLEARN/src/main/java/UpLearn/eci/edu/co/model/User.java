package UpLearn.eci.edu.co.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import org.springframework.data.annotation.Id;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @JsonProperty("userId")
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
    private List<Specialization> specializations; // Cambiado a objeto con metadatos de verificación
    private List<String> credentials;
    private boolean isVerified = false;

    // Tarifa en tokens por hora (solo relevante si el usuario tiene rol TUTOR)
    private Integer tokensPerHour; // null si no configurado aún
}
