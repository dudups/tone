package com.ezone.ezproject.common.storage;

import org.apache.commons.lang3.StringUtils;

public class StoragePathUtil {
    public final static String SEP = "/";

    public static String join(String... paths) {
        return StringUtils.joinWith(SEP, paths);
    }
}
