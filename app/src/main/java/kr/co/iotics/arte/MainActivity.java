package kr.co.iotics.arte;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.OutputStream;

public class MainActivity extends Activity {

    private WebView web;
    private ValueCallback<Uri[]> filePicker;
    private static final int PICK_FILE = 1001;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        web = new WebView(this);
        setContentView(web);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setTextZoom(100);

        WebView.setWebContentsDebuggingEnabled(false);

        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb,
                                             FileChooserParams params) {
                if (filePicker != null) filePicker.onReceiveValue(null);
                filePicker = cb;
                try {
                    Intent i = params.createIntent();
                    i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    startActivityForResult(i, PICK_FILE);
                } catch (Exception e) {
                    filePicker = null;
                    return false;
                }
                return true;
            }
        });

        web.addJavascriptInterface(new Saver(), "AndroidSave");
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == PICK_FILE && filePicker != null) {
            filePicker.onReceiveValue(
                    WebChromeClient.FileChooserParams.parseResult(res, data));
            filePicker = null;
            return;
        }
        super.onActivityResult(req, res, data);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    /** 자바스크립트에서 부르는 저장 기능 — 다운로드 폴더에 파일을 만든다. */
    private class Saver {
        @JavascriptInterface
        public void saveBase64(String b64, String name, String mime) {
            try {
                byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                cv.put(MediaStore.MediaColumns.MIME_TYPE,
                        mime == null || mime.isEmpty() ? "application/octet-stream" : mime);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cv.put(MediaStore.MediaColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS + "/아르떼계약서");
                }
                Uri uri = getContentResolver().insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (uri == null) throw new Exception("insert failed");
                OutputStream out = getContentResolver().openOutputStream(uri);
                out.write(bytes);
                out.flush();
                out.close();
                toast("다운로드 > 아르떼계약서 에 저장했습니다\n" + name);
            } catch (Exception e) {
                toast("저장에 실패했습니다: " + e.getMessage());
            }
        }

        private void toast(final String msg) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
