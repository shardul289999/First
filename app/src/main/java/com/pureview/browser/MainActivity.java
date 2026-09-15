package com.pureview.browser;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int RED = Color.rgb(255, 43, 43);

    private FrameLayout browserRoot;
    private WebView webView;
    private boolean browserMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Do not initialize WebView or immersive APIs during app startup.
        // Some OEM builds are fragile when these are touched before the first view is attached.
        try {
            getWindow().setStatusBarColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
        } catch (Throwable ignored) {
        }

        showLauncher(false);
    }

    private void showLauncher(boolean returningFromBrowser) {
        browserMode = false;

        if (returningFromBrowser) {
            exitImmersiveMode();
        }

        if (webView != null) {
            try {
                webView.stopLoading();
                webView.loadUrl("about:blank");
                webView.clearHistory();
                webView.onPause();
            } catch (Throwable ignored) {
            }
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);

        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        int margin = dp(28);
        panelParams.setMargins(margin, margin, margin, margin);
        root.addView(panel, panelParams);

        TextView title = new TextView(this);
        title.setText("FULLSCREEN BROWSER");
        title.setTextColor(Color.WHITE);
        title.setTextSize(25f);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.bottomMargin = dp(8);
        panel.addView(title, titleParams);

        TextView subtitle = new TextView(this);
        subtitle.setText("Enter a URL. After opening, only the webpage is visible.");
        subtitle.setTextColor(Color.rgb(150, 150, 150));
        subtitle.setTextSize(14f);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.bottomMargin = dp(24);
        panel.addView(subtitle, subtitleParams);

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("https://example.com");
        input.setHintTextColor(Color.rgb(105, 105, 105));
        input.setTextColor(Color.WHITE);
        input.setTextSize(17f);
        input.setPadding(dp(18), dp(12), dp(18), dp(12));
        input.setBackgroundTintList(ColorStateList.valueOf(RED));
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setImeOptions(EditorInfo.IME_ACTION_GO);

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
        );
        inputParams.bottomMargin = dp(14);
        panel.addView(input, inputParams);

        Button go = new Button(this);
        go.setText("OPEN FULLSCREEN");
        go.setTextColor(Color.WHITE);
        go.setTextSize(15f);
        go.setAllCaps(false);
        go.setBackgroundTintList(ColorStateList.valueOf(RED));
        panel.addView(go, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(54)
        ));

        View.OnClickListener open = v -> {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) {
                input.setError("Enter a URL or search");
                return;
            }
            showBrowser(normalizeInput(value));
        };

        go.setOnClickListener(open);
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                open.onClick(v);
                return true;
            }
            return false;
        });

        setContentView(root);
    }

    private String normalizeInput(String value) {
        String trimmed = value.trim();
        String lower = trimmed.toLowerCase();

        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return trimmed;
        }

        if (!trimmed.contains(" ") && (trimmed.contains(".") || trimmed.startsWith("localhost"))) {
            return "https://" + trimmed;
        }

        try {
            return "https://www.google.com/search?q=" +
                    URLEncoder.encode(trimmed, StandardCharsets.UTF_8.toString());
        } catch (Exception ignored) {
            return "https://www.google.com/search?q=" + Uri.encode(trimmed);
        }
    }

    private void showBrowser(String url) {
        if (!ensureBrowser()) {
            Toast.makeText(this,
                    "Android System WebView is unavailable. Update Chrome/System WebView and try again.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        browserMode = true;
        setContentView(browserRoot);

        try {
            webView.onResume();
            webView.loadUrl(url);
        } catch (Throwable error) {
            browserMode = false;
            Toast.makeText(this, "Could not open webpage", Toast.LENGTH_LONG).show();
            showLauncher(false);
            return;
        }

        browserRoot.post(this::enterImmersiveMode);
    }

    private boolean ensureBrowser() {
        if (webView != null && browserRoot != null) return true;

        try {
            browserRoot = new FrameLayout(this);
            browserRoot.setBackgroundColor(Color.BLACK);

            webView = new WebView(getApplicationContext());
            webView.setBackgroundColor(Color.BLACK);
            webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

            browserRoot.addView(webView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(false);
            settings.setSupportZoom(true);
            settings.setBuiltInZoomControls(true);
            settings.setDisplayZoomControls(false);
            settings.setMediaPlaybackRequiresUserGesture(true);
            settings.setSupportMultipleWindows(false);
            settings.setJavaScriptCanOpenWindowsAutomatically(false);

            try {
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.setAcceptCookie(true);
                cookieManager.setAcceptThirdPartyCookies(webView, true);
            } catch (Throwable ignored) {
            }

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    Uri uri = request.getUrl();
                    String scheme = uri == null ? null : uri.getScheme();
                    if (scheme == null || scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) {
                        return false;
                    }

                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (Throwable ignored) {
                    }
                    return true;
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (browserMode) enterImmersiveMode();
                }
            });

            return true;
        } catch (Throwable error) {
            if (webView != null) {
                try {
                    webView.destroy();
                } catch (Throwable ignored) {
                }
            }
            webView = null;
            browserRoot = null;
            return false;
        }
    }

    private void enterImmersiveMode() {
        if (!browserMode) return;

        try {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                getWindow().setDecorFitsSystemWindows(false);
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.systemBars());
                    controller.setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    );
                }
            } else {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                );
            }

            if (android.os.Build.VERSION.SDK_INT >= 28) {
                WindowManager.LayoutParams attributes = getWindow().getAttributes();
                attributes.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                getWindow().setAttributes(attributes);
            }
        } catch (Throwable ignored) {
            // Fullscreen is best-effort. Never crash the browser for system-bar handling.
        }
    }

    private void exitImmersiveMode() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                getWindow().setDecorFitsSystemWindows(true);
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) controller.show(WindowInsets.Type.systemBars());
            } else {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && browserMode) enterImmersiveMode();
    }

    @Override
    public void onBackPressed() {
        if (browserMode && webView != null) {
            try {
                if (webView.canGoBack()) {
                    webView.goBack();
                    return;
                }
            } catch (Throwable ignored) {
            }

            showLauncher(true);
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            try {
                webView.onPause();
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null && browserMode) {
            try {
                webView.onResume();
            } catch (Throwable ignored) {
            }
            enterImmersiveMode();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            try {
                webView.stopLoading();
                webView.setWebViewClient(null);
                webView.removeAllViews();
                webView.destroy();
            } catch (Throwable ignored) {
            }
            webView = null;
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
