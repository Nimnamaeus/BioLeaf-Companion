package com.example.plantdiseasedetection;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ImageAugmenter {
    private Random random = new Random();

    public List<Bitmap> augmentImage(Bitmap original, int numAugmentations) {
        List<Bitmap> augmentedImages = new ArrayList<>();
        augmentedImages.add(original); // Add original image

        for (int i = 0; i < numAugmentations; i++) {
            Bitmap augmented = original.copy(original.getConfig(), true);

            // Randomly apply different augmentations
            if (random.nextBoolean()) augmented = rotate(augmented);
            if (random.nextBoolean()) augmented = flip(augmented);
            if (random.nextBoolean()) augmented = adjustBrightness(augmented);
            if (random.nextBoolean()) augmented = adjustContrast(augmented);

            augmentedImages.add(augmented);
        }

        return augmentedImages;
    }

    private Bitmap rotate(Bitmap image) {
        Matrix matrix = new Matrix();
        float angle = random.nextInt(360); // Random rotation angle
        matrix.postRotate(angle);
        return Bitmap.createBitmap(image, 0, 0, image.getWidth(),
                image.getHeight(), matrix, true);
    }

    private Bitmap flip(Bitmap image) {
        Matrix matrix = new Matrix();
        if (random.nextBoolean()) {
            matrix.setScale(-1, 1); // Horizontal flip
        } else {
            matrix.setScale(1, -1); // Vertical flip
        }
        return Bitmap.createBitmap(image, 0, 0, image.getWidth(),
                image.getHeight(), matrix, true);
    }

    private Bitmap adjustBrightness(Bitmap image) {
        float brightness = 0.8f + random.nextFloat() * 0.4f; // 0.8 to 1.2
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.set(new float[] {
                brightness, 0, 0, 0, 0,
                0, brightness, 0, 0, 0,
                0, 0, brightness, 0, 0,
                0, 0, 0, 1, 0
        });
        return applyColorMatrix(image, colorMatrix);
    }

    private Bitmap adjustContrast(Bitmap image) {
        float contrast = 0.8f + random.nextFloat() * 0.4f; // 0.8 to 1.2
        float offset = (1 - contrast) * 128;
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.set(new float[] {
                contrast, 0, 0, 0, offset,
                0, contrast, 0, 0, offset,
                0, 0, contrast, 0, offset,
                0, 0, 0, 1, 0
        });
        return applyColorMatrix(image, colorMatrix);
    }

    private Bitmap applyColorMatrix(Bitmap image, ColorMatrix colorMatrix) {
        Bitmap result = Bitmap.createBitmap(image.getWidth(),
                image.getHeight(), image.getConfig());
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(image, 0, 0, paint);
        return result;
    }
}