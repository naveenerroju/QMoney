package com.qmoney.portfolio;

import com.qmoney.dto.AnnualizedReturn;
import com.qmoney.dto.PortfolioTrade;
import com.qmoney.exception.StockQuoteServiceException;

import java.time.LocalDate;
import java.util.List;

public interface PortfolioManager {


    List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
                                                     LocalDate endDate)
            throws StockQuoteServiceException;
}
