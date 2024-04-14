package com.ezone.ezproject.common;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.core.TimeValue;
// import org.elasticsearch.common.unit.TimeValue;

public class EsUtil {
    public static final RequestOptions REQUEST_OPTIONS = RequestOptions.DEFAULT;

    public static final TimeValue TIME_OUT = TimeValue.timeValueSeconds(5);
}
