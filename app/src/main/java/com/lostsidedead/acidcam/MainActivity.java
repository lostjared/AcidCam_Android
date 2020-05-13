package com.lostsidedead.acidcam;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Core;
import org.opencv.android.JavaCameraView;

import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.imgproc.Imgproc;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;

import org.opencv.imgcodecs.Imgcodecs;

import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;
import android.view.ContextMenu.*;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.*;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.app.Activity;

import java.util.Collections;
import android.content.ContentValues;
import android.provider.MediaStore.*;
import android.provider.*;
import android.content.Context;
import android.hardware.Camera;
import static org.opencv.imgproc.Imgproc.*;
import static org.opencv.imgcodecs.Imgcodecs.imread;

import java.util.Random;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.*;
import android.media.MediaRecorder;
import android.view.Surface;
import android.graphics.Canvas;
import android.media.CamcorderProfile;
import android.Manifest;
import android.content.pm.PackageManager;

public class MainActivity extends CameraActivity implements CvCameraViewListener2, OnTouchListener, PopupMenu.OnMenuItemClickListener {
    private static final String TAG = "OCVSample::Activity";
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;
    public AcidCam_Filter filter = new AcidCam_Filter();
    public int filter_index = 0;
    public int current_set_filter = 0;
    private MediaPlayer mp;
    private int menu_locked = 0;
    private boolean take_snapshot_now = false;
    private static final String STATE_CAMERA_INDEX = "cameraIndex";
    private static final String STATE_FLIP = "cameraFlip";
    private static final String FRONT_SIZE_INDEX = "camera_front_index";
    private static final String BACK_SIZE_INDEX = "camera_back_index";
    private static final String CURRENT_FILTER = "current_filter";
    private static final String CURRENT_SET_FILTER = "current_set_filter";
    private int flip_state = 0;
    private int front_size_index = 0;
    private int back_size_index = 0;
    private int filter_map_max = filter.maxFilters();
    private int is_sorted = 0;
/*
    private MediaRecorder recorder = new MediaRecorder();

    public void initRecorder() {
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        android.hardware.Camera.Size size = image_back_sizes.get(back_size_index);
        if (camera_index == 1) {
            size = image_front_sizes.get(front_size_index);
        }
        CamcorderProfile cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        recorder.setProfile(cpHigh);
        recorder.setOutputFile("out.mp4");
        recorder.setVideoSize(size.width,size.height);
        recorder.setOnInfoListener(this);
        recorder.setOnErrorListener(this);
        try {
            recorder.prepare();
        } catch(java.io.IOException ie) {
            Log.d("AC2", "Error IOException Recorder.prepare()");
        }
    } */

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    System.loadLibrary("acidcam");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    public int camera_index = 0;
    public int num_cameras = 2;
    public int isfront = 1;
    private List<android.hardware.Camera.Size> image_front_sizes, image_back_sizes;

    public List<android.hardware.Camera.Size> getListOfSizes(int camera_off) {
        Camera camera = null;
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(camera_off, info);
        num_cameras = Camera.getNumberOfCameras();
        camera = camera.open(camera_index);
        Parameters param = camera.getParameters();
        camera.release();
        List<android.hardware.Camera.Size> sz = param.getSupportedVideoSizes();
        List<android.hardware.Camera.Size> rt = new java.util.ArrayList<android.hardware.Camera.Size>();
        for(int i = 0; i < sz.size(); ++i) {
            android.hardware.Camera.Size v = sz.get(i);
            if(v.width <= 1280 && v.height <= 720)
                rt.add(v);
        }
        return rt;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState != null) {
            camera_index = savedInstanceState.getInt(STATE_CAMERA_INDEX,  1);
            flip_state = savedInstanceState.getInt(STATE_FLIP, 0);
            front_size_index = savedInstanceState.getInt(FRONT_SIZE_INDEX, 0);
            back_size_index = savedInstanceState.getInt(BACK_SIZE_INDEX, 0);
            filter_index = savedInstanceState.getInt(CURRENT_FILTER, 0);
            current_set_filter = savedInstanceState.getInt(CURRENT_SET_FILTER, 0);
        } else {
            camera_index = 1;
            flip_state = -1;
            front_size_index = 0;
            back_size_index = 0;
            filter_index = 0;
            current_set_filter = 0;
        }
        image_back_sizes = getListOfSizes(0);
        image_front_sizes = getListOfSizes(1);
        if(num_cameras <= 1) camera_index = 0;
        //int max_size = 0;
        android.hardware.Camera.Size size = image_back_sizes.get(back_size_index);
        if (camera_index == 1) {
            size = image_front_sizes.get(front_size_index);
        }

        for (int i = 0; i < image_front_sizes.size(); ++i) {
            android.hardware.Camera.Size size_x = image_front_sizes.get(i);
            Log.d("AC2", "Front Resolution: " + size_x.width + "x" + size_x.height);
        }

        for (int i = 0; i < image_back_sizes.size(); ++i) {
            android.hardware.Camera.Size size_x = image_back_sizes.get(i);
            Log.d("AC2", "Rear  Resolution: " + size_x.width + "x" + size_x.height);
        }


        setContentView(R.layout.tutorial1_surface_view);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        mOpenCvCameraView.setCameraIndex(camera_index);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        // Max Frame Size:
        mOpenCvCameraView.setMaxFrameSize(size.width, size.height);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setOnTouchListener(this);
        mp = MediaPlayer.create(this, R.raw.beep);
        getOffset();
    }

    int mOrientation = 0;
    public boolean filter_changed = false;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mOrientation = newConfig.orientation;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
        Log.d("AC2", "Camera Video Stopped");
    }

    private int MENU_GROUP_ID_SIZE = 1732;
    private int MENU_FILTER_MAP = 1133;
    private int MENU_FILTER_SORTED_MAP = 1134;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        //reset = menu.add("Reset");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        int num_sizes = 0;
        if (camera_index == 1) num_sizes = image_front_sizes.size();
        if (camera_index == 0) num_sizes = image_back_sizes.size();
        if (num_sizes > 1) {
            final SubMenu subSizes = menu.addSubMenu("Resolutions");
            for (int i = 0; i < num_sizes; ++i) {
                Size s = image_front_sizes.get(i);
                if (camera_index == 0)
                    s = image_back_sizes.get(i);

                subSizes.add(MENU_GROUP_ID_SIZE, i, Menu.NONE, "" + s.width + "x" + s.height);
            }
        }
        final SubMenu subSizes = menu.addSubMenu("Filters");
        for (int i = 0; i < filter.maxFilters(); ++i) {
            subSizes.add(MENU_FILTER_MAP, i, Menu.NONE, filter.getFilterName(i));
        }
        final SubMenu subSizesSorted = menu.addSubMenu("Filters (Sorted)");
        for (int i = 0; i <  filter.maxFilters(); ++i) {
            subSizesSorted.add(MENU_FILTER_SORTED_MAP, i, Menu.NONE, filter.getFilterSortedName(i));
        }
        menu_locked = 1;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (menu_locked != 1)
            return false;

        if (item.getGroupId() == MENU_GROUP_ID_SIZE) {
            if (camera_index == 1) front_size_index = item.getItemId();
            if (camera_index == 0) back_size_index = item.getItemId();
            super.recreate();
            return true;
        }

        if (item.getGroupId() == MENU_FILTER_MAP) {
            filter_index = item.getItemId();
            current_set_filter = filter.getFilterIndex(filter_index);
            Toast.makeText(this, "Filter changed to:  " + filter.getFilterName(filter_index), Toast.LENGTH_SHORT).show();
            return true;
        }

        if (item.getGroupId() == MENU_FILTER_SORTED_MAP) {
            filter_index = item.getItemId();
            current_set_filter = filter.getFilterSortedIndex(filter_index);
            Toast.makeText(this, "Filter changed to:  " + filter.getFilterSortedName(filter_index), Toast.LENGTH_SHORT).show();
            return true;
        }

        switch (item.getItemId()) {
            case R.id.takesnapshot:
                showImageSaved();
                break;
            case R.id.takesnapshot_now:
                showImageSavedNow();
                break;
            case R.id.moveleft:
                moveLeft();
                break;
            case R.id.moveright:
                moveRight();
                break;
            case R.id.fastforward:
                filter_index = filter_map_max-1;
                filter_changed = true;
                break;
            case R.id.rewind_left:
                filter_index = 0;
                filter_changed = true;
                break;
            case R.id.flipi:
                flip_state = 0;
                break;
            case R.id.flipy:
                flip_state = 1;
                break;
            case R.id.flipz:
                flip_state = -1;
                break;
            case R.id.switchcam:
                if (camera_index == 0) camera_index = 1;
                else camera_index = 0;
                menu_locked = 0;
                super.recreate();
                break;
        }
        return true;
    }

    private int takesnapshot_wait = 0, snap_shot_index = 0;
    private boolean take_snapshot = false;

    public void showImageSaved() {
        Toast.makeText(this, "Save Image: " + snap_shot_index + " in 2 seconds", Toast.LENGTH_SHORT).show();
        ++snap_shot_index;
        saveOffset();
        take_snapshot = true;
    }

    public void showImageSavedNow() {
        Toast.makeText(this, "Save Image: " + snap_shot_index + " now", Toast.LENGTH_SHORT).show();
        ++snap_shot_index;
        saveOffset();
        take_snapshot_now = true;
    }

    public void saveOffset() {
        SharedPreferences sp = getSharedPreferences("acidcam_prefs", android.app.Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("acidcam.key", snap_shot_index);
        editor.commit();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat mat = inputFrame.rgba();
        if(filter_changed == true) {
            filter_changed = false;
            filter.clear_frames();
        }
        filter.Filter(current_set_filter, mat.getNativeObjAddr());
        Core.flip(mat, mat, flip_state);
        if (take_snapshot == true) {
            ++takesnapshot_wait;
            if (takesnapshot_wait > 30) {
                takesnapshot_wait = 0;
                take_snapshot = false;
                saveImage(mat);
            }
        }
        if(take_snapshot_now == true) {
            saveImage(mat);
            take_snapshot_now = false;
        }
        return mat;
    }

    public void getOffset() {
        SharedPreferences sp = getSharedPreferences("acidcam_prefs", Activity.MODE_PRIVATE);
        snap_shot_index = sp.getInt("acidcam.key", 0);
    }

    public void moveRight() {

        if (filter_index < filter_map_max) {
            ++filter_index;
            current_set_filter = filter.getFilterIndex(filter_index);
            filter_changed = true;
        }
        Toast.makeText(this, "Filter changed to:  " + filter.getFilterName(filter_index), Toast.LENGTH_SHORT).show();


    }

    public void moveLeft() {
        if (filter_index > 0) {
            --filter_index;
            current_set_filter = filter.getFilterIndex(filter_index);
            filter_changed = true;
        }
        Toast.makeText(this, "Filter changed to:  " + filter.getFilterName(filter_index), Toast.LENGTH_SHORT).show();
    }

    private float x1, y1, x2, y2, diffx, diffy;


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // TODO put code in here
        return false;//false indicates the event is not consumed
    }

    @Override
    public boolean onTouchEvent(MotionEvent touchevent) {

        switch (touchevent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = touchevent.getX();
                y1 = touchevent.getY();
                break;
            case MotionEvent.ACTION_UP:
                x2 = touchevent.getX();
                y2 = touchevent.getY();

                if (x1 < x2 && Math.abs(x1 - x2) > 50) {
                    moveLeft();
                    return true;
                }

                if (x1 > x2 && Math.abs(x2 - x1) > 50) {
                    moveRight();
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(STATE_CAMERA_INDEX, camera_index);
        savedInstanceState.putInt(STATE_FLIP, flip_state);
        savedInstanceState.putInt(FRONT_SIZE_INDEX, front_size_index);
        savedInstanceState.putInt(BACK_SIZE_INDEX, back_size_index);
        savedInstanceState.putInt(CURRENT_FILTER, filter_index);
        savedInstanceState.putInt(CURRENT_SET_FILTER, current_set_filter);
        super.onSaveInstanceState(savedInstanceState);
    }


    public void saveImage(Mat mat) {
        File path = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String filename = "AcidCam_Image_" + "0000" + snap_shot_index + ".jpg";
        File file = new File(path, filename);
        Boolean bool = null;
        filename = file.toString();
        mp.start();
        Mat resizeImage = new Mat();
        org.opencv.core.Size scaleSize = new org.opencv.core.Size(mat.width()*2, mat.height()*2);
        Imgproc.resize(mat, resizeImage, scaleSize , 0, 0, Imgproc.INTER_CUBIC);
        Mat cmat = new Mat();
        Imgproc.cvtColor(resizeImage, cmat, Imgproc.COLOR_RGBA2BGR, 3);
        Log.d("AC2", "Resized Image: " + resizeImage.width() + "x" + resizeImage.height());
        bool = Imgcodecs.imwrite(filename, cmat);
        if (bool == true)
            Log.d("com.lostsidedead.AcidCam", "SUCCESS writing " + filename + " image to external storage");
        else
            Log.d("com.lostsidedead.AcidCam", "Fail writing image to external storage");
        //MediaScannerConnection.scanFile(this, new String[] { file.getPath() }, new String[] { "image/jpeg" }, null);
        addImageToGallery(filename, getApplicationContext());
        cmat.release();
        resizeImage.release();
    }

    public static void addImageToGallery(final String filePath, final Context context) {
        ContentValues values = new ContentValues();
        values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, filePath);
        context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
    }
}