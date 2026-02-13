package io.quarkus.gamemanager.game.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public record PlayerDto(
    @NotEmpty(message = "First name is required")
    String firstName,

    @NotEmpty(message = "Last name is required")
    String lastName,

    @Email(message = "Invalid email address")
    @NotEmpty(message = "Email is required")
    String email
) {
}
