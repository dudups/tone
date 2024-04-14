package com.ezone.ezproject.modules.card.bpm.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.dao.CardBpmFlowDao;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bean.CardBpmFlowBean;
import com.ezone.ezproject.modules.card.bean.CardBpmFlowsBean;
import com.ezone.ezproject.modules.card.bpm.bean.CardBpmFlow;
import com.ezone.ezproject.modules.cli.EzBpmCliService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class CardBpmQueryService {
    private CardBpmFlowDao cardBpmFlowDao;

    private EzBpmCliService ezBpmCliService;

    private UserService userService;

    private CompanyService companyService;

    public CardBpmFlowsBean cardBpmFlows(Long cardId) throws IOException {
        CardBpmFlowsBean.CardBpmFlowsBeanBuilder builder = CardBpmFlowsBean.builder();
        List<CardBpmFlow> flows = cardBpmFlowDao.searchByCardId(cardId);
        if (CollectionUtils.isNotEmpty(flows)) {
            List<CardBpmFlow> sortedFlows = flows.stream().sorted(Comparator.comparing(CardBpmFlow::getDate).reversed()).collect(Collectors.toList());
            builder.flows(sortedFlows);
            builder.details(ezBpmCliService.getFlows(sortedFlows.stream().map(flow -> flow.getFlowId()).collect(Collectors.toList()), companyService.currentCompany(), userService.currentUserName()));
        }
        return builder.build();
    }

    public CardBpmFlowBean cardBpmFlowBean(Long cardId, Long flowId) throws IOException {
        CardBpmFlow flow = cardBpmFlowDao.searchByFlowId(flowId);
        if (flow == null || !cardId.equals(flow.getCardId())) {
            throw CodedException.NOT_FOUND;
        }
        return CardBpmFlowBean.builder()
                .flow(flow)
                .detail(ezBpmCliService.getFlow(flowId))
                .build();
    }

    public CardBpmFlow cardBpmFlow(Long cardId, Long flowId) throws IOException {
        CardBpmFlow flow = cardBpmFlowDao.searchByFlowId(flowId);
        if (flow != null && flow.getCardId().equals(cardId)) {
            return flow;
        }
        return null;
    }

    public Map<Long, CardBpmFlow> cardBpmFlows(List<Long> flowIds) throws IOException {
        Map<Long, CardBpmFlow> result = new HashMap<>();
        if (CollectionUtils.isEmpty(flowIds)) {
            return result;
        }
        List<CardBpmFlow> flows = cardBpmFlowDao.searchByFlowIds(flowIds, 200);
        if (CollectionUtils.isNotEmpty(flows)) {
            for (CardBpmFlow flow : flows) {
                result.put(flow.getFlowId(), flow);
            }
        }
        return result;
    }

}
