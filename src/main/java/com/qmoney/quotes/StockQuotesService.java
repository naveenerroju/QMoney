package com.qmoney.quotes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qmoney.dto.Candle;
import com.qmoney.exception.StockQuoteServiceException;

import java.time.LocalDate;
import java.util.List;

public interface StockQuotesService {

    List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
            throws JsonProcessingException, StockQuoteServiceException;

}