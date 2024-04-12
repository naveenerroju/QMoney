package com.qmoney;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qmoney.dto.*;
import com.qmoney.exception.UncaughtExceptionHandler;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class PortfolioManagerApplication {

    private static String token = "5be7d004aae4c605d552e0bc77221b1a35029869";

    public static String getToken() {
        return token;
    }

    public static List<String> debugOutputs() {

        String valueOfArgument0 = "trades.json";
        String resultOfResolveFilePathArgs0 =
                "resources/sampletrades.json";
        String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@6150c3ec";
        String functionNameFromTestFileInStackTrace = "mainReadFile";
        String lineNumberFromTestFileInStackTrace = "50";

        return Arrays.asList(
                valueOfArgument0, resultOfResolveFilePathArgs0, toStringOfObjectMapper,
                functionNameFromTestFileInStackTrace, lineNumberFromTestFileInStackTrace);
    }

    public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {

        String fileName = args[0];
        String historicalDate = args[1];

        RestTemplate restTemplate = new RestTemplate();

        List<TotalReturnsDto> closingPRices = new ArrayList<>();
        List<PortfolioTrade> result = readTradesFromJson(fileName);

        for (PortfolioTrade trade : result) {
            validateDate(trade.getPurchaseDate(), historicalDate);
            String apiUrl = prepareUrl(trade, historicalDate, token);
            TiingoCandle[] candles = restTemplate.getForObject(apiUrl, TiingoCandle[].class);
            if (candles != null) {
                closingPRices
                        .add(new TotalReturnsDto(trade.getSymbol(), candles[candles.length - 1].getClose()));
            }
        }

        closingPRices.sort((c1, c2) -> c1.getClosingPrice().compareTo(c2.getClosingPrice()));
        return closingPRices.stream().map(TotalReturnsDto::getSymbol).toList();
    }

    public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
            throws Exception {
        return mainCalculateSingleReturn(args);
    }


    public static void validateDate(LocalDate purchaseDate, String historical) {
        LocalDate historicalDate = LocalDate.parse(historical);
        if (!purchaseDate.isBefore(historicalDate)) {
            throw new RuntimeException("Invalid DateTime");
        }
    }

    public static void validateDate(LocalDate purchaseDate, LocalDate historical) {
        if (!purchaseDate.isBefore(historical)) {
            throw new RuntimeException("Invalid DateTime");
        }
    }

    public static List<PortfolioTrade> readTradesFromJson(String filename)
            throws IOException, URISyntaxException {
        ObjectMapper objectMapper = getObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        File file = resolveFileFromResources(filename);
        return getObjectMapper().readValue(file, new TypeReference<List<PortfolioTrade>>() {
        });

    }

    public static String prepareUrl(PortfolioTrade trade, String endDate, String tokens) {
        return "https://api.tiingo.com/tiingo/daily/" + trade.getSymbol() + "/prices?startDate="
                + trade.getPurchaseDate().format(DateTimeFormatter.ISO_DATE) + "&endDate="
                + endDate + "&token=" + tokens;

    }

    public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String tokens) {
        return "https://api.tiingo.com/tiingo/daily/" + trade.getSymbol() + "/prices?startDate="
                + trade.getPurchaseDate().format(DateTimeFormatter.ISO_DATE).toString() + "&endDate="
                + endDate.toString() + "&token=" + tokens;

    }

    private static File resolveFileFromResources(String filename) throws URISyntaxException {
        return Paths.get(Thread.currentThread().getContextClassLoader().getResource(filename).toURI())
                .toFile();
    }

    public static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    public static LocalDate stringToDate(String dateString) {
        return LocalDate.parse(dateString);
    }

    static Double getOpeningPriceOnStartDate(List<Candle> candles) {
        return candles.get(0).getOpen();
    }


    public static Double getClosingPriceOnEndDate(List<Candle> candles) {
        return candles.get(candles.size() - 1).getClose();
    }

    public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
        RestTemplate restTemplate = new RestTemplate();
        String apiUrl = prepareUrl(trade, endDate, token);
        TiingoCandle[] candles = restTemplate.getForObject(apiUrl, TiingoCandle[].class);
        return Arrays.asList(candles);
    }

    public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
            throws IOException, URISyntaxException {
        String fileName = args[0];
        String historicalDate = args[1];

        RestTemplate restTemplate = new RestTemplate();

        List<AnnualizedReturn> annualisedReturn = new ArrayList<>();
        List<PortfolioTrade> result = readTradesFromJson(fileName);

        for (PortfolioTrade trade : result) {
            validateDate(trade.getPurchaseDate(), historicalDate);
            String url = prepareUrl(trade, historicalDate, token);
            TiingoCandle[] candle = restTemplate.getForObject(url, TiingoCandle[].class);
            annualisedReturn
                    .add(calculateAnnualizedReturns(LocalDate.parse(historicalDate), trade, candle[0].getOpen(), candle[candle.length - 1].getClose()));

        }

        annualisedReturn.sort((c1, c2) -> c2.getAnnualizedReturn().compareTo(c1.getAnnualizedReturn()));

        return annualisedReturn;
    }

    public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade,
                                                              Double buyPrice, Double sellPrice) {
        double numberOfDays = (double) ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate);
        double numberOfYears = numberOfDays / 365;
        Double totalReturn = (sellPrice - buyPrice) / buyPrice;
        Double annualizedReturn = Math.pow(1 + totalReturn, 1.0 / numberOfYears) - 1;
        return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
    }

    private static void printJsonObject(Object object) throws IOException {
        Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
        ObjectMapper mapper = new ObjectMapper();
        logger.info(mapper.writeValueAsString(object));
    }

    public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
        String fileName = args[0];

        List<String> tradesList = new ArrayList<>();
        PortfolioTrade[] result = getObjectMapper().readValue(resolveFileFromResources(fileName), PortfolioTrade[].class);

        for (PortfolioTrade trade : result) {
            tradesList.add(trade.getSymbol());
        }

        return tradesList;
    }


    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
        ThreadContext.put("runId", UUID.randomUUID().toString());

    }
}