package com.putuguna.uploadvideotoyoutube;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;

public class MainActivity extends AppCompatActivity {

    public static final int TAKE_VIDEO_INTEGER = 10011;

    private Button btnUpload;
    private Button btnSelectVideo;
    private VideoView videoView;
    private Intent intent;
    private String pathSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnUpload = (Button) findViewById(R.id.btn_upload);
        btnSelectVideo = (Button) findViewById(R.id.btn_select_video);
        videoView = (VideoView) findViewById(R.id.video_view);

        btnUpload.setEnabled(false);

        btnSelectVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectVideoFromGallery();
                startActivityForResult(intent,TAKE_VIDEO_INTEGER);
            }
        });

    }

    /**
     * this method used to get the video's path
     * @param uri
     * @param context
     * @return
     */
    public String generatePath(Uri uri,Context context) {
        String filePath = null;
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        if(isKitKat){
            filePath = generateFromKitkat(uri,context);
        }

        if(filePath != null){
            return filePath;
        }

        Cursor cursor = context.getContentResolver().query(uri, new String[] { MediaStore.MediaColumns.DATA }, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                filePath = cursor.getString(columnIndex);
            }
            cursor.close();
        }
        return filePath == null ? uri.getPath() : filePath;
    }

    /**
     * this method use to get the video's path on TargetAPI 19
     * @param uri
     * @param context
     * @return
     */
    @TargetApi(19)
    private String generateFromKitkat(Uri uri,Context context){
        String filePath = null;
        if(DocumentsContract.isDocumentUri(context, uri)){
            String wholeID = DocumentsContract.getDocumentId(uri);

            String id = wholeID.split(":")[1];

            String[] column = { MediaStore.Video.Media.DATA };
            String sel = MediaStore.Video.Media._ID + "=?";

            Cursor cursor = context.getContentResolver().
                    query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            column, sel, new String[]{ id }, null);



            int columnIndex = cursor.getColumnIndex(column[0]);

            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }

            cursor.close();
        }
        return filePath;
    }


    /**
     * this method used to select video from gallery
     */
    public void selectVideoFromGallery() {
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        } else {
            intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.INTERNAL_CONTENT_URI);
        }
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
    }

    /**
     * this method used to set video to VideoView
     * @param uri
     */
    private void setVideoIntoVideoView(Uri uri){
        MediaController mc = new MediaController(this);
        mc.setAnchorView(videoView);
        mc.setMediaPlayer(videoView);
        videoView.setMediaController(mc);
        videoView.setVideoURI(uri);
        videoView.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==TAKE_VIDEO_INTEGER){
            if(resultCode==RESULT_OK){
                if(data.getData()!=null){
                    pathSelected = generatePath(data.getData(),this);
                    //set video to VideoView
                    setVideoIntoVideoView(data.getData());
                    btnUpload.setEnabled(true);

                    btnUpload.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            //addVideo(pathSelected);
                            shareVideo(pathSelected);
                        }
                    });

                }else{
                    Toast.makeText(this, R.string.msg_failed_to_select_video, Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(this, R.string.msg_canceling, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * this method used to get video from path
     * @param ctx
     * @param filePath
     * @return
     */
    public static String getVideoContentUriFromFilePath(Context ctx, String filePath) {

        ContentResolver contentResolver = ctx.getContentResolver();
        String videoUriStr = null;
        long videoId = -1;
        Log.d("first log","Loading file " + filePath);

        // This returns us content://media/external/videos/media (or something like that)
        // I pass in "external" because that's the MediaStore's name for the external
        // storage on my device (the other possibility is "internal")
        Uri videosUri = MediaStore.Video.Media.getContentUri("external");

        Log.d("second log","videosUri = " + videosUri.toString());

        String[] projection = {MediaStore.Video.VideoColumns._ID};

        // TODO This will break if we have no matching item in the MediaStore.
        Cursor cursor = contentResolver.query(videosUri, projection, MediaStore.Video.VideoColumns.DATA + " LIKE ?", new String[] { filePath }, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(projection[0]);
        videoId = cursor.getLong(columnIndex);

        Log.d("third log","Video ID is " + videoId);
        cursor.close();
        if (videoId != -1 ) videoUriStr = videosUri.toString() + "/" + videoId;
        return videoUriStr;
    }

    /**
     * this method used to share video to youtube
     * @param path
     */
    public void shareVideo(final String path) {
        new Handler().post(new Runnable() {

            @Override
            public void run() {
                String newPath = getVideoContentUriFromFilePath(MainActivity.this, path);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(android.content.Intent.EXTRA_TITLE, getString(R.string.label_video_title));
                intent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.label_video_description));
                intent.setType("video/*");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(newPath));
                try {
                    startActivity(Intent.createChooser(intent, getString(R.string.label_uploading_via)));
                } catch (android.content.ActivityNotFoundException ex) {

                }
            }
        });
    }

    /**
     * usefull reference :
     * http://stackoverflow.com/questions/10246212/android-youtube-upload-video-with-static-username-and-password/10432215#10432215
     *
     * and the best is :
     * http://stackoverflow.com/questions/21334484/unable-to-upload-video-through-action-send-intent-in-android-4-3
     */

}
