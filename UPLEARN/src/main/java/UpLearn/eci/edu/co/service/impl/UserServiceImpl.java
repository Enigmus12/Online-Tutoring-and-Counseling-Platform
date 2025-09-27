package UpLearn.eci.edu.co.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import UpLearn.eci.edu.co.config.UserServiceException;
import UpLearn.eci.edu.co.dto.AuthenticationResponseDTO;
import UpLearn.eci.edu.co.dto.UserAuthenticationDTO;
import UpLearn.eci.edu.co.dto.UserDTO;
import UpLearn.eci.edu.co.dto.UserUpdateDTO;
import UpLearn.eci.edu.co.model.User;
import UpLearn.eci.edu.co.service.interfaces.UserRepository;
import UpLearn.eci.edu.co.service.interfaces.UserService;
import UpLearn.eci.edu.co.util.JwtUtil;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public void deleteUserByToken(String token) throws UserServiceException {
        String userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        User user = userRepository.findByUserId(userId);

        if (user == null) {
            throw new UserServiceException("Usuario no encontrado");
        }

        userRepository.deleteByUserId(userId);
    }



    @Override
    public AuthenticationResponseDTO authenticate(UserAuthenticationDTO authenticationDTO) {
        try {
            // Buscar usuario por userId
            User user = userRepository.findByUserId(authenticationDTO.getUserId());

            // Verificar la contraseña
            if (user.getPassword().equals(authenticationDTO.getPassword())) {
                // Generar token JWT con el userId y el role
                String token = jwtUtil.generateToken(user.getUserId(), user.getRole());
                return new AuthenticationResponseDTO(true, user, token, "Autenticación exitosa");
            } else {
                return new AuthenticationResponseDTO(false, null, null, "Contraseña incorrecta");
            }
        } catch (UserServiceException e) {
            // Usuario no encontrado
            return new AuthenticationResponseDTO(false, null, null, "Usuario no encontrado");
        }
    }


    @Override
    public User getUserByUserId(String userId) throws UserServiceException {
        return userRepository.findByUserId(userId);
    }

    @Override
    public User registerUser(UserDTO userDTO) throws UserServiceException {
        // 1. Validar que el userId no exista
        User existingUser = userRepository.findByUserId(userDTO.getUserId());
        if (existingUser != null) {
            throw new UserServiceException(
                "El userId " + userDTO.getUserId() + " ya existe. Debe ser único."
            );
        }

        // 2. Validar idNumber y rol
        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            if (u.getIdNumber().equals(userDTO.getIdNumber())) {
                if (u.getRole().equalsIgnoreCase(userDTO.getRole())) {
                    throw new UserServiceException(
                        "Ya existe un usuario con idNumber " + userDTO.getIdNumber() +
                        " y rol " + userDTO.getRole()
                    );
                }
                // Si tiene el mismo idNumber pero rol diferente -> permitir
            }
        }

        // 3. Crear usuario nuevo
        User user = new User();
        user.setName(userDTO.getName());
        user.setIdType(userDTO.getIdType());
        user.setIdNumber(userDTO.getIdNumber());
        user.setEmail(userDTO.getEmail());
        user.setUserId(userDTO.getUserId());
        user.setPassword(userDTO.getPassword());
        user.setPasswordConfirmation(userDTO.getPasswordConfirmation());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setRole(userDTO.getRole());

        // Validaciones según el rol
        if ("STUDENT".equalsIgnoreCase(userDTO.getRole())) {
            if (userDTO.getEducationLevel() == null) {
                throw new UserServiceException("El campo educationLevel es obligatorio para estudiantes");
            }
            user.setEducationLevel(userDTO.getEducationLevel());
        }

        if ("TUTOR".equalsIgnoreCase(userDTO.getRole())) {
            if (userDTO.getBio() == null) {
                throw new UserServiceException("El campo bio es obligatorio para tutores");
            }
            user.setBio(userDTO.getBio());
            user.setSpecializations(userDTO.getSpecializations());
            user.setCredentials(userDTO.getCredentials());
        }

        return userRepository.save(user);
    }

    @Override
    public User updateUser(String token, UserUpdateDTO updateDTO) throws UserServiceException {
        String userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        User user = userRepository.findByUserId(userId);

        if (user == null) {
            throw new UserServiceException("Usuario no encontrado");
        }

        // Solo actualizamos los atributos permitidos
        if (updateDTO.getName() != null) user.setName(updateDTO.getName());
        if (updateDTO.getEmail() != null) user.setEmail(updateDTO.getEmail());
        if (updateDTO.getPhoneNumber() != null) user.setPhoneNumber(updateDTO.getPhoneNumber());

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
    }

    @Override
    public UserUpdateDTO getEditableUser(String token) throws UserServiceException {
        String userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        User user = userRepository.findByUserId(userId);

        if (user == null) {
            throw new UserServiceException("Usuario no encontrado");
        }

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());

        if ("STUDENT".equalsIgnoreCase(user.getRole())) {
            dto.setEducationLevel(user.getEducationLevel());
        }

        if ("TUTOR".equalsIgnoreCase(user.getRole())) {
            dto.setBio(user.getBio());
            dto.setSpecializations(user.getSpecializations());
            dto.setCredentials(user.getCredentials());
        }

        return dto;
    }





}
