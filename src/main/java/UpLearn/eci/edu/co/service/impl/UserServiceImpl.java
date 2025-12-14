package UpLearn.eci.edu.co.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import UpLearn.eci.edu.co.config.UserServiceException;
import UpLearn.eci.edu.co.dto.CognitoTokenDTO;
import UpLearn.eci.edu.co.dto.ProfileStatusDTO;
import UpLearn.eci.edu.co.dto.StudentProfileDTO;
import UpLearn.eci.edu.co.dto.TutorProfileDTO;
import UpLearn.eci.edu.co.dto.N8nValidationResultDTO;
import UpLearn.eci.edu.co.model.Specialization;
import UpLearn.eci.edu.co.model.User;
import UpLearn.eci.edu.co.service.interfaces.UserRepository;
import UpLearn.eci.edu.co.service.interfaces.UserService;
import UpLearn.eci.edu.co.util.CognitoTokenDecoder;
import UpLearn.eci.edu.co.util.CognitoTokenDecoder.CognitoUserInfo;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserServiceImpl implements UserService {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_DELETED_KEY = "userDeleted";
    private static final String EMAIL_KEY = "email";
    private static final String ROLES_KEY = "roles";
    private static final String MESSAGE_KEY = "message";
    private static final String ROLE_STUDENT = "STUDENT";
    private static final String ROLE_TUTOR = "TUTOR";
    private static final String UPLOADED_KEY = "uploaded";
    private final UserRepository userRepository;
    private final CognitoTokenDecoder cognitoTokenDecoder;
    private final AzureBlobStorageService azureBlobStorageService;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Value("${n8n.webhook.url}")
    private String n8nWebhookUrl;

    public UserServiceImpl(
            UserRepository userRepository,
            CognitoTokenDecoder cognitoTokenDecoder,
            AzureBlobStorageService azureBlobStorageService) {
        this.userRepository = userRepository;
        this.cognitoTokenDecoder = cognitoTokenDecoder;
        this.azureBlobStorageService = azureBlobStorageService;
    }

    @Override
    public void deleteUserByToken(String token) throws UserServiceException {
        try {
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace(BEARER_PREFIX, ""));
            String sub = userInfo.getSub();

            User user = userRepository.findBySub(sub);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }

            userRepository.deleteBySub(sub);
        } catch (Exception e) {
            throw new UserServiceException("Error al eliminar usuario: " + e.getMessage());
        }
    }

    @Override
    public User getUserBySub(String sub) throws UserServiceException {
        User user = userRepository.findBySub(sub);
        if (user == null) {
            throw new UserServiceException("Usuario no encontrado con sub: " + sub);
        }
        return user;
    }

    @Override
    public User processUserFromCognito(CognitoTokenDTO cognitoTokenDTO) throws UserServiceException {
        try {
            // Validar que el token no esté vacío
            if (cognitoTokenDTO.getToken() == null || cognitoTokenDTO.getToken().trim().isEmpty()) {
                throw new UserServiceException("Token no puede estar vacío");
            }

            // Validar que el token sea válido
            if (!cognitoTokenDecoder.isTokenValid(cognitoTokenDTO.getToken())) {
                throw new UserServiceException("Token expirado o inválido");
            }

            // Extraer información del usuario desde el token
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(cognitoTokenDTO.getToken());

            // Verificar si el usuario ya existe por sub
            if (userRepository.existsBySub(userInfo.getSub())) {
                // Si existe, actualizar los roles SOLO si se proporcionan
                User existingUser = userRepository.findBySub(userInfo.getSub());
                if (cognitoTokenDTO.getRole() != null && !cognitoTokenDTO.getRole().isEmpty()) {
                    existingUser.setRole(cognitoTokenDTO.getRole());
                    return userRepository.save(existingUser);
                }
                return existingUser;
            }

            // Si no existe, crear nuevo usuario
            // Para usuarios nuevos, los roles son opcionales (se asignarán después)
            User newUser = new User();
            newUser.setSub(userInfo.getSub());
            newUser.setName(userInfo.getName());
            newUser.setEmail(userInfo.getEmail());
            newUser.setPhoneNumber(userInfo.getPhoneNumber());

            // Asignar roles si se proporcionan, sino dejar vacío para que el frontend los
            // asigne
            if (cognitoTokenDTO.getRole() != null && !cognitoTokenDTO.getRole().isEmpty()) {
                newUser.setRole(cognitoTokenDTO.getRole());
            } else {
                newUser.setRole(new ArrayList<>()); // Lista vacía para usuarios nuevos sin rol
            }

            // Guardar el nuevo usuario en la base de datos
            return userRepository.save(newUser);

        } catch (Exception e) {
            throw new UserServiceException("Error al procesar usuario desde Cognito: " + e.getMessage());
        }
    }

    /**
     * Procesa completamente un usuario de Cognito y retorna la respuesta del API
     * Este método encapsula toda la lógica de negocio para el endpoint
     * process-cognito-user
     */
    @Override
    public Map<String, Object> processCognitoUserComplete(CognitoTokenDTO cognitoTokenDTO) throws UserServiceException {
        // 1. Verificar si el usuario ya existe
        boolean isNewUser = !existsUserBySub(cognitoTokenDTO);

        // 2. Procesar el usuario (crear o actualizar)
        User user = processUserFromCognito(cognitoTokenDTO);

        // 3. Construir la respuesta del API
        Map<String, Object> response = new HashMap<>();

        // Crear objeto user con los campos requeridos por el frontend
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", user.getSub()); // ID único de Cognito
        userResponse.put(EMAIL_KEY, user.getEmail()); // Email del usuario
        // Lógica de roles: null indica que el frontend debe mostrar selección
        if (user.getRole() == null || user.getRole().isEmpty()) {
            userResponse.put(ROLES_KEY, null); // Trigger para selección de roles
        } else {
            userResponse.put(ROLES_KEY, user.getRole()); // Roles ya asignados
        }

        response.put("user", userResponse);
        response.put("isNewUser", isNewUser); // Flag para el frontend

        return response;
    }

    /**
     * Guarda roles de usuario y retorna la respuesta completa del API
     * Este método encapsula toda la lógica de negocio para el endpoint
     * save-user-role
     */
    @Override
    public Map<String, Object> saveUserRoleComplete(String token, List<String> roles) throws UserServiceException {
        // 1. Actualizar roles del usuario
        User user = updateUserRoles(token, roles);
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getSub());
        response.put(EMAIL_KEY, user.getEmail());
        response.put(ROLES_KEY, user.getRole()); // Roles recién guardados

        return response;
    }

    @Override
    public boolean existsUserBySub(CognitoTokenDTO cognitoTokenDTO) throws UserServiceException {
        try {
            // Validar que el token no esté vacío
            if (cognitoTokenDTO.getToken() == null || cognitoTokenDTO.getToken().trim().isEmpty()) {
                throw new UserServiceException("Token no puede estar vacío");
            }

            // Validar que el token sea válido
            if (!cognitoTokenDecoder.isTokenValid(cognitoTokenDTO.getToken())) {
                throw new UserServiceException("Token expirado o inválido");
            }

            // Extraer información del usuario desde el token
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(cognitoTokenDTO.getToken());

            // Verificar si el usuario ya existe por sub
            return userRepository.existsBySub(userInfo.getSub());

        } catch (Exception e) {
            throw new UserServiceException("Error al verificar existencia del usuario: " + e.getMessage());
        }
    }

    @Override
    public User updateUserRoles(String token, List<String> roles) throws UserServiceException {
        try {
            // Validar que se hayan proporcionado roles
            if (roles == null || roles.isEmpty()) {
                throw new UserServiceException("Debe proporcionar al menos un rol");
            }

            // Extraer información del token de Cognito
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace(BEARER_PREFIX, ""));
            String sub = userInfo.getSub();

            User user = userRepository.findBySub(sub);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }

            // Actualizar los roles del usuario
            user.setRole(roles);

            return userRepository.save(user);
        } catch (Exception e) {
            throw new UserServiceException("Error al actualizar roles del usuario: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> addRoleToUserComplete(String token, String userId, String newRole)
            throws UserServiceException {
        try {
            // Validar que el usuario esté autenticado (verificamos que el token sea válido)
            cognitoTokenDecoder.extractUserInfo(token.replace(BEARER_PREFIX, ""));
            cognitoTokenDecoder.extractUserInfo(token.replace(BEARER_PREFIX, ""));

            // Buscar el usuario por ID
            User targetUser = userRepository.findBySub(userId);
            if (targetUser == null) {
                throw new UserServiceException("Usuario no encontrado con ID: " + userId);
            }

            // Obtener roles actuales o crear lista vacía si no tiene
            List<String> currentRoles = targetUser.getRole();
            if (currentRoles == null) {
                currentRoles = new ArrayList<>();
            }

            // Verificar si el rol ya existe
            if (currentRoles.contains(newRole)) {
                throw new UserServiceException("El usuario ya tiene el rol: " + newRole);
            }

            // Añadir el nuevo rol
            currentRoles.add(newRole);
            targetUser.setRole(currentRoles);

            // Guardar los cambios
            User updatedUser = userRepository.save(targetUser);

            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedUser.getSub());
            response.put(EMAIL_KEY, updatedUser.getEmail());
            response.put(ROLES_KEY, updatedUser.getRole());
            response.put(MESSAGE_KEY, "Rol '" + newRole + "' añadido exitosamente");

            return response;
        } catch (UserServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new UserServiceException("Error al añadir rol al usuario: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getUserRolesComplete(String token) throws UserServiceException {
        try {
            // Extraer información del usuario desde el token
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace(BEARER_PREFIX, ""));
            String sub = userInfo.getSub();

            // Buscar el usuario en la base de datos
            User user = userRepository.findBySub(sub);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getSub());
            response.put(EMAIL_KEY, user.getEmail());
            response.put("name", user.getName());
            response.put(ROLES_KEY, user.getRole() != null ? user.getRole() : new ArrayList<>());
            response.put("hasRoles", user.getRole() != null && !user.getRole().isEmpty());

            return response;

        } catch (UserServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new UserServiceException("Error al obtener roles del usuario: " + e.getMessage());
        }
    }

    //
    // MÉTODOS ESPECÍFICOS PARA PERFILES DE ESTUDIANTE
    //

    @Override
    public StudentProfileDTO getStudentProfile(String token) throws UserServiceException {
        try {
            // Extraer información del token de Cognito
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace(BEARER_PREFIX, ""));
            String sub = userInfo.getSub();

            User user = userRepository.findBySub(sub);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }
            // Verificar que el usuario tenga el rol de estudiante (case-insensitive)
            boolean hasStudentRole = false;
            if (user.getRole() != null) {
                for (String role : user.getRole()) {
                    if (ROLE_STUDENT.equalsIgnoreCase(role)) {
                        hasStudentRole = true;
                        break;
                    }
                }
            }

            if (!hasStudentRole) {
                throw new UserServiceException("El usuario no tiene el rol de estudiante");
            }

            // Crear DTO específico para estudiante
            StudentProfileDTO dto = new StudentProfileDTO();
            dto.setName(user.getName());
            dto.setEmail(user.getEmail());
            dto.setPhoneNumber(user.getPhoneNumber());
            dto.setIdType(user.getIdType());
            dto.setIdNumber(user.getIdNumber());
            dto.setEducationLevel(user.getEducationLevel());

            return dto;
        } catch (UserServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new UserServiceException("Error al obtener perfil de estudiante: " + e.getMessage());
        }
    }

    @Override
    public StudentProfileDTO updateStudentProfile(String token, StudentProfileDTO studentDTO)
            throws UserServiceException {
        try {
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace(BEARER_PREFIX, ""));
            String sub = userInfo.getSub();

            User user = userRepository.findBySub(sub);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }
            if (!hasRole(user, ROLE_STUDENT)) {
                throw new UserServiceException("El usuario no tiene el rol de estudiante");
            }

            updateStudentFields(user, studentDTO);

            User savedUser = userRepository.save(user);

            return buildStudentProfileDTO(savedUser);
        } catch (UserServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new UserServiceException("Error al actualizar perfil de estudiante: " + e.getMessage());
        }
    }

    private boolean hasRole(User user, String roleName) {
        if (user.getRole() == null)
            return false;
        for (String role : user.getRole()) {
            if (roleName.equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }

    private void updateStudentFields(User user, StudentProfileDTO studentDTO) {
        if (studentDTO.getName() != null)
            user.setName(studentDTO.getName());
        if (studentDTO.getEmail() != null)
            user.setEmail(studentDTO.getEmail());
        if (studentDTO.getPhoneNumber() != null)
            user.setPhoneNumber(studentDTO.getPhoneNumber());
        if (studentDTO.getIdType() != null)
            user.setIdType(studentDTO.getIdType());
        if (studentDTO.getIdNumber() != null)
            user.setIdNumber(studentDTO.getIdNumber());
        if (studentDTO.getEducationLevel() != null)
            user.setEducationLevel(studentDTO.getEducationLevel());
    }

    private StudentProfileDTO buildStudentProfileDTO(User user) {
        StudentProfileDTO dto = new StudentProfileDTO();
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setIdType(user.getIdType());
        dto.setIdNumber(user.getIdNumber());
        dto.setEducationLevel(user.getEducationLevel());
        return dto;
    }

    @Override
    public Map<String, Object> removeStudentRole(String token) throws UserServiceException {
        try {
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace(BEARER_PREFIX, ""));
            String sub = userInfo.getSub();
            User user = userRepository.findBySub(sub);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }

            if (!hasRole(user, ROLE_STUDENT)) {
                throw new UserServiceException("El usuario no tiene el rol de estudiante");
            }
            List<String> updatedRoles = removeRoleFromUser(user, ROLE_STUDENT);

            // Limpiar campos específicos de estudiante
            user.setEducationLevel(null);
            user.setRole(updatedRoles);

            if (updatedRoles.isEmpty()) {
                userRepository.deleteBySub(sub);
                return buildRemoveStudentRoleResponse(true, null);
            } else {
                User savedUser = userRepository.save(user);
                return buildRemoveStudentRoleResponse(false, savedUser.getRole());
            }
        } catch (UserServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new UserServiceException("Error al eliminar rol de estudiante: " + e.getMessage());
        }
    }

    private List<String> removeRoleFromUser(User user, String roleToRemove) {
        List<String> updatedRoles = new ArrayList<>();
        if (user.getRole() != null) {
            for (String role : user.getRole()) {
                if (!roleToRemove.equalsIgnoreCase(role)) {
                    updatedRoles.add(role);
                }
            }
        }
        return updatedRoles;
    }

    private Map<String, Object> buildRemoveStudentRoleResponse(boolean userDeleted, List<String> remainingRoles) {
        Map<String, Object> response = new HashMap<>();
        response.put(USER_DELETED_KEY, userDeleted);
        if (!userDeleted) {
            response.put("remainingRoles", remainingRoles);
        }
        return response;
    }

    // =====================================================
    // MÉTODOS ESPECÍFICOS PARA PERFILES DE TUTOR
    // =====================================================

    @Override
    public TutorProfileDTO getTutorProfile(String token) throws UserServiceException {
        try {
            // Extraer información del token de Cognito
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace(BEARER_PREFIX, ""));
            String sub = userInfo.getSub();

            User user = userRepository.findBySub(sub);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }
            // Verificar que el usuario tenga el rol de tutor (case-insensitive)
            boolean hasTutorRole = false;
            if (user.getRole() != null) {
                for (String role : user.getRole()) {
                    if (ROLE_TUTOR.equalsIgnoreCase(role)) {
                        hasTutorRole = true;
                        break;
                    }
                }
            }

            if (!hasTutorRole) {
                throw new UserServiceException("El usuario no tiene el rol de tutor");
            }

            // Crear DTO específico para tutor
            TutorProfileDTO dto = new TutorProfileDTO();
            dto.setName(user.getName());
            dto.setEmail(user.getEmail());
            dto.setPhoneNumber(user.getPhoneNumber());
            dto.setIdType(user.getIdType());
            dto.setIdNumber(user.getIdNumber());
            dto.setBio(user.getBio());
            dto.setSpecializations(user.getSpecializations());
            dto.setCredentials(user.getCredentials());
            dto.setVerified(user.isVerified());
            dto.setTokensPerHour(user.getTokensPerHour());

            return dto;
        } catch (UserServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new UserServiceException("Error al obtener perfil de tutor: " + e.getMessage());
        }
    }

    @Override
    public TutorProfileDTO updateTutorProfile(String token, TutorProfileDTO tutorDTO) throws UserServiceException {
        try {
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace(BEARER_PREFIX, ""));
            String sub = userInfo.getSub();
            User user = userRepository.findBySub(sub);
            validateTutorUser(user);

            updateTutorFields(user, tutorDTO);
            handleTutorSpecializations(user, tutorDTO);
            if (tutorDTO.getTokensPerHour() != null)
                user.setTokensPerHour(tutorDTO.getTokensPerHour());

            User savedUser = userRepository.save(user);
            return buildTutorProfileDTO(savedUser);
        } catch (UserServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new UserServiceException("Error al actualizar perfil de tutor: " + e.getMessage());
        }
    }

    private void validateTutorUser(User user) throws UserServiceException {
        if (user == null) {
            throw new UserServiceException("Usuario no encontrado");
        }
        boolean hasTutorRole = user.getRole() != null && user.getRole().stream().anyMatch(ROLE_TUTOR::equalsIgnoreCase);
        if (!hasTutorRole) {
            throw new UserServiceException("El usuario no tiene el rol de tutor");
        }
    }

    private void updateTutorFields(User user, TutorProfileDTO tutorDTO) {
        if (tutorDTO.getName() != null)
            user.setName(tutorDTO.getName());
        if (tutorDTO.getEmail() != null)
            user.setEmail(tutorDTO.getEmail());
        if (tutorDTO.getPhoneNumber() != null)
            user.setPhoneNumber(tutorDTO.getPhoneNumber());
        if (tutorDTO.getIdType() != null)
            user.setIdType(tutorDTO.getIdType());
        if (tutorDTO.getIdNumber() != null)
            user.setIdNumber(tutorDTO.getIdNumber());
        if (tutorDTO.getBio() != null)
            user.setBio(tutorDTO.getBio());
    }

    private void handleTutorSpecializations(User user, TutorProfileDTO tutorDTO) {
        if (tutorDTO.getSpecializations() == null)
            return;
        List<Specialization> newSpecializations = new ArrayList<>();
        if (user.getSpecializations() != null) {
            for (Specialization existing : user.getSpecializations()) {
                if (existing.isVerified()) {
                    newSpecializations.add(existing);
                }
            }
        }
        for (Specialization incomingSpec : tutorDTO.getSpecializations()) {
            if (!incomingSpec.isVerified()) {
                boolean exists = newSpecializations.stream()
                        .anyMatch(s -> s.getName().equalsIgnoreCase(incomingSpec.getName()));
                if (!exists) {
                    incomingSpec.setVerified(false);
                    incomingSpec.setSource("MANUAL");
                    incomingSpec.setVerifiedAt(null);
                    incomingSpec.setDocumentUrl(null);
                    newSpecializations.add(incomingSpec);
                }
            }
        }
        user.setSpecializations(newSpecializations);
    }

    private TutorProfileDTO buildTutorProfileDTO(User user) {
        TutorProfileDTO dto = new TutorProfileDTO();
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setIdType(user.getIdType());
        dto.setIdNumber(user.getIdNumber());
        dto.setBio(user.getBio());
        dto.setSpecializations(user.getSpecializations());
        dto.setCredentials(user.getCredentials());
        dto.setVerified(user.isVerified());
        dto.setTokensPerHour(user.getTokensPerHour());
        return dto;
    }

    @Override
    public Map<String, Object> removeTutorRole(String token) throws UserServiceException {
        try {
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace(BEARER_PREFIX, ""));
            String sub = userInfo.getSub();
            User user = userRepository.findBySub(sub);
            validateTutorUser(user);

            List<String> updatedRoles = removeRoleFromUser(user, ROLE_TUTOR);

            clearTutorFields(user);
            user.setRole(updatedRoles);

            if (updatedRoles.isEmpty()) {
                userRepository.deleteBySub(sub);
                return buildRemoveTutorRoleResponse(true, null);
            } else {
                User savedUser = userRepository.save(user);
                return buildRemoveTutorRoleResponse(false, savedUser.getRole());
            }
        } catch (UserServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new UserServiceException("Error al eliminar rol de tutor: " + e.getMessage());
        }
    }

    private void clearTutorFields(User user) {
        user.setBio(null);
        user.setSpecializations(null);
        user.setCredentials(null);
    }

    private Map<String, Object> buildRemoveTutorRoleResponse(boolean userDeleted, List<String> remainingRoles) {
        Map<String, Object> response = new HashMap<>();
        if (userDeleted) {
            response.put(MESSAGE_KEY, "Perfil de tutor eliminado. Usuario completamente eliminado.");
            response.put(USER_DELETED_KEY, true);
        } else {
            response.put(MESSAGE_KEY, "Rol de tutor eliminado exitosamente");
            response.put(USER_DELETED_KEY, false);
            response.put("remainingRoles", remainingRoles);
        }
        return response;
    }

    // =====================================================
    // NUEVO MÉTODO PARA PERFIL PÚBLICO POR SUB
    @Override
    public Map<String, Object> getPublicProfileBySub(String sub) throws UserServiceException {
        if (sub == null || sub.isBlank())
            throw new UserServiceException("El parámetro 'sub' es requerido");
        User user = getUserBySub(sub);
        Map<String, Object> out = new HashMap<>();
        out.put("sub", user.getSub());
        out.put("name", user.getName());
        out.put(EMAIL_KEY, user.getEmail());
        boolean isTutor = user.getRole() != null && user.getRole().stream().anyMatch(ROLE_TUTOR::equalsIgnoreCase);
        if (isTutor) {
            out.put("specializations", user.getSpecializations());
            out.put("credentials", user.getCredentials());
            out.put("tokensPerHour", user.getTokensPerHour());
        }
        return out;
    }

    // =====================================================
    // MÉTODO PARA VERIFICAR ESTADO DE COMPLETITUD DEL PERFIL
    @Override
    public ProfileStatusDTO getProfileStatus(String token, String role) throws UserServiceException {
        try {
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace(BEARER_PREFIX, ""));
            String sub = userInfo.getSub();
            User user = userRepository.findBySub(sub);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }

            String currentRole = extractCurrentRole(user, role);
            ProfileStatusDTO profileStatus = new ProfileStatusDTO();
            profileStatus.setCurrentRole(currentRole);

            if (currentRole == null) {
                profileStatus.setComplete(false);
                profileStatus.setMissingFields(List.of("role"));
                return profileStatus;
            }

            List<String> missingFields = getMissingFieldsForRole(user, currentRole);
            profileStatus.setComplete(missingFields.isEmpty());
            profileStatus.setMissingFields(missingFields.isEmpty() ? null : missingFields);

            return profileStatus;
        } catch (UserServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new UserServiceException("Error al verificar estado del perfil: " + e.getMessage());
        }
    }

    private String extractCurrentRole(User user, String role) throws UserServiceException {
        if (role != null && !role.trim().isEmpty()) {
            boolean hasRole = user.getRole() != null &&
                    user.getRole().stream().anyMatch(r -> r.equalsIgnoreCase(role));
            if (!hasRole) {
                throw new UserServiceException("El usuario no tiene el rol: " + role);
            }
            return role.toUpperCase();
        } else if (user.getRole() != null && !user.getRole().isEmpty()) {
            return user.getRole().get(0).toUpperCase();
        }
        return null;
    }

    private List<String> getMissingFieldsForRole(User user, String currentRole) {
        List<String> missingFields = new ArrayList<>();
        if (ROLE_STUDENT.equalsIgnoreCase(currentRole)) {
            addIfEmpty(user.getName(), "name", missingFields);
            addIfEmpty(user.getEmail(), EMAIL_KEY, missingFields);
            addIfEmpty(user.getPhoneNumber(), "phoneNumber", missingFields);
            addIfEmpty(user.getEducationLevel(), "educationLevel", missingFields);
        } else if (ROLE_TUTOR.equalsIgnoreCase(currentRole)) {
            addIfEmpty(user.getName(), "name", missingFields);
            addIfEmpty(user.getEmail(), EMAIL_KEY, missingFields);
            addIfEmpty(user.getPhoneNumber(), "phoneNumber", missingFields);
            addIfEmpty(user.getBio(), "bio", missingFields);
            if (user.getSpecializations() == null || user.getSpecializations().isEmpty()) {
                missingFields.add("specializations");
            }
            if (user.getCredentials() == null || user.getCredentials().isEmpty()) {
                missingFields.add("credentials");
            }
        }
        return missingFields;
    }

    private void addIfEmpty(String value, String fieldName, List<String> missingFields) {
        if (value == null || value.trim().isEmpty()) {
            missingFields.add(fieldName);
        }
    }

    // =====================================================
    // SUBIDA, VALIDACIÓN Y GUARDADO AUTOMÁTICO DE CREDENCIALES
    // =====================================================
    @Override
    public Map<String, Object> uploadAndValidateTutorCredentials(String token, List<MultipartFile> files)
            throws UserServiceException {
        try {
            validateFilesInput(files);
            User user = getTutorUserFromToken(token);
            UploadValidationResult result = processFilesForTutor(user, files);
            updateTutorCredentialsAndVerification(user, result.validUrls);
            return buildUploadValidationResponse(files.size(), result, user.isVerified());
        } catch (UserServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new UserServiceException("Error al procesar credenciales: " + e.getMessage());
        }
    }

    private void validateFilesInput(List<MultipartFile> files) throws UserServiceException {
        if (files == null || files.isEmpty()) {
            throw new UserServiceException("Debe proporcionar al menos un archivo");
        }
    }

    private User getTutorUserFromToken(String token) throws UserServiceException {
        return getAndValidateTutorUser(token);
    }

    private static final String SAVED_KEY = "saved";
    private static final String STATUS_KEY = "status";
    private static final String TUTOR_VERIFIED_KEY = "tutorVerified";

    private static class UploadValidationResult {
        List<Map<String, Object>> results = new ArrayList<>();
        List<String> validUrls = new ArrayList<>();
        int uploaded = 0;
        int validated = 0;
        int rejected = 0;
    }

    private UploadValidationResult processFilesForTutor(User user, List<MultipartFile> files) {
        UploadValidationResult result = new UploadValidationResult();
        for (MultipartFile file : files) {
            Map<String, Object> fileResult = processSingleFile(user, file, result);
            result.results.add(fileResult);
        }
        return result;
    }

    private Map<String, Object> processSingleFile(User user, MultipartFile file, UploadValidationResult result) {
        Map<String, Object> fileResult = new HashMap<>();
        fileResult.put("fileName", file.getOriginalFilename());
        try {
            String fileUrl = uploadFileToAzure(user.getSub(), file);
            result.uploaded++;
            fileResult.put("uploadedUrl", fileUrl);
            fileResult.put(UPLOADED_KEY, true);

            N8nValidationResultDTO validationResult = validateFileWithN8n(fileUrl);
            fileResult.put("validation", validationResult);

            if (validationResult.isValid()) {
                fileResult.put(SAVED_KEY, true);
                fileResult.put(STATUS_KEY, "accepted");

                result.validated++;
                result.validUrls.add(fileUrl);

                addSpecializationIfNeeded(user, validationResult, fileUrl, fileResult);
            } else {
                result.rejected++;
                fileResult.put(SAVED_KEY, false);
                fileResult.put(STATUS_KEY, "rejected");
                fileResult.put("reason", validationResult.getMotivoNoValido());
            }

        } catch (Exception e) {
            fileResult.put(UPLOADED_KEY, false);
            fileResult.put(SAVED_KEY, false);
            fileResult.put(STATUS_KEY, "error");
            fileResult.put("error", e.getMessage());
            result.rejected++;
        }
        return fileResult;
    }

    private String uploadFileToAzure(String sub, MultipartFile file) throws UserServiceException {
        try {
            List<String> urls = azureBlobStorageService.uploadFiles(sub, List.of(file));
            return urls.get(0);
        } catch (Exception e) {
            throw new UserServiceException("Error uploading file to Azure: " + e.getMessage());
        }
    }

    private N8nValidationResultDTO validateFileWithN8n(String fileUrl) throws UserServiceException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("fileUrl", fileUrl);
        HttpEntity<Map<String, String>> req = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<N8nValidationResultDTO> resp = restTemplate.postForEntity(n8nWebhookUrl, req,
                    N8nValidationResultDTO.class);
            N8nValidationResultDTO validationResult = resp.getBody();
            if (validationResult == null)
                throw new UserServiceException("Respuesta inválida desde n8n");
            return validationResult;
        } catch (Exception e) {
            try {
                ResponseEntity<N8nValidationResultDTO[]> resp = restTemplate.postForEntity(n8nWebhookUrl, req,
                        N8nValidationResultDTO[].class);
                N8nValidationResultDTO[] resultsArray = resp.getBody();
                if (resultsArray != null && resultsArray.length > 0) {
                    return resultsArray[0];
                }
            } catch (Exception ex) {
                throw new UserServiceException("Respuesta inválida desde n8n: " + ex.getMessage());
            }
            throw new UserServiceException("Respuesta inválida desde n8n: " + e.getMessage());
        }
    }

    private void addSpecializationIfNeeded(User user, N8nValidationResultDTO validationResult, String fileUrl,
            Map<String, Object> fileResult) {
        String especialidadName = validationResult.getEspecialidad();
        if (especialidadName != null && !especialidadName.isBlank()) {
            if (user.getSpecializations() == null) {
                user.setSpecializations(new ArrayList<>());
            }
            boolean exists = user.getSpecializations().stream()
                    .anyMatch(s -> s.getName().equalsIgnoreCase(especialidadName));
            if (!exists) {
                Specialization newSpec = new Specialization();
                newSpec.setName(especialidadName);
                newSpec.setVerified(true);
                newSpec.setSource("AI_VALIDATION");
                newSpec.setVerifiedAt(Instant.now().toString());
                newSpec.setDocumentUrl(fileUrl);
                user.getSpecializations().add(newSpec);
                fileResult.put("addedSpecialization", especialidadName);
            }
        }
    }

    private void updateTutorCredentialsAndVerification(User user, List<String> validUrls) {
        if (!validUrls.isEmpty()) {
            if (user.getCredentials() == null) {
                user.setCredentials(new ArrayList<>());
            }
            for (String url : validUrls) {
                if (!user.getCredentials().contains(url)) {
                    user.getCredentials().add(url);
                }
            }
            if (!user.isVerified()) {
                user.setVerified(true);
            }
            userRepository.save(user);
        }
    }

    private Map<String, Object> buildUploadValidationResponse(
            int totalFiles, UploadValidationResult result, boolean tutorVerified) {

        Map<String, Object> response = new HashMap<>();
        response.put("totalFiles", totalFiles);
        response.put(UPLOADED_KEY, result.uploaded);
        response.put("validated", result.validated);
        response.put("rejected", result.rejected);
        response.put("details", result.results);
        response.put(TUTOR_VERIFIED_KEY, tutorVerified);

        response.put("savedCredentials", new ArrayList<>(result.validUrls));

        return response;
    }

    // =====================================================
    // ACTUALIZAR ESTADO DE VERIFICACIÓN DEL TUTOR
    // =====================================================
    @Override
    public Map<String, Object> deleteTutorCredentials(String token, List<String> urls) throws UserServiceException {
        try {
            User user = getAndValidateTutorUser(token);
            List<String> currentCredentials = user.getCredentials();
            if (currentCredentials == null || currentCredentials.isEmpty()) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("removedCount", 0);
                resp.put("remainingCredentials", new ArrayList<>());
                resp.put(TUTOR_VERIFIED_KEY, false);
                resp.put("deletedFromAzure", 0);
                resp.put("azureDeleteFailed", new ArrayList<>());
                resp.put("removedSpecializations", new ArrayList<>());
                resp.put("notFound", urls == null ? new ArrayList<>() : new ArrayList<>(urls));
                return resp;
            }
            List<String> urlsToRemove = (urls == null) ? List.of() : urls;
            RemovalResult removalResult = removeCredentialsAndSpecializations(user, currentCredentials, urlsToRemove);
            updateUserVerificationAndSave(user, currentCredentials);
            AzureRemovalResult azureResult = removeFromAzure(removedUrls(removalResult));
            return buildDeleteTutorCredentialsResponse(removalResult, currentCredentials, user, azureResult);
        } catch (UserServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new UserServiceException("Error al eliminar credenciales: " + e.getMessage());
        }
    }

    private User getAndValidateTutorUser(String token) throws UserServiceException {
        CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace(BEARER_PREFIX, ""));
        String sub = userInfo.getSub();
        User user = userRepository.findBySub(sub);
        if (user == null) {
            throw new UserServiceException("Usuario no encontrado");
        }
        boolean hasTutorRole = user.getRole() != null
                && user.getRole().stream().anyMatch(ROLE_TUTOR::equalsIgnoreCase);
        if (!hasTutorRole) {
            throw new UserServiceException("El usuario no tiene el rol de tutor");
        }
        return user;
    }

    private static class RemovalResult {
        List<String> notFound = new ArrayList<>();
        List<String> removedUrls = new ArrayList<>();
        List<String> removedSpecializations = new ArrayList<>();
    }

    private RemovalResult removeCredentialsAndSpecializations(User user, List<String> current, List<String> urls) {
        RemovalResult result = new RemovalResult();
        for (String url : urls) {
            if (current.remove(url)) {
                result.removedUrls.add(url);
            } else {
                result.notFound.add(url);
            }
        }
        if (user.getSpecializations() != null && !user.getSpecializations().isEmpty()) {
            List<Specialization> remainingSpecs = new ArrayList<>();
            for (Specialization spec : user.getSpecializations()) {
                if (!spec.isVerified() || !result.removedUrls.contains(spec.getDocumentUrl())) {
                    remainingSpecs.add(spec);
                } else {
                    result.removedSpecializations.add(spec.getName());
                }
            }
            user.setSpecializations(remainingSpecs);
        }
        return result;
    }

    private void updateUserVerificationAndSave(User user, List<String> current) {
        if (current.isEmpty()) {
            user.setVerified(false);
        }
        user.setCredentials(current);
        userRepository.save(user);
    }

    private static class AzureRemovalResult {
        int deletedFromAzure = 0;
        List<String> azureDeleteFailed = new ArrayList<>();
    }

    private AzureRemovalResult removeFromAzure(List<String> removedUrls) {
        AzureRemovalResult result = new AzureRemovalResult();
        for (String u : removedUrls) {
            try {
                boolean ok = azureBlobStorageService.deleteByUrl(u);
                if (ok)
                    result.deletedFromAzure++;
                else
                    result.azureDeleteFailed.add(u);
            } catch (Exception ex) {
                result.azureDeleteFailed.add(u);
            }
        }
        return result;
    }

    private List<String> removedUrls(RemovalResult result) {
        return result.removedUrls;
    }

    private Map<String, Object> buildDeleteTutorCredentialsResponse(RemovalResult removalResult, List<String> current,
            User user, AzureRemovalResult azureResult) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("removedCount", removalResult.removedUrls.size());
        resp.put("remainingCredentials", new ArrayList<>(current));
        resp.put(TUTOR_VERIFIED_KEY, user.isVerified());
        resp.put("deletedFromAzure", azureResult.deletedFromAzure);
        resp.put("azureDeleteFailed", azureResult.azureDeleteFailed);
        resp.put("removedSpecializations", removalResult.removedSpecializations);
        resp.put("notFound", removalResult.notFound);
        return resp;
    }

    // =====================================================
    // ACTUALIZAR ESTADO DE VERIFICACIÓN DEL TUTOR
    // =====================================================
    @Override
    public void updateTutorVerificationStatus(String userId, boolean isVerified) throws UserServiceException {
        try {
            User user = userRepository.findBySub(userId);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }

            // Verificar que el usuario tenga el rol de tutor
            boolean hasTutorRole = user.getRole() != null &&
                    user.getRole().stream().anyMatch(ROLE_TUTOR::equalsIgnoreCase);

            if (!hasTutorRole) {
                throw new UserServiceException("El usuario no tiene el rol de tutor");
            }

            // Actualizar el estado de verificación
            user.setVerified(isVerified);
            userRepository.save(user);

        } catch (UserServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new UserServiceException("Error al actualizar estado de verificación: " + e.getMessage());
        }

    }

    // =====================================================
    // OBTENER TARIFA DE TOKENS POR HORA DEL TUTOR AUTENTICADO
    // =====================================================
    @Override
    public Integer getTutorTokensPerHour(String token) throws UserServiceException {
        try {
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace(BEARER_PREFIX, ""));
            String sub = userInfo.getSub();
            User user = userRepository.findBySub(sub);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }
            boolean hasTutorRole = user.getRole() != null
                    && user.getRole().stream().anyMatch(ROLE_TUTOR::equalsIgnoreCase);
            if (!hasTutorRole) {
                throw new UserServiceException("El usuario no tiene el rol de tutor");
            }
            return user.getTokensPerHour();
        } catch (UserServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new UserServiceException("Error al obtener tarifa de tokens: " + e.getMessage());
        }
    }

    @Override
    public Integer getTutorTokensPerHourBySub(String sub) throws UserServiceException {
        if (sub == null || sub.isBlank())
            throw new UserServiceException("El parámetro 'sub' es requerido");
        User user = userRepository.findBySub(sub);
        boolean hasTutorRole = user.getRole() != null
                && user.getRole().stream().anyMatch(ROLE_TUTOR::equalsIgnoreCase);
        if (!hasTutorRole)
            throw new UserServiceException("El usuario no tiene el rol de tutor");
        return user.getTokensPerHour();
    }
}
