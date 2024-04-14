package com.ezone.ezproject.modules.project.bean;

import com.ezone.ezproject.modules.card.bean.query.Query;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 查询项目过滤条件，指定项目扩展字段值
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ProjectExtendFieldQueryParam {
    private List<Query> queries;
}

