package api.auth_service.service.impl;


import api.auth_service.dto.AuthResponse;
import api.auth_service.dto.LoginRequest;
import api.auth_service.dto.RegisterRequest;
import api.auth_service.entity.User;
import api.auth_service.enums.Role;
import api.auth_service.exception.InvalidCredentialsException;
import api.auth_service.exception.UserAlreadyExistsException;
import api.auth_service.repository.UserRepository;
import api.auth_service.security.JwtService;
import api.auth_service.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("Username is already registered");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);

        return createAuthResponse(savedUser, token);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new InvalidCredentialsException("Account is not active");
        }

        boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPassword());

        if (!passwordMatches) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        String token = jwtService.generateToken(user);
        return createAuthResponse(user, token);
    }

    private AuthResponse createAuthResponse(User user, String token) {
        return new AuthResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name(), token, "Bearer");
    }
}