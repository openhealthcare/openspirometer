package org.iandavies.android.flowrateexperiment;

import android.util.Log;

import com.badlogic.gdx.audio.analysis.FFT;

import java.util.ArrayList;

import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterCoefficients;
import biz.source_code.dsp.filter.IirFilterDesignFisher;

/**
 * Created by Ian on 24/10/2014.
 */
public class Analysis {

    public final int sampleRate;

    public final float[] signal;
    public final float[] spectrum;
    public final float[] envelope;
    public final float[] flow;
    public final float[] volume;

    public final int startTimeSamples;
    public final int peakFlowTimeSamples;

    public Analysis(final float[] signal, int sampleRate) {
        this.signal = signal;
        this.sampleRate = sampleRate;
         // Calculate spectrum.

        int paddedLength = (int)Math.pow(2, Math.ceil(Math.log(signal.length) / Math.log(2))+1);
        Log.v("FFT SIZE", "Padded length is " + paddedLength);

        float[] signalPadded = new float[paddedLength];
        for (int i = 0; i < signal.length; i++)
            signalPadded[i] = signal[i];

        FFT fft = new FFT(paddedLength, sampleRate);
        fft.noAverages();
        fft.forward(signalPadded);

        float[] real = fft.getRealPart();
        float[] imag = fft.getImaginaryPart();

        spectrum = new float[signalPadded.length];
        for (int i = 0; i < spectrum.length; i++) {
            spectrum[i] = (float)Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
        }

        // Calculate envelope

        float[] rawEnvelope = UsefulMaths.magnitudeOfTheAnalyticFunctionDefinitely(signalPadded);

        // Filter envelope

        IirFilterCoefficients coeff = IirFilterDesignFisher.design(FilterPassType.lowpass, FilterCharacteristicsType.butterworth, 4, 0, 26.0 / sampleRate, 0);
        IirFilter filter = new IirFilter(coeff);

        envelope = new float[paddedLength];
        for (int i = 0; i < paddedLength; i++) {
            envelope[i] = (float)filter.step(rawEnvelope[i]);
        }

        // Calculate flow from envelope (pressure)

        flow = UsefulMaths.pressureToFlow(envelope);

        // Calculate integral of flow. This is volume.

        volume = new float[paddedLength];
        volume[0] = flow[0];
        for (int i = 1; i < volume.length; i++) {
            volume[i] = volume[i-1] + flow[i];
        }

        // Calculate start time (t0). This is done by extrapolating backwards from the steepest part of the volume-time curve.

        // Steepest point (peak flow) on volume-time curve would be highest peak in envelope, except we have to look at 80ms windows.

        int windowSize = (int)(80e-3 * sampleRate);
        float peakFlow = (volume[windowSize]- volume[0]) / windowSize;
        int peakFlowSample = 0;
        for (int i = 0; i < signal.length - windowSize; i++) {
            float gradient = (volume[i+windowSize] - volume[i]) / windowSize;
            if (gradient > peakFlow) {
                peakFlow = gradient;
                peakFlowSample = i;
            }
        }

        peakFlowTimeSamples = peakFlowSample;

        // Now we have peak flow, and it's position, extrapolate back to zero.


        startTimeSamples = peakFlowSample - (int)(volume[peakFlowSample] / peakFlow);

    }
}
