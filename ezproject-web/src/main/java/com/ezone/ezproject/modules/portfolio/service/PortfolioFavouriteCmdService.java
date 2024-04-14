package com.ezone.ezproject.modules.portfolio.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.dal.entity.Portfolio;
import com.ezone.ezproject.dal.entity.PortfolioFavourite;
import com.ezone.ezproject.dal.entity.PortfolioFavouriteExample;
import com.ezone.ezproject.dal.mapper.PortfolioFavouriteMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
@AllArgsConstructor
public class PortfolioFavouriteCmdService {
    private PortfolioFavouriteMapper projectFavouriteMapper;
    private PortfolioFavouriteQueryService projectFavouriteQueryService;
    private PortfolioQueryService projectQueryService;

    public void favouritePortfolio(String user, Long portfolioId) {
        Portfolio project = projectQueryService.select(portfolioId);
        if (projectFavouriteQueryService.exist(user, portfolioId)) {
            return;
        }
        projectFavouriteMapper.insert(PortfolioFavourite.builder()
                .id(IdUtil.generateId())
                .createUser(user)
                .createTime(new Date())
                .companyId(project.getCompanyId())
                .portfolioId(portfolioId)
                .build());
    }

    public void unFavouritePortfolio(String user, Long portfolioId) {
        PortfolioFavouriteExample example = new PortfolioFavouriteExample();
        example.createCriteria().andCreateUserEqualTo(user).andPortfolioIdEqualTo(portfolioId);
        projectFavouriteMapper.deleteByExample(example);
    }

    public void deleteByPortfolioId(Long portfolioId) {
        PortfolioFavouriteExample example = new PortfolioFavouriteExample();
        example.createCriteria().andPortfolioIdEqualTo(portfolioId);
        projectFavouriteMapper.deleteByExample(example);
    }

}