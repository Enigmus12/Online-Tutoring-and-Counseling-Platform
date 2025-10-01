package UpLearn.eci.edu.co.dto;

import lombok.Data;

@Data
public class StudentProfileDTO {
    private String name;
    private String email;
    private String phoneNumber;
    
    // Campos adicionales del perfil
    private String idType;
    private String idNumber;
    private String profilePicture;
    private String nickname;

    // Campos específicos de estudiante
    private String educationLevel;
}