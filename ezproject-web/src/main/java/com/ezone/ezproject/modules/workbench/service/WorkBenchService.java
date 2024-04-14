package com.ezone.ezproject.modules.workbench.service;

import com.ezone.ezproject.configuration.CacheManagers;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author cf
 */
@Service
@Slf4j
@AllArgsConstructor
public class WorkBenchService {
    public static final String[] DEFAULT_CARD_FIELD = new String[]{CardField.COMPANY_ID, CardField.PROJECT_ID, CardField.PLAN_ID, CardField.TITLE,
            CardField.TYPE, CardField.STATUS, CardField.START_DATE, CardField.END_DATE, CardField.CREATE_TIME, CardField.CREATE_USER};

    protected UserService userService;


    @Cacheable(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:WorkBenchService.getSearchCardRequest"},
            key = "#userId",
            unless = "#result == null"
    )
    public SearchEsRequest getSearchCardRequest(Long userId) {
        String userName = userService.currentUserName();
        return SearchEsRequest.builder()
                .queries(Arrays.asList(Eq.builder().field(CardField.OWNER_USERS).value(userName).build(), Eq.builder().field(CardField.PLAN_IS_ACTIVE).value("true").build()))
                .sorts(Collections.singletonList(SearchEsRequest.Sort.builder().field(CardField.CREATE_TIME).order(SortOrder.DESC).build()))
                .build();
    }


    @CachePut(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:WorkBenchService.getSearchCardRequest"},
            key = "#userId",
            unless = "#result == null"
    )
    public SearchEsRequest cacheUserQuery(Long userId, SearchEsRequest request) {
        return request;
    }

    @CacheEvict(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:WorkBenchService.getSearchCardRequest"},
            allEntries = true
    )
    public void clearAllCache() {
    }

}
