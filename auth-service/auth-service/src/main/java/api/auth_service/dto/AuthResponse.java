package api.auth_service.dto;

public record AuthResponse(

        Long userId,
        String username,
        String email,
        String role,
        String accessToken,
        String tokenType

) {
}