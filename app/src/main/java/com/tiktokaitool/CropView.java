package com.tiktokaitool;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.*;

public class CropView extends View {
    private Bitmap bitmap;
    private RectF crop = new RectF(100,100,600,600);
    private Paint overlay, border, handle, grid;
    private int drag = -1; // -1=none 0=move 1-4=corners
    private float lx, ly;
    private static final float H = 44f;

    public CropView(Context ctx, AttributeSet a) { super(ctx,a); init(); }

    private void init() {
        overlay = new Paint(); overlay.setColor(0xAA000000);
        border  = new Paint(); border.setColor(0xFFFF3B6B); border.setStrokeWidth(2.5f); border.setStyle(Paint.Style.STROKE);
        handle  = new Paint(); handle.setColor(0xFFFF3B6B); handle.setStyle(Paint.Style.FILL);
        grid    = new Paint(); grid.setColor(0x44FFFFFF);   grid.setStrokeWidth(1f); grid.setStyle(Paint.Style.STROKE);
    }

    public void setBitmap(Bitmap b) { bitmap=b; if(b!=null&&getWidth()>0) initCrop(); invalidate(); }

    private void initCrop() {
        float pw=getWidth()*0.75f, ph=getHeight()*0.55f;
        float cx=getWidth()/2f, cy=getHeight()/2f;
        crop.set(cx-pw/2,cy-ph/2,cx+pw/2,cy+ph/2);
    }

    @Override protected void onSizeChanged(int w,int h,int ow,int oh) { super.onSizeChanged(w,h,ow,oh); initCrop(); }

    @Override protected void onDraw(Canvas c) {
        if (bitmap==null) return;
        float sc=Math.min((float)getWidth()/bitmap.getWidth(),(float)getHeight()/bitmap.getHeight());
        float bw=bitmap.getWidth()*sc, bh=bitmap.getHeight()*sc;
        float bx=(getWidth()-bw)/2f, by=(getHeight()-bh)/2f;
        c.drawBitmap(bitmap,null,new RectF(bx,by,bx+bw,by+bh),null);

        Path p=new Path();
        p.addRect(0,0,getWidth(),getHeight(),Path.Direction.CW);
        p.addRect(crop,Path.Direction.CCW);
        c.drawPath(p,overlay);
        c.drawRect(crop,border);

        float dx=crop.width()/3f, dy=crop.height()/3f;
        c.drawLine(crop.left+dx,crop.top,crop.left+dx,crop.bottom,grid);
        c.drawLine(crop.left+2*dx,crop.top,crop.left+2*dx,crop.bottom,grid);
        c.drawLine(crop.left,crop.top+dy,crop.right,crop.top+dy,grid);
        c.drawLine(crop.left,crop.top+2*dy,crop.right,crop.top+2*dy,grid);

        float hs=H/2f;
        c.drawRoundRect(new RectF(crop.left-hs,crop.top-hs,crop.left+hs,crop.top+hs),6,6,handle);
        c.drawRoundRect(new RectF(crop.right-hs,crop.top-hs,crop.right+hs,crop.top+hs),6,6,handle);
        c.drawRoundRect(new RectF(crop.left-hs,crop.bottom-hs,crop.left+hs,crop.bottom+hs),6,6,handle);
        c.drawRoundRect(new RectF(crop.right-hs,crop.bottom-hs,crop.right+hs,crop.bottom+hs),6,6,handle);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        float x=e.getX(), y=e.getY();
        switch(e.getAction()) {
            case MotionEvent.ACTION_DOWN: drag=getHandle(x,y); lx=x; ly=y; return true;
            case MotionEvent.ACTION_MOVE:
                if(drag<0) return true;
                float dx=x-lx, dy=y-ly;
                if(drag==0){crop.offset(dx,dy);clamp();}
                else if(drag==1){crop.left=Math.min(crop.left+dx,crop.right-80);crop.top=Math.min(crop.top+dy,crop.bottom-80);}
                else if(drag==2){crop.right=Math.max(crop.right+dx,crop.left+80);crop.top=Math.min(crop.top+dy,crop.bottom-80);}
                else if(drag==3){crop.left=Math.min(crop.left+dx,crop.right-80);crop.bottom=Math.max(crop.bottom+dy,crop.top+80);}
                else if(drag==4){crop.right=Math.max(crop.right+dx,crop.left+80);crop.bottom=Math.max(crop.bottom+dy,crop.top+80);}
                lx=x; ly=y; invalidate(); return true;
            case MotionEvent.ACTION_UP: drag=-1; return true;
        }
        return false;
    }

    private int getHandle(float x,float y) {
        if(hit(x,y,crop.left,crop.top)) return 1;
        if(hit(x,y,crop.right,crop.top)) return 2;
        if(hit(x,y,crop.left,crop.bottom)) return 3;
        if(hit(x,y,crop.right,crop.bottom)) return 4;
        if(crop.contains(x,y)) return 0;
        return -1;
    }
    private boolean hit(float x,float y,float hx,float hy) { return Math.abs(x-hx)<H&&Math.abs(y-hy)<H; }
    private void clamp() {
        float w=crop.width(), h=crop.height();
        if(crop.left<0) crop.offsetTo(0,crop.top);
        if(crop.top<0) crop.offsetTo(crop.left,0);
        if(crop.right>getWidth()) crop.offsetTo(getWidth()-w,crop.top);
        if(crop.bottom>getHeight()) crop.offsetTo(crop.left,getHeight()-h);
    }
    public Bitmap getCropped() {
        if(bitmap==null) return null;
        float sc=Math.min((float)getWidth()/bitmap.getWidth(),(float)getHeight()/bitmap.getHeight());
        float bx=(getWidth()-bitmap.getWidth()*sc)/2f, by=(getHeight()-bitmap.getHeight()*sc)/2f;
        int l=(int)Math.max(0,(crop.left-bx)/sc), t=(int)Math.max(0,(crop.top-by)/sc);
        int r=(int)Math.min(bitmap.getWidth(),(crop.right-bx)/sc);
        int bot=(int)Math.min(bitmap.getHeight(),(crop.bottom-by)/sc);
        if(r<=l||bot<=t) return bitmap;
        return Bitmap.createBitmap(bitmap,l,t,r-l,bot-t);
    }
}
