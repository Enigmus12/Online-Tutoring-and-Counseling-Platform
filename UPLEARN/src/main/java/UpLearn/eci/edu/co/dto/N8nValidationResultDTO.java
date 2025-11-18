package UpLearn.eci.edu.co.dto;

import lombok.Data;

@Data
public class N8nValidationResultDTO {
    private boolean esDocumentoAcademico;
    private String motivoNoValido;
    private String tipoDocumento;
    private String nombrePersona;
    private String institucion;
    private String programaOEspecializacion;
    private String fechaEmision;
    private String nivel;
    private int confianza;
}
