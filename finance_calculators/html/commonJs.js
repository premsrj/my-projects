function formatForIndia(number) {
    const parts = String(number).split(".");
    let decimal = "00";
    if (parts.length === 2) {
        decimal = parts[1].slice(0, 2);
    }

    let formattedString = parts[0].slice(-3);
    parts[0] = parts[0].slice(0, -3);

    while (parts[0].length > 0) {
        formattedString = parts[0].slice(-2) + "," + formattedString;
        parts[0] = parts[0].slice(0, -2);
    }

    formattedString = formattedString.padStart(11, " ");
    formattedString += "." + decimal.padEnd(2, "0");
    return formattedString;
}