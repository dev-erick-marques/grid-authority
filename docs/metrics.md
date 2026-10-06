# Voltage Metrics — Formulas and Definitions

## Arithmetic Mean ($\bar{V}$)

$$
\bar{V} = \frac{1}{n} \sum_{i=1}^{n} V_i
$$

## Sample Standard Deviation ($s$)

$$
s = \sqrt{\frac{\sum_{i=1}^{n}(V_i - \bar{V})^2}{n - 1}}
$$

> Uses Bessel's correction (n−1) for unbiased sample variance, matching the implementation in the source code.

## Coefficient of Variation ($CV$)

$$
CV = \frac{s}{\bar{V}} \times 100
$$