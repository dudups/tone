package com.ezone.ezproject.modules.card.copy;

import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.mapper.CardMapper;
import com.ezone.ezproject.es.dao.CardDao;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Slf4j
public abstract class AbstractCardMigrate {
    protected String user;
    protected List<Card> cards;
    protected Plan targetPlan;
    protected CardMapper cardMapper;
    protected CardDao cardDao;

    public abstract void run() throws IOException;
}
