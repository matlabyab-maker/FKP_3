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
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

public class MouseAccessibilityService extends AccessibilityService {
    private static MouseAccessibilityService instance;
    private WindowManager wm;
    private ComputerCursorView cursor;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private float x = -1, y = -1;
    private int screenW, screenH;
    private int cursorSize;
    private boolean dragMode=false;

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
        GestureDescription.StrokeDescription stroke =
            new GestureDescription.StrokeDescription(p, 0, 45);
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
        super.onDestroy();
    }
}
