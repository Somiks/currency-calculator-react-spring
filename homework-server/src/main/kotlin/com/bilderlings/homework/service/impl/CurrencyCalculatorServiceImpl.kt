package com.bilderlings.homework.service.impl

import com.bilderlings.homework.builder.RateEntityBuilder
import com.bilderlings.homework.model.RateEntity
import com.bilderlings.homework.proxy.FixerRatesProxy
import com.bilderlings.homework.repository.CurrencyCalculatorRepository
import com.bilderlings.homework.service.CurrencyCalculatorService
import com.google.common.util.concurrent.RateLimiter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class CurrencyCalculatorServiceImpl(
        private val currencyCalculatorRepository: CurrencyCalculatorRepository,
        private val fixerRatesProxy: FixerRatesProxy
) : CurrencyCalculatorService {
    private final var logger: Logger = LoggerFactory.getLogger(CurrencyCalculatorServiceImpl::class.java)

    override fun getCalculatedRate(fromCurrency: String, toCurrency: String, amount: BigDecimal): BigDecimal {
        val rateAmount = fixerRatesProxy.getRate(fromCurrency, toCurrency);
        return currencyCalculatorRepository
                .findBy(fromCurrency, toCurrency)
                .map { rate -> amount.multiply(rateAmount.minus(rate.fee)) }
                .orElseGet {
                    logger.warn("No fee specified for $fromCurrency and $toCurrency")
                    amount.multiply(rateAmount)
                }
    }

    override fun removeRate(id: Long) {
        currencyCalculatorRepository.deleteById(id);
    }

    override fun listRates(): List<RateEntity> {
        return currencyCalculatorRepository.findAll();
    }

    override fun addOrUpdateRate(rateEntity: RateEntity): RateEntity {
        return currencyCalculatorRepository
                .findBy(rateEntity.fromCurrency, rateEntity.toCurrency)
                .map { rateFromDB ->
                    currencyCalculatorRepository.save(RateEntityBuilder().build(rateFromDB.id, rateEntity))
                }.orElseGet {
                    currencyCalculatorRepository.save(rateEntity);
                }
    }

}