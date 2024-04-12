package com.qmoney.quotes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.qmoney.PortfolioManagerApplication;
import com.qmoney.dto.Candle;
import com.qmoney.dto.TiingoCandle;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {

    private RestTemplate restTemplate;

    protected TiingoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public  List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
            throws JsonProcessingException {
        PortfolioManagerApplication.validateDate(from, to);
        String url = prepareUrl(symbol, from, to, PortfolioManagerApplication.getToken());
        String urlResponse =   restTemplate.getForObject(url, String.class);
        ObjectMapper objectmapper =  new ObjectMapper().registerModule(new JavaTimeModule());

        TiingoCandle[] candles =  objectmapper.readValue(urlResponse, TiingoCandle[].class);
        //candles is becoming null.
        return Arrays.asList(candles);
    }

    private String prepareUrl(String trade, LocalDate from, LocalDate to, String tokens) {
        return "https://api.tiingo.com/tiingo/daily/" + trade + "/prices?startDate="
                + from.toString() + "&endDate="
                + to.toString() + "&token=" + tokens;
    }

}
