package com.eventflow.service.impl;

import com.eventflow.dto.AuthResponse;
import com.eventflow.dto.LoginRequest;
import com.eventflow.dto.RegisterRequest;
import com.eventflow.dto.UserSummaryResponse;
import com.eventflow.entity.Role;
import com.eventflow.entity.User;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.exception.BadRequestException;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.RoleRepository;
import com.eventflow.repository.UserRepository;
import com.eventflow.security.CustomUserDetails;
import com.eventflow.security.JwtTokenProvider;
import com.eventflow.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("An account with this email address already exists");
        }

        RoleName requestedRoleName = RoleName.ROLE_ATTENDEE;
        if (request.getRole() != null) {
            try {
                requestedRoleName = RoleName.valueOf(request.getRole());
            } catch (IllegalArgumentException e) {
                requestedRoleName = RoleName.ROLE_ATTENDEE;
            }
        }

        final RoleName roleToAssign = requestedRoleName;
        Role assignedRole = roleRepository.findByName(roleToAssign)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleToAssign.name()));

        User user = new User(
                request.getEmail().toLowerCase().trim(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName().trim(),
                request.getPhone() != null ? request.getPhone().trim() : null
        );
        user.setRoles(Collections.singleton(assignedRole));

        User savedUser = userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase().trim(), request.getPassword())
        );

        String jwt = tokenProvider.generateToken(authentication);
        UserSummaryResponse summary = mapToSummary(savedUser);

        return new AuthResponse(jwt, tokenProvider.getExpirationMs(), summary);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase().trim(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));

        UserSummaryResponse summary = mapToSummary(user);
        return new AuthResponse(jwt, tokenProvider.getExpirationMs(), summary);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BadRequestException("No authenticated user in session");
        }

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));

        return mapToSummary(user);
    }

    @Override
    @Transactional
    public UserSummaryResponse updateProfile(com.eventflow.dto.UpdateProfileRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BadRequestException("No authenticated user in session");
        }

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));

        user.setFullName(request.getFullName().trim());
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }

        User saved = userRepository.save(user);
        return mapToSummary(saved);
    }

    private UserSummaryResponse mapToSummary(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getStatus().name(),
                roleNames
        );
    }
}
