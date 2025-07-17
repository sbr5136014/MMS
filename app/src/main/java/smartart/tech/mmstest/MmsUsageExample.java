package smartart.tech.mmstest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;

public class MmsUsageExample extends Activity {

    private static final String TAG = "MmsUsageExample";
    private MmsImageExtractor mmsExtractor;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the MMS extractor
        mmsExtractor = new MmsImageExtractor(this);

        // Example 1: Get all MMS images
        getAllMmsImages();

        // Example 2: Get latest 5 MMS images
        getLatestImages();

        // Example 3: Get MMS images from specific thread
        // getMmsImagesFromThread(123);
    }

    /**
     * Example 1: Get all MMS images with callback
     */
    private void getAllMmsImages() {
        mmsExtractor.getAllMmsImages(new MmsImageExtractor.OnImageFoundListener() {
            @Override
            public void onImageFound(Bitmap image, String contentType, String mmsId) {
                Log.d(TAG, "Found image in MMS " + mmsId + " with type: " + contentType);

                // Display image in ImageView
                if (imageView != null) {
                    imageView.setImageBitmap(image);
                }

                // Save to file
                String filename = "mms_image_" + mmsId + "_" + System.currentTimeMillis() + ".jpg";
                boolean saved = mmsExtractor.saveBitmapToFile(image, filename);
                if (saved) {
                    Log.d(TAG, "Image saved to: " + filename);
                }

                // Save to gallery
                String galleryUri = mmsExtractor.saveBitmapToGallery(image,
                        "MMS Image", "Image from MMS " + mmsId);
                if (galleryUri != null) {
                    Log.d(TAG, "Image saved to gallery: " + galleryUri);
                }
            }

            @Override
            public void onTextFound(String text, String mmsId) {
                Log.d(TAG, "Found text in MMS " + mmsId + ": " + text);
                Toast.makeText(MmsUsageExample.this, "Text: " + text, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onVideoFound(Uri videoUri, String mmsId) {
                Log.d(TAG, "Found video in MMS " + mmsId + ": " + videoUri);
                // Handle video playback here
            }
        });
    }

    /**
     * Example 2: Get latest MMS images (simple method)
     */
    private void getLatestImages() {
        java.util.List<Bitmap> latestImages = mmsExtractor.getLatestMmsImages(5);

        Log.d(TAG, "Found " + latestImages.size() + " recent images");

        for (int i = 0; i < latestImages.size(); i++) {
            Bitmap image = latestImages.get(i);

            // Process each image
            String filename = "latest_image_" + i + ".jpg";
            mmsExtractor.saveBitmapToFile(image, filename);

            Log.d(TAG, "Processed image " + i + ", size: " +
                    image.getWidth() + "x" + image.getHeight());
        }
    }

    /**
     * Example 3: Get MMS images from specific conversation thread
     */
    private void getMmsImagesFromThread(int threadId) {
        mmsExtractor.getMmsImagesFromThread(threadId, new MmsImageExtractor.OnImageFoundListener() {
            @Override
            public void onImageFound(Bitmap image, String contentType, String mmsId) {
                Log.d(TAG, "Found image in thread " + threadId + ", MMS: " + mmsId);

                // Get sender information
                String sender = mmsExtractor.getMmsSender(mmsId);
                Log.d(TAG, "Image sender: " + sender);

                // Process the image
                processImage(image, mmsId, sender);
            }

            @Override
            public void onTextFound(String text, String mmsId) {
                Log.d(TAG, "Found text in thread " + threadId + ": " + text);
            }

            @Override
            public void onVideoFound(Uri videoUri, String mmsId) {
                Log.d(TAG, "Found video in thread " + threadId + ": " + videoUri);
            }
        });
    }

    /**
     * Process individual image
     */
    private void processImage(Bitmap image, String mmsId, String sender) {
        // Example processing
        int width = image.getWidth();
        int height = image.getHeight();

        Log.d(TAG, "Processing image from " + sender +
                " (MMS: " + mmsId + ") - Size: " + width + "x" + height);

        // Convert to byte array if needed
        byte[] imageBytes = mmsExtractor.getBitmapBytes(image);
        Log.d(TAG, "Image byte size: " + imageBytes.length);

        // Save with sender info in filename
        String filename = "mms_" + mmsId + "_from_" +
                (sender != null ? sender.replaceAll("[^a-zA-Z0-9]", "_") : "unknown") + ".jpg";
        mmsExtractor.saveBitmapToFile(image, filename);

        // You can also send to server, process with ML, etc.
        // sendImageToServer(imageBytes, mmsId, sender);
    }

    /**
     * Check if specific MMS has images before processing
     */
    private void checkMmsForImages(String mmsId) {
        if (mmsExtractor.mmsHasImages(mmsId)) {
            Log.d(TAG, "MMS " + mmsId + " contains images");
            // Process this MMS
        } else {
            Log.d(TAG, "MMS " + mmsId + " has no images");
        }
    }
}