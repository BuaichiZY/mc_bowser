package cn.mcbowser.client.browser;

import cn.mcbowser.McBowser;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefAudioHandlerAdapter;
import org.cef.misc.CefAudioParameters;
import org.cef.misc.DataPointer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps CEF audio alive independently of the full browser control screen. */
final class BrowserAudioPlayer extends CefAudioHandlerAdapter {
    private final Map<Integer, Stream> streams = new ConcurrentHashMap<>();

    @Override
    public boolean getAudioParameters(CefBrowser browser, CefAudioParameters parameters) {
        return DisplayBrowserManager.isManagedBrowser(browser);
    }

    @Override
    public void onAudioStreamStarted(CefBrowser browser, CefAudioParameters parameters, int channels) {
        stop(browser);
        try {
            AudioFormat format = new AudioFormat(parameters.getSampleRate(), 16, channels, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format, Math.max(16_384, parameters.getFramesPerBuffer() * channels * 8));
            line.start();
            streams.put(browser.getIdentifier(), new Stream(line, channels));
        } catch (LineUnavailableException | IllegalArgumentException error) {
            McBowser.LOGGER.warn("Unable to open audio output for browser {}", browser.getIdentifier(), error);
        }
    }

    @Override
    public void onAudioStreamPacket(CefBrowser browser, DataPointer data, int frames, long pts) {
        Stream stream = streams.get(browser.getIdentifier());
        if (stream == null) return;
        byte[] pcm = new byte[frames * stream.channels() * 2];
        int index = 0;
        for (int frame = 0; frame < frames; frame++) {
            for (int channel = 0; channel < stream.channels(); channel++) {
                float sample = Math.clamp(data.getData(channel).getFloat(frame), -1.0F, 1.0F);
                short value = (short) Math.round(sample * 32767.0F);
                pcm[index++] = (byte) value;
                pcm[index++] = (byte) (value >>> 8);
            }
        }
        stream.line().write(pcm, 0, pcm.length);
    }

    @Override public void onAudioStreamStopped(CefBrowser browser) { stop(browser); }
    @Override public void onAudioStreamError(CefBrowser browser, String text) {
        McBowser.LOGGER.warn("Browser audio stream error: {}", text);
        stop(browser);
    }

    private void stop(CefBrowser browser) {
        Stream stream = streams.remove(browser.getIdentifier());
        if (stream != null) {
            stream.line().flush();
            stream.line().stop();
            stream.line().close();
        }
    }

    private record Stream(SourceDataLine line, int channels) {}
}
