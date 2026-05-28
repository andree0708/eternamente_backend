package com.eternamente.user.api;

import com.eternamente.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Ingresa un correo válido")
    String email,
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    String password,
    @NotNull UserRole role,
    @NotBlank(message = "El nombre es obligatorio")
    String fullName
) {
}
