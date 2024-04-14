package com.ezone.ezproject.modules.portfolio.service;

import com.ezone.ezproject.es.dao.PortfolioConfigDao;
import com.ezone.ezproject.es.entity.PortfolioConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
@AllArgsConstructor
public class PortfolioConfigService {
    private PortfolioConfigDao portfolioConfigDao;

    public void saveOrUpdate(Long portfolioId, PortfolioConfig config) throws IOException {
        portfolioConfigDao.saveOrUpdate(portfolioId, config);
    }

    public PortfolioConfig getConfig(Long portfolioId) throws IOException {
        PortfolioConfig portfolioConfig = portfolioConfigDao.find(portfolioId);
        if (portfolioConfig == null) {
            portfolioConfig = PortfolioConfig.builder().chartContainDescendant(false).build();
        }
        return portfolioConfig;
    }
}
