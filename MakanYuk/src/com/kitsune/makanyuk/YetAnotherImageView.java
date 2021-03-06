package com.kitsune.makanyuk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class YetAnotherImageView extends ImageView {

    public YetAnotherImageView(Context context) {
        super(context);
    }
    
    public YetAnotherImageView(Context context, AttributeSet attrSet) {
        super(context, attrSet);
    }

    OnImageChangeListener onImageChangeListener;

    public void setOnImageChangeListener(
    		OnImageChangeListener onImageChangeListiner) {
        this.onImageChangeListener = onImageChangeListiner;
    }

    
    @Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
        if (onImageChangeListener != null)
            onImageChangeListener.imageDrawed();
	}

	@Override
    public void setBackgroundResource(int resid) {
        super.setBackgroundResource(resid);
        if (onImageChangeListener != null)
            onImageChangeListener.imageChangedinView( getResources().getDrawable(resid) );
    }


  @Override
	public void setImageDrawable(Drawable drawable) {
		super.setImageDrawable(drawable);
		if (onImageChangeListener != null)
            onImageChangeListener.imageChangedinView( drawable );
	}

	public static interface OnImageChangeListener {
        public void imageChangedinView(Drawable drawable);
        public void imageDrawed();
    }
    
}
