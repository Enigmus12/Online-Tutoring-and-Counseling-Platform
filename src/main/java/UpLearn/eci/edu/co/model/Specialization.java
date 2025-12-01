package UpLearn.eci.edu.co.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Representa una especialización del tutor con metadatos de verificación.
 * Puede ser declarada manualmente por el tutor o verificada automáticamente por IA.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Specialization {
    
    /**
     * Nombre de la especialización (ej: "Agropecuaria", "Matemáticas")
     */
    private String name;
    
    /**
     * Indica si la especialización fue verificada por IA mediante validación de documentos
     */
    private boolean verified;
    
    /**
     * Origen de la especialización: "AI_VALIDATION" o "MANUAL"
     */
    private String source;
    
    /**
     * Fecha y hora de verificación (solo para especializaciones verificadas)
     */
    private String verifiedAt;
    
    /**
     * URL del documento que validó esta especialización (solo para especializaciones verificadas)
     * Permite eliminar la especialización si se elimina el documento
     */
    private String documentUrl;
}
