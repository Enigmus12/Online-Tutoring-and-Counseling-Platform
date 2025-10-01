package UpLearn.eci.edu.co.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import UpLearn.eci.edu.co.config.UserServiceException;
import UpLearn.eci.edu.co.model.User;
import UpLearn.eci.edu.co.service.interfaces.SearchService;
import UpLearn.eci.edu.co.service.interfaces.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private UserRepository userRepository;

    private static final Set<String> STOPWORDS = Set.of(
        "con","de","del","la","el","los","las","y","o","para","por","una",
        "unos","unas","al","en","que","se","curso"
    );

    @Override
    public List<User> searchTutors(String query) throws UserServiceException {
        try {
            final String phrase = (query == null ? "" : query).trim().toLowerCase(Locale.ROOT);
            final Set<String> tokens = Arrays.stream(phrase.split("[^\\p{L}\\p{Nd}]+"))
                    .map(String::trim)
                    .filter(s -> s.length() >= 3)
                    .filter(s -> !STOPWORDS.contains(s))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            List<User> all = userRepository.findAll();

            return all.stream()
                    .filter(u -> u.getRole() != null && u.getRole().stream().anyMatch(role -> "TUTOR".equalsIgnoreCase(role)))
                    .map(u -> new AbstractMap.SimpleEntry<>(u, scoreTutor(u, phrase, tokens)))
                    .filter(e -> e.getValue() > 0)
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new UserServiceException("Error en búsqueda de tutores: " + e.getMessage());
        }
    }

    private String safe(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT); }

    private int scoreTutor(User u, String phrase, Set<String> tokens) {
        int score = 0;

        String name = safe(u.getName());
        String bio = safe(u.getBio());
        List<String> specsList = (u.getSpecializations() == null) ? List.of() : u.getSpecializations();
        String specs = safe(String.join(" ", specsList));

        // Bonus por frase completa
        if (!phrase.isBlank()) {
            if (specs.contains(phrase)) score += 8; // prioridad en specializations
            if (name.contains(phrase))  score += 6; // nombre
            if (bio.contains(phrase))   score += 4; // bio
        }

        // Bonus por tokens
        for (String t : tokens) {
            if (name.contains(t)) score += 3;
            if (bio.contains(t))  score += 2;
            for (String sp : specsList) {
                if (safe(sp).contains(t)) score += 5;
            }
        }

        return score;
    }
}
