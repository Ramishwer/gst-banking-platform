package api.auth_service.service;

import api.auth_service.dto.AuthResponse;
import api.auth_service.dto.LoginRequest;
import api.auth_service.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}