# Simulation Disclaimer

**For hackathon/demo purposes only** — the voltage metrics and stability computations (including Coefficient of Variation)
are performed on an already-rectified voltage signal. The model simplifies by assuming Vrms ≈ Vrectified and disregards any
post-rectification voltage increase or ripple effects typical in real AC-to-DC conversion scenarios.

In a **production deployment**, the system should:
- Compute statistics directly over true RMS values (Vrms) derived from the AC waveform (e.g., via proper sampling and RMS
  calculation per IEC 61000 or equivalent).
- Incorporate additional power quality indicators, such as:
    - Load factor analysis
    - Crest factor
    - Total harmonic distortion (THD)
    - Flicker severity (Pst / Plt per EN 50160 / IEC 61000-4-15)
    - Rapid voltage change limits

This would enable more comprehensive instability detection beyond short-term CV fluctuations.

The current 10-second window (10 samples @ 1 Hz) and 10% CV threshold remain a valid proactive safeguard, inspired by EN 50160 voltage variation
limits (±10% on 10-min rms averages for 95% of a week), but tuned for fast, real-time protection rather than long-term compliance monitoring.
This short window enables millisecond-to-second intervention, complementing (but not replacing) slower PQ monitoring systems.
