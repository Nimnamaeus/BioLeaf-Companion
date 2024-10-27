package com.example.plantdiseasedetection;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Picture;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.app.AlertDialog;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdiseasedetection.ml.DiseaseDetection;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity{

    DatabaseHelper databaseHelper;

    TextView result, demoTxt, classification, clickHere;
    ImageView imageView, arrowImage;
    Button picture, galleryButton, moreInfoButton;

    int imageSize = 224; //default image size

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this); // Declare DatabaseHelper

        result = findViewById(R.id.result);
        imageView = findViewById(R.id.imageView);
        picture = findViewById(R.id.button);
        galleryButton = findViewById(R.id.gallery_button);  // New gallery button
        moreInfoButton = findViewById(R.id.moreInfoButton); // New button for more info

        moreInfoButton.setVisibility(View.GONE);  // Initially hidden
        demoTxt = findViewById(R.id.demoText);
        clickHere = findViewById(R.id.clickhere);
        arrowImage = findViewById(R.id.demoArrow);
        classification = findViewById(R.id.classification);

        demoTxt.setVisibility(View.VISIBLE);
        clickHere.setVisibility(View.GONE);
        arrowImage.setVisibility(View.VISIBLE);
        classification.setVisibility(View.GONE);
        result.setVisibility(View.GONE);

        picture.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                //launch camera if permitted
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 1);
                } else {
                    //request camera not permitted
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });

        // Set up gallery button to select image from gallery
        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, 2);  // Request code 2 for gallery selection
            }
        });

        moreInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String diseaseName = result.getText().toString();
                String description = databaseHelper.getDiseaseDescription(diseaseName);

                if (description != null) {
                    // Display the disease information in a pop-up
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Diagnosis for " + diseaseName)
                            .setMessage(description)
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    // Handle case where no description is found
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Information not found")
                            .setMessage("No information available for " + diseaseName)
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Bitmap image = null;

            if (requestCode == 1) {
                // Handle camera image
                image = (Bitmap) data.getExtras().get("data");
                int dimension = Math.min(image.getWidth(), image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
            } else if (requestCode == 2) {
                // Handle gallery image
                Uri imageUri = data.getData();
                try {
                    image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (image != null) {
                imageView.setImageBitmap(image);

                demoTxt.setVisibility(View.GONE);
                clickHere.setVisibility(View.VISIBLE);
                arrowImage.setVisibility(View.GONE);
                classification.setVisibility(View.VISIBLE);
                result.setVisibility(View.VISIBLE);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);
            }
        }
    }

    private void classifyImage(Bitmap image) {
        try {
            DiseaseDetection model = DiseaseDetection.newInstance(getApplicationContext());

            // Create input for reference
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, imageSize, imageSize, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            // Get 1D array of 224 * 224 pixels
            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

            // Iterate over pixels and extract RGB values, add to bytebuffer
            int pixel = 0;
            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Run model inference and get result
            DiseaseDetection.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidence = outputFeature0.getFloatArray();

            // Find the index of the class with the biggest confidence
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidence.length; i++) {
                if (confidence[i] > maxConfidence) {
                    maxConfidence = confidence[i];
                    maxPos = i;
                }
            }

            String[] classes = {"Maize Leaf Blight", "Coconut Yellowing Leaf", "Rice Bacterial Blight"};
            result.setText(classes[maxPos]);
            result.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Search the disease on the internet
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/search?q=" + result.getText())));
                }
            });

            moreInfoButton.setVisibility(View.VISIBLE);

            model.close();

        } catch (IOException e) {
            // Handle exception
        }
    }
}
