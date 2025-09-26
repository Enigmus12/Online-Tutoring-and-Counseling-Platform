package UpLearn.eci.edu.co.service.interfaces;

import UpLearn.eci.edu.co.model.User;
import UpLearn.eci.edu.co.config.UserServiceException;
import UpLearn.eci.edu.co.dto.AuthenticationResponseDTO;
import UpLearn.eci.edu.co.dto.UserAuthenticationDTO;
import UpLearn.eci.edu.co.dto.UserDTO;

import java.util.List;

public interface UserService {
    List<User> getAllUsers();
    void deleteUser(String userId) throws UserServiceException;
    AuthenticationResponseDTO authenticate(UserAuthenticationDTO authenticationDTO);
    User getUserByUserId(String userId) throws UserServiceException;
    User registerUser(UserDTO userDTO) throws UserServiceException;

}
