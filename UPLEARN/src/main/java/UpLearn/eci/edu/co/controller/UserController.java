package UpLearn.eci.edu.co.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import UpLearn.eci.edu.co.config.UserServiceException;
import UpLearn.eci.edu.co.dto.AuthenticationResponseDTO;
import UpLearn.eci.edu.co.dto.UserAuthenticationDTO;
import UpLearn.eci.edu.co.dto.UserDTO;
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


    @GetMapping("/users/{userId}")
    public User user (@PathVariable String userId) throws UserServiceException {
        return userService.getUserByUserId(userId);
    }

    @DeleteMapping("/delete-profile")
    public String deleteUser(@RequestHeader("Authorization") String token) throws UserServiceException {
        userService.deleteUserByToken(token);
        return "Usuario eliminado correctamente";
    }


    @PostMapping("/login")
    public AuthenticationResponseDTO authenticate(@RequestBody UserAuthenticationDTO authenticationDTO) {
        return userService.authenticate(authenticationDTO);
    }

    @PostMapping("/register-student")
    public User registerStudent(@RequestBody UserDTO userDTO) throws UserServiceException {
        if (!"STUDENT".equalsIgnoreCase(userDTO.getRole())) {
            throw new UserServiceException("El rol debe ser STUDENT para este endpoint");
        }

        return userService.registerUser(userDTO);
    }

    @PostMapping("/register-tutor")
    public User registerTutor(@RequestBody UserDTO userDTO) throws UserServiceException {
        if (!"TUTOR".equalsIgnoreCase(userDTO.getRole())) {
            throw new UserServiceException("El rol debe ser TUTOR para este endpoint");
        }

        return userService.registerUser(userDTO);
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
