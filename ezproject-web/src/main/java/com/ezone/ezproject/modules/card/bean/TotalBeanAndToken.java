package com.ezone.ezproject.modules.card.bean;

import com.ezone.ezproject.common.bean.TotalBean;
import lombok.Data;

import java.util.Set;

@Data
public class TotalBeanAndToken<E, T> extends TotalBean<E> {
    private Set<T> validAccessTokens;

    public TotalBeanAndToken(TotalBean<E> totalBean, Set<T> validAccessTokens) {
        this.setTotal(totalBean.getTotal());
        this.setList(totalBean.getList());
        this.setRefs(totalBean.getRefs());
        this.validAccessTokens = validAccessTokens;
    }
}
