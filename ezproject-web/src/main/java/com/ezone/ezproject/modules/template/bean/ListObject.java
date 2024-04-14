package com.ezone.ezproject.modules.template.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListObject<T> {
    @NotNull
    private List<T> list;
}
