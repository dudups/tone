package com.ezone.ezproject.modules.portfolio.service;

import com.ezone.ezproject.dal.entity.PortfolioFavourite;
import com.ezone.ezproject.dal.entity.PortfolioFavouriteExample;
import com.ezone.ezproject.dal.mapper.PortfolioFavouriteMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
@AllArgsConstructor
public class PortfolioFavouriteQueryService {
    private PortfolioFavouriteMapper portfolioFavouriteMapper;

    public @NotNull List<Long> selectFavouritePortfolioIds(Long company, String user) {
        PortfolioFavouriteExample example = new PortfolioFavouriteExample();
        example.createCriteria().andCompanyIdEqualTo(company).andCreateUserEqualTo(user);
        List<PortfolioFavourite> favourites = portfolioFavouriteMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(favourites)) {
            return ListUtils.EMPTY_LIST;
        }
        return favourites.stream().map(PortfolioFavourite::getPortfolioId).collect(Collectors.toList());
    }

    public boolean exist(String user, Long portfolioId) {
        PortfolioFavouriteExample example = new PortfolioFavouriteExample();
        example.createCriteria().andCreateUserEqualTo(user).andPortfolioIdEqualTo(portfolioId);
        return portfolioFavouriteMapper.countByExample(example) > 0;
    }
}
