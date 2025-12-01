package UpLearn.eci.edu.co.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CognitoTokenDTO {
    private String token;
    private List<String> role; // Roles enviados desde el frontend
}