package no.fintlabs.operator.application.deployment;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImagePullSecret {
    @JsonPropertyDescription("The name of the image pull secret.")
    private String name;

    @JsonPropertyDescription("Indicates if the operator should manage the creation of the image pull secret.")
    private Boolean managed = true;
}

