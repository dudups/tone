package com.ezone.ezproject.modules.project.bean;

import com.ezone.ezproject.es.entity.enums.RoleSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RoleKeySource {
    private String role;
    private RoleSource roleSource;
}
