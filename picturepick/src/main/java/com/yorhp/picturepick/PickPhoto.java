package com.yorhp.picturepick;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import static android.app.Activity.RESULT_OK;
import static com.yorhp.picturepick.BitmapUtil.copyFile;
import static com.yorhp.picturepick.BitmapUtil.imgCompress;
import static com.yorhp.picturepick.PicturePickUtil.CROP_PHOTO;
import static com.yorhp.picturepick.PicturePickUtil.PICK_PHOTO;
import static com.yorhp.picturepick.PicturePickUtil.TAKE_PHOTO;
import static com.yorhp.picturepick.PicturePickUtil.aspectX;
import static com.yorhp.picturepick.PicturePickUtil.aspectY;
import static com.yorhp.picturepick.PicturePickUtil.cropPic;
import static com.yorhp.picturepick.PicturePickUtil.fileSize;
import static com.yorhp.picturepick.PicturePickUtil.imgHeight;
import static com.yorhp.picturepick.PicturePickUtil.imgWidth;

/**
 * Created by Tyhj on 2017/8/29.
 */

public class PickPhoto {

    private static String AUTHORITY;
    public static String date;
    public static String path;
    public final static int CHOOSE_BY_VIEW = 0;
    public final static int CHOOSE_BY_CAMERA = 1;
    public final static int CHOOSE_BY_ALBUM = 2;

    public static int rotateAngle = 0;


    static void init(String authority) {
        AUTHORITY = authority;
    }


    //选择图片
    static void choosePicture(final Activity context) {
        path = context.getExternalCacheDir().getPath();
        switch (PicturePickUtil.choosePicWay) {
            case CHOOSE_BY_VIEW:
                chooseByView(context);
                break;
            case CHOOSE_BY_CAMERA:
                openCamera(context);
                break;
            case CHOOSE_BY_ALBUM:
                openAlbum(context);
                break;
            default:
                break;
        }

    }

    /**
     * 使用默认界面选择
     *
     * @param context
     */
    private static void chooseByView(final Activity context) {
        Button camoral, images;
        final AlertDialog.Builder di;
        di = new AlertDialog.Builder(context);
        di.setCancelable(true);
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.dialog_headchoose, null);
        di.setView(layout);
        final Dialog dialog = di.show();
        camoral = (Button) layout.findViewById(R.id.camoral);
        images = (Button) layout.findViewById(R.id.images);

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                context.finish();
                context.overridePendingTransition(0, 0);
            }
        });

        // 相机
        camoral.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.setOnDismissListener(null);
                dialog.dismiss();
                openCamera(context);

            }
        });
        // 相册
        images.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.setOnDismissListener(null);
                dialog.dismiss();
                openAlbum(context);
            }
        });
    }

    /**
     * 打开相册
     *
     * @param context
     */
    private static void openAlbum(Activity context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            PicturePickUtil.reSetListener();
            context.finish();
            return;
        }
        getDate();
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        context.startActivityForResult(intent, PICK_PHOTO);
    }

    /**
     * 打开相机
     *
     * @param context
     */
    private static void openCamera(Activity context) {
        getDate();
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.CAMERA}, 1);
            PicturePickUtil.reSetListener();
            context.finish();
            return;
        }
        File file = new File(path, date);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {//7.0及以上
            Uri uriForFile = FileProvider.getUriForFile(context, AUTHORITY, file);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uriForFile);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } else {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
        }
        context.startActivityForResult(intent, TAKE_PHOTO);
    }


    //随机获取文件名字
    static void getDate() {
        date = System.currentTimeMillis() + ".JPEG";
    }

    //剪裁图片
    static void cropPhoto(Uri imageUri, Context context) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            File file = new File(PickPhoto.path, PickPhoto.date);
            Uri outPutUri = FileProvider.getUriForFile(context, AUTHORITY, file);
            intent.setDataAndType(FileProvider.getUriForFile(context, AUTHORITY, file), "image/*");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outPutUri);
            intent.putExtra("crop", true);
            intent.putExtra("noFaceDetection", false);//去除默认的人脸识别，否则和剪裁匡重叠
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } else {
            intent.setDataAndType(imageUri, "image/*");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        }
        // aspectX aspectY 是宽高的比例
        if (aspectX != null && aspectY != null) {
            intent.putExtra("aspectX", aspectX);
            intent.putExtra("aspectY", aspectY);
        }
        ((Activity) context).startActivityForResult(intent, CROP_PHOTO);
    }


    //获取返回值
    static void getPhoto(int requestCode, int resultCode, Context context, Intent data, CamerabakListener getFile) {
        switch (requestCode) {
            //这是从相机返回的数据
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    File file = new File(PickPhoto.path, PickPhoto.date);
                    rotateAngle = BitmapUtil.getExifOrientation(PickPhoto.path);
                    if (cropPic) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            //通过FileProvider创建一个content类型的Uri
                            Uri inputUri = FileProvider.getUriForFile(context, AUTHORITY, file);
                            cropPhoto(inputUri, context);
                        } else {
                            cropPhoto(Uri.fromFile(file), context);
                        }
                    } else {
                        //压缩文件
                        if (PicturePickUtil.compressedFile) {
                            imgCompress(file.getPath(), file, imgWidth, imgHeight, fileSize);
                        }
                        getFile.getFile(file);
                        return;
                    }
                }
                break;
            //这是从相册返回的数据
            case PICK_PHOTO:
                if (resultCode == RESULT_OK) {
                    String path_pre = GetImagePath.getPath(context, data.getData());
                    rotateAngle = BitmapUtil.getExifOrientation(path_pre);
                    File newFile = new File(PickPhoto.path, PickPhoto.date);
                    Toast.makeText(context, "加载中", Toast.LENGTH_SHORT).show();
                    try {
                        copyFile(new File(path_pre), newFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (cropPic) {
                        //如果大于等于7.0使用FileProvider
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            Uri dataUri = FileProvider.getUriForFile(context, AUTHORITY, newFile);
                            cropPhoto(dataUri, context);
                        } else {
                            cropPhoto(Uri.fromFile(newFile), context);
                        }
                    } else {
                        //压缩文件
                        if (PicturePickUtil.compressedFile) {
                            imgCompress(newFile.getPath(), newFile, imgWidth, imgHeight, fileSize);
                        }
                        getFile.getFile(newFile);
                        return;
                    }


                }
                break;
            //剪裁图片返回数据,就是原来的文件
            case CROP_PHOTO:
                if (resultCode == RESULT_OK) {
                    String fileName = PickPhoto.path + "/" + PickPhoto.date;
                    File newFile = new File(PickPhoto.path, PickPhoto.date);
                    if (PicturePickUtil.compressedFile) {
                        imgCompress(fileName, newFile, imgWidth, imgHeight, fileSize);
                    }
                    //获取到的就是new File或fileName
                    getFile.getFile(newFile);
                }

                break;
            default:
                break;
        }
    }


    interface CamerabakListener {
        void getFile(File file);
    }


}
