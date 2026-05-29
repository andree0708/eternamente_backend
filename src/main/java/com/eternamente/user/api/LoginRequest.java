package com.eternamente.user.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @JsonProperty("email") @NotBlank(message = "El correo es obligatorio") @Email(message = "Ingresa un correo válido") @Size(max = 254, message = "El correo no puede tener más de 254 caracteres") String email,
    @JsonProperty("password") @NotBlank(message = "La contraseña es obligatoria") @Size(min = 6, max = 128, message = "La contraseña debe tener entre 6 y 128 caracteres") String password
) {
}
