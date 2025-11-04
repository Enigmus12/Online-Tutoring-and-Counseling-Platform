package UpLearn.eci.edu.co.service.interfaces;

import UpLearn.eci.edu.co.model.User;
import UpLearn.eci.edu.co.config.UserServiceException;
import UpLearn.eci.edu.co.dto.CognitoTokenDTO;
import UpLearn.eci.edu.co.dto.ProfileStatusDTO;
import UpLearn.eci.edu.co.dto.StudentProfileDTO;
import UpLearn.eci.edu.co.dto.TutorProfileDTO;

import java.util.List;
import java.util.Map;

public interface UserService {
    List<User> getAllUsers();
    void deleteUserByToken(String token) throws UserServiceException;
    User getUserBySub(String sub) throws UserServiceException;
    User processUserFromCognito(CognitoTokenDTO cognitoTokenDTO) throws UserServiceException;
    boolean existsUserBySub(CognitoTokenDTO cognitoTokenDTO) throws UserServiceException;
    User updateUserRoles(String token, List<String> roles) throws UserServiceException;
    
    // Métodos para respuestas completas del API (Clean Controller Pattern)
    Map<String, Object> processCognitoUserComplete(CognitoTokenDTO cognitoTokenDTO) throws UserServiceException;
    Map<String, Object> saveUserRoleComplete(String token, List<String> roles) throws UserServiceException;
    
    // Nuevos métodos para manejo de roles individuales
    Map<String, Object> addRoleToUserComplete(String token, String userId, String newRole) throws UserServiceException;
    Map<String, Object> getUserRolesComplete(String token) throws UserServiceException;
    
    // Métodos específicos para perfiles de estudiante
    StudentProfileDTO getStudentProfile(String token) throws UserServiceException;
    StudentProfileDTO updateStudentProfile(String token, StudentProfileDTO studentDTO) throws UserServiceException;
    Map<String, Object> removeStudentRole(String token) throws UserServiceException;
    
    // Métodos específicos para perfiles de tutor
    TutorProfileDTO getTutorProfile(String token) throws UserServiceException;
    TutorProfileDTO updateTutorProfile(String token, TutorProfileDTO tutorDTO) throws UserServiceException;
    Map<String, Object> removeTutorRole(String token) throws UserServiceException;
    
    // Método para obtener perfil público por sub
    Map<String, Object> getPublicProfileBySub(String sub) throws UserServiceException;
    
    // Método para verificar el estado de completitud del perfil
    ProfileStatusDTO getProfileStatus(String token, String role) throws UserServiceException;

}
