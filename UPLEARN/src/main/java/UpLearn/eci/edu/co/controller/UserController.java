package UpLearn.eci.edu.co.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import UpLearn.eci.edu.co.config.UserServiceException;
import UpLearn.eci.edu.co.dto.CognitoTokenDTO;
import UpLearn.eci.edu.co.dto.ProfileStatusDTO;
import UpLearn.eci.edu.co.dto.StudentProfileDTO;
import UpLearn.eci.edu.co.dto.TutorProfileDTO;
import UpLearn.eci.edu.co.model.User;
import org.springframework.web.multipart.MultipartFile;
import UpLearn.eci.edu.co.service.interfaces.UserService;
/**
 * Controlador para la gestión de usuarios
 */
@RestController
@RequestMapping("/Api-user")
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }
    /* Obtener todos los usuarios */
    @GetMapping("/users")
    public List<User> users(){
        return userService.getAllUsers();
    }
    /* Obtener un usuario por su sub */
    @GetMapping("/users/{sub}")
    public User user (@PathVariable String sub) throws UserServiceException {
        return userService.getUserBySub(sub);
    }
    /* Eliminar un usuario por su token */
    @DeleteMapping("/delete-profile")
    public String deleteUser(@RequestHeader("Authorization") String token) throws UserServiceException {
        userService.deleteUserByToken(token);
        return "Usuario eliminado correctamente";
    }

    /**
     * Procesar Usuario Cognito
     * Si roles=null → Frontend muestra selección de roles
     * Si roles=[...] → Usuario ya tiene roles asignados
     */
    @PostMapping("/process-cognito-user")
    public Map<String, Object> processCognitoUser(@RequestBody CognitoTokenDTO cognitoTokenDTO) throws UserServiceException {
        return userService.processCognitoUserComplete(cognitoTokenDTO);
    }

    /**
     * Guardar Roles de Usuario
     */
    @PostMapping("/save-user-role")
    public Map<String, Object> saveUserRole(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, List<String>> roleData
    ) throws UserServiceException {
        List<String> roles = roleData.get("roles");
        return userService.saveUserRoleComplete(token, roles);
    }

    /**
     * Añade un rol adicional a un usuario existente
     * Recibe el token de autenticación, el ID del usuario y el nuevo rol a añadir
     */
    @PostMapping("/add-role")
    public Map<String, Object> addRoleToUser(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> requestData
    ) throws UserServiceException {
        String userId = requestData.get("userId");
        String newRole = requestData.get("role");
        return userService.addRoleToUserComplete(token, userId, newRole);
    }

    /**
     * Obtiene los roles actuales del usuario autenticado
     * Solo requiere el token de autenticación en el header
     */
    @GetMapping("/my-roles")
    public Map<String, Object> getMyRoles(@RequestHeader("Authorization") String token) throws UserServiceException {
        return userService.getUserRolesComplete(token);
    }

    /**
     * Verifica el estado de completitud del perfil del usuario autenticado
     * Retorna si el perfil está completo, los campos faltantes y el rol actual
     */
    @GetMapping("/profile-status")
    public ProfileStatusDTO getProfileStatus(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String role
    ) throws UserServiceException {
        return userService.getProfileStatus(token, role);
    }

    // 
    // ENDPOINTS ESPECÍFICOS PARA ESTUDIANTE
    // 

    /**
     * Obtiene el perfil específico de estudiante
     * Solo funciona si el usuario tiene rol STUDENT
     */
    @GetMapping("/student/profile")
    public StudentProfileDTO getStudentProfile(@RequestHeader("Authorization") String token) throws UserServiceException {
        return userService.getStudentProfile(token);
    }

    /**
     * Actualiza el perfil específico de estudiante
     * Solo permite editar campos relacionados con el rol de estudiante
     */
    @PutMapping("/student/profile")
    public StudentProfileDTO updateStudentProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody StudentProfileDTO studentDTO
    ) throws UserServiceException {
        return userService.updateStudentProfile(token, studentDTO);
    }

    /**
     * Elimina el rol de estudiante del usuario
     * Si es el único rol, elimina completamente el usuario
     */
    @DeleteMapping("/student/profile")
    public Map<String, Object> removeStudentRole(@RequestHeader("Authorization") String token) throws UserServiceException {
        return userService.removeStudentRole(token);
    }

    // 
    // ENDPOINTS ESPECÍFICOS PARA TUTOR
    // 

    /**
     * Obtiene el perfil específico de tutor
     * Solo funciona si el usuario tiene rol TUTOR
     */
    @GetMapping("/tutor/profile")
    public TutorProfileDTO getTutorProfile(@RequestHeader("Authorization") String token) throws UserServiceException {
        return userService.getTutorProfile(token);
    }

    /**
     * Obtiene la tarifa de tokens por hora del tutor autenticado
     */
    @GetMapping("/tutor/tokens-rate")
    public Map<String, Object> getTutorTokensRate(@RequestHeader("Authorization") String token) throws UserServiceException {
        Integer rate = userService.getTutorTokensPerHour(token);
        return Map.of("tokensPerHour", rate);
    }

    /**
     * Obtiene la tarifa de tokens por hora de un tutor público por sub o id (sin token)
     */
    @GetMapping("/tutor/tokens-rate-by-sub")
    public Map<String, Object> getTutorTokensRateBySub(
            @RequestParam(value = "sub", required = false) String sub,
            @RequestParam(value = "id", required = false) String id) throws UserServiceException {
        String key = (sub != null && !sub.isBlank()) ? sub : id;
        if (key == null || key.isBlank()) {
            throw new UserServiceException("Debe proporcionar 'sub' o 'id'");
        }
        Integer rate = userService.getTutorTokensPerHourBySub(key);
        return Map.of("tokensPerHour", rate);
    }

    /**
     * Actualiza el perfil específico de tutor
     * Solo permite editar campos relacionados con el rol de tutor
     */
    @PutMapping("/tutor/profile")
    public TutorProfileDTO updateTutorProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody TutorProfileDTO tutorDTO
    ) throws UserServiceException {
        return userService.updateTutorProfile(token, tutorDTO);
    }

    /**
     * Elimina el rol de tutor del usuario
     * Si es el único rol, elimina completamente el usuario
     */
    @DeleteMapping("/tutor/profile")
    public Map<String, Object> removeTutorRole(@RequestHeader("Authorization") String token) throws UserServiceException {
        return userService.removeTutorRole(token);
    }


    /**
     * Sube archivos de credenciales para el tutor autenticado.
     * Automáticamente: sube a Azure, valida con n8n, y guarda en BD solo documentos académicos válidos.
     * Recibe multipart/form-data con la clave 'files'.
     */
    @PostMapping(value = "/tutor/credentials", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> uploadTutorCredentials(
            @RequestHeader("Authorization") String token,
            @RequestParam("files") List<MultipartFile> files
    ) throws UserServiceException {
        Map<String, Object> result = userService.uploadAndValidateTutorCredentials(token, files);
        return ResponseEntity.ok(result);
    }

    /**
     * Elimina URLs de credenciales del tutor autenticado. Si al finalizar no quedan
     * credenciales, el usuario queda no verificado y se devuelve en la respuesta.
     * Espera un JSON como: { "urls": ["https://.../file1.pdf", "https://.../file2.pdf"] }
     */
    @DeleteMapping("/tutor/credentials")
    public ResponseEntity<Map<String, Object>> deleteTutorCredentials(
            @RequestHeader("Authorization") String token,
            @RequestBody(required = false) Map<String, List<String>> body
    ) throws UserServiceException {
        List<String> urls = body != null ? body.get("urls") : null;
        Map<String, Object> result = userService.deleteTutorCredentials(token, urls);
        return ResponseEntity.ok(result);
    }

    /**
     * Obtiene el perfil público de un usuario dado su sub
     * @param sub Sub del usuario cuyo perfil se desea obtener
     * @return Mapa con la información pública del usuario
     * @throws UserServiceException en caso de error al obtener el perfil
     */
    @GetMapping("/public/profile")
    public ResponseEntity<Map<String, Object>> getPublicProfile(
            @RequestParam(value = "sub", required = false) String sub,
            @RequestParam(value = "id", required = false) String id
    ) throws UserServiceException {
        String key = (sub != null && !sub.isBlank()) ? sub : id;
        if (key == null || key.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debe proporcionar 'sub' o 'id'"));
        }
        return ResponseEntity.ok(userService.getPublicProfileBySub(key));
    }

}
