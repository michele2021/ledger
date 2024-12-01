package com.accounting.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Value
@Builder
public class ClientDetailsDTO {

    @NotEmpty
    @Schema(description = "Client email", required = true)
    String email;

    @NotNull
    @Schema(description = "Street address", required = true)
    String streetAddress;

    @NotNull
    @Schema(description = "Telephone Number", required = true)
    String telephoneNumber;

    @NotNull
    @Schema(description = "City name", required = true)
    String cityName;

    @NotNull
    @Schema(description = "Country", required = true)
    String country;

    @NotNull
    @Schema(description = "Contact reference", required = true)
    String contactRef;

    @NotNull
    @Schema(description = "Postal code", required = true)
    String postalCode;
}
