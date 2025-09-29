package UpLearn.eci.edu.co.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;// Cuando el usuario llega a dashboard después de autenticarse con Cognito

import UpLearn.eci.edu.co.config.UserServiceException;
import UpLearn.eci.edu.co.dto.CognitoTokenDTO;
import UpLearn.eci.edu.co.dto.UserUpdateDTO;
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
                // Si existe, retornar el usuario existente (sin crear duplicado)
                return userRepository.findBySub(userInfo.getSub());
            }

            // Si no existe, crear nuevo usuario con la información del token
            User newUser = new User();
            newUser.setSub(userInfo.getSub());
            newUser.setName(userInfo.getName());
            newUser.setEmail(userInfo.getEmail());
            newUser.setRole(userInfo.getRole() != null ? userInfo.getRole().toUpperCase() : "STUDENT");
            newUser.setPhoneNumber(userInfo.getPhoneNumber());

            // Guardar el nuevo usuario en la base de datos
            return userRepository.save(newUser);

        } catch (Exception e) {
            throw new UserServiceException("Error al procesar usuario desde Cognito: " + e.getMessage());
        }
    }

    @Override
    public User updateUser(String token, UserUpdateDTO updateDTO) throws UserServiceException {
        try {
            // Extraer información del token de Cognito
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace("Bearer ", ""));
            String sub = userInfo.getSub();
            
            User user = userRepository.findBySub(sub);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }

            // Solo actualizamos los atributos permitidos (no los de autenticación de Cognito)
            if (updateDTO.getName() != null) user.setName(updateDTO.getName());
            if (updateDTO.getEmail() != null) user.setEmail(updateDTO.getEmail());
            if (updateDTO.getPhoneNumber() != null) user.setPhoneNumber(updateDTO.getPhoneNumber());
            if (updateDTO.getIdType() != null) user.setIdType(updateDTO.getIdType());
            if (updateDTO.getIdNumber() != null) user.setIdNumber(updateDTO.getIdNumber());

            // Actualizaciones específicas por rol
            if ("STUDENT".equalsIgnoreCase(user.getRole())) {
                if (updateDTO.getEducationLevel() != null) {
                    user.setEducationLevel(updateDTO.getEducationLevel());
                }
            }

            if ("TUTOR".equalsIgnoreCase(user.getRole())) {
                if (updateDTO.getBio() != null) user.setBio(updateDTO.getBio());
                if (updateDTO.getSpecializations() != null) user.setSpecializations(updateDTO.getSpecializations());
                if (updateDTO.getCredentials() != null) user.setCredentials(updateDTO.getCredentials());
            }

            return userRepository.save(user);
        } catch (Exception e) {
            throw new UserServiceException("Error al actualizar usuario: " + e.getMessage());
        }
    }

    @Override
    public UserUpdateDTO getEditableUser(String token) throws UserServiceException {
        try {
            // Extraer información del token de Cognito
            CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(token.replace("Bearer ", ""));
            String sub = userInfo.getSub();
            
            User user = userRepository.findBySub(sub);
            if (user == null) {
                throw new UserServiceException("Usuario no encontrado");
            }

            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setName(user.getName());
            dto.setEmail(user.getEmail());
            dto.setPhoneNumber(user.getPhoneNumber());
            dto.setIdType(user.getIdType());
            dto.setIdNumber(user.getIdNumber());

            if ("STUDENT".equalsIgnoreCase(user.getRole())) {
                dto.setEducationLevel(user.getEducationLevel());
            }

            if ("TUTOR".equalsIgnoreCase(user.getRole())) {
                dto.setBio(user.getBio());
                dto.setSpecializations(user.getSpecializations());
                dto.setCredentials(user.getCredentials());
            }

            return dto;
        } catch (Exception e) {
            throw new UserServiceException("Error al obtener información del usuario: " + e.getMessage());
        }
    }
}
