package kr.hongik.myproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.pedro.library.AutoPermissions;
import com.pedro.library.AutoPermissionsListener;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements AutoPermissionsListener {
    boolean isPageOpen = false;

    Animation translateLeftAnim;
    Animation translateRightAnim;

    LinearLayout page;
    Button menubutton;
    ImageView imageView;

    static File currentPhotoFile;
    static Uri currentPhotoUri;
    static String currentPhotoPath;
    static String currentPhotoFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        page = findViewById(R.id.page);
        imageView = findViewById(R.id.photo);

        translateLeftAnim = AnimationUtils.loadAnimation(this, R.anim.translate_left); //왼쪽 이동
        translateRightAnim = AnimationUtils.loadAnimation(this, R.anim.translate_right); //오른쪽이동

        SlidingPageAnimationListener animListener = new SlidingPageAnimationListener(); //슬라이딩 애니메이션
        translateLeftAnim.setAnimationListener(animListener); //왼쪽 이동 애니메이션
        translateRightAnim.setAnimationListener(animListener); //오른쪽 이동 애니메이션

        menubutton = findViewById(R.id.menuButton); //메뉴 버튼
        menubutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPageOpen){
                    page.startAnimation(translateRightAnim);
                } else {
                    page.setVisibility(View.VISIBLE);
                    page.startAnimation(translateLeftAnim);
                }
            }
        });

        AutoPermissions.Companion.loadAllPermissions(this, 101); //자동권한 부여
    }

    private class SlidingPageAnimationListener implements Animation.AnimationListener { //슬라이딩 애니메이션
        public void onAnimationEnd(Animation animation) {
            if (isPageOpen) {
                page.setVisibility(View.INVISIBLE);

                menubutton.setText("Open");
                isPageOpen = false;
            } else {
                menubutton.setText("Close");
                isPageOpen = true;
            }
        }

        @Override
        public void onAnimationStart(Animation animation) { }

        @Override
        public void onAnimationRepeat(Animation animation) { }
    }

    public void takePickture(View view) throws IOException { //이 메서드 호출시 파일 만듬

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); //카메라 앱 띄우기
        if(intent.resolveActivity(getPackageManager()) !=null) { //이미지파일생성
            File imageFile = createImageFile();
            if(imageFile != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File imagePath = getExternalFilesDir("images"); //외부저장소 사용
        File newFile = File.createTempFile(imageFileName, ".jpg", imagePath); //파일생성

        currentPhotoFile = newFile;
        currentPhotoFileName = newFile.getName();
        currentPhotoPath = newFile.getAbsolutePath(); //파일의 절대경로를 저장한다.

        try {
            currentPhotoUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    newFile);
        } catch (Exception ex) {
            Log.d("FileProvider", ex.getMessage());
            ex.printStackTrace();
            throw ex;
        }

        return newFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) { //카메라 앱 닫기
        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode ==REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if(currentPhotoUri != null) {
                imageView.setImageURI(currentPhotoUri);

                galleryAddPic(currentPhotoUri, currentPhotoFileName);
            }
        }
        if(requestCode ==REQUEST_IMAGE_OPEN && resultCode ==RESULT_OK){
            Uri selectImageUri = data.getData();

            Bitmap antiRotationBitmap = createAntiRotationSampleBitmap(selectImageUri, imageView.getWidth(), imageView.getHeight());
            imageView.setImageBitmap(antiRotationBitmap);
        }
    }

    private Uri galleryAddPic(Uri srcImageFileUri, String srcImageFileName) {
        ContentValues contentValues = new ContentValues();
        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, srcImageFileName);
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/MyImages");
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 1);
        Uri newImageFileUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        try{
            AssetFileDescriptor afdInput = contentResolver.openAssetFileDescriptor(srcImageFileUri, "r");
            AssetFileDescriptor afdOutput = contentResolver.openAssetFileDescriptor(newImageFileUri, "w");
            FileInputStream fis = afdInput.createInputStream();
            FileOutputStream fos = afdOutput.createOutputStream();

            byte[] readByteBuf = new byte[1024];
            while(true){
                int readLen = fis.read(readByteBuf);
                if(readLen <= 0){
                    break;
                }
                fos.write(readByteBuf, 0, readLen);
            }

            fos.flush();
            fos.close();
            afdOutput.close();

            fis.close();
            afdInput.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        contentValues.clear();
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0);
        contentResolver.update(newImageFileUri, contentValues, null, null);
        return newImageFileUri;
    }

    private Bitmap createAntiRotationSampleBitmap(Uri scrImageFileUri, int dstWidth, int dstHeight) {
        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        ParcelFileDescriptor pfdExif = null;
        int orientation = 0;
        try {
            pfdExif = contentResolver.openFileDescriptor(scrImageFileUri, "r");
            FileDescriptor fdExif = pfdExif.getFileDescriptor();
            ExifInterface exifInterface = null;
            exifInterface = new ExifInterface(fdExif);
            orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            Log.d("orientation", String.valueOf(orientation));
            pfdExif.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Bitmap bitmap = createSampledBitmap(scrImageFileUri, dstWidth, dstHeight);

        Matrix matrix = new Matrix();

        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1); //좌우반전
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
//              matrix.setRotate(180);
//              matrix.postScale(-1, 1); //좌우반전
                matrix.setScale(1, -1); //상하반전
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1); //좌우반전
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1); //좌우반전
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap antiRotationBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle(); //bitmap은 더이상 필요 없음으로 메모리에서 free시킨다.
            return antiRotationBitmap;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) { //접근 권한 결과
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AutoPermissions.Companion.parsePermissions(this, requestCode, permissions, this);
    }

    @Override
    public void onDenied(int requestCode, @NonNull String[] permissions) { //접근 실패시
        Toast.makeText(this, "permissions denied : " + permissions.length, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onGranted(int requestCode, @NonNull String[] permissions) { //접근 성공시
        Toast.makeText(this, "permissions granted : " + permissions.length, Toast.LENGTH_LONG).show();
    }
}