package UpLearn.eci.edu.co.service.interfaces;

import java.util.List;

import UpLearn.eci.edu.co.config.UserServiceException;
import UpLearn.eci.edu.co.model.User;

public interface SearchService {
    List<User> searchTutors(String query) throws UserServiceException;

}
