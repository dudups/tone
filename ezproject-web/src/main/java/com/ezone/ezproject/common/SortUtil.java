package com.ezone.ezproject.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author cf
 */
public class SortUtil {

    private SortUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static <OBJECT, KEY> List<OBJECT> sortByIds(List<KEY> ids, List<OBJECT> list, Function<OBJECT, KEY> getIdFunction) {
        Map<KEY, OBJECT> addMap = new HashMap<>(ids.size());
        for (OBJECT obj : list) {
            addMap.put(getIdFunction.apply(obj), obj);
        }
        List<OBJECT> sortList = new ArrayList<>(list.size());
        for (KEY id : ids) {
            OBJECT rel = addMap.get(id);
            if (rel != null) {
                sortList.add(rel);
            }
        }
        return sortList;
    }
}
