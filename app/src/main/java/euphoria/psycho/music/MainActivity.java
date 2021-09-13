package euphoria.psycho.music;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

public class MainActivity extends Activity {
    WebView mWebView;

    private void initializeWebView() {
        mWebView = findViewById(R.id.web_view);
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptThirdPartyCookies(mWebView, true);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if ((url.startsWith("https://") || url.startsWith("http://"))) {
                    view.loadUrl(url);
                }
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String uri = mWebView.getUrl();
        if (uri.startsWith("https://m.youtube.com/watch?v=")) {
            new YouTubeExtractor(MainActivity.this) {
                @Override
                public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                    if (ytFiles == null) {
                        return;
                    }
                    List<Pair<String, YtFile>> files = new ArrayList<>();
                    // Iterate over itags
                    for (int i = 0, itag; i < ytFiles.size(); i++) {
                        itag = ytFiles.keyAt(i);
                        // ytFile represents one file with its url and meta data
                        YtFile ytFile = ytFiles.get(itag);
                        // Just add videos in a decent format => height -1 = audio
                        if (ytFile.getFormat().getHeight() == -1) {
                            files.add(Pair.create(vMeta.getTitle(), ytFile));
                        }
                    }
                    Log.e("TAG", String.format("%s %s", files.get(0).second.getFormat().getAudioBitrate(), files.get(0).second.getUrl()));
                }

            }.extract(uri);

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.add(0, 1, 0, "播放");
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItem.setIcon(R.drawable.ic_action_play_arrow);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeWebView();
        String uri = getPreferences(MODE_PRIVATE).getString("uri", "https://m.youtube.com/");
        mWebView.loadUrl(uri);
        Intent musicService = new Intent(this, MusicService.class);
        musicService.putExtra("music", new String[]{"其他", "https://r1---sn-5uh5o-f5f6.googlevideo.com/videoplayback?expire=1631537373&ei=ffQ-Yf_0EdHFyQXYz5zIBw&ip=5.187.49.190&id=o-AGPPWTFjxhRrKcgjqArH1LK0XpNBZGb1rK3e6o4wHMop&itag=140&source=youtube&requiressl=yes&mh=DW&mm=31%2C29&mn=sn-5uh5o-f5f6%2Csn-u2oxu-f5fez&ms=au%2Crdu&mv=m&mvi=1&pl=24&pcm2=no&initcwndbps=233750&vprv=1&mime=audio%2Fmp4&ns=zyVkgMDGgoCoy8VHrMgXMOgG&gir=yes&clen=2419578&dur=149.443&lmt=1594222690663455&mt=1631515497&fvip=1&keepalive=yes&fexp=24001373%2C24007246&c=WEB&txp=5431432&n=mGGIgsnL_CgLY8rD&sparams=expire%2Cei%2Cip%2Cid%2Citag%2Csource%2Crequiressl%2Cpcm2%2Cvprv%2Cmime%2Cns%2Cgir%2Cclen%2Cdur%2Clmt&lsparams=mh%2Cmm%2Cmn%2Cms%2Cmv%2Cmvi%2Cpl%2Cinitcwndbps&lsig=AG3C_xAwRgIhAJULHS-1XlmFXW4_FdUFEguix7uuwwZJWjtMTilck7X5AiEAwI--a684nhhz-G3xG7JCMLxdRbAixHF4sxr84veV890%3D&sig=AOq0QJ8wRQIgUf6pGLHcP3I2MtwsGpyRoWWQpr9070EDfU4kE7E5KTECIQCRo1aJc1FbxysUQI7IV6rqrlDrp_n_tGe0aK9FaOv0NA=="});
        startService(musicService);
    }

    @Override
    protected void onPause() {
        getPreferences(MODE_PRIVATE).edit().putString("uri", mWebView.getUrl()).apply();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else
            super.onBackPressed();
    }
}
