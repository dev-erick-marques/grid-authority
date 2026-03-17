# Voltage Metrics — Formulas and Definitions

> **Simulation disclaimer:** For hackathon purposes, metrics are collected over 
> an already-rectified voltage signal, disregarding the post-rectification voltage increase —
> the model assumes $V_{RMS} = V_{rectified}$. In production, the model should be computed over 
> the true $V_{RMS}$ and extended with additional statistics such as load factor analysis.

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
