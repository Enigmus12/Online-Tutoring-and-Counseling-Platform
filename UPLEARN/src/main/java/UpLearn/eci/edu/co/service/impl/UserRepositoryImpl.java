package UpLearn.eci.edu.co.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import UpLearn.eci.edu.co.service.interfaces.UserRepository;
import UpLearn.eci.edu.co.service.interfaces.repository.UserMongoRepository;
import UpLearn.eci.edu.co.config.UserServiceException;
import UpLearn.eci.edu.co.model.User;

@Service
public class UserRepositoryImpl implements UserRepository {
    @Autowired
    private UserMongoRepository userMongoRepository;

    @Override
    public List<User> findAll() {
        return userMongoRepository.findAll();
    }

    @Override
    public User findByUserId(String userId) {
        Optional<User> user = userMongoRepository.findById(userId);
        return user.orElse(null);
    }


    @Override
    public void deleteByUserId(String userId) throws UserServiceException {
        userMongoRepository.deleteById(userId);
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
