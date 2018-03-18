package com.example.cameraalbumdemo;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Picture;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public static final int TAKE_PHOTO = 1;
    public static final int CHOOSE_FROM_ALBUM = 2;

    private ImageView imageView;
    private Button btnTakePjoto, btnChoose;
    private Uri imageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnChoose = findViewById(R.id.choose_from_album);
        btnTakePjoto = findViewById(R.id.take_photo);
        imageView = findViewById(R.id.picture);

        btnChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //运行时权限
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission
                        .WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {//没获取权限
                    ActivityCompat.requestPermissions(MainActivity.this
                            , new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}//申请权限
                            , 1);
                } else {
                    //已经得到权限
                    openAlbum();
                }

            }
        });

        btnTakePjoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //创建file  对象用于存储拍照后的图片
                File outImage = new File(getExternalCacheDir(), "outIma.jpg");
                try {
                    if (outImage.exists()) {
                        outImage.delete();
                    }
                    outImage.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (Build.VERSION.SDK_INT >= 24) {
                    imageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.camerealbumdemo", outImage);

                } else {
                    imageUri = Uri.fromFile(outImage);//将File 对像转换成uri 对象 兼容7.0之后
                }

                //启动相机
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);//指定图片的输出地址
                startActivityForResult(intent, TAKE_PHOTO);


            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {//拍照返回成功
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        imageView.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case CHOOSE_FROM_ALBUM:
                if(resultCode==RESULT_OK){
                    //判断系统版本号
                    if(Build.VERSION.SDK_INT>=19){
                        //4.4 以上 选取相册的图片不在返回真实的uri 而是封装过的uri
                        handleImageOnKitkat(data);
                    }else {

                        handleImagebeforekitkat(data);
                    }
                }
                break;
        }
    }

private void handleImageOnKitkat(Intent data){
        Uri uri=data.getData();

        String imagePath=null;
        if(DocumentsContract.isDocumentUri(this,uri)){
            //如果是document 类型的uri 通过document id 处理
            String docId=DocumentsContract.getDocumentId(uri);

            if("com.android.providers.media.documents".equals(uri.getAuthority())){
                String id=docId.split(":")[1];//解析出数字格式的id
                String selection=MediaStore.Images.Media._ID+"="+id;

                imagePath=getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,selection);

            }else if ("com.android.providers,downloads.documents".equals(uri.getAuthority())){

                Uri contentUri= ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.valueOf(docId));

                imagePath=getImagePath(contentUri,null);
            }
        }else if("content".equalsIgnoreCase(uri.getScheme())){
            //如果是content 类型的uri 普通方式处理
            imagePath=getImagePath(uri,null);
        }else if("file".equalsIgnoreCase(uri.getScheme())){
            //如果是file 类型的uri 直接获取图片路径
            imagePath=uri.getPath();
        }
        //根据图片路径显示图片
        displayIamge(imagePath);
}

private void handleImagebeforekitkat(Intent data){
    Uri uri=data.getData();
    String imagePath=getImagePath(uri,null);

    displayIamge(imagePath);
}
   //通过uri  和selection 获取真实的图片路径
private String getImagePath(Uri uri,String selection){
    String path=null;
    Cursor cursor=getContentResolver().query(uri,null,selection,null,null);
    if(cursor!=null){
        if(cursor.moveToFirst()){
            path=cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));

        }cursor.close();
    }
    return path;
}

private void displayIamge(String imagepath){
    if(imagepath!=null){
        Bitmap bitmap=BitmapFactory.decodeFile(imagepath);
        imageView.setImageBitmap(bitmap);
    }else {
        Toast.makeText(this,"faile",Toast.LENGTH_SHORT).show();
    }
}
    //打开相册
    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");

        startActivityForResult(intent, CHOOSE_FROM_ALBUM);
    }

    //返回权限 用户是否同意
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode){
            case 1:
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    openAlbum();
                }else {
                    Toast.makeText(this,"youd did't have permission",Toast.LENGTH_SHORT).show();

                }
                break;
                default:
                    break;
        }
    }
}
