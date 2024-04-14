package com.ezone.ezproject.modules.card.bpm.service;

import com.ezone.ezproject.es.dao.CardIncrWorkloadDao;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bean.CardWorkloadBpmFlowBean;
import com.ezone.ezproject.modules.card.event.model.CardIncrWorkload;
import com.ezone.ezproject.modules.cli.EzBpmCliService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class CardWorkloadBpmQueryService {
    private CardIncrWorkloadDao cardIncrWorkloadDao;

    private EzBpmCliService ezBpmCliService;

    private UserService userService;

    private CompanyService companyService;

    public List<CardIncrWorkload> cardIncrWorkloads(Long cardId) throws IOException {
        if (cardId == null || cardId <= 0) {
            return ListUtils.EMPTY_LIST;
        }
        List<CardIncrWorkload> flows = cardIncrWorkloadDao.searchByCardId(cardId);
        return flows;
    }

    public CardWorkloadBpmFlowBean cardWorkloadBpmFlowBean(Long cardId, Long workloadId) throws IOException {
        CardIncrWorkload workload = cardWorkload(cardId, workloadId);
        return CardWorkloadBpmFlowBean.builder()
                .workload(workload)
                .detail(ezBpmCliService.getFlow(workload.getFlowId()))
                .build();
    }

    public CardIncrWorkload cardWorkload(Long cardId, Long workloadId) throws IOException {
        if (workloadId == null || workloadId <= 0 || cardId == null || cardId <= 0) {
            return null;
        }
        CardIncrWorkload workload = cardIncrWorkloadDao.find(workloadId);
        if (workload != null && workload.getCardId().equals(cardId)) {
            return workload;
        }
        return null;
    }

    public CardIncrWorkload cardIncrWorkload(Long cardId, Long flowId) throws IOException {
        if (flowId == null || flowId <= 0 || cardId == null || cardId <= 0) {
            return null;
        }
        CardIncrWorkload workload = cardIncrWorkloadDao.searchByFlowId(flowId);
        if (workload != null && workload.getCardId().equals(cardId)) {
            return workload;
        }
        return null;
    }

    public Map<Long, CardIncrWorkload> cardIncrWorkloads(List<Long> flowIds) throws IOException {
        Map<Long, CardIncrWorkload> result = new HashMap<>();
        if (CollectionUtils.isEmpty(flowIds)) {
            return result;
        }
        List<CardIncrWorkload> flows = cardIncrWorkloadDao.searchByFlowIds(flowIds, 200);
        if (CollectionUtils.isNotEmpty(flows)) {
            for (CardIncrWorkload flow : flows) {
                result.put(flow.getFlowId(), flow);
            }
        }
        return result;
    }

    public CardIncrWorkload cardRevertWorkload(Long cardId, Long flowId) throws IOException {
        CardIncrWorkload workload = cardIncrWorkloadDao.searchByRevertFlowId(flowId);
        if (workload != null && workload.getCardId().equals(cardId)) {
            return workload;
        }
        return null;
    }

}
