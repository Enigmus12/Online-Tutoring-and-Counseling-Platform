package UpLearn.eci.edu.co.service.impl;

import org.springframework.stereotype.Service;

import UpLearn.eci.edu.co.config.UserServiceException;
import UpLearn.eci.edu.co.model.User;
import UpLearn.eci.edu.co.service.interfaces.SearchService;
import UpLearn.eci.edu.co.service.interfaces.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

/** Implementación del servicio de búsqueda */
@Service
public class SearchServiceImpl implements SearchService {

    private final UserRepository userRepository;

    public SearchServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /* Palabras vacías */
    private static final Set<String> STOPWORDS = Set.of(
            "con", "de", "del", "la", "el", "los", "las", "y", "o", "para", "por", "una",
            "unos", "unas", "al", "en", "que", "se", "curso");

    /** Búsqueda de tutores */
    @Override
    public List<User> searchTutors(String query) throws UserServiceException {
        try {
            final String phrase = (query == null ? "" : query).trim().toLowerCase(Locale.ROOT);

            final Set<String> tokens = Arrays.stream(phrase.split("[^\\p{L}\\p{Nd}]+"))
                    .map(String::trim)
                    .filter(s -> s.length() >= 3)
                    .filter(s -> !STOPWORDS.contains(s))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            List<User> allTutors = userRepository.findAll().stream()
                    .filter(u -> u.getRole() != null &&
                            u.getRole().stream().anyMatch("TUTOR"::equalsIgnoreCase))
                    .toList();

            if (phrase.isBlank() && tokens.isEmpty()) {
                return allTutors.stream()
                        .sorted(Comparator.comparing(
                                u -> Optional.ofNullable(u.getName()).orElse("")))
                        .toList();
            }

            return allTutors.stream()
                    .map(u -> new AbstractMap.SimpleEntry<>(u, scoreTutor(u, phrase, tokens)))
                    .filter(e -> e.getValue() > 0)
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .map(Map.Entry::getKey)
                    .toList();

        } catch (Exception e) {
            throw new UserServiceException("Error en búsqueda de tutores: " + e.getMessage());
        }
    }

    /* Normaliza una cadena para búsqueda */
    private String safe(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    /* Puntúa un tutor según frase y tokens */
    private int scoreTutor(User u, String phrase, Set<String> tokens) {
        String name = safe(u.getName());
        String bio = safe(u.getBio());
        List<String> specsList = (u.getSpecializations() == null) ? List.of() : u.getSpecializations();
        String specs = safe(String.join(" ", specsList));

        return scorePhraseBonus(phrase, name, bio, specs) + scoreTokenBonus(tokens, name, bio, specsList);
    }

    /* Puntúa bonificación por frase completa */
    private int scorePhraseBonus(String phrase, String name, String bio, String specs) {
        if (phrase == null || phrase.isBlank())
            return 0;
        int s = 0;
        if (specs.contains(phrase))
            s += 8; // prioridad en specializations
        if (name.contains(phrase))
            s += 6; // nombre
        if (bio.contains(phrase))
            s += 4; // bio
        return s;
    }

    /* Puntúa bonificación por tokens individuales */
    private int scoreTokenBonus(Set<String> tokens, String name, String bio, List<String> specsList) {
        int s = 0;
        for (String t : tokens) {
            if (name.contains(t))
                s += 3;
            if (bio.contains(t))
                s += 2;
            for (String sp : specsList) {
                if (safe(sp).contains(t))
                    s += 5;
            }
        }
        return s;
    }
}
