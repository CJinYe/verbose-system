package cjy.scrawltest;

import android.app.Activity;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity implements View.OnClickListener {

    private ScrawlView mScrawlView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initWindow();
        setContentView(R.layout.activity_main);
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(point);

        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.activity_main);
        mScrawlView = new ScrawlView(this,point.x,point.y);
        relativeLayout.addView(mScrawlView);

        ImageButton butRedo= (ImageButton) findViewById(R.id.main_but_redo);
        ImageButton butUndo= (ImageButton) findViewById(R.id.main_but_undo);
        ImageButton butSave= (ImageButton) findViewById(R.id.main_but_save);
        butRedo.setOnClickListener(this);
        butUndo.setOnClickListener(this);
        butSave.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_but_redo:
                mScrawlView.redo();
                break;
            case R.id.main_but_undo:
                mScrawlView.undo();
                break;
            case R.id.main_but_save:
                Date date = new Date(System.currentTimeMillis());
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
                String time = simpleDateFormat.format(date);
                try {
                    String path = mScrawlView.saveBitmap(time,"测试");
                    Toast.makeText(MainActivity.this,"保存成功："+path,Toast.LENGTH_LONG).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this,"保存失败："+e,Toast.LENGTH_LONG).show();
                }
                break;
            default:
                break;
        }
    }

    private void initWindow() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
