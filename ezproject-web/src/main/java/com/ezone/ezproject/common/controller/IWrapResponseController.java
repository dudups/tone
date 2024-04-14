package com.ezone.ezproject.common.controller;

import com.ezone.galaxy.framework.common.bean.BaseResponse;
import com.ezone.galaxy.framework.common.bean.HttpCode;

public interface IWrapResponseController {
    BaseResponse SUCCESS_RESPONSE = new BaseResponse<>(HttpCode.SUCCESS, null);

    default <T> BaseResponse<T> success(T t) {
        return new BaseResponse<>(HttpCode.SUCCESS, t);
    }
}
