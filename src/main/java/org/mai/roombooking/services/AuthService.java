package org.mai.roombooking.services;

import lombok.AllArgsConstructor;
import org.mai.roombooking.entities.User;
import org.mai.roombooking.security.requestDTO.AuthResponse;
import org.mai.roombooking.security.requestDTO.UserLoginRequest;
import org.mai.roombooking.security.requestDTO.UserRegistrationRequest;
import org.mai.roombooking.repositories.UserRepository;
import org.mai.roombooking.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse registerUser(UserRegistrationRequest registrationRequest) {
        User user = User.builder()
                .username(registrationRequest.getUsername())
                .fullName(registrationRequest.getFullName())
                .password(passwordEncoder.encode(registrationRequest.getPassword()))
                .phoneNumber(registrationRequest.getPhoneNumber())
                .role(User.UserRole.ADMINISTRATOR)
                .isAccountLocked(false)
                .build();
        userRepository.save(user);
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("fullname", user.getFullName());
        extraClaims.put("role", user.getRole());
        var jwtToken = jwtService.generateToken(extraClaims, user);
        return AuthResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthResponse loginUser(UserLoginRequest loginRequest) {
        System.out.println("---1");
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        System.out.println("---1?2");
        var user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        System.out.println("---2");

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("fullName", user.getFullName());
        extraClaims.put("role", user.getRole());
        System.out.println("---3");
        var jwtToken = jwtService.generateToken(extraClaims, user);
        System.out.println("---4");

        return AuthResponse.builder()
                .token(jwtToken)
                .build();
    }
}
