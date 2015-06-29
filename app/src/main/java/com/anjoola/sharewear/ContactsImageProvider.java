package com.anjoola.sharewear;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Gets the corresponding photo for a contact. If one does not exist, creates
 * a generic one with just the first letter of their name. Courtesy of
 * http://stackoverflow.com/questions/23122088/colored-boxed-with-letters-a-la-gmail.
 */
public class ContactsImageProvider {
    private Context mContext;

    // Possible default background colors.
    private final int[] DEFAULT_COLORS = {
            Color.parseColor("#f16364"),
            Color.parseColor("#f58559"),
            Color.parseColor("#f9a43e"),
            Color.parseColor("#e4c62e"),
            Color.parseColor("#67bf74"),
            Color.parseColor("#59a2be"),
            Color.parseColor("#2093cd"),
            Color.parseColor("#ad62a7")
    };

    // Sizes.
    private final int FONT_SIZE = 140;
    private final int IMAGE_SIZE = 200;

    // Used for drawing the letter-image.
    private final TextPaint mPaint = new TextPaint();
    private final Rect mBounds = new Rect();
    private final Canvas mCanvas = new Canvas();

    // Character to draw on default image.
    private final char[] mFirstChar = new char[1];

    public ContactsImageProvider(Context context) {
        mContext = context;

        mPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        mPaint.setColor(Color.WHITE);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setAntiAlias(true);
    }

    /**
     * Get the default contact photo for the given name. Generate the color
     * based on the hash code of the name.
     *
     * @param displayName The name used for the first letter.
     * @return A bitmap that contains a letter used in the English alphabet
     *         or digit. If there is no letter or digit available, a default
     *         image is shown instead.
     */
    public Bitmap getDefaultContactPhoto(String displayName) {
        final Bitmap bitmap = Bitmap.createBitmap(IMAGE_SIZE, IMAGE_SIZE,
                Bitmap.Config.ARGB_8888);
        final char firstChar = displayName.charAt(0);

        final Canvas canvas = mCanvas;
        canvas.setBitmap(bitmap);
        canvas.drawColor(pickColor(displayName.hashCode()));

        // Get first character.
        if (isEnglishLetterOrDigit(firstChar))
            mFirstChar[0] = Character.toUpperCase(firstChar);
        else
            mFirstChar[0] = '\u263a';

        mPaint.setTextSize(FONT_SIZE);
        mPaint.getTextBounds(mFirstChar, 0, 1, mBounds);
        canvas.drawText(mFirstChar, 0, 1, IMAGE_SIZE / 2, IMAGE_SIZE / 2
                + (mBounds.bottom - mBounds.top) / 2, mPaint);

        return bitmap;
    }

    /**
     * Get the image path for a given array of bytes. Creates a temporary file
     * with the bytes as a bitmap. If bytes is null, then create a default
     * image with just the first letter of the name as the image.
     *
     * @param bytes Array of bytes for the image.
     * @param contactName Name of the contact.
     * @return A string containing the file path of the image.
     */
    public String getImagePath(byte[] bytes, String contactName) {
        Bitmap bitmap;

        // No contact photo, create default letter-image.
        if (bytes == null)
            bitmap = getDefaultContactPhoto(contactName);
        else
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        // Create temporary file to store contact image.
        File cacheDirectory = mContext.getCacheDir();
        File tmpFile = new File(cacheDirectory.getPath() + "/wpta_" +
                contactName.hashCode() + ".png");
        try {
            FileOutputStream fOutStream = new FileOutputStream(tmpFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOutStream);

            fOutStream.flush();
            fOutStream.close();
        } catch (Exception e) { }

        return tmpFile.getPath();
    }

    /**
     * Returns whether or not the character is a letter or digit.
     *
     * @param c The char to check
     * @return True if it is in the English alphabet or is a digit, false
     *         otherwise.
     */
    private static boolean isEnglishLetterOrDigit(char c) {
        return ('A' <= c && c <= 'Z' || 'a' <= c && c <= 'z' ||
                '0' <= c && c <= '9');
    }

    /**
     * Pick a color based on an integer key.
     *
     * @param key The key used to choose the color.
     * @return A color.
     */
    private int pickColor(int key) {
        return DEFAULT_COLORS[Math.abs(key) % DEFAULT_COLORS.length];
    }
}
