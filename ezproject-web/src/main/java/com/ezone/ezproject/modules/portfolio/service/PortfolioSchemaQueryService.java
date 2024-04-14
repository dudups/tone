package com.ezone.ezproject.modules.portfolio.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.dao.PortfolioRoleSchemaDao;
import com.ezone.ezproject.es.entity.PortfolioRoleSchema;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@Slf4j
@AllArgsConstructor
public class PortfolioSchemaQueryService {
    private PortfolioRoleSchemaDao portfolioRoleSchemaDao;

    public PortfolioRoleSchema getPortfolioRoleSchema(Long portfolioId) {
        try {
            return portfolioRoleSchemaDao.find(portfolioId);
        } catch (IOException e) {
            log.error(String.format("getPortfolioRoleSchema for portfolioId:[%s] exception!", portfolioId), e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
