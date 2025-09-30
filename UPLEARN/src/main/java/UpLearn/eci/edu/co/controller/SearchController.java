package UpLearn.eci.edu.co.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

import UpLearn.eci.edu.co.config.UserServiceException;
import UpLearn.eci.edu.co.model.User;
import UpLearn.eci.edu.co.service.interfaces.SearchService;

@RestController
@RequestMapping("/Api-search")
@CrossOrigin(origins = "*")
public class SearchController {

    @Autowired
    private SearchService searchService;

    // Búsqueda combinada (nombre, bio, specializations) y ranking por score
    @GetMapping("/tutors")
    public List<User> searchTutors(@RequestParam(value = "q", required = false) String q)
            throws UserServiceException {
        return searchService.searchTutors(q);
    }
}
