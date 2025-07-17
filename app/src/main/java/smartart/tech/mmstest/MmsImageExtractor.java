package smartart.tech.mmstest;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MmsImageExtractor {

    private static final String TAG = "MmsImageExtractor";
    private Context context;
    private ContentResolver contentResolver;

    public MmsImageExtractor(Context context) {
        this.context = context;
        this.contentResolver = context.getContentResolver();
    }

    // Interface for callback when images are found
    public interface OnImageFoundListener {
        void onImageFound(Bitmap image, String contentType, String mmsId);
        void onTextFound(String text, String mmsId);
        void onVideoFound(Uri videoUri, String mmsId);
    }

    /**
     * Get all MMS messages and extract images
     */
    public void getAllMmsImages(OnImageFoundListener listener) {
        Cursor cursor = contentResolver.query(
                Uri.parse("content://mms"),
                new String[]{"_id", "date", "thread_id", "msg_box", "read"},
                null, null, "date DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String mmsId = cursor.getString(cursor.getColumnIndexOrThrow("_id"));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                int threadId = cursor.getInt(cursor.getColumnIndexOrThrow("thread_id"));
                int msgBox = cursor.getInt(cursor.getColumnIndexOrThrow("msg_box"));

                Log.d(TAG, "Processing MMS ID: " + mmsId + ", Date: " + date);

                // Extract parts from this MMS
                extractMmsParts(mmsId, listener);

            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    /**
     * Get MMS messages from a specific thread
     */
    public void getMmsImagesFromThread(int threadId, OnImageFoundListener listener) {
        Cursor cursor = contentResolver.query(
                Uri.parse("content://mms"),
                new String[]{"_id", "date", "msg_box", "read"},
                "thread_id = ?",
                new String[]{String.valueOf(threadId)},
                "date DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String mmsId = cursor.getString(cursor.getColumnIndexOrThrow("_id"));
                extractMmsParts(mmsId, listener);
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    /**
     * Extract parts from a specific MMS message
     */
    private void extractMmsParts(String mmsId, OnImageFoundListener listener) {
        Cursor partCursor = contentResolver.query(
                Uri.parse("content://mms/part"),
                new String[]{"_id", "ct", "_data", "text", "name", "filename"},
                "mid = ?",
                new String[]{mmsId},
                null
        );

        if (partCursor != null && partCursor.moveToFirst()) {
            do {
                String partId = partCursor.getString(partCursor.getColumnIndexOrThrow("_id"));
                String contentType = partCursor.getString(partCursor.getColumnIndexOrThrow("ct"));
                String data = partCursor.getString(partCursor.getColumnIndexOrThrow("_data"));
                String text = partCursor.getString(partCursor.getColumnIndexOrThrow("text"));
                String name = partCursor.getString(partCursor.getColumnIndexOrThrow("name"));
                String filename = partCursor.getString(partCursor.getColumnIndexOrThrow("filename"));

                Log.d(TAG, "Part ID: " + partId + ", Content Type: " + contentType + ", Data: " + data);

                if (contentType != null) {
                    if (contentType.startsWith("image/")) {
                        // Handle image
                        Bitmap image = getMmsImage(partId, data);
                        if (image != null && listener != null) {
                            listener.onImageFound(image, contentType, mmsId);
                        }
                    } else if (contentType.startsWith("video/")) {
                        // Handle video
                        Uri videoUri = Uri.parse("content://mms/part/" + partId);
                        if (listener != null) {
                            listener.onVideoFound(videoUri, mmsId);
                        }
                    } else if (contentType.equals("text/plain")) {
                        // Handle text part
                        String textContent = getMmsText(partId);
                        if (textContent != null && listener != null) {
                            listener.onTextFound(textContent, mmsId);
                        }
                    }
                }

            } while (partCursor.moveToNext());
            partCursor.close();
        }
    }

    /**
     * Get image from MMS part
     */
    private Bitmap getMmsImage(String partId, String data) {
        try {
            Uri partUri = Uri.parse("content://mms/part/" + partId);
            InputStream inputStream = contentResolver.openInputStream(partUri);

            if (inputStream != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                return bitmap;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading MMS image from part " + partId, e);
        }

        // Try alternative method using file path
        if (data != null && !data.isEmpty()) {
            return getMmsImageFromPath(data);
        }

        return null;
    }

    /**
     * Get image from file path
     */
    private Bitmap getMmsImageFromPath(String filePath) {
        if (filePath != null && !filePath.isEmpty()) {
            try {
                return BitmapFactory.decodeFile(filePath);
            } catch (Exception e) {
                Log.e(TAG, "Error loading image from path: " + filePath, e);
            }
        }
        return null;
    }

    /**
     * Get text content from MMS part
     */
    private String getMmsText(String partId) {
        try {
            Uri partUri = Uri.parse("content://mms/part/" + partId);
            InputStream inputStream = contentResolver.openInputStream(partUri);

            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder text = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    text.append(line);
                }

                inputStream.close();
                return text.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading MMS text from part " + partId, e);
        }

        return null;
    }

    /**
     * Save bitmap to external storage
     */
    public boolean saveBitmapToFile(Bitmap bitmap, String filename) {
        try {
            File file = new File(context.getExternalFilesDir(null), filename);
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error saving bitmap to file", e);
            return false;
        }
    }

    /**
     * Save bitmap to gallery
     */
    public String saveBitmapToGallery(Bitmap bitmap, String title, String description) {
        try {
            return MediaStore.Images.Media.insertImage(contentResolver, bitmap, title, description);
        } catch (Exception e) {
            Log.e(TAG, "Error saving bitmap to gallery", e);
            return null;
        }
    }

    /**
     * Get byte array from bitmap
     */
    public byte[] getBitmapBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        return stream.toByteArray();
    }

    /**
     * Get latest MMS images (last 10)
     */
    public List<Bitmap> getLatestMmsImages(int limit) {
        List<Bitmap> images = new ArrayList<>();

        Cursor cursor = contentResolver.query(
                Uri.parse("content://mms"),
                new String[]{"_id"},
                null, null, "date DESC LIMIT " + limit
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String mmsId = cursor.getString(cursor.getColumnIndexOrThrow("_id"));

                // Get images from this MMS
                Cursor partCursor = contentResolver.query(
                        Uri.parse("content://mms/part"),
                        new String[]{"_id", "ct", "_data"},
                        "mid = ? AND ct LIKE 'image/%'",
                        new String[]{mmsId},
                        null
                );

                if (partCursor != null && partCursor.moveToFirst()) {
                    do {
                        String partId = partCursor.getString(partCursor.getColumnIndexOrThrow("_id"));
                        String data = partCursor.getString(partCursor.getColumnIndexOrThrow("_data"));

                        Bitmap image = getMmsImage(partId, data);
                        if (image != null) {
                            images.add(image);
                        }
                    } while (partCursor.moveToNext());
                    partCursor.close();
                }

            } while (cursor.moveToNext());
            cursor.close();
        }

        return images;
    }

    /**
     * Check if MMS has images
     */
    public boolean mmsHasImages(String mmsId) {
        Cursor cursor = contentResolver.query(
                Uri.parse("content://mms/part"),
                new String[]{"_id"},
                "mid = ? AND ct LIKE 'image/%'",
                new String[]{mmsId},
                null
        );

        boolean hasImages = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }

        return hasImages;
    }

    /**
     * Get MMS sender address
     */
    public String getMmsSender(String mmsId) {
        Cursor cursor = contentResolver.query(
                Uri.parse("content://mms/" + mmsId + "/addr"),
                new String[]{"address", "type"},
                "type = 137", // 137 = FROM
                null, null
        );

        String sender = null;
        if (cursor != null && cursor.moveToFirst()) {
            sender = cursor.getString(cursor.getColumnIndexOrThrow("address"));
            cursor.close();
        }

        return sender;
    }
}