package UpLearn.eci.edu.co.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class CognitoTokenFilter extends OncePerRequestFilter {

    @Autowired
    private CognitoTokenDecoder cognitoTokenDecoder;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String sub = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                // Validar que el token de Cognito sea válido
                if (cognitoTokenDecoder.isTokenValid(jwt)) {
                    CognitoTokenDecoder.CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(jwt);
                    sub = userInfo.getSub();
                }
            } catch (Exception e) {
                logger.error("Error validating Cognito token", e);
            }
        }

        if (sub != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                CognitoTokenDecoder.CognitoUserInfo userInfo = cognitoTokenDecoder.extractUserInfo(jwt);
                
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                // Usar el rol del token de Cognito
                if (userInfo.getRole() != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + userInfo.getRole().toUpperCase()));
                } else {
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        sub, null, authorities);

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (Exception e) {
                logger.error("Error setting up security context with Cognito token", e);
            }
        }
        chain.doFilter(request, response);
    }
}