package com.fuelnet.fuelnet.config;

import com.fuelnet.fuelnet.models.AppUser;
import com.fuelnet.fuelnet.models.StationUser;
import com.fuelnet.fuelnet.repositories.IAppUserRepository;
import com.fuelnet.fuelnet.repositories.IStationUserRepository;
import com.fuelnet.fuelnet.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final IStationUserRepository stationUserRepository;
    private final IAppUserRepository appUserRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String userEmail = jwtService.extractUsername(token);
        String userType = jwtService.extractUserType(token);

        if (userEmail != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            if ("STATION_USER".equals(userType)) {

                StationUser user = stationUserRepository
                        .findByEmail(userEmail)
                        .orElseThrow();

                if (jwtService.isTokenValid(token, user.getEmail())) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            user.getAuthorities());

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            else if ("APP_USER".equals(userType)) {

                AppUser user = appUserRepository
                        .findByEmail(userEmail)
                        .orElseThrow();

                if (jwtService.isTokenValid(token, user.getEmail())) {

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
