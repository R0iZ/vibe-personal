package com.maydings.vibelauncher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Local wallpaper behind the home screen: one image, or a slideshow of images from a chosen folder /
 * album. Photos are decoded only for this ImageView — never uploaded or shared.
 */
final class WallpaperController {

    static final int MODE_OFF = 0;
    static final int MODE_SINGLE = 1;
    static final int MODE_SLIDESHOW = 2;

    private static final long SLIDE_MS = 30_000L;
    private static final int MAX_DECODE = 2048;   // long edge; TVs don't need full camera resolution

    private final Context app;
    private final VibeSettings settings;
    private final ImageView target;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "wallpaper");
        t.setDaemon(true);
        return t;
    });
    private final Runnable nextSlide = this::advanceSlideshow;
    private List<Uri> slides = Collections.emptyList();
    private int slideIndex;
    private boolean started;

    WallpaperController(Context context, VibeSettings settings, ImageView target) {
        this.app = context.getApplicationContext();
        this.settings = settings;
        this.target = target;
    }

    void start() {
        started = true;
        apply();
    }

    void stop() {
        started = false;
        main.removeCallbacks(nextSlide);
    }

    void close() {
        stop();
        worker.shutdownNow();
        target.setImageDrawable(null);
    }

    /** Re-reads settings (after Save) and shows the current wallpaper mode. */
    void apply() {
        main.removeCallbacks(nextSlide);
        int mode = settings.wallpaperMode();
        if (mode == MODE_OFF) {
            target.animate().alpha(0f).setDuration(300).withEndAction(() -> target.setImageDrawable(null)).start();
            return;
        }
        if (mode == MODE_SINGLE) {
            String uri = settings.wallpaperUri();
            if (uri.isEmpty()) {
                target.setImageDrawable(null);
                target.setAlpha(0f);
                return;
            }
            load(Uri.parse(uri), false);
            return;
        }
        // Slideshow: resolve the folder's images, then cycle.
        worker.execute(() -> {
            List<Uri> found = MediaStoreHelper.imagesInFolder(app, settings.wallpaperFolder());
            main.post(() -> {
                if (!started || settings.wallpaperMode() != MODE_SLIDESHOW) return;
                slides = found;
                slideIndex = 0;
                if (slides.isEmpty()) {
                    target.setImageDrawable(null);
                    target.setAlpha(0f);
                    return;
                }
                showSlide(0, false);
                main.postDelayed(nextSlide, SLIDE_MS);
            });
        });
    }

    private void advanceSlideshow() {
        if (!started || settings.wallpaperMode() != MODE_SLIDESHOW || slides.isEmpty()) return;
        slideIndex = (slideIndex + 1) % slides.size();
        showSlide(slideIndex, true);
        main.postDelayed(nextSlide, SLIDE_MS);
    }

    private void showSlide(int index, boolean fade) {
        if (index < 0 || index >= slides.size()) return;
        load(slides.get(index), fade);
    }

    private void load(Uri uri, boolean fade) {
        worker.execute(() -> {
            Bitmap bitmap = decode(uri);
            main.post(() -> {
                if (!started) return;
                if (bitmap == null) {
                    if (!fade) {
                        target.setImageDrawable(null);
                        target.setAlpha(0f);
                    }
                    return;
                }
                if (fade && target.getDrawable() != null) {
                    target.animate().alpha(0f).setDuration(400).withEndAction(() -> {
                        target.setImageBitmap(bitmap);
                        target.animate().alpha(1f).setDuration(500).start();
                    }).start();
                } else {
                    target.setImageBitmap(bitmap);
                    target.animate().alpha(1f).setDuration(450).start();
                }
            });
        });
    }

    private Bitmap decode(Uri uri) {
        try (InputStream boundsIn = app.getContentResolver().openInputStream(uri)) {
            if (boundsIn == null) return null;
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(boundsIn, null, bounds);
            int sample = 1;
            int longEdge = Math.max(bounds.outWidth, bounds.outHeight);
            while (longEdge / sample > MAX_DECODE) sample *= 2;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            try (InputStream data = app.getContentResolver().openInputStream(uri)) {
                if (data == null) return null;
                return BitmapFactory.decodeStream(data, null, opts);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
