package com.syfe.finance.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.syfe.finance.repository.UserRepository;

@Service
public class FinanceUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public FinanceUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsernameIgnoreCase(username)
            .map(user -> User.withUsername(user.getUsername()).password(user.getPassword()).roles("USER").build())
            .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));
    }
}
