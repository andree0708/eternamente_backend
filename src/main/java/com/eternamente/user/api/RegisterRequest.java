package com.eternamente.user.api;

import com.eternamente.user.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @JsonProperty("email") @NotBlank(message = "El correo es obligatorio") @Email(message = "Ingresa un correo válido") String email,
    @JsonProperty("password") @NotBlank(message = "La contraseña es obligatoria") @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres") String password,
    @JsonProperty("role") @NotNull UserRole role,
    @JsonProperty("fullName") @NotBlank(message = "El nombre es obligatorio") String fullName,
    @JsonProperty("age") @NotNull @Min(60) @Max(120) Integer age
) {
}
