package UpLearn.eci.edu.co.dto;

import lombok.Data;
import java.util.List;

@Data
public class TutorProfileDTO {
    private String name;
    private String email;
    private String phoneNumber;
    
    // Campos adicionales del perfil
    private String idType;
    private String idNumber;
    private String profilePicture;
    private String nickname;

    // Campos específicos de tutor
    private String bio;
    private List<String> specializations;
    private List<String> credentials;
    private boolean isVerified = false;
    private Integer tokensPerHour; // Nueva tarifa de tokens por hora
}