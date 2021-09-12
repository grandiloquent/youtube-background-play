package euphoria.psycho.music;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
                if (url.startsWith("https://m.youtube.com/watch?v=")) {
                    final FutureTask<Object> ft = new FutureTask<>(() -> {
                    }, new Object());
                    List<Pair<String, YtFile>> files = new ArrayList<>();
                    new YouTubeExtractor(MainActivity.this) {
                        @Override
                        public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                            if (ytFiles == null) {
                                ft.run();
                                return;
                            }
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
                            ft.run();
                        }

                    }.extract(url);
                    try {
                        ft.get();
                    } catch (Exception e) {
                    }
                    Log.e("TAG", String.format("%s", files.get(0).second.getFormat().getAudioBitrate()));
                } else if ((url.startsWith("https://") || url.startsWith("http://"))) {
                    view.loadUrl(url);
                }
                return true;
            }
        });
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
    }

    @Override
    protected void onPause() {
        getPreferences(MODE_PRIVATE).edit().putString("uri", mWebView.getUrl()).apply();
        super.onPause();
    }
}
