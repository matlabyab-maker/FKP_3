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
    private boolean autoTargetMode = false;
    private Button autoTargetButton;
    private final Runnable autoTargetRunnable = new Runnable() {
        @Override public void run() {
            if (!autoTargetMode || mousePanel == null || cursor == null) return;
            moveToNextClickableTarget();
            handler.postDelayed(this, 900);
        }
    };

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
            if (Build.VERSION.SDK_INT >= 21) {
                info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            }
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
        title.setText("موس FKP_3  •  برای جابه‌جایی بکشید");
        root.addView(title, new LinearLayout.LayoutParams(-1, 38));

        final float[] panelLast = {0f,0f};
        final boolean[] panelMoving = {false};
        title.setOnTouchListener((v,e)->{
            if (mousePanelLp == null || wm == null) return false;
            if (e.getAction()==MotionEvent.ACTION_DOWN) {
                panelLast[0]=e.getRawX();
                panelLast[1]=e.getRawY();
                panelMoving[0]=true;
                return true;
            }
            if (e.getAction()==MotionEvent.ACTION_MOVE && panelMoving[0]) {
                float dx=e.getRawX()-panelLast[0];
                float dy=e.getRawY()-panelLast[1];
                if (Math.abs(dx)>=1f || Math.abs(dy)>=1f) {
                    mousePanelLp.x -= Math.round(dx);
                    mousePanelLp.y -= Math.round(dy);
                    int maxX=Math.max(0, screenW-mousePanel.getWidth()-dp(4));
                    int maxY=Math.max(0, screenH-mousePanel.getHeight()-dp(4));
                    mousePanelLp.x=Math.max(0,Math.min(maxX,mousePanelLp.x));
                    mousePanelLp.y=Math.max(0,Math.min(maxY,mousePanelLp.y));
                    try { wm.updateViewLayout(mousePanel,mousePanelLp); } catch(Exception ignored) {}
                    panelLast[0]=e.getRawX();
                    panelLast[1]=e.getRawY();
                }
                return true;
            }
            if (e.getAction()==MotionEvent.ACTION_UP || e.getAction()==MotionEvent.ACTION_CANCEL) {
                panelMoving[0]=false;
                return true;
            }
            return true;
        });

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

        autoTargetButton = new Button(this);
        autoTargetButton.setText("حرکت خودکار: خاموش");
        autoTargetButton.setTextSize(13);
        LinearLayout.LayoutParams autoLp = new LinearLayout.LayoutParams(-1, 54);
        autoLp.topMargin = 4;
        root.addView(autoTargetButton, autoLp);
        autoTargetButton.setOnClickListener(v -> toggleAutoTargetMode());

        mousePanel = root;
        mousePanelLp = new WindowManager.LayoutParams(
            Math.min(dp(330), screenW - dp(16)), dp(305),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT);
        mousePanelLp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        mousePanelLp.x = dp(8); mousePanelLp.y = dp(72);
        root.setElevation(30f);
        wm.addView(root, mousePanelLp);
    }

    private void toggleAutoTargetMode() {
        autoTargetMode = !autoTargetMode;
        if (autoTargetButton != null) {
            autoTargetButton.setText(autoTargetMode ? "حرکت خودکار: روشن" : "حرکت خودکار: خاموش");
        }
        handler.removeCallbacks(autoTargetRunnable);
        if (autoTargetMode) {
            moveToNextClickableTarget();
            handler.postDelayed(autoTargetRunnable, 900);
        }
    }

    private void stopAutoTargetMode() {
        autoTargetMode = false;
        handler.removeCallbacks(autoTargetRunnable);
        if (autoTargetButton != null) autoTargetButton.setText("حرکت خودکار: خاموش");
    }

    private void moveToNextClickableTarget() {
        if (cursor == null || wm == null) return;
        android.view.accessibility.AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        java.util.ArrayList<android.view.accessibility.AccessibilityNodeInfo> targets = new java.util.ArrayList<>();
        try {
            collectClickableTargets(root, targets);
        } finally {
            root.recycle();
        }
        if (targets.isEmpty()) return;

        // Choose the next target in screen order. Keep an index in a field so
        // repeated timer ticks walk through links/buttons instead of staying put.
        int start = autoTargetIndex % targets.size();
        android.view.accessibility.AccessibilityNodeInfo target = targets.get(start);
        autoTargetIndex = (start + 1) % targets.size();

        android.graphics.Rect r = new android.graphics.Rect();
        target.getBoundsInScreen(r);
        target.recycle();

        if (r.width() <= 0 || r.height() <= 0) return;
        float tx = r.left + Math.min(r.width() / 2f, 20f);
        float ty = r.top + Math.min(r.height() / 2f, 20f);
        moveCursorToScreenPoint(tx, ty);
    }

    private int autoTargetIndex = 0;

    private void collectClickableTargets(android.view.accessibility.AccessibilityNodeInfo node,
                                         java.util.ArrayList<android.view.accessibility.AccessibilityNodeInfo> out) {
        if (node == null) return;
        android.graphics.Rect r = new android.graphics.Rect();
        node.getBoundsInScreen(r);
        boolean usable = node.isVisibleToUser() && r.width() > 4 && r.height() > 4;
        boolean clickable = node.isClickable();
        if (!clickable && Build.VERSION.SDK_INT >= 21) {
            java.util.List<android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction> actions = node.getActionList();
            for (android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction a : actions) {
                if (a.getId() == android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) {
                    clickable = true;
                    break;
                }
            }
        }
        if (usable && clickable) out.add(android.view.accessibility.AccessibilityNodeInfo.obtain(node));

        for (int i = 0; i < node.getChildCount(); i++) {
            android.view.accessibility.AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                collectClickableTargets(child, out);
                child.recycle();
            }
        }
    }

    private void moveCursorToScreenPoint(float tx, float ty) {
        if (cursor == null || wm == null) return;
        float maxX = Math.max(0, screenW - cursorSize);
        float maxY = Math.max(0, screenH - cursorSize);
        x = Math.max(0, Math.min(maxX, tx));
        y = Math.max(0, Math.min(maxY, ty));
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) cursor.getTag();
        lp.x = Math.round(x);
        lp.y = Math.round(y);
        try { wm.updateViewLayout(cursor, lp); } catch (Exception ignored) {}
    }

    public void hideMouseOverlay() {
        stopAutoTargetMode();
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
        if (cursor == null) return;
        float cx = x + 2f;
        float cy = y + 2f;

        // First try the accessibility node actually under the cursor. This is more
        // reliable for browser links, buttons and other accessible controls than
        // relying only on a synthetic screen gesture.
        boolean nodeHandled = performNodeClickAt(Math.round(cx), Math.round(cy), right);
        if (nodeHandled) return;

        // Fallback for arbitrary screen content: inject a real short touch gesture.
        if (Build.VERSION.SDK_INT >= 24) {
            Path p = new Path();
            p.moveTo(cx, cy);
            p.lineTo(cx + 1f, cy + 1f);
            GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(p, 0, 70);
            dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), null, null);
        }
    }

    private boolean performNodeClickAt(int px, int py, boolean right) {
        if (Build.VERSION.SDK_INT < 21) return false;
        android.view.accessibility.AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        android.view.accessibility.AccessibilityNodeInfo node = findNodeAt(root, px, py);
        if (node == null) return false;
        try {
            if (right && Build.VERSION.SDK_INT >= 24 &&
                node.getActionList().toString().contains("ACTION_CONTEXT_CLICK")) {
                /* ACTION_CONTEXT_CLICK is not available in this compile SDK; use gesture fallback for right-click. */
            }
            if (node.isClickable() && node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)) return true;
            // Some browser controls expose ACTION_CLICK without reporting clickable.
            if (node.getActionList().toString().contains("ACTION_CLICK")) {
                return node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);
            }
        } finally {
            node.recycle();
        }
        return false;
    }

    private android.view.accessibility.AccessibilityNodeInfo findNodeAt(android.view.accessibility.AccessibilityNodeInfo node, int px, int py) {
        android.graphics.Rect r = new android.graphics.Rect();
        node.getBoundsInScreen(r);
        if (!r.contains(px, py)) return null;
        // Search children first so the most specific control at the cursor wins.
        for (int i = node.getChildCount() - 1; i >= 0; i--) {
            android.view.accessibility.AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            android.view.accessibility.AccessibilityNodeInfo hit = findNodeAt(child, px, py);
            if (hit != null) return hit;
            child.recycle();
        }
        return android.view.accessibility.AccessibilityNodeInfo.obtain(node);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }

    @Override public void onDestroy() {
        stopAutoTargetMode();
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
