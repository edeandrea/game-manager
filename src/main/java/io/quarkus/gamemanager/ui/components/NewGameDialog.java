package io.quarkus.gamemanager.ui.components;

import java.util.Optional;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

import io.quarkus.gamemanager.game.domain.PlayerDto;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormRow;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.dom.Style.FlexWrap;

public final class NewGameDialog extends Dialog {
  private final TextField firstNameField = new TextField("First Name");
  private final TextField lastNameField = new TextField("Last Name");
  private final EmailField emailField = new EmailField("Email");
  private final Binder<PlayerForm> binder = new BeanValidationBinder<>(PlayerForm.class);
  private final PlayerForm player = new PlayerForm();
  private PlayerDto newPlayer;

  public NewGameDialog() {
    super("New Game");
    setModality(ModalityMode.STRICT);
    setResizable(true);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(true);
    setDraggable(true);

    this.firstNameField.setRequired(true);
    this.firstNameField.setRequiredIndicatorVisible(true);
    this.firstNameField.setErrorMessage("First name is required");
    this.lastNameField.setRequired(true);
    this.lastNameField.setRequiredIndicatorVisible(true);
    this.lastNameField.setErrorMessage("Last name is required");
    this.emailField.setRequired(true);
    this.emailField.setRequiredIndicatorVisible(true);
    this.emailField.setErrorMessage("Email is required");

    var layout = new FormLayout();
    layout.setAutoResponsive(true);
    layout.setExpandColumns(true);
    layout.setExpandFields(true);

    var nameRow = new FormRow();
    nameRow.add(this.firstNameField, this.lastNameField);

    var emailRow = new FormRow();
    emailRow.add(this.emailField, 2);

    layout.add(nameRow, emailRow);
    add(layout);

    var cancelButton = new Button("Cancel", event -> close());
    var okButton = new Button("Ok", event -> handleOkButtonClick());
    okButton.addClickShortcut(Key.ENTER);
    okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    var buttonLayout = new HorizontalLayout(okButton, cancelButton);
    buttonLayout.getStyle().setFlexWrap(FlexWrap.WRAP);
    buttonLayout.setJustifyContentMode(JustifyContentMode.END);

    getFooter().add(buttonLayout);
    createBindings();

    this.firstNameField.focus();
  }

  private void createBindings() {
    this.binder.setBean(this.player);
    this.binder.bind(this.firstNameField, "firstName");
    this.binder.bind(this.lastNameField, "lastName");
    this.binder.bind(this.emailField, "email");
  }

  private void handleOkButtonClick() {
    if (this.binder.writeBeanIfValid(this.player)) {
      this.newPlayer = this.player.toDto();
      close();
    }
  }

  public Optional<PlayerDto> getPlayer() {
    return Optional.ofNullable(this.newPlayer);
  }

  public static final class PlayerForm {
    @NotEmpty(message = "First name is required")
    private String firstName;

    @NotEmpty(message = "Last name is required")
    private String lastName;

    @Email(message = "Invalid email address")
    @NotEmpty(message = "Email is required")
    private String email;

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getFirstName() {
      return firstName;
    }

    public void setFirstName(String firstName) {
      this.firstName = firstName;
    }

    public String getLastName() {
      return lastName;
    }

    public void setLastName(String lastName) {
      this.lastName = lastName;
    }

    public static PlayerForm fromDto(PlayerDto dto) {
      var form = new PlayerForm();
      form.setEmail(dto.email());
      form.setFirstName(dto.firstName());
      form.setLastName(dto.lastName());
      return form;
    }

    public PlayerDto toDto() {
      return new PlayerDto(this.firstName, this.lastName, this.email);
    }
  }
}
