package org.iandavies.android.flowrateexperiment;

import com.badlogic.gdx.audio.analysis.FFT;

/**
 * Created by Ian on 24/10/2014.
 */
public class UsefulMaths {


    public static float[] magnitudeOfTheAnalyticFunctionDefinitely(float[] signal) {

        int N = signal.length;

        FFT fft = new FFT(N, 0);
        fft.noAverages();
        fft.forward(signal);

        float[] real = fft.getRealPart();
        float[] imag = fft.getImaginaryPart();

        float[] h = new float[N];

        h[0] = 1;
        h[N/2 + 1] = 1;

        for (int i = 2; i <= N/2; i++)
            h[i] = 2;

        for (int i = 0; i < N; i++)
            real[i] *= h[i];

        float[] result = new float[N];

        fft.inverse(real, imag, result);

        real = fft.getRealPart();
        imag = fft.getImaginaryPart();
        for (int i = 0; i < N; i++)
            result[i] = (float)Math.sqrt(real[i] * real[i] + imag[i] * imag[i]) / N;

        return result;
    }

    public static float[] pressureToFlow(float[] pressure) {

        float[] flow = new float[pressure.length];

        for (int i = 0; i < pressure.length; i++) {
            // For now, just say that flow = pressure
            flow[i] = pressure[i];
        }
        return flow;
    }
}
