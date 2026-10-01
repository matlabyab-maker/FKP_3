package com.fkp2.keyboard;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.MotionEvent;
import android.graphics.Typeface;

public class MouseAccessibilityService extends AccessibilityService {
    private static MouseAccessibilityService instance;
    private WindowManager wm;
    private ComputerCursorView cursor;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private float x = -1, y = -1;
    private int screenW, screenH;
    private int cursorSize;
    private boolean dragMode=false;
    private View mousePanel;
    private WindowManager.LayoutParams mousePanelLp;

    public static MouseAccessibilityService getInstance() { return instance; }

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        screenW = dm.widthPixels;
        screenH = dm.heightPixels;
        cursorSize = Math.max(42, Math.round(42 * dm.density));
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
        }
    }

    public static void showCursorFromKeyboard() {
        MouseAccessibilityService s=instance;
        if(s!=null) s.showCursor();
    }

    public static void hideCursorFromKeyboard() {
        MouseAccessibilityService s=instance;
        if(s!=null) s.hideCursor();
    }

    public boolean isMouseOverlayShown() { return mousePanel != null; }

    public void showMouseOverlay() {
        if (wm == null) return;
        showCursor();
        if (mousePanel != null) return;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(8,8,8,8);
        root.setBackgroundColor(Color.argb(238, 250, 250, 250));

        TextView title = new TextView(this);
        title.setText("موس FKP_3");
        title.setTextSize(15);
        title.setTextColor(Color.rgb(10,38,92));
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, 38));

        final TextView pad = new TextView(this);
        pad.setText("میدان لمسی موس\nحرکت نشانگر روی کل صفحه");
        pad.setTextSize(13);
        pad.setTextColor(Color.rgb(10,38,92));
        pad.setGravity(Gravity.CENTER);
        GradientDrawable pg = new GradientDrawable();
        pg.setColor(Color.rgb(232,236,242));
        pg.setCornerRadius(10);
        pad.setBackground(pg);
        root.addView(pad, new LinearLayout.LayoutParams(-1, 125));

        final float[] last = {0,0};
        final boolean[] moving = {false};
        pad.setOnTouchListener((v,e)->{
            if (e.getAction()==MotionEvent.ACTION_DOWN) {
                last[0]=e.getRawX(); last[1]=e.getRawY(); moving[0]=true; return true;
            }
            if (e.getAction()==MotionEvent.ACTION_MOVE && moving[0]) {
                float dx=e.getRawX()-last[0], dy=e.getRawY()-last[1];
                if (Math.abs(dx)>=0.5f || Math.abs(dy)>=0.5f) {
                    moveRelative(dx*2.0f,dy*2.0f);
                    last[0]=e.getRawX(); last[1]=e.getRawY();
                }
                return true;
            }
            if (e.getAction()==MotionEvent.ACTION_UP || e.getAction()==MotionEvent.ACTION_CANCEL) { moving[0]=false; return true; }
            return true;
        });

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button left = new Button(this); left.setText("کلیک چپ");
        Button right = new Button(this); right.setText("کلیک راست");
        row.addView(left,new LinearLayout.LayoutParams(0,58,1));
        row.addView(right,new LinearLayout.LayoutParams(0,58,1));
        root.addView(row);
        left.setOnClickListener(v->click(false));
        right.setOnClickListener(v->click(true));

        mousePanel = root;
        mousePanelLp = new WindowManager.LayoutParams(
            Math.min(dp(330), screenW - dp(16)), dp(245),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT);
        mousePanelLp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        mousePanelLp.x = dp(8); mousePanelLp.y = dp(72);
        root.setElevation(30f);
        wm.addView(root, mousePanelLp);
    }

    public void hideMouseOverlay() {
        if (mousePanel != null && wm != null) { try { wm.removeView(mousePanel); } catch(Exception ignored) {} }
        mousePanel = null; mousePanelLp = null;
        hideCursor();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void hideCursor() {
        if(cursor!=null && wm!=null){
            try{wm.removeView(cursor);}catch(Exception ignored){}
        }
        cursor=null;
    }

    private void showCursor() {
        if (cursor != null || wm == null) return;
        cursor = new ComputerCursorView(this);
        cursor.setElevation(20f);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            cursorSize, cursorSize,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        );
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        // x/y are the cursor hotspot (the sharp arrow tip), not its center.
        x = Math.max(0, screenW / 2f - cursorSize / 2f);
        y = Math.max(0, screenH / 2f - cursorSize / 2f);
        lp.x = Math.round(x);
        lp.y = Math.round(y);
        cursor.setTag(lp);
        wm.addView(cursor, lp);
        updateCursorAppearance(Color.BLACK);
    }

    private void updateCursorAppearance(int bg) {
        if (cursor == null) return;
        // Fixed desktop-style appearance requested for FKP_2:
        // deep navy arrow with a darker navy outline.
        int c = Color.rgb(10, 38, 92);
        int outline = Color.rgb(2, 12, 34);
        cursor.setCursorColors(c, outline);
    }

    public static void moveRelativeFromKeyboard(float dx, float dy) {
        MouseAccessibilityService s = instance;
        if (s != null) s.moveRelative(dx, dy);
    }

    public static void clickFromKeyboard(boolean right) {
        MouseAccessibilityService s = instance;
        if (s != null) s.click(right);
    }

    public static void resetFromKeyboard() {
        MouseAccessibilityService s = instance;
        if (s != null) s.resetCursor();
    }

    public static void beginDragFromKeyboard() {
        MouseAccessibilityService s=instance;
        if(s!=null){s.dragMode=true;}
    }

    public static void endDragFromKeyboard() {
        MouseAccessibilityService s=instance;
        if(s!=null){s.dragMode=false;}
    }

    private void moveRelative(float dx, float dy) {
        if (cursor == null) showCursor();
        float oldX=x, oldY=y;
        float maxX = Math.max(0, screenW - cursorSize);
        float maxY = Math.max(0, screenH - cursorSize);
        x = Math.max(0, Math.min(maxX, x + dx));
        y = Math.max(0, Math.min(maxY, y + dy));
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams)cursor.getTag();
        lp.x = Math.round(x);
        lp.y = Math.round(y);
        wm.updateViewLayout(cursor, lp);
        if(dragMode && Build.VERSION.SDK_INT>=24){
            dispatchSwipe(oldX+3f,oldY+3f,x+3f,y+3f,90);
        }
    }

    private void resetCursor() {
        x = Math.max(0, screenW / 2f - cursorSize / 2f);
        y = Math.max(0, screenH / 2f - cursorSize / 2f);
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams)cursor.getTag();
        lp.x = Math.round(x); lp.y = Math.round(y);
        wm.updateViewLayout(cursor, lp);
    }

    private void dispatchSwipe(float sx,float sy,float ex,float ey,long duration){
        Path path=new Path();
        path.moveTo(sx,sy);
        path.lineTo(ex,ey);
        GestureDescription.StrokeDescription stroke=new GestureDescription.StrokeDescription(path,0,Math.max(40,duration));
        dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(),null,null);
    }

    private void click(boolean right) {
        if (Build.VERSION.SDK_INT < 24 || cursor == null) return;
        float cx = x + 3f;
        float cy = y + 2f;
        Path p = new Path();
        p.moveTo(cx, cy);
        // A zero-length accessibility stroke is ignored on some Android builds.
        // Give it a tiny movement so it is recognized as a real tap.
        p.lineTo(cx + 1f, cy + 1f);
        GestureDescription.StrokeDescription stroke =
            new GestureDescription.StrokeDescription(p, 0, 80);
        dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), null, null);
        // Android accessibility gesture dispatch is a screen tap; the right/left
        // distinction is not exposed by dispatchGesture. The right flag is retained
        // so the keyboard UI can expose both controls without pretending to inject
        // an unsupported mouse-button event.
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }

    @Override public void onDestroy() {
        instance = null;
        if (cursor != null && wm != null) {
            try { wm.removeView(cursor); } catch (Exception ignored) {}
        }
        cursor = null;
        if (mousePanel != null && wm != null) { try { wm.removeView(mousePanel); } catch(Exception ignored) {} }
        mousePanel = null;
        super.onDestroy();
    }
}
