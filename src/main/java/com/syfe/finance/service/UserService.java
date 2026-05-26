package com.syfe.finance.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syfe.finance.domain.AppUser;
import com.syfe.finance.dto.RegisterRequest;
import com.syfe.finance.dto.RegisterResponse;
import com.syfe.finance.exception.ConflictException;
import com.syfe.finance.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username already exists");
        }

        AppUser user = new AppUser(username, passwordEncoder.encode(request.password()), request.fullName().trim(),
                request.phoneNumber().trim());
        AppUser saved = userRepository.save(user);
        return new RegisterResponse("User registered successfully", saved.getId());
    }
}
