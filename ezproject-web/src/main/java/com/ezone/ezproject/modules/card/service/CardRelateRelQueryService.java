package com.ezone.ezproject.modules.card.service;

import com.ezone.ezproject.dal.entity.CardRelateRel;
import com.ezone.ezproject.dal.entity.CardRelateRelExample;
import com.ezone.ezproject.dal.mapper.CardRelateRelMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class CardRelateRelQueryService {
    private CardRelateRelMapper cardRelateRelMapper;

    @NotNull
    public List<Long> selectRelateCardIds(Long cardId) {
        CardRelateRelExample example = new CardRelateRelExample();
        example.createCriteria().andCardIdEqualTo(cardId);
        List<CardRelateRel> rels = cardRelateRelMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(rels)) {
            return ListUtils.EMPTY_LIST;
        }
        return rels.stream().map(r -> r.getRelatedCardId()).collect(Collectors.toList());
    }

    public List<CardRelateRel> selectByCardId(List<Long> cardIds) {
        if (CollectionUtils.isEmpty(cardIds)) {
            return ListUtils.EMPTY_LIST;
        }
        CardRelateRelExample example = new CardRelateRelExample();
        example.createCriteria().andCardIdIn(cardIds);
        return cardRelateRelMapper.selectByExample(example);
    }

    @NotNull
    public Map<Long, List<Long>> selectRelateCardIds(List<Long> cardIds) {
        if (CollectionUtils.isEmpty(cardIds)) {
            return MapUtils.EMPTY_MAP;
        }
        CardRelateRelExample example = new CardRelateRelExample();
        example.createCriteria().andCardIdIn(cardIds);
        List<CardRelateRel> rels = selectByCardId(cardIds);
        if (CollectionUtils.isEmpty(rels)) {
            return MapUtils.EMPTY_MAP;
        }
        return rels.stream().collect(Collectors
                .groupingBy(r -> r.getCardId(), Collectors.mapping(r -> r.getRelatedCardId(), Collectors.toList())));
    }
}
