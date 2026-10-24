package com.maydings.vibelauncher;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lists photo albums and images from the local MediaStore for the built-in TV picker. Used only as
 * wallpaper sources on device — nothing is uploaded.
 */
final class MediaStoreHelper {

    static final class Album {
        final String id;     // bucket id, or absolute folder path for filesystem fallback
        final String name;
        final int count;

        Album(String id, String name, int count) {
            this.id = id;
            this.name = name;
            this.count = count;
        }
    }

    static final class Entry {
        final Uri uri;
        final String name;

        Entry(Uri uri, String name) {
            this.uri = uri;
            this.name = name;
        }
    }

    private MediaStoreHelper() {
    }

    /** Permission to ask for before listing photos (API 33+ vs older READ_EXTERNAL_STORAGE). */
    static String requiredPermission() {
        if (Build.VERSION.SDK_INT >= 33) return Manifest.permission.READ_MEDIA_IMAGES;
        return Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    static boolean hasPhotoAccess(Context context) {
        return context.checkSelfPermission(requiredPermission()) == PackageManager.PERMISSION_GRANTED;
    }

    /** Photo albums (MediaStore buckets), newest activity first. */
    static List<Album> albums(Context context) {
        List<Album> out = new ArrayList<>();
        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        };
        // Distinct buckets via a map; MediaStore doesn't offer GROUP BY on all TVs.
        Map<String, Album> byId = new LinkedHashMap<>();
        try (Cursor c = context.getContentResolver().query(collection, projection, null, null,
                MediaStore.Images.Media.DATE_MODIFIED + " DESC")) {
            if (c == null) return filesystemAlbums();
            int idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);
            int nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            while (c.moveToNext()) {
                String id = c.getString(idCol);
                if (id == null || byId.containsKey(id)) {
                    if (id != null && byId.containsKey(id)) {
                        Album prev = byId.get(id);
                        byId.put(id, new Album(prev.id, prev.name, prev.count + 1));
                    }
                    continue;
                }
                String name = c.getString(nameCol);
                if (name == null || name.isEmpty()) name = id;
                byId.put(id, new Album(id, name, 1));
            }
        } catch (SecurityException | IllegalArgumentException e) {
            return filesystemAlbums();
        }
        out.addAll(byId.values());
        if (out.isEmpty()) return filesystemAlbums();
        return out;
    }

    /** Images in one album (bucket id) or filesystem folder path. */
    static List<Entry> imagesInAlbum(Context context, String albumId) {
        if (albumId == null || albumId.isEmpty()) return new ArrayList<>();
        if (albumId.startsWith("/")) return filesystemImages(albumId);
        List<Entry> out = new ArrayList<>();
        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME
        };
        String selection = MediaStore.Images.Media.BUCKET_ID + "=?";
        try (Cursor c = context.getContentResolver().query(collection, projection, selection,
                new String[]{albumId}, MediaStore.Images.Media.DATE_MODIFIED + " DESC")) {
            if (c == null) return out;
            int idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            while (c.moveToNext()) {
                long id = c.getLong(idCol);
                String name = c.getString(nameCol);
                Uri uri = Uri.withAppendedPath(collection, Long.toString(id));
                out.add(new Entry(uri, name == null ? uri.getLastPathSegment() : name));
            }
        } catch (SecurityException | IllegalArgumentException ignored) {
        }
        return out;
    }

    /** All image URIs under a stored folder id (bucket or path), for slideshow. */
    static List<Uri> imagesInFolder(Context context, String folderId) {
        List<Uri> uris = new ArrayList<>();
        for (Entry e : imagesInAlbum(context, folderId)) uris.add(e.uri);
        return uris;
    }

    /**
     * When MediaStore is empty or blocked, list common public picture folders on shared storage.
     * Still read-only and local-only.
     */
    private static List<Album> filesystemAlbums() {
        List<Album> out = new ArrayList<>();
        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        addDirAlbum(out, root);
        File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        addDirAlbum(out, dcim);
        if (dcim != null) {
            File camera = new File(dcim, "Camera");
            addDirAlbum(out, camera);
        }
        File download = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        addDirAlbum(out, download);
        return out;
    }

    private static void addDirAlbum(List<Album> out, File dir) {
        if (dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        int images = 0;
        for (File f : files) {
            if (f.isFile() && isImageName(f.getName())) images++;
        }
        if (images == 0) return;
        out.add(new Album(dir.getAbsolutePath(), dir.getName(), images));
    }

    private static List<Entry> filesystemImages(String path) {
        List<Entry> out = new ArrayList<>();
        File dir = new File(path);
        File[] files = dir.listFiles();
        if (files == null) return out;
        for (File f : files) {
            if (!f.isFile() || !isImageName(f.getName())) continue;
            out.add(new Entry(Uri.fromFile(f), f.getName()));
        }
        return out;
    }

    private static boolean isImageName(String name) {
        String n = name.toLowerCase(Locale.US);
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")
                || n.endsWith(".webp") || n.endsWith(".gif") || n.endsWith(".bmp");
    }
}
