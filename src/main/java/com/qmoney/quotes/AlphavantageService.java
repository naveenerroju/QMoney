package com.qmoney.quotes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.qmoney.dto.AlphavantageCandle;
import com.qmoney.dto.AlphavantageDailyResponse;
import com.qmoney.dto.Candle;
import org.springframework.web.client.RestTemplate;

public class AlphavantageService implements StockQuotesService {

    public static final String token = "";
    public static final String function = "TIME_SERIES_DAILY";
    private RestTemplate restTemplate;

    public AlphavantageService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
            throws JsonProcessingException {
        String url = buildUri(symbol);
        String apiResponse = restTemplate.getForObject(url, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        Map<LocalDate, AlphavantageCandle> dailyResponse =
                objectMapper.readValue(apiResponse, AlphavantageDailyResponse.class).getCandles();

        List<Candle> stocks = new ArrayList<>();

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            AlphavantageCandle candle = dailyResponse.get(date);
            if (candle != null) {

                candle.setDate(date);
                stocks.add(candle);
            }
        }
        return stocks.stream().sorted(Comparator.comparing(Candle::getDate)).toList();

    }

    protected String buildUri(String symbol) {
        return "https://www.alphavantage.co/query?function=" + function + "&symbol=" + symbol
                + "&output=full&apikey=" + token;

    }

}

