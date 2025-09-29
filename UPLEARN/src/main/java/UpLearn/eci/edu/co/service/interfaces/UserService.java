package UpLearn.eci.edu.co.service.interfaces;

import UpLearn.eci.edu.co.model.User;
import UpLearn.eci.edu.co.config.UserServiceException;
import UpLearn.eci.edu.co.dto.CognitoTokenDTO;
import UpLearn.eci.edu.co.dto.UserUpdateDTO;

import java.util.List;

public interface UserService {
    List<User> getAllUsers();
    void deleteUserByToken(String token) throws UserServiceException;
    User getUserBySub(String sub) throws UserServiceException;
    User processUserFromCognito(CognitoTokenDTO cognitoTokenDTO) throws UserServiceException;
    User updateUser(String token, UserUpdateDTO updateDTO) throws UserServiceException;
    UserUpdateDTO getEditableUser(String token) throws UserServiceException;
}
