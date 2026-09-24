/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.media3.exoplayer.util;

import android.os.Debug;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Temporary debug logging for the fubo short-form player OOM investigation (SFP_OOM_DEBUG).
 *
 * <p>Every line is written under the {@link #TAG} tag so the app, the fubo player library and this
 * media3 fork can be captured with a single filter: {@code adb logcat -s SFP_OOM_DEBUG}.
 *
 * <p>This intentionally uses {@link android.util.Log} directly instead of the media3 {@code Log}
 * wrapper: the fubo player library redirects the media3 logger through its event pipeline (a main
 * thread hop per line), and these diagnostics must stay cheap and synchronous when the heap is
 * nearly exhausted.
 *
 * <p>Remove this class and every {@code SfpOomDebugLog} call site once the investigation is over.
 */
@UnstableApi
public final class SfpOomDebugLog {

  /** Logcat tag shared by all three layers of the investigation. */
  public static final String TAG = "SFP_OOM_DEBUG";

  /** Live {@code ExoPlayerImpl} instances (constructed minus released). */
  public static final AtomicInteger LIVE_EXO_PLAYERS = new AtomicInteger();

  /** Live {@code MediaCodec} adapters currently held by {@code MediaCodecRenderer}s. */
  public static final AtomicInteger LIVE_CODECS = new AtomicInteger();

  /** Live {@code PreloadMediaSource} instances (created minus released by the preload manager). */
  public static final AtomicInteger LIVE_PRELOAD_SOURCES = new AtomicInteger();

  /** Live {@code PreloadMediaPeriod} instances (created minus released). */
  public static final AtomicInteger LIVE_PRELOAD_PERIODS = new AtomicInteger();

  private static final long MB = 1024L * 1024L;
  private static final int DEFAULT_TAIL_LENGTH = 60;

  private SfpOomDebugLog() {}

  /** Logs a debug line as {@code component | message}. */
  public static void d(String component, String message) {
    Log.d(TAG, component + " | " + message);
  }

  /** Logs a warning line as {@code component | message}. */
  public static void w(String component, String message) {
    Log.w(TAG, component + " | " + message);
  }

  /** Returns a compact identity string for {@code object}, or {@code "null"}. */
  public static String id(@Nullable Object object) {
    return object == null ? "null" : Integer.toHexString(System.identityHashCode(object));
  }

  /** Returns the last {@link #DEFAULT_TAIL_LENGTH} characters of {@code text}. */
  public static String tail(@Nullable Object text) {
    return tail(text, DEFAULT_TAIL_LENGTH);
  }

  /** Returns the last {@code maxLength} characters of {@code text}, or {@code "null"}. */
  public static String tail(@Nullable Object text, int maxLength) {
    if (text == null) {
      return "null";
    }
    String string = text.toString();
    return string.length() <= maxLength
        ? string
        : "…" + string.substring(string.length() - maxLength);
  }

  /** Returns the playback URI (or media id) of {@code mediaItem} for logging. */
  public static String describe(@Nullable MediaItem mediaItem) {
    if (mediaItem == null) {
      return "null";
    }
    if (mediaItem.localConfiguration != null) {
      return tail(mediaItem.localConfiguration.uri);
    }
    return "mediaId=" + mediaItem.mediaId;
  }

  /** Formats {@code bytes} as megabytes with one decimal. */
  public static String mb(long bytes) {
    return String.format(Locale.US, "%.1fMB", bytes / (double) MB);
  }

  /** Java heap, native heap and the live object counters, on one line. */
  public static String heapSummary() {
    Runtime runtime = Runtime.getRuntime();
    long maxBytes = runtime.maxMemory();
    long usedBytes = runtime.totalMemory() - runtime.freeMemory();
    long usedPercent = maxBytes == 0 ? 0 : usedBytes * 100 / maxBytes;
    return String.format(
        Locale.US,
        "heap=%d/%dMB(%d%%) native=%dMB live[exo=%d codecs=%d preloadSrc=%d preloadPeriods=%d]",
        usedBytes / MB,
        maxBytes / MB,
        usedPercent,
        Debug.getNativeHeapAllocatedSize() / MB,
        LIVE_EXO_PLAYERS.get(),
        LIVE_CODECS.get(),
        LIVE_PRELOAD_SOURCES.get(),
        LIVE_PRELOAD_PERIODS.get());
  }
}
