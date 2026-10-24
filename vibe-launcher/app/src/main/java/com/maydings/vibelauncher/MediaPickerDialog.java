package com.maydings.vibelauncher;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.function.Consumer;

/**
 * Built-in album/file browser for Android TV, where the system folder picker is often missing.
 * Lists local MediaStore albums (and a filesystem fallback), then either a folder for slideshow or
 * a single image. Photos stay on device for wallpaper only.
 */
final class MediaPickerDialog {

    private MediaPickerDialog() {
    }

    /**
     * Pick a folder/album (slideshow). Calls {@code onPicked} with the album id to store in settings,
     * or nothing when cancelled.
     */
    static void pickFolder(Activity activity, Consumer<MediaStoreHelper.Album> onPicked) {
        ensureAccess(activity, () -> showAlbums(activity, true, onPicked, null));
    }

    /**
     * Pick a single image file. {@code onPicked} receives a content/file URI string and a display name.
     */
    static void pickFile(Activity activity, Consumer<MediaStoreHelper.Entry> onPicked) {
        ensureAccess(activity, () -> showAlbums(activity, false, null, onPicked));
    }

    private static void ensureAccess(Activity activity, Runnable next) {
        if (MediaStoreHelper.hasPhotoAccess(activity)) {
            next.run();
            return;
        }
        // Ask; MainActivity routes the result and the user can open the picker again.
        activity.requestPermissions(new String[]{MediaStoreHelper.requiredPermission()}, 3);
        Toast.makeText(activity, R.string.photos_permission_needed, Toast.LENGTH_LONG).show();
    }

    private static void showAlbums(Activity activity, boolean folderMode,
            Consumer<MediaStoreHelper.Album> onFolder, Consumer<MediaStoreHelper.Entry> onFile) {
        List<MediaStoreHelper.Album> albums = MediaStoreHelper.albums(activity);
        if (albums.isEmpty()) {
            Toast.makeText(activity, R.string.no_photos_found, Toast.LENGTH_LONG).show();
            return;
        }
        float density = activity.getResources().getDisplayMetrics().density;
        LinearLayout rows = new LinearLayout(activity);
        rows.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(activity);
        scroll.setBackgroundResource(R.drawable.glass_panel);
        int pad = (int) (10 * density);
        scroll.setPadding(pad, pad, pad, pad);
        scroll.addView(rows);

        TextView title = new TextView(activity);
        title.setText(folderMode ? R.string.pick_folder_title : R.string.pick_album_title);
        title.setTextColor(0xE6FFFFFF);
        title.setTextSize(16);
        title.setTypeface(UiFont.get(activity, R.string.font_medium));
        title.setPadding(pad, pad, pad, pad);
        rows.addView(title);

        Dialog dialog = glassDialog(activity, scroll, density);
        for (MediaStoreHelper.Album album : albums) {
            View row = activity.getLayoutInflater().inflate(R.layout.item_input, rows, false);
            ((TextView) row.findViewById(R.id.title)).setText(album.name);
            TextView status = row.findViewById(R.id.status);
            status.setVisibility(View.VISIBLE);
            status.setText(activity.getString(R.string.photo_count, album.count));
            row.findViewById(R.id.icon).setVisibility(View.GONE);
            row.findViewById(R.id.dot).setVisibility(View.GONE);
            row.setOnClickListener(v -> {
                if (folderMode) {
                    dialog.dismiss();
                    if (onFolder != null) onFolder.accept(album);
                } else {
                    dialog.dismiss();
                    showFiles(activity, album, onFile);
                }
            });
            rows.addView(row);
        }
        dialog.show();
        if (rows.getChildCount() > 1) rows.getChildAt(1).requestFocus();
    }

    private static void showFiles(Activity activity, MediaStoreHelper.Album album,
            Consumer<MediaStoreHelper.Entry> onFile) {
        List<MediaStoreHelper.Entry> files = MediaStoreHelper.imagesInAlbum(activity, album.id);
        if (files.isEmpty()) {
            Toast.makeText(activity, R.string.no_photos_found, Toast.LENGTH_LONG).show();
            return;
        }
        float density = activity.getResources().getDisplayMetrics().density;
        LinearLayout rows = new LinearLayout(activity);
        rows.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(activity);
        scroll.setBackgroundResource(R.drawable.glass_panel);
        int pad = (int) (10 * density);
        scroll.setPadding(pad, pad, pad, pad);
        scroll.addView(rows);

        TextView title = new TextView(activity);
        title.setText(album.name);
        title.setTextColor(0xE6FFFFFF);
        title.setTextSize(16);
        title.setTypeface(UiFont.get(activity, R.string.font_medium));
        title.setPadding(pad, pad, pad, pad);
        rows.addView(title);

        Dialog dialog = glassDialog(activity, scroll, density);
        for (MediaStoreHelper.Entry entry : files) {
            View row = activity.getLayoutInflater().inflate(R.layout.item_input, rows, false);
            ((TextView) row.findViewById(R.id.title)).setText(entry.name);
            row.findViewById(R.id.status).setVisibility(View.GONE);
            row.findViewById(R.id.icon).setVisibility(View.GONE);
            row.findViewById(R.id.dot).setVisibility(View.GONE);
            row.setOnClickListener(v -> {
                dialog.dismiss();
                if (onFile != null) onFile.accept(entry);
            });
            rows.addView(row);
        }
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                dialog.dismiss();
                // Back returns to albums for another try.
                pickFile(activity, onFile);
                return true;
            }
            return false;
        });
        dialog.show();
        if (rows.getChildCount() > 1) rows.getChildAt(1).requestFocus();
    }

    private static Dialog glassDialog(Activity activity, View content, float density) {
        Dialog dialog = new Dialog(activity, android.R.style.Theme_Material_Dialog_NoActionBar);
        dialog.setContentView(content);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER);
            int height = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.75f);
            window.setLayout((int) (520 * density), height);
        }
        // ScrollView as content needs a LayoutParams on the dialog root sometimes.
        ViewGroup.LayoutParams lp = content.getLayoutParams();
        if (lp != null) {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            content.setLayoutParams(lp);
        }
        return dialog;
    }
}
