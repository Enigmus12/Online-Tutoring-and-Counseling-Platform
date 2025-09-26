package UpLearn.eci.edu.co.dto;

import lombok.Data;
import UpLearn.eci.edu.co.model.User;

@Data
public class AuthenticationResponseDTO {
    private boolean authenticated;
    private User user;
    private String token;
    private String message;

    public AuthenticationResponseDTO(boolean authenticated, User user, String token, String message) {
        this.authenticated = authenticated;
        this.user = user;
        this.token = token;
        this.message = message;
    }
}
