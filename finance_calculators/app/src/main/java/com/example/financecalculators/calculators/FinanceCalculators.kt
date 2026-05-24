package com.example.financecalculators.calculators

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

data class InflationInput(
    val amount: Double,
    val inflationPercent: Double,
    val years: Double
)

data class SwpInput(
    val corpus: Double,
    val swp: Double,
    val swpYears: Double,
    val debtReturnPercent: Double,
    val equityReturnPercent: Double,
    val inflationPercent: Double,
    val retirementAge: Double,
    val targetAge: Double
)

data class InvestmentInput(
    val npsBalance: Double,
    val npsReturnPercent: Double,
    val npsInvestment: Double,
    val debtBalance: Double,
    val debtReturnPercent: Double,
    val debtInvestment: Double,
    val mfBalance: Double,
    val mfReturnPercent: Double,
    val mfInvestment: Double,
    val mfIncrement: Double,
    val mfIncrementAfterLoan: Double,
    val currentAge: Double,
    val retirementAge: Double,
    val inflationPercent: Double,
    val loanTerm: Double,
    val annuityRatePercent: Double,
    val swpRatePercent: Double
)

data class YearlyCorpusPoint(
    val age: Double,
    val totalCorpus: Double,
    val inflationAdjustedCorpus: Double
)

data class InvestmentCalculationResult(
    val lines: List<String>,
    val yearlyProgression: List<YearlyCorpusPoint>
)

object FinanceCalculatorEngine {
    fun calculateInflation(input: InflationInput): List<String> {
        val inflation = input.inflationPercent / 100.0
        val futureValue = input.amount * (1.0 + inflation).pow(input.years)
        val pastValue = input.amount / (1.0 + inflation).pow(input.years)
        val yearsText = formatWholeOrOneDecimal(input.years)
        return listOf(
            "After $yearsText years, the amount should be ${formatForIndia(futureValue)}",
            "$yearsText years ago, the amount would have been ${formatForIndia(pastValue)}"
        )
    }

    fun calculateSwp(input: SwpInput): List<String> {
        val lines = mutableListOf<String>()
        var month = 0
        var index = 0
        var amountNeeded = 0.0
        var corpus = input.corpus
        var swp = input.swp
        val swpYears = input.swpYears
        val debtReturn = input.debtReturnPercent / 100.0
        val equityReturn = input.equityReturnPercent / 100.0
        val inflation = input.inflationPercent / 100.0
        val retirementAge = input.retirementAge
        val targetAge = input.targetAge

        if (targetAge <= retirementAge || swpYears <= 0.0) {
            return listOf("Please keep target age greater than retirement age and SWP years above 0.")
        }

        while (index < swpYears) {
            amountNeeded += (swp * 12.0) * (1.0 + inflation).pow(index.toDouble())
            index++
        }

        var debtPortion = amountNeeded
        if (debtPortion > corpus) {
            debtPortion = corpus
            corpus = 0.0
        } else {
            corpus -= debtPortion
        }

        lines += "Take ${formatForIndia(amountNeeded)} from ${formatForIndia(corpus + debtPortion)} now you have ${formatForIndia(corpus + debtPortion - amountNeeded)} left."

        val retirementMonths = ((targetAge - retirementAge) * 12.0).toInt()
        val cycleMonths = swpYears * 12.0

        while (month < retirementMonths) {
            if (debtPortion + corpus <= 0.0) {
                break
            }

            debtPortion -= swp
            month++

            if (month % 12 == 0) {
                debtPortion += debtPortion * debtReturn
                corpus += corpus * equityReturn
                swp = (((1.0 + inflation).pow(1.0)) * (swp * 12.0)) / 12.0
                lines += "From age ${formatWholeOrOneDecimal(retirementAge + month / 12.0)} onwards your SWP will be ${formatForIndia(swp)}"
            }

            if (isMultiple(month.toDouble(), cycleMonths)) {
                lines += "Debt portion is now ${formatForIndia(debtPortion)} and equity portion is now ${formatForIndia(corpus)}"

                index = 0
                amountNeeded = 0.0
                while (index < swpYears) {
                    amountNeeded += (swp * 12.0) * (1.0 + inflation).pow(index.toDouble())
                    index++
                }

                if (amountNeeded > corpus + debtPortion) {
                    amountNeeded = corpus + debtPortion
                }

                lines += "Take ${formatForIndia(amountNeeded)} from ${formatForIndia(corpus + debtPortion)} now you have ${formatForIndia(corpus + debtPortion - amountNeeded)} left."

                var debtPortionDifference = amountNeeded - debtPortion
                if (debtPortionDifference > corpus) {
                    debtPortionDifference = corpus
                    debtPortion += debtPortionDifference
                    corpus = 0.0
                } else {
                    corpus -= debtPortionDifference
                    debtPortion += debtPortionDifference
                }
            }
        }

        corpus += debtPortion

        if (corpus > 0.0) {
            val years = floor(month / 12.0).toInt()
            val months = month % 12
            lines += "In $years years & $months months your asset will be ${formatForIndia(corpus)}"
            lines += "This is equal to ${formatForIndia(corpus / (1.0 + inflation).pow(years.toDouble()))} when you retired."
        } else {
            month--
            val years = floor(month / 12.0).toInt()
            val months = month % 12
            lines += "Your asset will run out in $years years & $months months"
            lines += "Your total age will be ${years + retirementAge}"
        }

        return lines
    }

    fun calculateInvestment(input: InvestmentInput): InvestmentCalculationResult {
        val lines = mutableListOf<String>()
        val yearlyProgression = mutableListOf<YearlyCorpusPoint>()

        var npsBalance = input.npsBalance
        val npsReturn = input.npsReturnPercent / 100.0
        var npsInvestment = input.npsInvestment

        var debtBalance = input.debtBalance
        val debtReturn = input.debtReturnPercent / 100.0
        val debtInvestment = input.debtInvestment

        var mfBalance = input.mfBalance
        val mfReturn = input.mfReturnPercent / 100.0
        var mfInvestment = input.mfInvestment
        val mfIncrement = input.mfIncrement
        val mfIncrementAfterLoan = input.mfIncrementAfterLoan

        val currentAge = input.currentAge
        val retirementAge = input.retirementAge
        val inflation = input.inflationPercent / 100.0
        val loanTerm = input.loanTerm
        val annuityRate = input.annuityRatePercent / 100.0
        val swpRate = input.swpRatePercent / 100.0

        if (retirementAge <= currentAge) {
            return InvestmentCalculationResult(
                lines = listOf("Please keep retirement age greater than current age."),
                yearlyProgression = emptyList()
            )
        }

        val years = retirementAge - currentAge
        val paymentTermMonths = (years * 12.0).toInt()
        val currentAgeInMonths = currentAge * 12.0

        for (month in 1..paymentTermMonths) {
            npsBalance += npsInvestment
            debtBalance += debtInvestment
            mfBalance += mfInvestment

            val currentMonthAge = currentAgeInMonths + month
            if (isRemainder(currentMonthAge, 12.0, 0.0)) {
                val totalCorpus = npsBalance + mfBalance + debtBalance
                val inflationAdjusted =
                    totalCorpus / (1.0 + inflation).pow(month / 12.0)
                val age = currentMonthAge / 12.0
                yearlyProgression += YearlyCorpusPoint(
                    age = age,
                    totalCorpus = totalCorpus,
                    inflationAdjustedCorpus = inflationAdjusted
                )
                lines += "Value at age ${formatOneDecimal(age)} is ${formatForIndia(totalCorpus)} inflation adjusted: ${formatForIndia(inflationAdjusted)}"
            }

            if (isRemainder(currentMonthAge, 12.0, 4.0)) {
                npsBalance += npsBalance * npsReturn
                mfBalance += mfBalance * mfReturn
                mfInvestment += mfIncrement
                npsInvestment = npsInvestment
            }

            if (month % 3 == 0) {
                debtBalance += debtBalance * (debtReturn / 4.0)
            }

            if (isSameNumber(month.toDouble(), loanTerm)) {
                mfInvestment += mfIncrementAfterLoan
            }
        }

        val finalMonth = paymentTermMonths + 1
        val monthlyAnnuity = ((npsBalance + debtBalance) * annuityRate) / 12.0
        val swp = (mfBalance * swpRate / 12.0)
        val monthlyPension = monthlyAnnuity + swp
        val currentEquivalent = monthlyPension / (1.0 + inflation).pow(years)
        val totalInflationAdjusted =
            (npsBalance + debtBalance + mfBalance) / (1.0 + inflation).pow(finalMonth / 12.0)

        lines += "NPS Corpus ${formatForIndia(npsBalance)}"
        lines += "RD Corpus ${formatForIndia(debtBalance)}"
        lines += "MF Corpus ${formatForIndia(mfBalance)}"
        lines += "Total Corpus ${formatForIndia(npsBalance + debtBalance + mfBalance)}"
        lines += "Total Inflation ${formatForIndia(totalInflationAdjusted)}"
        lines += "Annuity ${formatForIndia(monthlyAnnuity)}"
        lines += "SWP ${formatForIndia(swp)}"
        lines += "Pension ${formatForIndia(monthlyPension)}"
        lines += "Inflation Adjusted ${formatForIndia(currentEquivalent)}"

        return InvestmentCalculationResult(
            lines = lines,
            yearlyProgression = yearlyProgression
        )
    }

    fun formatForIndia(number: Double): String {
        if (!number.isFinite()) {
            return "--"
        }
        val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 2
        return formatter.format(number)
    }

    private fun formatWholeOrOneDecimal(value: Double): String {
        val rounded = value.toLong().toDouble()
        return if (abs(value - rounded) < 0.0000001) {
            rounded.toLong().toString()
        } else {
            formatOneDecimal(value)
        }
    }

    private fun formatOneDecimal(value: Double): String {
        return String.format(Locale.US, "%.1f", value)
    }

    private fun isMultiple(value: Double, divisor: Double): Boolean {
        if (divisor <= 0.0) {
            return false
        }
        val remainder = value % divisor
        return abs(remainder) < 0.0000001 || abs(remainder - divisor) < 0.0000001
    }

    private fun isRemainder(value: Double, divisor: Double, targetRemainder: Double): Boolean {
        if (divisor <= 0.0) {
            return false
        }
        val remainder = value % divisor
        return abs(remainder - targetRemainder) < 0.0000001
    }

    private fun isSameNumber(a: Double, b: Double): Boolean {
        return abs(a - b) < 0.0000001
    }
}
