package UpLearn.eci.edu.co.service.interfaces;

import UpLearn.eci.edu.co.config.UserServiceException;
import UpLearn.eci.edu.co.model.User;
import java.util.List;

public interface UserRepository{
    List<User> findAll();
    User findByUserId(String userId) throws UserServiceException;
    User findByName(String name) throws UserServiceException;
    void deleteByUserId(String userId) throws UserServiceException;
    User save(User user);
}
    