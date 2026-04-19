package com.rentdrive.security;

import com.rentdrive.entity.User;
import com.rentdrive.enums.UserStatus;
import com.rentdrive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Chargé par le filtre JWT — le "username" est l'UUID du user.
     * Utilisé aussi pour le login (chargement par email séparé dans AuthService).
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findByIdWithAllRelations(UUID.fromString(userId))
                .orElseThrow(() -> new UsernameNotFoundException("User non trouvé : " + userId));

        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName().name()))
                .collect(Collectors.toSet());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getId().toString())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(user.getStatus() == UserStatus.BANNED || user.getStatus() == UserStatus.SUSPENDED)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
