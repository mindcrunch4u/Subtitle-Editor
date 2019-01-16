package com.wong.peppa.subt;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PersistableBundle;
import android.renderscript.ScriptGroup;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    public static final int SELECT_FILE = 55;
    public static final String DATA_VIDEO_POSITION = "bundle_video_position";
    public static final String DATA_VIDEO_PATH = "bundle_video_path";
    public static final String DATA_EDITTEXT = "bundle_edittext";

    public static VideoView sPlayer = null;
    public static MediaController sMediaController = null;
    public static EditText sEditText = null;
    public static Button sButton_Back = null;
    public static Button sButton_Control = null;


    public static boolean isVideoLoaded = false;
    public static Uri video_path = null;
    public int video_positon = 0;

    private String fetched_string = new String();

    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if( savedInstanceState != null ){
            sEditText.setText( savedInstanceState.getString(DATA_EDITTEXT) );
            sEditText.setSelection( sEditText.getText().toString().length() );
            sButton_Control.setText(R.string.button_start);

            video_positon = savedInstanceState.getInt(DATA_VIDEO_POSITION);
            video_path = Uri.parse(savedInstanceState.getString(DATA_VIDEO_PATH));
            loadVideo();
        }

        sEditText = (EditText)findViewById(R.id.EditText);
        sButton_Back = (Button)findViewById(R.id.button_back); sButton_Back.setOnClickListener(this);
        sButton_Control = (Button)findViewById(R.id.button_control); sButton_Control.setOnClickListener(this);


        sPlayer = (VideoView)findViewById(R.id.player);
        //sMediaController = new MediaController(MainActivity.this);
        //sPlayer.setMediaController(sMediaController);


        if(!sPlayer.isPlaying()) sButton_Control.setText(R.string.button_start);

        sPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                isVideoLoaded = true;
                sPlayer.seekTo( video_positon );
            }
        });

    }

    protected void onPause() {
        super.onPause();
        this.video_positon = sPlayer.getCurrentPosition();
        sPlayer.pause();
        sButton_Control.setText(R.string.button_start);
    }
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(isVideoLoaded){
            if( sEditText.getText() != null ){
                if(sEditText.getText().toString().length() != 0 ) {
                    outState.putString(DATA_EDITTEXT, sEditText.getText().toString());
                }
            }
            outState.putInt( DATA_VIDEO_POSITION, video_positon );
            outState.putString(DATA_VIDEO_PATH, video_path.toString());
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SELECT_FILE && resultCode == RESULT_OK && data != null ){
            video_path = data.getData();
            loadVideo();
        }
    }

    public boolean loadVideo(){
        if( video_path != null && video_path.toString().length() != 0 ){
            sPlayer.setVideoURI( video_path );
            return true;
        }
        return false;
    }

/* Codes below: all about options menu */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.topright, menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        if( item.getItemId() == R.id.item_copy_to_clipboard ){
            performCopy();
        }
        else if( item.getItemId() == R.id.item_select_file ){
            performSearch();
        }
        else if( item.getItemId() == R.id.item_save ){
            performSave();
        }
        else if( item.getItemId() == R.id.item_jump ){
            if( true ){ //isVideoLoaded
                performFetch();
            }
        }
        return false;
    }
    public void performSearch(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");
        startActivityForResult(intent, SELECT_FILE );
    }
    public void performCopy(){
        int tmp_time_in_mili = sPlayer.getCurrentPosition();
        ClipboardManager clipboardManager = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("subtitle", sEditText.getText().toString() + "\n" + tmp_time_in_mili );
        clipboardManager.setPrimaryClip(clipData);
        Toast.makeText(MainActivity.this,"Text Copied", Toast.LENGTH_SHORT).show();
    }
    public void performSave(){
        msg.s_toast(this,"Not Implemented");
    }
    public void performFetch(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.jump_time);
        builder.setMessage(R.string.dialog_fetch_msg);
        builder.setCancelable(false);


        final EditText input = new EditText(MainActivity.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView( input );


        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                fetched_string = input.getText().toString();
                boolean isPlayingBeforeClick = sPlayer.isPlaying();
                if( isVideoLoaded ){
                    try{
                        msg.s_toast(MainActivity.this, "Jumping to " + fetched_string );
                        sPlayer.pause();
                        sPlayer.seekTo( Integer.parseInt( fetched_string ) );
                        if(isPlayingBeforeClick){
                            sPlayer.start();
                        }
                    }
                    catch (Exception e){
                        /* Not jumping in that input can't be parsed integer */
                    }
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                fetched_string = "-1";
                dialog.cancel();
            }
        });

        builder.show();

    }

/* Codes below: all about back and control buttons */
    public void onClick(View v) {
        if( v.getId() == R.id.button_back ){
            video_positon = sPlayer.getCurrentPosition();
            if( video_positon <= 1000 ){
                video_positon = 0;
            }
            else{
                video_positon -= 1000;
            }
            sPlayer.seekTo( video_positon );
        }
        else if( v.getId() == R.id.button_control ){
            if( sPlayer.isPlaying() ){
                sPlayer.pause();
                sButton_Control.setText(R.string.button_start);
            }
            else if( !sPlayer.isPlaying() && isVideoLoaded ){
                sPlayer.start();
                sButton_Control.setText(R.string.button_stop);
            }
        }
    }
}


class msg{
    public static void s_toast(Context context, String input){
        Toast.makeText(context, input, Toast.LENGTH_SHORT).show();
    }
    public static void l_toast(Context context, String input){
        Toast.makeText(context, input, Toast.LENGTH_LONG).show();
    }
}