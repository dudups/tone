package com.ezone.ezproject.modules.card.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AtUsersChange {
    private List<String> atUsers;
    private List<String> cancelAtUsers;

    public boolean isEmpty() {
        return CollectionUtils.isEmpty(atUsers) && CollectionUtils.isEmpty(cancelAtUsers);
    }

    public static AtUsersChange diff(List<String> fromUsers, List<String> toUsers) {
        AtUsersChange atUsersChange = new AtUsersChange();
        if (CollectionUtils.isEmpty(fromUsers)) {
            atUsersChange.atUsers = toUsers;
            return atUsersChange;
        }
        if (CollectionUtils.isEmpty(toUsers)) {
            atUsersChange.cancelAtUsers = fromUsers;
            return atUsersChange;
        }
        atUsersChange.atUsers = ListUtils.subtract(toUsers, fromUsers);
        atUsersChange.cancelAtUsers = ListUtils.subtract(fromUsers, toUsers);
        return atUsersChange;
    }

    public static List<String> subtract(List<String> toUsers, List<String> fromUsers) {
        if (CollectionUtils.isEmpty(toUsers)) {
            return ListUtils.EMPTY_LIST;
        }
        if (CollectionUtils.isEmpty(fromUsers)) {
            return toUsers;
        }
        return ListUtils.subtract(toUsers, fromUsers);
    }
}
