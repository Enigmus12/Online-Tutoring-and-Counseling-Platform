package UpLearn.eci.edu.co.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileStatusDTO {
    private boolean isComplete;
    private List<String> missingFields;
    private String currentRole;
}
