package UpLearn.eci.edu.co.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import UpLearn.eci.edu.co.config.UserServiceException;
import UpLearn.eci.edu.co.dto.CognitoTokenDTO;
import UpLearn.eci.edu.co.dto.ProfileStatusDTO;
import UpLearn.eci.edu.co.dto.StudentProfileDTO;
import UpLearn.eci.edu.co.dto.TutorProfileDTO;
import UpLearn.eci.edu.co.model.User;
import UpLearn.eci.edu.co.service.interfaces.UserRepository;
import UpLearn.eci.edu.co.service.interfaces.UserService;
import UpLearn.eci.edu.co.util.CognitoTokenDecoder;
import UpLearn.eci.edu.co.util.CognitoTokenDecoder.CognitoUserInfo;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CognitoTokenDecoder cognitoTokenDecoder;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public void deleteUserByToken(String token) throws UserServiceException {
        try {
            // Extraer información del token de Cognito
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace("Bearer ", ""));
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
            
            // Asignar roles si se proporcionan, sino dejar vacío para que el frontend los asigne
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
     * Este método encapsula toda la lógica de negocio para el endpoint process-cognito-user
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
        userResponse.put("id", user.getSub());  // ID único de Cognito
        userResponse.put("email", user.getEmail());  // Email del usuario
        
        // Lógica de roles: null indica que el frontend debe mostrar selección
        if (user.getRole() == null || user.getRole().isEmpty()) {
            userResponse.put("roles", null);  // Trigger para selección de roles
        } else {
            userResponse.put("roles", user.getRole());  // Roles ya asignados
        }
        
        response.put("user", userResponse);
        response.put("isNewUser", isNewUser);  // Flag para el frontend
        
        return response;
    }

    /**
     * Guarda roles de usuario y retorna la respuesta completa del API
     * Este método encapsula toda la lógica de negocio para el endpoint save-user-role
     */
    @Override
    public Map<String, Object> saveUserRoleComplete(String token, List<String> roles) throws UserServiceException {
        // 1. Actualizar roles del usuario
        User user = updateUserRoles(token, roles);
        
        // 2. Construir respuesta del API con datos actualizados
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getSub());
        response.put("email", user.getEmail());
        response.put("roles", user.getRole());  // Roles recién guardados
        
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
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace("Bearer ", ""));
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
    public Map<String, Object> addRoleToUserComplete(String token, String userId, String newRole) throws UserServiceException {
        try {
            // Validar que el usuario esté autenticado (verificamos que el token sea válido)
            cognitoTokenDecoder.extractUserInfo(token.replace("Bearer ", ""));
            
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
            
            // Construir respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedUser.getSub());
            response.put("email", updatedUser.getEmail());
            response.put("roles", updatedUser.getRole());
            response.put("message", "Rol '" + newRole + "' añadido exitosamente");
            
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
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace("Bearer ", ""));
            String sub = userInfo.getSub();
            
            // Buscar el usuario en la base de datos
            User user = userRepository.findBySub(sub);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }
            
            // Construir respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getSub());
            response.put("email", user.getEmail());
            response.put("name", user.getName());
            response.put("roles", user.getRole() != null ? user.getRole() : new ArrayList<>());
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
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace("Bearer ", ""));
            String sub = userInfo.getSub();
            
            User user = userRepository.findBySub(sub);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }

            // Verificar que el usuario tenga el rol de estudiante (case-insensitive)
            boolean hasStudentRole = false;
            if (user.getRole() != null) {
                for (String role : user.getRole()) {
                    if ("STUDENT".equalsIgnoreCase(role)) {
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
    public StudentProfileDTO updateStudentProfile(String token, StudentProfileDTO studentDTO) throws UserServiceException {
        try {
            // Extraer información del token de Cognito
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace("Bearer ", ""));
            String sub = userInfo.getSub();
            
            User user = userRepository.findBySub(sub);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }

            // Verificar que el usuario tenga el rol de estudiante (case-insensitive)
            boolean hasStudentRole = false;
            if (user.getRole() != null) {
                for (String role : user.getRole()) {
                    if ("STUDENT".equalsIgnoreCase(role)) {
                        hasStudentRole = true;
                        break;
                    }
                }
            }
            
            if (!hasStudentRole) {
                throw new UserServiceException("El usuario no tiene el rol de estudiante");
            }

            // Actualizar solo campos permitidos para estudiante
            if (studentDTO.getName() != null) user.setName(studentDTO.getName());
            if (studentDTO.getEmail() != null) user.setEmail(studentDTO.getEmail());
            if (studentDTO.getPhoneNumber() != null) user.setPhoneNumber(studentDTO.getPhoneNumber());
            if (studentDTO.getIdType() != null) user.setIdType(studentDTO.getIdType());
            if (studentDTO.getIdNumber() != null) user.setIdNumber(studentDTO.getIdNumber());
            if (studentDTO.getEducationLevel() != null) user.setEducationLevel(studentDTO.getEducationLevel());

            User savedUser = userRepository.save(user);

            // Retornar DTO actualizado
            StudentProfileDTO updatedDTO = new StudentProfileDTO();
            updatedDTO.setName(savedUser.getName());
            updatedDTO.setEmail(savedUser.getEmail());
            updatedDTO.setPhoneNumber(savedUser.getPhoneNumber());
            updatedDTO.setIdType(savedUser.getIdType());
            updatedDTO.setIdNumber(savedUser.getIdNumber());
            updatedDTO.setEducationLevel(savedUser.getEducationLevel());

            return updatedDTO;
        } catch (UserServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new UserServiceException("Error al actualizar perfil de estudiante: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> removeStudentRole(String token) throws UserServiceException {
        try {
            // Extraer información del token de Cognito
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace("Bearer ", ""));
            String sub = userInfo.getSub();
            
            User user = userRepository.findBySub(sub);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }

            // Verificar que el usuario tenga el rol de estudiante (case-insensitive)
            boolean hasStudentRole = false;
            if (user.getRole() != null) {
                for (String role : user.getRole()) {
                    if ("STUDENT".equalsIgnoreCase(role)) {
                        hasStudentRole = true;
                        break;
                    }
                }
            }
            
            if (!hasStudentRole) {
                throw new UserServiceException("El usuario no tiene el rol de estudiante");
            }

            // Remover el rol de estudiante de la lista (case-insensitive)
            List<String> updatedRoles = new ArrayList<>();
            if (user.getRole() != null) {
                for (String role : user.getRole()) {
                    if (!"STUDENT".equalsIgnoreCase(role)) {
                        updatedRoles.add(role);
                    }
                }
            }
            
            // Limpiar campos específicos de estudiante
            user.setEducationLevel(null);
            
            user.setRole(updatedRoles);

            // Si ya no tiene roles, eliminar completamente el usuario
            if (updatedRoles.isEmpty()) {
                userRepository.deleteBySub(sub);
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Perfil de estudiante eliminado. Usuario completamente eliminado.");
                response.put("userDeleted", true);
                return response;
            } else {
                // Solo actualizar el usuario removiendo el rol
                User savedUser = userRepository.save(user);
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Rol de estudiante eliminado exitosamente");
                response.put("userDeleted", false);
                response.put("remainingRoles", savedUser.getRole());
                return response;
            }
        } catch (UserServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new UserServiceException("Error al eliminar rol de estudiante: " + e.getMessage());
        }
    }

    // =====================================================
    // MÉTODOS ESPECÍFICOS PARA PERFILES DE TUTOR
    // =====================================================

    @Override
    public TutorProfileDTO getTutorProfile(String token) throws UserServiceException {
        try {
            // Extraer información del token de Cognito
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace("Bearer ", ""));
            String sub = userInfo.getSub();
            
            User user = userRepository.findBySub(sub);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }

            // Verificar que el usuario tenga el rol de tutor (case-insensitive)
            boolean hasTutorRole = false;
            if (user.getRole() != null) {
                for (String role : user.getRole()) {
                    if ("TUTOR".equalsIgnoreCase(role)) {
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
            // Extraer información del token de Cognito
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace("Bearer ", ""));
            String sub = userInfo.getSub();
            
            User user = userRepository.findBySub(sub);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }

            // Verificar que el usuario tenga el rol de tutor (case-insensitive)
            boolean hasTutorRole = false;
            if (user.getRole() != null) {
                for (String role : user.getRole()) {
                    if ("TUTOR".equalsIgnoreCase(role)) {
                        hasTutorRole = true;
                        break;
                    }
                }
            }
            
            if (!hasTutorRole) {
                throw new UserServiceException("El usuario no tiene el rol de tutor");
            }

            // Actualizar solo campos permitidos para tutor
            if (tutorDTO.getName() != null) user.setName(tutorDTO.getName());
            if (tutorDTO.getEmail() != null) user.setEmail(tutorDTO.getEmail());
            if (tutorDTO.getPhoneNumber() != null) user.setPhoneNumber(tutorDTO.getPhoneNumber());
            if (tutorDTO.getIdType() != null) user.setIdType(tutorDTO.getIdType());
            if (tutorDTO.getIdNumber() != null) user.setIdNumber(tutorDTO.getIdNumber());
            if (tutorDTO.getBio() != null) user.setBio(tutorDTO.getBio());
            if (tutorDTO.getSpecializations() != null) user.setSpecializations(tutorDTO.getSpecializations());
            if (tutorDTO.getCredentials() != null) user.setCredentials(tutorDTO.getCredentials());

            User savedUser = userRepository.save(user);

            // Retornar DTO actualizado
            TutorProfileDTO updatedDTO = new TutorProfileDTO();
            updatedDTO.setName(savedUser.getName());
            updatedDTO.setEmail(savedUser.getEmail());
            updatedDTO.setPhoneNumber(savedUser.getPhoneNumber());
            updatedDTO.setIdType(savedUser.getIdType());
            updatedDTO.setIdNumber(savedUser.getIdNumber());
            updatedDTO.setBio(savedUser.getBio());
            updatedDTO.setSpecializations(savedUser.getSpecializations());
            updatedDTO.setCredentials(savedUser.getCredentials());

            return updatedDTO;
        } catch (UserServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new UserServiceException("Error al actualizar perfil de tutor: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> removeTutorRole(String token) throws UserServiceException {
        try {
            // Extraer información del token de Cognito
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace("Bearer ", ""));
            String sub = userInfo.getSub();
            
            User user = userRepository.findBySub(sub);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }

            // Verificar que el usuario tenga el rol de tutor (case-insensitive)
            boolean hasTutorRole = false;
            if (user.getRole() != null) {
                for (String role : user.getRole()) {
                    if ("TUTOR".equalsIgnoreCase(role)) {
                        hasTutorRole = true;
                        break;
                    }
                }
            }
            
            if (!hasTutorRole) {
                throw new UserServiceException("El usuario no tiene el rol de tutor");
            }

            // Remover el rol de tutor de la lista (case-insensitive)
            List<String> updatedRoles = new ArrayList<>();
            if (user.getRole() != null) {
                for (String role : user.getRole()) {
                    if (!"TUTOR".equalsIgnoreCase(role)) {
                        updatedRoles.add(role);
                    }
                }
            }
            
            // Limpiar campos específicos de tutor
            user.setBio(null);
            user.setSpecializations(null);
            user.setCredentials(null);
            
            user.setRole(updatedRoles);

            // Si ya no tiene roles, eliminar completamente el usuario
            if (updatedRoles.isEmpty()) {
                userRepository.deleteBySub(sub);
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Perfil de tutor eliminado. Usuario completamente eliminado.");
                response.put("userDeleted", true);
                return response;
            } else {
                // Solo actualizar el usuario removiendo el rol
                User savedUser = userRepository.save(user);
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Rol de tutor eliminado exitosamente");
                response.put("userDeleted", false);
                response.put("remainingRoles", savedUser.getRole());
                return response;
            }
        } catch (UserServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new UserServiceException("Error al eliminar rol de tutor: " + e.getMessage());
        }
    }
    // =====================================================
    // NUEVO MÉTODO PARA PERFIL PÚBLICO POR SUB
    @Override
    public Map<String, Object> getPublicProfileBySub(String sub) throws UserServiceException {
        if (sub == null || sub.isBlank()) throw new UserServiceException("El parámetro 'sub' es requerido");
        User user = getUserBySub(sub);
        Map<String, Object> out = new HashMap<>();
        out.put("sub", user.getSub());
        out.put("name", user.getName());
        out.put("role", user.getRole());
        boolean isTutor = user.getRole() != null && user.getRole().stream().anyMatch(r -> "TUTOR".equalsIgnoreCase(r));
        if (isTutor) {
            out.put("specializations", user.getSpecializations());
            out.put("credentials", user.getCredentials());
        }
        return out;
    }

    // =====================================================
    // MÉTODO PARA VERIFICAR ESTADO DE COMPLETITUD DEL PERFIL
    @Override
    public ProfileStatusDTO getProfileStatus(String token, String role) throws UserServiceException {
        try {
            // Extraer información del token de Cognito
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace("Bearer ", ""));
            String sub = userInfo.getSub();
            
            User user = userRepository.findBySub(sub);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }

            ProfileStatusDTO profileStatus = new ProfileStatusDTO();
            List<String> missingFields = new ArrayList<>();
            
            // Determinar el rol a verificar
            String currentRole = null;
            
            // Si se proporciona el parámetro role, usarlo; de lo contrario, usar el primer rol del usuario
            if (role != null && !role.trim().isEmpty()) {
                // Validar que el usuario tenga ese rol
                boolean hasRole = user.getRole() != null && 
                    user.getRole().stream().anyMatch(r -> r.equalsIgnoreCase(role));
                
                if (!hasRole) {
                    throw new UserServiceException("El usuario no tiene el rol: " + role);
                }
                currentRole = role.toUpperCase();
            } else {
                // Tomar el primer rol si tiene múltiples
                if (user.getRole() != null && !user.getRole().isEmpty()) {
                    currentRole = user.getRole().get(0).toUpperCase();
                }
            }
            
            profileStatus.setCurrentRole(currentRole);
            
            // Si no tiene rol, el perfil está incompleto
            if (currentRole == null) {
                profileStatus.setComplete(false);
                missingFields.add("role");
                profileStatus.setMissingFields(missingFields);
                return profileStatus;
            }
            
            // Verificar campos según el rol
            if ("STUDENT".equalsIgnoreCase(currentRole)) {
                // Para estudiante: name, email, phoneNumber, educationLevel
                if (user.getName() == null || user.getName().trim().isEmpty()) {
                    missingFields.add("name");
                }
                if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                    missingFields.add("email");
                }
                if (user.getPhoneNumber() == null || user.getPhoneNumber().trim().isEmpty()) {
                    missingFields.add("phoneNumber");
                }
                if (user.getEducationLevel() == null || user.getEducationLevel().trim().isEmpty()) {
                    missingFields.add("educationLevel");
                }
            } else if ("TUTOR".equalsIgnoreCase(currentRole)) {
                // Para tutor: name, email, phoneNumber, bio, specializations (al menos 1), credentials (al menos 1)
                if (user.getName() == null || user.getName().trim().isEmpty()) {
                    missingFields.add("name");
                }
                if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                    missingFields.add("email");
                }
                if (user.getPhoneNumber() == null || user.getPhoneNumber().trim().isEmpty()) {
                    missingFields.add("phoneNumber");
                }
                if (user.getBio() == null || user.getBio().trim().isEmpty()) {
                    missingFields.add("bio");
                }
                if (user.getSpecializations() == null || user.getSpecializations().isEmpty()) {
                    missingFields.add("specializations");
                }
                if (user.getCredentials() == null || user.getCredentials().isEmpty()) {
                    missingFields.add("credentials");
                }
            }
            
            // Determinar si el perfil está completo
            boolean isComplete = missingFields.isEmpty();
            profileStatus.setComplete(isComplete);
            profileStatus.setMissingFields(missingFields.isEmpty() ? null : missingFields);
            
            return profileStatus;
            
        } catch (UserServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new UserServiceException("Error al verificar estado del perfil: " + e.getMessage());
        }
    }

}
