package com.qmoney;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qmoney.dto.AnnualizedReturn;
import com.qmoney.dto.Candle;
import com.qmoney.dto.PortfolioTrade;
import com.qmoney.dto.TiingoCandle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PortfolioManagerApplicationTest {

    private final String googlQuotes = "[{\"date\":\"2019-01-02T00:00:00.000Z\",\"close\":1054.68,"
            + "\"high\":1060.79,\"low\":1025.28,\"open\":1027.2,\"volume\":1593395,\"adjClose\":1054.68,"
            + "\"adjHigh\":1060.79,\"adjLow\":1025.28,\""
            + "adjOpen\":1027.2,\"adjVolume\":1593395,\"divCash\""
            + ":0.0,\"splitFactor\":1.0},{\"date\":\""
            + "2019-01-03T00:00:00.000Z\",\"close\":1025.47,\"high\""
            + ":1066.26,\"low\":1022.37,\"open\":1050.67,\"volume\":2097957,\"adjClose\":1025.47,"
            + "\"adjHigh\":1066.26,\"adjLow\":1022.37,\"adjOpen\":1050.67,\"adjVolume\":2097957,"
            + "\"divCash\":0.0,\"splitFactor\":1.0},{\"date\":\"2019-12-12T00:00:00.000Z\","
            + "\"close\":1348.49,\"high\":1080.0,\"low\":1036.86,\"open\":1042.56,\"volume\":2301428,"
            + "\"adjClose\":1078.07,\"adjHigh\":1080.0,\"adjLow\":1036.86,\"adjOpen\":1042.56,\"adjVolume"
            + "\":2301428,\"divCash\":0.0,\"splitFactor\":1.0}]";

    @Test
    void mainReadFile() throws Exception {
        //given
        String filename = "assessments/trades.json";
        List<String> expected = Arrays.asList("MSFT", "CSCO", "CTS");

        //when
        List<String> results = PortfolioManagerApplication
                .mainReadFile(new String[]{filename});

        //then
        Assertions.assertEquals(expected, results);
    }

    @Test
    void mainReadFileEdgecase() throws Exception {
        //given
        String filename = "assessments/empty.json";
        List<String> expected = List.of();

        //when
        List<String> results = PortfolioManagerApplication
                .mainReadFile(new String[]{filename});

        //then
        Assertions.assertEquals(expected, results);
    }


    private List<Candle> getCandles(String responseText) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return Arrays.asList(mapper.readValue(responseText, TiingoCandle[].class));
    }

    @Test
    void getOpeningPriceOnStartDate() throws JsonProcessingException {
        Double price = PortfolioManagerApplication.getOpeningPriceOnStartDate(getCandles(googlQuotes));
        Assertions.assertEquals(1027.2, price, 0.1);
    }

    @Test
    void getClosingPriceOnEndDate() throws JsonProcessingException {
        Double price = PortfolioManagerApplication.getClosingPriceOnEndDate(getCandles(googlQuotes));
        Assertions.assertEquals(1348.49, price, 0.1);
    }

    @Test
    void fetchCandles() throws JsonProcessingException {
        PortfolioTrade trade = new PortfolioTrade();
        trade.setPurchaseDate(LocalDate.parse("2020-01-01"));
        trade.setSymbol("AAPL");
        List<Candle> candleList = PortfolioManagerApplication.fetchCandles(trade, LocalDate.parse("2020-01-05"),
                PortfolioManagerApplication.getToken());
        Assertions.assertEquals(296.24, candleList.get(0).getOpen(), 0.1);
        Assertions.assertEquals(297.15, candleList.get(candleList.size() - 1).getOpen(), 0.1);
    }


    @Test
    void mainCalculateReturns() throws Exception {
        //given
        String filename = "assessments/trades.json";

        //when
        List<AnnualizedReturn> result = PortfolioManagerApplication
                .mainCalculateSingleReturn(new String[]{filename, "2019-12-12"});

        //then
        List<String> symbols = result.stream().map(AnnualizedReturn::getSymbol)
                .collect(Collectors.toList());
        Assertions.assertEquals(0.556, result.get(0).getAnnualizedReturn(), 0.01);
        Assertions.assertEquals(0.044, result.get(1).getAnnualizedReturn(), 0.01);
        Assertions.assertEquals(0.025, result.get(2).getAnnualizedReturn(), 0.01);
        Assertions.assertEquals(Arrays.asList("MSFT", "CSCO", "CTS"), symbols);
    }

    @Test
    void mainCalculateReturnsEdgeCase() throws Exception {
        //given
        String filename = "assessments/empty.json";

        //when
        List<AnnualizedReturn> result = PortfolioManagerApplication
                .mainCalculateSingleReturn(new String[]{filename, "2019-12-12"});

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void mainCalculateReturnsVaryingDateRanges() throws Exception {
        //given
        String filename = "assessments/trades_invalid_dates.json";
        //when
        List<AnnualizedReturn> result = PortfolioManagerApplication
                .mainCalculateSingleReturn(new String[]{filename, "2019-12-12"});

        //then
        List<String> symbols = result.stream().map(AnnualizedReturn::getSymbol)
                .collect(Collectors.toList());
        Assertions.assertEquals(0.36, result.get(0).getAnnualizedReturn(), 0.01);
        Assertions.assertEquals(0.15, result.get(1).getAnnualizedReturn(), 0.01);
        Assertions.assertEquals(0.02, result.get(2).getAnnualizedReturn(), 0.01);
        Assertions.assertEquals(Arrays.asList("MSFT", "CSCO", "CTS"), symbols);

    }


    @Test
    void mainCalculateReturnsInvalidStocks() throws Exception {
        //given
        String filename = "assessments/trades_invalid_stock.json";
        //when
        Assertions.assertThrows(RuntimeException.class, () -> PortfolioManagerApplication
                .mainCalculateSingleReturn(new String[]{filename, "2019-12-12"}));

    }

    @Test
    void mainCalculateReturnsOldTrades() throws Exception {
        //given
        String filename = "assessments/trades_old.json";

        //when
        List<AnnualizedReturn> result = PortfolioManagerApplication
                .mainCalculateSingleReturn(new String[]{filename, "2019-12-20"});

        //then
        List<String> symbols = result.stream().map(AnnualizedReturn::getSymbol)
                .collect(Collectors.toList());
        Assertions.assertEquals(0.141, result.get(0).getAnnualizedReturn(), 0.01);
        Assertions.assertEquals(0.091, result.get(1).getAnnualizedReturn(), 0.01);
        Assertions.assertEquals(0.056, result.get(2).getAnnualizedReturn(), 0.01);
        Assertions.assertEquals(Arrays.asList("ABBV", "CTS", "MMM"), symbols);
    }


    @Test
    void mainReadQuotes() throws Exception {
        //given
        String filename = "assessments/trades.json";
        List<String> expected = Arrays.asList("CTS", "CSCO", "MSFT");

        //when
        List<String> actual = PortfolioManagerApplication
                .mainReadQuotes(new String[]{filename, "2019-12-12"});

        //then
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void mainReadQuotesEdgeCase() throws Exception {
        //given
        String filename = "assessments/empty.json";
        List<String> expected = List.of();

        //when
        List<String> actual = PortfolioManagerApplication
                .mainReadQuotes(new String[]{filename, "2019-12-12"});

        //then
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void mainReadQuotesInvalidDates() throws Exception {
        //given
        String filename = "assessments/trades_invalid_dates.json";
        //when
        Assertions.assertThrows(RuntimeException.class, () -> PortfolioManagerApplication
                .mainReadQuotes(new String[]{filename, "2017-12-12"}));

    }


    @Test
    void mainReadQuotesInvalidStocks() throws Exception {
        //given
        String filename = "assessments/trades_invalid_stock.json";
        //when
        Assertions.assertThrows(RuntimeException.class, () -> PortfolioManagerApplication
                .mainReadQuotes(new String[]{filename, "2017-12-12"}));

    }

    @Test
    void mainReadQuotesOldTrades() throws Exception {
        //given
        String filename = "assessments/trades_old.json";
        List<String> expected = Arrays.asList("CTS", "ABBV", "MMM");

        //when
        List<String> actual = PortfolioManagerApplication
                .mainReadQuotes(new String[]{filename, "2019-12-12"});

        //then
        Assertions.assertEquals(expected, actual);
    }


    @Test
    void mainCalculateAnnualReturn() throws Exception {
        //given
        String filename = "sampletrades.json";
        //when
        List<AnnualizedReturn> result = PortfolioManagerApplication
                .mainCalculateSingleReturn(new String[]{filename, "2019-12-12"});

        //then
        List<String> symbols = result.stream().map(AnnualizedReturn::getSymbol)
                .collect(Collectors.toList());
        Assertions.assertEquals(0.814, result.get(0).getAnnualizedReturn(), 0.01);
        Assertions.assertEquals(0.584, result.get(1).getAnnualizedReturn(), 0.01);
        Assertions.assertEquals(0.33, result.get(2).getAnnualizedReturn(),0.01);
        Assertions.assertEquals(Arrays.asList("AAPL", "MSFT", "GOOGL"), symbols);

    }

    @Test
    void testCalculateAnnualizedReturn() {
        PortfolioTrade trade = new PortfolioTrade("AAPL", 50, LocalDate.parse("2015-01-01"));
        AnnualizedReturn returns = PortfolioManagerApplication
                .calculateAnnualizedReturns(LocalDate.parse("2018-01-01"),
                        trade, 10000.00, 11000.00);
        Assertions.assertEquals(0.0322, returns.getAnnualizedReturn(), 0.0001);
    }

    @Test
    void testCalculateAnnualizedReturnGoogl() {
        PortfolioTrade trade = new PortfolioTrade("GOOGL", 50, LocalDate.parse("2019-01-02"));
        AnnualizedReturn returns = PortfolioManagerApplication
                .calculateAnnualizedReturns(LocalDate.parse("2019-12-12"),
                        trade, 1054.00, 1348.00);
        Assertions.assertEquals(0.298, returns.getAnnualizedReturn(), 0.001);
    }

    @Test
    void testAllDebugValues() {
        List<String> responses = PortfolioManagerApplication.debugOutputs();
        Assertions.assertTrue(responses.get(0).contains("trades.json"));
        Assertions.assertTrue(responses.get(1).contains("trades.json"));
        Assertions.assertTrue(responses.get(2).contains("ObjectMapper"));
        Assertions.assertTrue(responses.get(3).contains("mainReadFile"));
    }

    @Test
    void testDebugValues() {
        List<String> responses = PortfolioManagerApplication.debugOutputs();
        Assertions.assertTrue(responses.get(0).contains("trades.json"));
    }


}
