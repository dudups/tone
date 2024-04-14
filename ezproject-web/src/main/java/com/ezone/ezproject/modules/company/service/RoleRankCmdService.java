package com.ezone.ezproject.modules.company.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.Role;
import com.ezone.ezproject.es.entity.RoleSchema;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.modules.company.rank.RankLocation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ezone.ezproject.modules.card.service.CardHelper.RANKER;

@Service
@Slf4j
@AllArgsConstructor
public class RoleRankCmdService {
    public static final String START_RANK = "001";

    public static final String COMPANY_FIRST_ROLE_DEFAULT_RANK = "010";

    public static final int RANK_LENGTH = START_RANK.length();


    public RoleSchema roleRank(RoleSchema roleSchema, String roleKey, String referenceRoleKey, RankLocation location, RoleSource rankSourceType) {
        List<Role> roles = roleSchema.toBaseRoles();
        Role changeRole = null;
        Role referenceRole = null;
        for (Role role : roles) {
            if (role.getSource().equals(rankSourceType) && role.getKey().equals(roleKey)) {
                changeRole = role;
            }
            if (role.getSource().equals(rankSourceType) && role.getKey().equals(referenceRoleKey)) {
                referenceRole = role;
            }
        }
        if (changeRole == null || referenceRole == null) {
            throw new CodedException(HttpStatus.NOT_FOUND, "未找到角色！");
        }
        String rank = rank(roleSchema, referenceRole.getRank(), location, rankSourceType);
        changeRole.setRank(rank);

        sortCustomRole(roleSchema, rankSourceType);
        return roleSchema;
    }

    private String rank(RoleSchema roleSchema, String referenceRank, RankLocation location, RoleSource rankSourceType) {
        String start;
        String end;
        switch (location) {
            case HIGHER:
                start = referenceRank;
                end = higherRank(roleSchema, referenceRank, rankSourceType);
                break;
            case LOWER:
            default:
                end = referenceRank;
                start = lowerRank(roleSchema, referenceRank, rankSourceType);
        }
        return RANKER.ranks(start, end, 1).get(0);
    }

    private void sortCustomRole(RoleSchema roleSchema, RoleSource sortType) {
        List<Role> roles = roleSchema.toBaseRoles();
        if (CollectionUtils.isEmpty(roles)) {
            return;
        }
        List<Role> sortRolesByRank = new ArrayList<>();
        Map<RoleSource, List<Role>> groupBySource = roles.stream().collect(Collectors.groupingBy(Role::getSource));
        for (RoleSource source : RoleSource.values()) {
            List<Role> sourceRoles = groupBySource.get(source);
            if (CollectionUtils.isEmpty(sourceRoles)) {
                continue;
            }
            if (sortType == source) {
                List<Role> sortedRoles = sourceRoles
                        .stream()
                        .sorted((o1, o2) -> StringUtils.compare(o1.getRank(), o2.getRank()))
                        .collect(Collectors.toList());
                sortRolesByRank.addAll(sortedRoles);
            } else {
                sortRolesByRank.addAll(sourceRoles);
            }
        }
        roleSchema.resetRoles(sortRolesByRank);
    }

    @NotNull
    protected String higherRank(RoleSchema roleSchema, String referenceRank, RoleSource rankSourceType) {
        List<Role> roles = roleSchema.toBaseRoles();
        String higherMin = null;
        for (int i = 0; i < roles.size(); i++) {
            Role role = roles.get(i);
            boolean isTargetSource = role.getSource() == rankSourceType;
            boolean isGtReference = StringUtils.compare(role.getRank(), referenceRank, false) > 0;
            boolean isLtHigherMin = StringUtils.compare(role.getRank(), higherMin, false) < 0;
            if (isTargetSource && isGtReference && isLtHigherMin) {
                higherMin = role.getRank();
            }
        }
        if (higherMin == null) {
            higherMin = nextRank(roleSchema);
        }
        return higherMin;
    }

    @NotNull
    protected String lowerRank(RoleSchema companyRoleSchema, String referenceRank, RoleSource rankSourceType) {
        List<Role> roles = companyRoleSchema.toBaseRoles();
        String lowerMax = null;
        for (int i = 0; i < roles.size(); i++) {
            Role role = roles.get(i);
            boolean isTargetSource = role.getSource() == rankSourceType;
            boolean isLtReference = StringUtils.compare(role.getRank(), referenceRank, false) < 0;
            boolean isGtLowerMax = StringUtils.compare(role.getRank(), lowerMax, true) > 0;
            if (isTargetSource && isLtReference && isGtLowerMax) {
                lowerMax = role.getRank();
            }
        }
        if (lowerMax == null) {
            lowerMax = START_RANK;
        }
        return lowerMax;
    }


    public String nextRank(RoleSchema roleSchema) {
        String maxRank = roleSchema.getMaxRank();
        if (StringUtils.isBlank(maxRank) || COMPANY_FIRST_ROLE_DEFAULT_RANK.compareTo(maxRank) > 0) {
            maxRank = COMPANY_FIRST_ROLE_DEFAULT_RANK;
        }
        roleSchema.setMaxRank(RANKER.next(maxRank, RANK_LENGTH));
        return roleSchema.getMaxRank();
    }
}
