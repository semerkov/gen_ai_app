package com.epam.training.gen.ai.plugin.currencyExchangeRate;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CurrencyExchangeRateResponseDto {

    private String table;
    private String currency;
    private String code;
    private List<CurrencyExchangeItemResponseDto> rates;
}
