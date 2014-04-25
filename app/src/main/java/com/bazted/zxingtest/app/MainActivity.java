package com.bazted.zxingtest.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.aztec.AztecReader;
import com.google.zxing.common.HybridBinarizer;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends ActionBarActivity {


    private ImageView originImageView;
    private ImageView effectedImageView;
    private EditText brightnessEt;
    private EditText contrastEt;
    private CheckBox greyScaleCb;

    private final static String IMAGE_PATH = "content://media/external/images/media/971";

    private String selectedImagePath = IMAGE_PATH;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        originImageView = (ImageView) findViewById(R.id.origin_bitmap_image);
        effectedImageView = (ImageView) findViewById(R.id.effected_bitmap_image);
        brightnessEt = (EditText) findViewById(R.id.brightness);
        contrastEt = (EditText) findViewById(R.id.contrast);
        greyScaleCb = (CheckBox) findViewById(R.id.grey_scale_cb);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
//            IntentIntegrator integrator = new IntentIntegrator(this);
//            integrator.initiateScan();

            dispatchTakePictureIntent();

            return true;
        }
        if (id == R.id.action_galery) {
            callGaleryIntent();
            return true;
        }
        if (id == R.id.action_load_image) {
            startAsyncDecode(selectedImagePath);
            return true;
        }
        if (id == R.id.action_scan) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static final int REQUEST_TAKE_PHOTO = 9123;
    private static final int REQUEST_CHOOSE_PHOTO = 9124;

    private String mCurrentPhotoPath;

    protected void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        PackageManager packageManager = getPackageManager();
        if (packageManager != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("CameraListenerFragment", "Error occurred while creating the File:" + ex);
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }


    protected void callGaleryIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CHOOSE_PHOTO);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
//        if (scanResult != null) {
//            Log.e("scan", scanResult.toString());
//            handle scan result
//        }
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_TAKE_PHOTO) {
                galleryAddPic();
                startAsyncDecode(mCurrentPhotoPath);

//                receivedPath(mCurrentPhotoPath);
            } else if (requestCode == REQUEST_CHOOSE_PHOTO) {
                if (data != null && data.getData() != null) {
                    startAsyncDecode(data.getData().toString());

//                    receivedPath(data.getData().toString());
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startAsyncDecode(String path) {
        if (path != null) {
            selectedImagePath = path;
            new AsyncDecode(path, getBrightness(), getContrast(), isGreyScale()).execute();
        }
    }

    private int getBrightness() {
        if (brightnessEt != null && brightnessEt.getText() != null) {
            String string = brightnessEt.getText().toString();
            if (!TextUtils.isEmpty(string)) {
                int i = Integer.parseInt(string);
                if (i >= -255 && i <= 255) {
                    return i;
                }
            }
        }
        return 0;
    }

    private int getContrast() {
        if (contrastEt != null && contrastEt.getText() != null) {
            String string = contrastEt.getText().toString();
            if (!TextUtils.isEmpty(string)) {
                int i = Integer.parseInt(string);
                if (i >= 0 && i <= 10) {
                    return i;
                }
            }
        }
        return 1;
    }

    private boolean isGreyScale() {
        return greyScaleCb != null && greyScaleCb.isChecked();
    }

    //
    private class AsyncDecode extends AsyncTask<Void, Void, Bitmap[]> {

        private final String photoPath;
        private final int brightness;
        private final int contrast;
        private final boolean greyScale;

        private AsyncDecode(String photoPath, int brightness, int contrast, boolean greyScale) {
            this.photoPath = photoPath;
            this.brightness = brightness;
            this.contrast = contrast;
            this.greyScale = greyScale;
        }

        @Override
        protected Bitmap[] doInBackground(Void... params) {
            return decodePathToBitmapAndDecode(photoPath,
                    brightness, contrast, greyScale);
        }

        @Override
        protected void onPostExecute(Bitmap[] bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap != null) {
                originImageView.setImageBitmap(bitmap[0]);
                effectedImageView.setImageBitmap(bitmap[1]);
            }
        }
    }

    private Bitmap[] decodePathToBitmapAndDecode(String mCurrentPhotoPath,
                                                 int brightness,
                                                 int contrast,
                                                 boolean greyScale) {

        Log.e("pathForamCamera", mCurrentPhotoPath);

        Bitmap originalBitmap = null;
        try {
            originalBitmap = Picasso.with(this).load(mCurrentPhotoPath).get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (originalBitmap != null) {
            Bitmap effected = originalBitmap;
            if (greyScale) {
                effected = toGrayscale(originalBitmap);
            }
            effected = changeBitmapContrastBrightness(effected, contrast, brightness);

            int[] intArray = new int[effected.getWidth() * effected.getHeight()];
            effected.getPixels(intArray, 0, effected.getWidth(), 0, 0, effected.getWidth(), effected.getHeight());
            LuminanceSource source = new RGBLuminanceSource(effected.getWidth(), effected.getHeight(), intArray);

            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            Reader reader = new AztecReader();
            try {
                Result decode = reader.decode(binaryBitmap);
                if (decode != null) {
                    Log.e("decoded aztec", decode.toString());
                }

            } catch (NotFoundException e) {
                Log.e("reader", "not found=" + e);
                e.printStackTrace();
            } catch (ChecksumException e) {
                Log.e("reader", "ChecksumException=" + e);

                e.printStackTrace();
            } catch (FormatException e) {
                Log.e("reader", "FormatException=" + e);
                e.printStackTrace();
            }
            return new Bitmap[]{originalBitmap, effected};
        } else {
            Log.e("decode", "bitmap ==null");
        }
        return null;
    }


    /**
     * @param bmp        input bitmap
     * @param contrast   0..10 1 is default
     * @param brightness -255..255 0 is default
     * @return new bitmap
     */
    public static Bitmap changeBitmapContrastBrightness(Bitmap bmp, float contrast, float brightness) {
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        contrast, 0, 0, 0, brightness,
                        0, contrast, 0, 0, brightness,
                        0, 0, contrast, 0, brightness,
                        0, 0, 0, 1, 0
                });

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

        Canvas canvas = new Canvas(ret);

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, 0, 0, paint);

        return ret;
    }

    public Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }
}
