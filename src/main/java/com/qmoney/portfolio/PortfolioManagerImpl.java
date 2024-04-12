package com.qmoney.portfolio;


import static java.time.temporal.ChronoUnit.DAYS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qmoney.dto.AnnualizedReturn;
import com.qmoney.dto.Candle;
import com.qmoney.dto.PortfolioTrade;
import com.qmoney.exception.StockQuoteServiceException;
import com.qmoney.quotes.StockQuotesService;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PortfolioManagerImpl implements PortfolioManager {
    RestTemplate restTemplate = new RestTemplate();

    StockQuotesService stockQuotesService;

    // Caution: Do not delete or modify the constructor, or else your build will break!
    // This is absolutely necessary for backward compatibility
    protected PortfolioManagerImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public PortfolioManagerImpl(StockQuotesService stockQuotesService) {
        this.stockQuotesService = stockQuotesService;
    }

    private Comparator<AnnualizedReturn> getComparator() {
        return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
    }

    public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
            throws JsonProcessingException, StockQuoteServiceException {
        return stockQuotesService.getStockQuote(symbol, from, to);
    }


    @Override
    public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
                                                            LocalDate endDate) throws StockQuoteServiceException {

        List<AnnualizedReturn> result = new ArrayList<>();
        for (PortfolioTrade portfolioTrade : portfolioTrades) {
            List<Candle> candles = new ArrayList<>();
            try {
                candles = getStockQuote(portfolioTrade.getSymbol(), portfolioTrade.getPurchaseDate(), endDate);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            Double buyPrice = candles.get(0).getOpen();
            Double sellPrice = candles.get(candles.size() - 1).getClose();
            AnnualizedReturn anAnnualizedReturn = calculateAnnualizedReturn(endDate, portfolioTrade, buyPrice, sellPrice);
            result.add(anAnnualizedReturn);
        }
        return result.stream().sorted(getComparator()).toList();
    }

    private AnnualizedReturn calculateAnnualizedReturn(LocalDate endDate, PortfolioTrade trade,
                                                       Double buyPrice, Double sellPrice) {
        double numberOfYears = DAYS.between(trade.getPurchaseDate(), endDate) / 365.2422;

        Double totalReturn = (sellPrice - buyPrice) / buyPrice;
        Double annualizedReturn = Math.pow(1.0 + totalReturn, 1.0 / numberOfYears) - 1;
        AnnualizedReturn vardebug = new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
        return vardebug;
    }

}
