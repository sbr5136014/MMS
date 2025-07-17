package smartart.tech.mmstest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MmsReceiver extends BroadcastReceiver {

    private static final String TAG = "MmsReceiver";
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received broadcast with action: " + action);

        if ("android.provider.Telephony.SMS_RECEIVED".equals(action)) {
            handleSmsReceived(context, intent);
        } else if ("android.provider.Telephony.WAP_PUSH_RECEIVED".equals(action)) {
            handleWapPushReceived(context, intent);
        } else if ("android.provider.Telephony.MMS_RECEIVED".equals(action)) {
            handleMmsReceived(context, intent);
        }
    }

    /**
     * Handle regular SMS messages
     */
    private void handleSmsReceived(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus != null) {
                for (Object pdu : pdus) {
                    SmsMessage message = SmsMessage.createFromPdu((byte[]) pdu);
                    String sender = message.getOriginatingAddress();
                    String body = message.getMessageBody();

                    Log.d(TAG, "SMS from " + sender + ": " + body);

                    // Handle SMS message
                    onSmsReceived(context, sender, body);
                }
            }
        }
    }

    /**
     * Handle WAP Push messages (MMS notifications)
     */
    private void handleWapPushReceived(Context context, Intent intent) {
        Log.d(TAG, "WAP Push received - MMS notification");

        // MMS notification received, wait a bit and then check for new MMS
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Wait for MMS to be processed by system
                    Thread.sleep(3000);

                    // Check for new MMS messages
                    checkForNewMms(context);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Thread interrupted", e);
                }
            }
        });
    }

    /**
     * Handle MMS received (Android 4.4+)
     */
    private void handleMmsReceived(Context context, Intent intent) {
        Log.d(TAG, "MMS received directly");

        // For Android 4.4+, MMS is handled differently
        Uri mmsUri = intent.getData();
        if (mmsUri != null) {
            Log.d(TAG, "MMS URI: " + mmsUri.toString());

            // Process MMS immediately
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    processMmsFromUri(context, mmsUri);
                }
            });
        } else {
            // Fallback to checking for new MMS
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                        checkForNewMms(context);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Thread interrupted", e);
                    }
                }
            });
        }
    }

    /**
     * Process MMS from specific URI
     */
    private void processMmsFromUri(Context context, Uri mmsUri) {
        try {
            // Extract MMS ID from URI
            String mmsId = mmsUri.getLastPathSegment();
            Log.d(TAG, "Processing MMS ID: " + mmsId);

            if (mmsId != null) {
                MmsImageExtractor extractor = new MmsImageExtractor(context);

                // Check if this MMS has images
                if (extractor.mmsHasImages(mmsId)) {
                    Log.d(TAG, "MMS " + mmsId + " contains images");

                    // Extract images from this specific MMS
                    extractor.extractMmsParts(mmsId, new MmsImageExtractor.OnImageFoundListener() {
                        @Override
                        public void onImageFound(Bitmap image, String contentType, String mmsId) {
                            Log.d(TAG, "Image found in received MMS: " + mmsId);

                            // Get sender information
                            String sender = extractor.getMmsSender(mmsId);

                            // Handle the received image
                            onMmsImageReceived(context, image, contentType, mmsId, sender);
                        }

                        @Override
                        public void onTextFound(String text, String mmsId) {
                            Log.d(TAG, "Text found in received MMS: " + text);

                            String sender = extractor.getMmsSender(mmsId);
                            onMmsTextReceived(context, text, mmsId, sender);
                        }

                        @Override
                        public void onVideoFound(Uri videoUri, String mmsId) {
                            Log.d(TAG, "Video found in received MMS: " + videoUri);

                            String sender = extractor.getMmsSender(mmsId);
                            onMmsVideoReceived(context, videoUri, mmsId, sender);
                        }
                    });
                } else {
                    Log.d(TAG, "MMS " + mmsId + " has no images");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing MMS from URI", e);
        }
    }

    /**
     * Check for new MMS messages (fallback method)
     */
    private void checkForNewMms(Context context) {
        try {
            Log.d(TAG, "Checking for new MMS messages");

            MmsImageExtractor extractor = new MmsImageExtractor(context);

            // Get latest MMS and check for images
            // This is a fallback when we can't get specific MMS ID
            extractor.getAllMmsImages(new MmsImageExtractor.OnImageFoundListener() {
                private boolean isFirstCall = true;

                @Override
                public void onImageFound(Bitmap image, String contentType, String mmsId) {
                    if (isFirstCall) {
                        // Only process the most recent MMS to avoid duplicates
                        Log.d(TAG, "New MMS image found: " + mmsId);

                        String sender = extractor.getMmsSender(mmsId);
                        onMmsImageReceived(context, image, contentType, mmsId, sender);

                        isFirstCall = false;
                    }
                }

                @Override
                public void onTextFound(String text, String mmsId) {
                    if (isFirstCall) {
                        Log.d(TAG, "New MMS text found: " + text);

                        String sender = extractor.getMmsSender(mmsId);
                        onMmsTextReceived(context, text, mmsId, sender);
                    }
                }

                @Override
                public void onVideoFound(Uri videoUri, String mmsId) {
                    if (isFirstCall) {
                        Log.d(TAG, "New MMS video found: " + videoUri);

                        String sender = extractor.getMmsSender(mmsId);
                        onMmsVideoReceived(context, videoUri, mmsId, sender);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error checking for new MMS", e);
        }
    }

    /**
     * Called when SMS is received
     * Override this method to handle SMS messages
     */
    protected void onSmsReceived(Context context, String sender, String body) {
        Log.d(TAG, "SMS received from " + sender + ": " + body);

        // Show toast notification
        Toast.makeText(context, "SMS from " + sender + ": " + body, Toast.LENGTH_SHORT).show();

        // TODO: Add your SMS handling logic here
        // For example:
        // - Save to database
        // - Send to server
        // - Show notification
        // - Process commands
    }

    /**
     * Called when MMS image is received
     * Override this method to handle MMS images
     */
    protected void onMmsImageReceived(Context context, Bitmap image, String contentType, String mmsId, String sender) {
        Log.d(TAG, "MMS image received from " + sender + " (MMS: " + mmsId + ")");

        // Show toast notification
        Toast.makeText(context, "MMS image from " + sender, Toast.LENGTH_SHORT).show();

        // Save image automatically
        MmsImageExtractor extractor = new MmsImageExtractor(context);
        String filename = "mms_" + mmsId + "_" + System.currentTimeMillis() + ".jpg";
        boolean saved = extractor.saveBitmapToFile(image, filename);

        if (saved) {
            Log.d(TAG, "MMS image saved to: " + filename);
        }

        // Save to gallery
        String galleryUri = extractor.saveBitmapToGallery(image,
                "MMS from " + sender, "Received MMS image");

        if (galleryUri != null) {
            Log.d(TAG, "MMS image saved to gallery: " + galleryUri);
        }

        // TODO: Add your MMS image handling logic here
        // For example:
        // - Process image with AI/ML
        // - Send to server
        // - Show in app UI
        // - Extract text from image (OCR)
        // - Resize/compress image

        // Example: Send to server
        // sendImageToServer(image, sender, mmsId);

        // Example: Show notification
        // showMmsNotification(context, sender, "Image received");
    }

    /**
     * Called when MMS text is received
     * Override this method to handle MMS text
     */
    protected void onMmsTextReceived(Context context, String text, String mmsId, String sender) {
        Log.d(TAG, "MMS text received from " + sender + ": " + text);

        // Show toast notification
        Toast.makeText(context, "MMS text from " + sender + ": " + text, Toast.LENGTH_SHORT).show();

        // TODO: Add your MMS text handling logic here
    }

    /**
     * Called when MMS video is received
     * Override this method to handle MMS videos
     */
    protected void onMmsVideoReceived(Context context, Uri videoUri, String mmsId, String sender) {
        Log.d(TAG, "MMS video received from " + sender + ": " + videoUri);

        // Show toast notification
        Toast.makeText(context, "MMS video from " + sender, Toast.LENGTH_SHORT).show();

        // TODO: Add your MMS video handling logic here
        // For example:
        // - Save video to file
        // - Extract thumbnail
        // - Send to server
        // - Show in video player
    }

    /**
     * Example method to send image to server
     */
    private void sendImageToServer(Bitmap image, String sender, String mmsId) {
        // Convert bitmap to byte array
        MmsImageExtractor extractor = new MmsImageExtractor(null);
        byte[] imageBytes = extractor.getBitmapBytes(image);

        // TODO: Implement server upload logic
        Log.d(TAG, "Sending image to server - Size: " + imageBytes.length + " bytes");

        // Example HTTP POST request would go here
    }

    /**
     * Example method to show notification
     */
    private void showMmsNotification(Context context, String sender, String message) {
        // TODO: Implement notification display
        // Use NotificationManager to show notification
        Log.d(TAG, "Showing notification: " + message + " from " + sender);
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}