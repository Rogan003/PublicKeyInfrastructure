package org.example.publickeyinfrastructure.DTOs.Auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPotentialOwnerDTO {
    private Long id;
    private String nameWithOrg;
}
