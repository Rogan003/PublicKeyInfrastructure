package org.example.publickeyinfrastructure.Utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.example.publickeyinfrastructure.Services.Auth.UsersDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UsersDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        
        // Ako nema Authorization header-a, nastavi sa filter chain-om
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // Izdvoji JWT token (ukloni "Bearer " prefix)
            final String jwt = authHeader.substring(7);
            
            // Proveri da li je access token
            if (!jwtUtil.isAccessToken(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // Izdvoji email iz tokena
            final String userEmail = jwtUtil.extractEmail(jwt);
            
            // Ako imamo email i nema već postavljenog authentication-a
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Učitaj UserDetails iz baze
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                
                // Validiraj token
                if (jwtUtil.validateToken(jwt)) {
                    
                    // Kreiraj Authentication token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,           // Principal (UserDetails)
                        null,                  // Credentials (password - null jer je JWT)
                        userDetails.getAuthorities()  // Authorities (roles)
                    );
                    
                    // Dodaj detalje o request-u
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Postavi authentication u SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication", e);
        }
        
        // Nastavi sa filter chain-om
        filterChain.doFilter(request, response);
    }
}