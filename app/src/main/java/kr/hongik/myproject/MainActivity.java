package kr.hongik.myproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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

public class MainActivity extends AppCompatActivity implements AutoPermissionsListener {
    boolean isPageOpen = false;

    Animation translateLeftAnim;
    Animation translateRightAnim;

    LinearLayout page;
    Button menubutton, photobutton;
    ImageView imageView;
    File file;

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

        photobutton = findViewById(R.id.photoButton); //사진찍기 버튼
        photobutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePickture();
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

    public void takePickture() { //이 메서드 호출시 파일 만듬
        if(file == null){
            file = createFile();
        }

        Uri fileUri = FileProvider.getUriForFile(this, "kr.hongik.myproject.fileprovider", file); //Uri 객체
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); //카메라 앱 띄우기
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        if(intent.resolveActivity(getPackageManager()) !=null) {
            startActivityForResult(intent, 101);
        }
    }

    private File createFile() { //사진을 찍은 후 저장할 파일
        String filename = "capture.jpg";
        File storageDir = Environment.getExternalStorageDirectory();
        File outFile = new File(storageDir, filename);

        return outFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) { //카메라 앱 닫기
        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode ==101 && resultCode == RESULT_OK) {
            BitmapFactory.Options options = new BitmapFactory.Options(); //이미지 파일을 bitmap 객체로 만들기
            options.inSampleSize = 8;
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);

            imageView.setImageBitmap(bitmap); //이미지뷰에 bitmap 설정하기
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