package com.ezone.ezproject.common.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 使用场景：满足下面两个条件可以使用 </br>
 * 1. 弥补使用list或数组直接作为@RequestBody参数在接口升级时，增加参数不兼容老版本api的问题。</br>
 * 2. 同时又不想专门为请求创建一个专有对象用来接收list类型的参数。</br>
 * 当接口升级需添加RequestBody参数时，可再创建新类代替该类，并包含list属性，并添加其他参数即可实现兼容原先参数。
 * @param <T>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListRequestBody<T> {
    @NotNull
    private List<T> list;
}
