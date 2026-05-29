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
    @JsonProperty("email") @NotBlank(message = "El correo es obligatorio") @Email(message = "Ingresa un correo válido") @Size(max = 30, message = "El correo no puede tener más de 30 caracteres") String email,
    @JsonProperty("password") @NotBlank(message = "La contraseña es obligatoria") @Size(min = 6, max = 128, message = "La contraseña debe tener entre 6 y 128 caracteres") String password,
    @JsonProperty("role") @NotNull UserRole role,
    @JsonProperty("fullName") @NotBlank(message = "El nombre es obligatorio") @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres") String fullName,
    @JsonProperty("age") @NotNull @Min(60) @Max(120) Integer age
) {
}
