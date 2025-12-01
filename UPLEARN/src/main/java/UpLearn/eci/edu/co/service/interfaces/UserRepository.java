package UpLearn.eci.edu.co.service.interfaces;

import UpLearn.eci.edu.co.config.UserServiceException;
import UpLearn.eci.edu.co.model.User;
import java.util.List;

public interface UserRepository{
    List<User> findAll();
    User findBySub(String sub) throws UserServiceException;
    User findByName(String name) throws UserServiceException;
    boolean existsBySub(String sub);
    void deleteBySub(String sub) throws UserServiceException;
    User save(User user);
}
    