package smartart.tech.mmstest;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Custom MmsReceiver that extends the base MmsReceiver
 * Add your specific logic here
 */
public class CustomMmsReceiver extends MmsReceiver {

    private static final String TAG = "CustomMmsReceiver";
    private static final String CHANNEL_ID = "mms_channel";
    private static final String PREFS_NAME = "mms_prefs";

    @Override
    protected void onSmsReceived(Context context, String sender, String body) {
        super.onSmsReceived(context, sender, body);

        Log.d(TAG, "Processing SMS from " + sender + ": " + body);

        // Save SMS to preferences for later reference
        saveSmsToPrefs(context, sender, body);

        // Check for specific keywords
        if (body.toLowerCase().contains("urgent") || body.toLowerCase().contains("emergency")) {
            showHighPriorityNotification(context, sender, body, false);
        }

        // Auto-reply logic (if needed)
        if (body.toLowerCase().contains("status")) {
            // TODO: Send auto-reply
            Log.d(TAG, "Status request received from " + sender);
        }
    }

    @Override
    protected void onMmsImageReceived(Context context, Bitmap image, String contentType, String mmsId, String sender) {
        super.onMmsImageReceived(context, image, contentType, mmsId, sender);

        Log.d(TAG, "Custom processing MMS image from " + sender);

        // Create notification channel if needed
        createNotificationChannel(context);

        // Save with custom naming
        String customFilename = generateCustomFilename(sender, mmsId);
        MmsImageExtractor extractor = new MmsImageExtractor(context);
        boolean saved = extractor.saveBitmapToFile(image, customFilename);

        if (saved) {
            Log.d(TAG, "Image saved with custom filename: " + customFilename);

            // Show custom notification
            showImageReceivedNotification(context, sender, customFilename);

            // Save metadata
            saveMmsMetadata(context, mmsId, sender, customFilename, contentType);
        }

        // Analyze image (example: check if it's a document, face, etc.)
        analyzeImage(context, image, sender, mmsId);

        // Send to your backend server
        // uploadImageToServer(image, sender, mmsId);
    }

    @Override
    protected void onMmsTextReceived(Context context, String text, String mmsId, String sender) {
        super.onMmsTextReceived(context, text, mmsId, sender);

        Log.d(TAG, "Custom processing MMS text from " + sender + ": " + text);

        // Save text to file
        saveMmsTextToFile(context, text, sender, mmsId);

        // Process text for keywords
        if (text.toLowerCase().contains("urgent") || text.toLowerCase().contains("emergency")) {
            showHighPriorityNotification(context, sender, text, true);
        }
    }

    @Override
    protected void onMmsVideoReceived(Context context, Uri videoUri, String mmsId, String sender) {
        super.onMmsVideoReceived(context, videoUri, mmsId, sender);

        Log.d(TAG, "Custom processing MMS video from " + sender);

        // Save video metadata
        saveMmsVideoMetadata(context, videoUri, sender, mmsId);

        // Show notification
        showVideoReceivedNotification(context, sender);
    }

    /**
     * Generate custom filename based on sender and timestamp
     */
    private String generateCustomFilename(String sender, String mmsId) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String safeSender = sender != null ? sender.replaceAll("[^a-zA-Z0-9]", "_") : "unknown";
        return "mms_" + safeSender + "_" + timestamp + "_" + mmsId + ".jpg";
    }

    /**
     * Create notification channel for Android 8.0+
     */
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "MMS Notifications";
            String description = "Notifications for received MMS messages";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Show notification when image is received
     */
    private void showImageReceivedNotification(Context context, String sender, String filename) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("MMS Image Received")
                .setContentText("Image from " + sender + " saved as " + filename)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }

    /**
     * Show high priority notification for urgent messages
     */
    private void showHighPriorityNotification(Context context, String sender, String message, boolean isMms) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String title = isMms ? "Urgent MMS" : "Urgent SMS";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText("From " + sender + ": " + message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 1000, 500, 1000});

        notificationManager.notify(2, builder.build());
    }

    /**
     * Show notification when video is received
     */
    private void showVideoReceivedNotification(Context context, String sender) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("MMS Video Received")
                .setContentText("Video from " + sender)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(3, builder.build());
    }

    /**
     * Save SMS to SharedPreferences
     */
    private void saveSmsToPrefs(Context context, String sender, String body) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String timestamp = String.valueOf(System.currentTimeMillis());
        editor.putString("sms_" + timestamp, sender + "|" + body);
        editor.apply();
    }

    /**
     * Save MMS metadata
     */
    private void saveMmsMetadata(Context context, String mmsId, String sender, String filename, String contentType) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String timestamp = String.valueOf(System.currentTimeMillis());
        String metadata = mmsId + "|" + sender + "|" + filename + "|" + contentType + "|" + timestamp;
        editor.putString("mms_" + mmsId, metadata);
        editor.apply();

        Log.d(TAG, "MMS metadata saved: " + metadata);
    }

    /**
     * Save MMS text to file
     */
    private void saveMmsTextToFile(Context context, String text, String sender, String mmsId) {
        try {
            String filename = "mms_text_" + mmsId + "_" + System.currentTimeMillis() + ".txt";
            File file = new File(context.getExternalFilesDir(null), filename);

            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write("From: " + sender + "\n");
            writer.write("MMS ID: " + mmsId + "\n");
            writer.write("Timestamp: " + new Date().toString() + "\n");
            writer.write("Content:\n" + text);
            writer.close();

            Log.d(TAG, "MMS text saved to: " + filename);
        } catch (Exception e) {
            Log.e(TAG, "Error saving MMS text", e);
        }
    }

    /**
     * Save MMS video metadata
     */
    private void saveMmsVideoMetadata(Context context, Uri videoUri, String sender, String mmsId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String timestamp = String.valueOf(System.currentTimeMillis());
        String metadata = mmsId + "|" + sender + "|" + videoUri.toString() + "|video|" + timestamp;
        editor.putString("mms_video_" + mmsId, metadata);
        editor.apply();

        Log.d(TAG, "MMS video metadata saved: " + metadata);
    }

    /**
     * Analyze image content (example implementation)
     */
    private void analyzeImage(Context context, Bitmap image, String sender, String mmsId) {
        // Get image dimensions
        int width = image.getWidth();
        int height = image.getHeight();

        Log.d(TAG, "Image analysis - Size: " + width + "x" + height);

        // Check if image is very small (might be emoji or icon)
        if (width < 100 && height < 100) {
            Log.d(TAG, "Small image detected - possibly emoji or icon");
        }

        // Check aspect ratio
        float aspectRatio = (float) width / height;
        if (aspectRatio > 2.0f || aspectRatio < 0.5f) {
            Log.d(TAG, "Unusual aspect ratio detected: " + aspectRatio);
        }

        // TODO: Add more sophisticated image analysis
        // - Face detection
        // - Text recognition (OCR)
        // - Object detection
        // - Image quality assessment
    }

    /**
     * Upload image to server (example implementation)
     */
    private void uploadImageToServer(Bitmap image, String sender, String mmsId) {
        // Convert to byte array
        MmsImageExtractor extractor = new MmsImageExtractor(null);
        byte[] imageBytes = extractor.getBitmapBytes(image);

        // TODO: Implement actual server upload
        Log.d(TAG, "Would upload image to server - Size: " + imageBytes.length + " bytes");

        // Example server upload logic:
        // 1. Create HTTP client
        // 2. Prepare multipart request
        // 3. Add image data and metadata
        // 4. Send request
        // 5. Handle response
    }

    /**
     * Get all saved MMS metadata
     */
    public void getAllSavedMmsMetadata(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith("mms_")) {
                String metadata = prefs.getString(key, "");
                Log.d(TAG, "Saved MMS: " + key + " = " + metadata);
            }
        }
    }
}