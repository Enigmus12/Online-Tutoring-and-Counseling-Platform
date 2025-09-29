package UpLearn.eci.edu.co.service.interfaces.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.repository.MongoRepository;
import UpLearn.eci.edu.co.model.User; 

@Repository
public interface UserMongoRepository extends MongoRepository<User, String>{
    User findByName(String name);
    User findBySub(String sub); // Buscar por el ID de Cognito
    boolean existsBySub(String sub); // Verificar existencia por sub
}
