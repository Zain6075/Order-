package com.saeedi.essenceorders;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String TAG = "SaeediOrders";
    private static final String APP_ASSETS_HOST = "appassets.androidplatform.net";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, true);
        window.setStatusBarColor(0xFF0F3D2E);
        window.setNavigationBarColor(0xFF0B1510);

        webView = new WebView(this);
        webView.setBackgroundColor(0xFFEDF2EC);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);

        webView.addJavascriptInterface(new AndroidBridge(), "AndroidApp");
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        webView.setWebViewClient(new WebViewClientCompat() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                return !APP_ASSETS_HOST.equals(uri.getHost());
            }
        });

        setContentView(webView);
        webView.loadUrl("https://" + APP_ASSETS_HOST + "/assets/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("AndroidApp");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private final class AndroidBridge {
        @JavascriptInterface
        public boolean saveFile(String requestedName, String encoded, String requestedMime) {
            Uri inserted = null;
            try {
                String name = safeFileName(requestedName);
                String mime = safeMimeType(requestedMime);
                byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
                ContentResolver resolver = getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/Saeedi Essence Orders");
                values.put(MediaStore.MediaColumns.IS_PENDING, 1);
                inserted = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (inserted == null) return false;

                try (OutputStream output = resolver.openOutputStream(inserted, "w")) {
                    if (output == null) throw new IllegalStateException("Unable to open output stream");
                    output.write(bytes);
                    output.flush();
                }

                ContentValues ready = new ContentValues();
                ready.put(MediaStore.MediaColumns.IS_PENDING, 0);
                resolver.update(inserted, ready, null, null);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Could not save order file", e);
                if (inserted != null) getContentResolver().delete(inserted, null, null);
                return false;
            }
        }

        @JavascriptInterface
        public boolean shareFile(String requestedName, String encoded, String requestedMime) {
            try {
                String name = safeFileName(requestedName);
                String mime = safeMimeType(requestedMime);
                byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
                File sharedDir = new File(getCacheDir(), "shared");
                if (!sharedDir.exists() && !sharedDir.mkdirs()) return false;

                File file = new File(sharedDir, name);
                try (FileOutputStream output = new FileOutputStream(file, false)) {
                    output.write(bytes);
                    output.flush();
                }

                Uri uri = FileProvider.getUriForFile(
                        MainActivity.this,
                        getPackageName() + ".fileprovider",
                        file
                );
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType(mime);
                send.putExtra(Intent.EXTRA_STREAM, uri);
                send.setClipData(ClipData.newUri(getContentResolver(), name, uri));
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                runOnUiThread(() -> startActivity(Intent.createChooser(send, "Share order")));
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Could not share order file", e);
                return false;
            }
        }
    }

    private static String safeFileName(String requested) {
        String candidate = requested == null ? "order" : requested;
        int slash = Math.max(candidate.lastIndexOf('/'), candidate.lastIndexOf('\\'));
        if (slash >= 0) candidate = candidate.substring(slash + 1);
        candidate = candidate.replaceAll("[^A-Za-z0-9._-]", "_");
        if (candidate.isEmpty() || candidate.equals(".") || candidate.equals("..")) return "order-file";
        return candidate;
    }

    private static String safeMimeType(String requested) {
        if (requested == null) return "application/octet-stream";
        String mime = requested.toLowerCase(Locale.ROOT);
        return mime.matches("[a-z0-9.+-]+/[a-z0-9.+-]+") ? mime : "application/octet-stream";
    }
}
