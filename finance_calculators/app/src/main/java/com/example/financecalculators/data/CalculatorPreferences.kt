package com.example.financecalculators.data

import android.content.Context

data class InflationFormState(
    val amount: String,
    val inflation: String,
    val years: String
)

data class SwpFormState(
    val corpus: String,
    val swp: String,
    val swpYears: String,
    val debtReturn: String,
    val equityReturn: String,
    val inflation: String,
    val retirementAge: String,
    val targetAge: String
)

data class InvestmentFormState(
    val npsBalance: String,
    val npsReturn: String,
    val npsInvestment: String,
    val debtBalance: String,
    val debtReturn: String,
    val debtInvestment: String,
    val mfBalance: String,
    val mfReturn: String,
    val mfInvestment: String,
    val mfIncrement: String,
    val mfIncrementAfterLoan: String,
    val currentAge: String,
    val retirementAge: String,
    val inflation: String,
    val loanTerm: String,
    val annuityRate: String,
    val swpRate: String
)

object CalculatorPreferences {
    private const val PREF_NAME = "finance_calculator_fields"

    private const val INFLATION_AMOUNT = "inflation_amount"
    private const val INFLATION_PERCENT = "inflation_percent"
    private const val INFLATION_YEARS = "inflation_years"

    private const val SWP_CORPUS = "swp_corpus"
    private const val SWP_VALUE = "swp_value"
    private const val SWP_YEARS = "swp_years"
    private const val SWP_DEBT_RETURN = "swp_debt_return"
    private const val SWP_EQUITY_RETURN = "swp_equity_return"
    private const val SWP_INFLATION = "swp_inflation"
    private const val SWP_RETIREMENT_AGE = "swp_retirement_age"
    private const val SWP_TARGET_AGE = "swp_target_age"

    private const val INV_NPS_BALANCE = "inv_nps_balance"
    private const val INV_NPS_RETURN = "inv_nps_return"
    private const val INV_NPS_INVESTMENT = "inv_nps_investment"
    private const val INV_DEBT_BALANCE = "inv_debt_balance"
    private const val INV_DEBT_RETURN = "inv_debt_return"
    private const val INV_DEBT_INVESTMENT = "inv_debt_investment"
    private const val INV_MF_BALANCE = "inv_mf_balance"
    private const val INV_MF_RETURN = "inv_mf_return"
    private const val INV_MF_INVESTMENT = "inv_mf_investment"
    private const val INV_MF_INCREMENT = "inv_mf_increment"
    private const val INV_MF_INCREMENT_AFTER_LOAN = "inv_mf_increment_after_loan"
    private const val INV_CURRENT_AGE = "inv_current_age"
    private const val INV_RETIREMENT_AGE = "inv_retirement_age"
    private const val INV_INFLATION = "inv_inflation"
    private const val INV_LOAN_TERM = "inv_loan_term"
    private const val INV_ANNUITY_RATE = "inv_annuity_rate"
    private const val INV_SWP_RATE = "inv_swp_rate"

    fun loadInflation(context: Context): InflationFormState {
        val pref = prefs(context)
        return InflationFormState(
            amount = pref.getString(INFLATION_AMOUNT, "100000") ?: "100000",
            inflation = pref.getString(INFLATION_PERCENT, "7") ?: "7",
            years = pref.getString(INFLATION_YEARS, "") ?: ""
        )
    }

    fun saveInflation(
        context: Context,
        amount: String,
        inflation: String,
        years: String
    ) {
        prefs(context).edit()
            .putString(INFLATION_AMOUNT, amount)
            .putString(INFLATION_PERCENT, inflation)
            .putString(INFLATION_YEARS, years)
            .apply()
    }

    fun loadSwp(context: Context): SwpFormState {
        val pref = prefs(context)
        return SwpFormState(
            corpus = pref.getString(SWP_CORPUS, "17000000") ?: "17000000",
            swp = pref.getString(SWP_VALUE, "35000") ?: "35000",
            swpYears = pref.getString(SWP_YEARS, "7") ?: "7",
            debtReturn = pref.getString(SWP_DEBT_RETURN, "7") ?: "7",
            equityReturn = pref.getString(SWP_EQUITY_RETURN, "10") ?: "10",
            inflation = pref.getString(SWP_INFLATION, "7") ?: "7",
            retirementAge = pref.getString(SWP_RETIREMENT_AGE, "51") ?: "51",
            targetAge = pref.getString(SWP_TARGET_AGE, "100") ?: "100"
        )
    }

    fun saveSwp(
        context: Context,
        corpus: String,
        swp: String,
        swpYears: String,
        debtReturn: String,
        equityReturn: String,
        inflation: String,
        retirementAge: String,
        targetAge: String
    ) {
        prefs(context).edit()
            .putString(SWP_CORPUS, corpus)
            .putString(SWP_VALUE, swp)
            .putString(SWP_YEARS, swpYears)
            .putString(SWP_DEBT_RETURN, debtReturn)
            .putString(SWP_EQUITY_RETURN, equityReturn)
            .putString(SWP_INFLATION, inflation)
            .putString(SWP_RETIREMENT_AGE, retirementAge)
            .putString(SWP_TARGET_AGE, targetAge)
            .apply()
    }

    fun loadInvestment(context: Context): InvestmentFormState {
        val pref = prefs(context)
        return InvestmentFormState(
            npsBalance = pref.getString(INV_NPS_BALANCE, "1056564") ?: "1056564",
            npsReturn = pref.getString(INV_NPS_RETURN, "8") ?: "8",
            npsInvestment = pref.getString(INV_NPS_INVESTMENT, "4200") ?: "4200",
            debtBalance = pref.getString(INV_DEBT_BALANCE, "2793596") ?: "2793596",
            debtReturn = pref.getString(INV_DEBT_RETURN, "5") ?: "5",
            debtInvestment = pref.getString(INV_DEBT_INVESTMENT, "29422") ?: "29422",
            mfBalance = pref.getString(INV_MF_BALANCE, "5012333") ?: "5012333",
            mfReturn = pref.getString(INV_MF_RETURN, "10") ?: "10",
            mfInvestment = pref.getString(INV_MF_INVESTMENT, "72000") ?: "72000",
            mfIncrement = pref.getString(INV_MF_INCREMENT, "6000") ?: "6000",
            mfIncrementAfterLoan = pref.getString(INV_MF_INCREMENT_AFTER_LOAN, "20000") ?: "20000",
            currentAge = pref.getString(INV_CURRENT_AGE, "35") ?: "35",
            retirementAge = pref.getString(INV_RETIREMENT_AGE, "52") ?: "52",
            inflation = pref.getString(INV_INFLATION, "7") ?: "7",
            loanTerm = pref.getString(INV_LOAN_TERM, "17") ?: "17",
            annuityRate = pref.getString(INV_ANNUITY_RATE, "6.4") ?: "6.4",
            swpRate = pref.getString(INV_SWP_RATE, "3") ?: "3"
        )
    }

    fun saveInvestment(
        context: Context,
        npsBalance: String,
        npsReturn: String,
        npsInvestment: String,
        debtBalance: String,
        debtReturn: String,
        debtInvestment: String,
        mfBalance: String,
        mfReturn: String,
        mfInvestment: String,
        mfIncrement: String,
        mfIncrementAfterLoan: String,
        currentAge: String,
        retirementAge: String,
        inflation: String,
        loanTerm: String,
        annuityRate: String,
        swpRate: String
    ) {
        prefs(context).edit()
            .putString(INV_NPS_BALANCE, npsBalance)
            .putString(INV_NPS_RETURN, npsReturn)
            .putString(INV_NPS_INVESTMENT, npsInvestment)
            .putString(INV_DEBT_BALANCE, debtBalance)
            .putString(INV_DEBT_RETURN, debtReturn)
            .putString(INV_DEBT_INVESTMENT, debtInvestment)
            .putString(INV_MF_BALANCE, mfBalance)
            .putString(INV_MF_RETURN, mfReturn)
            .putString(INV_MF_INVESTMENT, mfInvestment)
            .putString(INV_MF_INCREMENT, mfIncrement)
            .putString(INV_MF_INCREMENT_AFTER_LOAN, mfIncrementAfterLoan)
            .putString(INV_CURRENT_AGE, currentAge)
            .putString(INV_RETIREMENT_AGE, retirementAge)
            .putString(INV_INFLATION, inflation)
            .putString(INV_LOAN_TERM, loanTerm)
            .putString(INV_ANNUITY_RATE, annuityRate)
            .putString(INV_SWP_RATE, swpRate)
            .apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
