package UpLearn.eci.edu.co.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

import UpLearn.eci.edu.co.config.UserServiceException;
import UpLearn.eci.edu.co.model.User;
import UpLearn.eci.edu.co.service.interfaces.SearchService;

/**
 * Controlador para la búsqueda de tutores
 */
@RestController
@RequestMapping("/Api-search")
@CrossOrigin(origins = "*")
public class SearchController {

    private final SearchService searchService;

    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Endpoint para buscar tutores por nombre o especialidad
     * 
     * @param q Parámetro de búsqueda (opcional)
     * @return Lista de tutores que coinciden con la búsqueda
     * @throws UserServiceException en caso de error durante la búsqueda
     */
    @GetMapping("/tutors")
    public List<User> searchTutors(@RequestParam(value = "q", required = false) String q)
            throws UserServiceException {
        return searchService.searchTutors(q);
    }

    /**
     * Endpoint para obtener los 10 mejores tutores
     * Ordenados por cantidad de credenciales y especializaciones
     * 
     * @return Lista de los 10 mejores tutores disponibles
     * @throws UserServiceException en caso de error durante la búsqueda
     */
    @GetMapping("/tutors/top")
    public List<User> getTopTutors() throws UserServiceException {
        return searchService.getTopTutors();
    }
}
