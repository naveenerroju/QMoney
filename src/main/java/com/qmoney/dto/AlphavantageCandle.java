package com.qmoney.dto;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AlphavantageCandle implements Candle {
    @JsonProperty("1. open")
    private Double open;
    @JsonProperty("2. high")
    private Double high;
    @JsonProperty("3. low")
    private Double low;
    @JsonProperty("4. close")
    private Double close;
    @JsonIgnore
    private LocalDate date;
    public Double getOpen() {
        return open;
    }
    public Double getClose() {
        return close;
    }
    public Double getHigh() {
        return high;
    }
    public Double getLow() {
        return low;
    }
    public LocalDate getDate() {
        return date;
    }
    public void setOpen(Double open) {
        this.open = open;
    }
    public void setClose(Double close) {
        this.close = close;
    }
    public void setHigh(Double high) {
        this.high = high;
    }
    public void setLow(Double low) {
        this.low = low;
    }
    public void setDate(LocalDate date) {
        this.date = date;
    }

}
