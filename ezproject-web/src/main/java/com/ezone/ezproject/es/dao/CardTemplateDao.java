package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.es.util.EsIndexUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class CardTemplateDao extends CardDao {

    @Override
    protected String index() {
        return EsIndexUtil.indexForCardTemplate();
    }

}
