package com.example.financecalculators.calculators

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.example.financecalculators.data.CalculatorPreferences
import com.example.financecalculators.navigation.AppDestination
import kotlin.math.max

@Composable
fun HomeScreen(onNavigate: (AppDestination) -> Unit) {
    val cards = listOf(
        AppDestination.Swp to "Simulate how long your retirement corpus can last.",
        AppDestination.Inflation to "See historical and future inflation-adjusted value.",
        AppDestination.Investment to "Project your final corpus and pension estimates."
    )

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Your native finance toolkit",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Converted from your web calculators into Android-native Compose screens.",
            style = MaterialTheme.typography.bodyMedium
        )

        cards.forEach { (destination, description) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(destination) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = destination.title, style = MaterialTheme.typography.titleMedium)
                    Text(text = description, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun InflationCalculatorScreen() {
    val context = LocalContext.current
    var amount by rememberSaveable { mutableStateOf("100000") }
    var inflation by rememberSaveable { mutableStateOf("7") }
    var years by rememberSaveable { mutableStateOf("") }
    var results by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var loadedFromStorage by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(loadedFromStorage) {
        if (!loadedFromStorage) {
            val savedState = CalculatorPreferences.loadInflation(context)
            amount = savedState.amount
            inflation = savedState.inflation
            years = savedState.years
            loadedFromStorage = true
        }
    }

    CalculatorScaffold(
        title = "Inflation Calculator",
        subtitle = "Calculate how amounts shift over time with inflation.",
        results = results
    ) {
        NumericField(value = amount, onValueChange = { amount = it }, label = "Corpus")
        NumericField(value = inflation, onValueChange = { inflation = it }, label = "Inflation %")
        NumericField(value = years, onValueChange = { years = it }, label = "Years")
        Button(
            onClick = {
                CalculatorPreferences.saveInflation(
                    context = context,
                    amount = amount,
                    inflation = inflation,
                    years = years
                )
                val input = InflationInput(
                    amount = amount.toDoubleOrNull() ?: Double.NaN,
                    inflationPercent = inflation.toDoubleOrNull() ?: Double.NaN,
                    years = years.toDoubleOrNull() ?: Double.NaN
                )
                results = if (
                    input.amount.isFinite() && input.inflationPercent.isFinite() && input.years.isFinite()
                ) {
                    FinanceCalculatorEngine.calculateInflation(input)
                } else {
                    listOf("Please enter valid numeric values.")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate")
        }
    }
}

@Composable
fun SwpCalculatorScreen() {
    val context = LocalContext.current
    var corpus by rememberSaveable { mutableStateOf("17000000") }
    var swp by rememberSaveable { mutableStateOf("35000") }
    var swpYears by rememberSaveable { mutableStateOf("7") }
    var debtReturn by rememberSaveable { mutableStateOf("7") }
    var equityReturn by rememberSaveable { mutableStateOf("10") }
    var inflation by rememberSaveable { mutableStateOf("7") }
    var retirementAge by rememberSaveable { mutableStateOf("51") }
    var targetAge by rememberSaveable { mutableStateOf("100") }
    var results by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var loadedFromStorage by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(loadedFromStorage) {
        if (!loadedFromStorage) {
            val savedState = CalculatorPreferences.loadSwp(context)
            corpus = savedState.corpus
            swp = savedState.swp
            swpYears = savedState.swpYears
            debtReturn = savedState.debtReturn
            equityReturn = savedState.equityReturn
            inflation = savedState.inflation
            retirementAge = savedState.retirementAge
            targetAge = savedState.targetAge
            loadedFromStorage = true
        }
    }

    CalculatorScaffold(
        title = "SWP Calculator",
        subtitle = "Estimate how long your savings can support withdrawals.",
        results = results
    ) {
        NumericField(value = corpus, onValueChange = { corpus = it }, label = "Corpus")
        NumericField(value = swp, onValueChange = { swp = it }, label = "SWP")
        NumericField(value = swpYears, onValueChange = { swpYears = it }, label = "SWP Years")
        NumericField(value = debtReturn, onValueChange = { debtReturn = it }, label = "Returns from Debt %")
        NumericField(value = equityReturn, onValueChange = { equityReturn = it }, label = "Returns from Equity %")
        NumericField(value = inflation, onValueChange = { inflation = it }, label = "Expected Inflation %")
        NumericField(value = retirementAge, onValueChange = { retirementAge = it }, label = "Age at Retirement")
        NumericField(value = targetAge, onValueChange = { targetAge = it }, label = "Target Age")

        Button(
            onClick = {
                CalculatorPreferences.saveSwp(
                    context = context,
                    corpus = corpus,
                    swp = swp,
                    swpYears = swpYears,
                    debtReturn = debtReturn,
                    equityReturn = equityReturn,
                    inflation = inflation,
                    retirementAge = retirementAge,
                    targetAge = targetAge
                )
                val input = SwpInput(
                    corpus = corpus.toDoubleOrNull() ?: Double.NaN,
                    swp = swp.toDoubleOrNull() ?: Double.NaN,
                    swpYears = swpYears.toDoubleOrNull() ?: Double.NaN,
                    debtReturnPercent = debtReturn.toDoubleOrNull() ?: Double.NaN,
                    equityReturnPercent = equityReturn.toDoubleOrNull() ?: Double.NaN,
                    inflationPercent = inflation.toDoubleOrNull() ?: Double.NaN,
                    retirementAge = retirementAge.toDoubleOrNull() ?: Double.NaN,
                    targetAge = targetAge.toDoubleOrNull() ?: Double.NaN
                )
                results = if (
                    input.corpus.isFinite() &&
                    input.swp.isFinite() &&
                    input.swpYears.isFinite() &&
                    input.debtReturnPercent.isFinite() &&
                    input.equityReturnPercent.isFinite() &&
                    input.inflationPercent.isFinite() &&
                    input.retirementAge.isFinite() &&
                    input.targetAge.isFinite()
                ) {
                    FinanceCalculatorEngine.calculateSwp(input)
                } else {
                    listOf("Please enter valid numeric values.")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate")
        }
    }
}

@Composable
fun InvestmentCalculatorScreen() {
    val context = LocalContext.current
    var npsBalance by rememberSaveable { mutableStateOf("1056564") }
    var npsReturn by rememberSaveable { mutableStateOf("8") }
    var npsInvestment by rememberSaveable { mutableStateOf("4200") }

    var debtBalance by rememberSaveable { mutableStateOf("2793596") }
    var debtReturn by rememberSaveable { mutableStateOf("5") }
    var debtInvestment by rememberSaveable { mutableStateOf("29422") }

    var mfBalance by rememberSaveable { mutableStateOf("5012333") }
    var mfReturn by rememberSaveable { mutableStateOf("10") }
    var mfInvestment by rememberSaveable { mutableStateOf("72000") }
    var mfIncrement by rememberSaveable { mutableStateOf("6000") }
    var mfIncrementAfterLoan by rememberSaveable { mutableStateOf("20000") }

    var currentAge by rememberSaveable { mutableStateOf("35") }
    var retirementAge by rememberSaveable { mutableStateOf("52") }
    var inflation by rememberSaveable { mutableStateOf("7") }
    var loanTerm by rememberSaveable { mutableStateOf("17") }
    var annuityRate by rememberSaveable { mutableStateOf("6.4") }
    var swpRate by rememberSaveable { mutableStateOf("3") }

    var results by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var yearlyProgression by remember { mutableStateOf(emptyList<YearlyCorpusPoint>()) }
    var loadedFromStorage by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(loadedFromStorage) {
        if (!loadedFromStorage) {
            val savedState = CalculatorPreferences.loadInvestment(context)
            npsBalance = savedState.npsBalance
            npsReturn = savedState.npsReturn
            npsInvestment = savedState.npsInvestment
            debtBalance = savedState.debtBalance
            debtReturn = savedState.debtReturn
            debtInvestment = savedState.debtInvestment
            mfBalance = savedState.mfBalance
            mfReturn = savedState.mfReturn
            mfInvestment = savedState.mfInvestment
            mfIncrement = savedState.mfIncrement
            mfIncrementAfterLoan = savedState.mfIncrementAfterLoan
            currentAge = savedState.currentAge
            retirementAge = savedState.retirementAge
            inflation = savedState.inflation
            loanTerm = savedState.loanTerm
            annuityRate = savedState.annuityRate
            swpRate = savedState.swpRate
            loadedFromStorage = true
        }
    }

    CalculatorScaffold(
        title = "Investment Based",
        subtitle = "Project final corpus from NPS, debt, and mutual fund investments.",
        results = results,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        InputSection(title = "NPS") {
            NumericField(value = npsBalance, onValueChange = { npsBalance = it }, label = "NPS Balance")
            NumericField(value = npsReturn, onValueChange = { npsReturn = it }, label = "NPS Return %")
            NumericField(value = npsInvestment, onValueChange = { npsInvestment = it }, label = "NPS Monthly Investment")
        }

        InputSection(title = "Debt Investments") {
            NumericField(value = debtBalance, onValueChange = { debtBalance = it }, label = "Balance in debt instruments")
            NumericField(value = debtReturn, onValueChange = { debtReturn = it }, label = "Returns from debt %")
            NumericField(value = debtInvestment, onValueChange = { debtInvestment = it }, label = "Debt Monthly Investment")
        }

        InputSection(title = "Mutual Funds") {
            NumericField(value = mfBalance, onValueChange = { mfBalance = it }, label = "Mutual Funds Balance")
            NumericField(value = mfReturn, onValueChange = { mfReturn = it }, label = "Mutual Funds Return %")
            NumericField(value = mfInvestment, onValueChange = { mfInvestment = it }, label = "Mutual Funds Monthly Investment")
            NumericField(value = mfIncrement, onValueChange = { mfIncrement = it }, label = "Mutual Funds Yearly Increment")
            NumericField(
                value = mfIncrementAfterLoan,
                onValueChange = { mfIncrementAfterLoan = it },
                label = "Mutual Funds Increment After Loan"
            )
        }

        InputSection(title = "General Info") {
            NumericField(value = currentAge, onValueChange = { currentAge = it }, label = "Current Age")
            NumericField(value = retirementAge, onValueChange = { retirementAge = it }, label = "Retirement Age")
            NumericField(value = inflation, onValueChange = { inflation = it }, label = "Inflation %")
            NumericField(value = loanTerm, onValueChange = { loanTerm = it }, label = "Loan Payment Left in Months")
            NumericField(value = annuityRate, onValueChange = { annuityRate = it }, label = "Annuity Return %")
            NumericField(value = swpRate, onValueChange = { swpRate = it }, label = "SWP Rate %")
        }

        Button(
            onClick = {
                CalculatorPreferences.saveInvestment(
                    context = context,
                    npsBalance = npsBalance,
                    npsReturn = npsReturn,
                    npsInvestment = npsInvestment,
                    debtBalance = debtBalance,
                    debtReturn = debtReturn,
                    debtInvestment = debtInvestment,
                    mfBalance = mfBalance,
                    mfReturn = mfReturn,
                    mfInvestment = mfInvestment,
                    mfIncrement = mfIncrement,
                    mfIncrementAfterLoan = mfIncrementAfterLoan,
                    currentAge = currentAge,
                    retirementAge = retirementAge,
                    inflation = inflation,
                    loanTerm = loanTerm,
                    annuityRate = annuityRate,
                    swpRate = swpRate
                )
                val input = InvestmentInput(
                    npsBalance = npsBalance.toDoubleOrNull() ?: Double.NaN,
                    npsReturnPercent = npsReturn.toDoubleOrNull() ?: Double.NaN,
                    npsInvestment = npsInvestment.toDoubleOrNull() ?: Double.NaN,
                    debtBalance = debtBalance.toDoubleOrNull() ?: Double.NaN,
                    debtReturnPercent = debtReturn.toDoubleOrNull() ?: Double.NaN,
                    debtInvestment = debtInvestment.toDoubleOrNull() ?: Double.NaN,
                    mfBalance = mfBalance.toDoubleOrNull() ?: Double.NaN,
                    mfReturnPercent = mfReturn.toDoubleOrNull() ?: Double.NaN,
                    mfInvestment = mfInvestment.toDoubleOrNull() ?: Double.NaN,
                    mfIncrement = mfIncrement.toDoubleOrNull() ?: Double.NaN,
                    mfIncrementAfterLoan = mfIncrementAfterLoan.toDoubleOrNull() ?: Double.NaN,
                    currentAge = currentAge.toDoubleOrNull() ?: Double.NaN,
                    retirementAge = retirementAge.toDoubleOrNull() ?: Double.NaN,
                    inflationPercent = inflation.toDoubleOrNull() ?: Double.NaN,
                    loanTerm = loanTerm.toDoubleOrNull() ?: Double.NaN,
                    annuityRatePercent = annuityRate.toDoubleOrNull() ?: Double.NaN,
                    swpRatePercent = swpRate.toDoubleOrNull() ?: Double.NaN
                )

                val isValid = listOf(
                    input.npsBalance,
                    input.npsReturnPercent,
                    input.npsInvestment,
                    input.debtBalance,
                    input.debtReturnPercent,
                    input.debtInvestment,
                    input.mfBalance,
                    input.mfReturnPercent,
                    input.mfInvestment,
                    input.mfIncrement,
                    input.mfIncrementAfterLoan,
                    input.currentAge,
                    input.retirementAge,
                    input.inflationPercent,
                    input.loanTerm,
                    input.annuityRatePercent,
                    input.swpRatePercent
                ).all { it.isFinite() }

                val calculationResult = if (isValid) {
                    FinanceCalculatorEngine.calculateInvestment(input)
                } else {
                    InvestmentCalculationResult(
                        lines = listOf("Please enter valid numeric values."),
                        yearlyProgression = emptyList()
                    )
                }

                results = calculationResult.lines
                yearlyProgression = calculationResult.yearlyProgression
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate")
        }

        if (yearlyProgression.isNotEmpty()) {
            InvestmentProgressionChart(points = yearlyProgression)
        }
    }
}

@Composable
private fun CalculatorScaffold(
    title: String,
    subtitle: String,
    results: List<String>,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(contentPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)

        content()

        if (results.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Results", style = MaterialTheme.typography.titleMedium)
                    results.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InputSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun NumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            if (newValue.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                onValueChange(newValue)
            }
        },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
private fun InvestmentProgressionChart(points: List<YearlyCorpusPoint>) {
    val nominalColor = MaterialTheme.colorScheme.primary
    val inflationAdjustedColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Yearly Corpus Progression", style = MaterialTheme.typography.titleMedium)
            Text(
                "Nominal and inflation-adjusted corpus at each year end.",
                style = MaterialTheme.typography.bodySmall
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                val minAge = points.first().age
                val maxAge = points.last().age
                val ageRange = (maxAge - minAge).takeIf { it > 0.0 } ?: 1.0
                val maxCorpus = max(
                    points.maxOf { it.totalCorpus },
                    points.maxOf { it.inflationAdjustedCorpus }
                ).coerceAtLeast(1.0)

                for (index in 0..4) {
                    val y = size.height * (index / 4f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }

                fun xPosition(age: Double): Float {
                    return ((age - minAge) / ageRange).toFloat() * size.width
                }

                fun yPosition(value: Double): Float {
                    val ratio = (value / maxCorpus).toFloat()
                    return size.height - (ratio * size.height)
                }

                val nominalPath = Path()
                val inflationAdjustedPath = Path()

                points.forEachIndexed { index, point ->
                    val x = xPosition(point.age)
                    val yNominal = yPosition(point.totalCorpus)
                    val yAdjusted = yPosition(point.inflationAdjustedCorpus)

                    if (index == 0) {
                        nominalPath.moveTo(x, yNominal)
                        inflationAdjustedPath.moveTo(x, yAdjusted)
                    } else {
                        nominalPath.lineTo(x, yNominal)
                        inflationAdjustedPath.lineTo(x, yAdjusted)
                    }
                }

                drawPath(
                    path = nominalPath,
                    color = nominalColor,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
                drawPath(
                    path = inflationAdjustedPath,
                    color = inflationAdjustedColor,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ChartLegendItem(color = nominalColor, label = "Total corpus")
                ChartLegendItem(color = inflationAdjustedColor, label = "Inflation-adjusted")
            }

            Text(
                text = "From age ${formatAge(points.first().age)} to ${formatAge(points.last().age)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ChartLegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color)
        }
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatAge(age: Double): String {
    return String.format(java.util.Locale.US, "%.1f", age)
}
