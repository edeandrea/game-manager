package io.quarkus.gamemanager.game.domain.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

@Embeddable
public record Player(
    @NotEmpty(message = "First name is required")
    @Column(nullable = false)
    String firstName,

    @NotEmpty(message = "Last name is required")
    @Column(nullable = false)
    String lastName,

    @Email(message = "Invalid email address")
    String email
) { }
