package UpLearn.eci.edu.co.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import UpLearn.eci.edu.co.service.interfaces.UserRepository;
import UpLearn.eci.edu.co.service.interfaces.repository.UserMongoRepository;
import UpLearn.eci.edu.co.config.UserServiceException;
import UpLearn.eci.edu.co.model.User;

@Service
public class UserRepositoryImpl implements UserRepository {
    private final UserMongoRepository userMongoRepository;

    @Autowired
    public UserRepositoryImpl(UserMongoRepository userMongoRepository) {
        this.userMongoRepository = userMongoRepository;
    }

    @Override
    public List<User> findAll() {
        return userMongoRepository.findAll();
    }

    @Override
    public User findBySub(String sub) {
        return userMongoRepository.findBySub(sub);
    }

    @Override
    public boolean existsBySub(String sub) {
        return userMongoRepository.existsBySub(sub);
    }

    @Override
    public void deleteBySub(String sub) throws UserServiceException {
        User user = findBySub(sub);
        if (user != null) {
            userMongoRepository.delete(user);
        } else {
            throw new UserServiceException("Usuario no encontrado con sub: " + sub);
        }
    }

    @Override
    public User findByName(String name) throws UserServiceException {
        User user = userMongoRepository.findByName(name);
        if (user == null) throw new UserServiceException("User Not found");
        return user;
    }

    @Override
    public User save(User user) {
        return userMongoRepository.save(user);
    }
}
