package UpLearn.eci.edu.co.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import UpLearn.eci.edu.co.config.UserServiceException;
import UpLearn.eci.edu.co.dto.CognitoTokenDTO;
import UpLearn.eci.edu.co.dto.UserUpdateDTO;
import UpLearn.eci.edu.co.model.User;
import UpLearn.eci.edu.co.service.interfaces.UserService;

@RestController
@RequestMapping("/Api-user")
@CrossOrigin(origins = "*")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/users")
    public List<User> users(){
        return userService.getAllUsers();
    }

    @GetMapping("/users/{sub}")
    public User user (@PathVariable String sub) throws UserServiceException {
        return userService.getUserBySub(sub);
    }

    @DeleteMapping("/delete-profile")
    public String deleteUser(@RequestHeader("Authorization") String token) throws UserServiceException {
        userService.deleteUserByToken(token);
        return "Usuario eliminado correctamente";
    }

    /**
     * Nuevo endpoint para procesar usuarios desde tokens de Cognito
     * Este endpoint recibe el token del frontend y almacena los datos del usuario
     * si no existe, o retorna el usuario existente si ya está registrado
     */
    @PostMapping("/process-cognito-user")
    public User processCognitoUser(@RequestBody CognitoTokenDTO cognitoTokenDTO) throws UserServiceException {
        return userService.processUserFromCognito(cognitoTokenDTO);
    }

    @PutMapping("/update-profile")
    public User updateProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody UserUpdateDTO updateDTO
    ) throws UserServiceException {
        return userService.updateUser(token, updateDTO);
    }

    @GetMapping("/editable-profile")
    public UserUpdateDTO getEditableProfile(@RequestHeader("Authorization") String token) throws UserServiceException {
        return userService.getEditableUser(token);
    }
}
