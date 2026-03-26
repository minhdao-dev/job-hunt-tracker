package com.jobhunt.tracker.config.security;

import com.jobhunt.tracker.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        return userRepository.findByEmail(email)
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPasswordHash())
                        .authorities("ROLE_USER")
                        .accountExpired(!user.getIsActive())
                        .accountLocked(!user.getIsActive())
                        .credentialsExpired(false)
                        .disabled(!user.getIsActive())
                        .build()
                )
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + email)
                );
    }
}