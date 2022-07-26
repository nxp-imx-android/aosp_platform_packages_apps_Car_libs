/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.ui.shortcutspopup;

import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.GravityInt;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.car.ui.R;

/**
 * <p>
 * Wrapper view which has an optional Arrow. Positioned with
 * {@link android.R.styleable#CarUiArrowContainerView_carUiArrowGravity}
 * </p>
 *
 * <p>
 * The {@link CarUiArrowContainerView#mArrowViewSpace} has its own ViewBounds defined by
 * Width: {@link android.view.ViewGroup.LayoutParams#MATCH_PARENT}
 * and Height {@link CarUiArrowContainerView#mArrowHeight}, which lie outside Bounds of ContentView
 * ({@link CarUiArrowContainerView#mContentViewId})
 *
 * Arrow Path uses the space of view {@link CarUiArrowContainerView#mArrowViewSpace},
 * Its width is defined by: {@link CarUiArrowContainerView#mArrowWidth}
 * and Height: {@link CarUiArrowContainerView#mArrowHeight}
 * </p>
 *
 * @attr ref R.styleable.CarUiArrowContainerView_carUiHasArrow
 * @attr ref R.styleable.CarUiArrowContainerView_carUiArrowColor
 * @attr ref R.styleable.CarUiArrowContainerView_carUiArrowWidth
 * @attr ref R.styleable.CarUiArrowContainerView_carUiArrowHeight
 * @attr ref R.styleable.CarUiArrowContainerView_carUiArrowRadius
 * @attr ref R.styleable.CarUiArrowContainerView_carUiOffsetX
 * @attr ref R.styleable.CarUiArrowContainerView_carUiOffsetY
 * @attr ref R.styleable.CarUiArrowContainerView_carUiArrowGravity
 * @attr ref R.styleable.CarUiArrowContainerView_carUiContentView
 * @attr ref R.styleable.CarUiArrowContainerView_carUiContentViewDrawable
 */
public class CarUiArrowContainerView extends LinearLayout {

    private final Paint mPaint = new Paint();
    private final Path mPath = new Path();

    private boolean mHasArrow;
    private boolean mArrowGravityLeft;
    private boolean mArrowGravityTop;
    private float mArrowWidth;
    private float mArrowHeight;
    private float mArrowRadius;
    private float mArrowOffsetX;
    private float mArrowOffsetY;
    private View mArrowViewSpace;

    @IdRes
    private int mContentViewId;
    @DrawableRes
    private int mContentDrawableId;


    //Gravity default is left + top
    @GravityInt
    private static final int ARROW_GRAVITY_LEFT = 0x01;
    @GravityInt
    private static final int ARROW_GRAVITY_TOP = 0x02;
    @GravityInt
    private static final int ARROW_DEFAULT_GRAVITY = ARROW_GRAVITY_LEFT | ARROW_GRAVITY_TOP;
    //the arrow view should slightly overlap with the content view to blend in,
    private static final int ARROW_BLEND_IN_HEIGHT = -1;
    private static final String TAG = CarUiArrowContainerView.class.getSimpleName();
    private static final String ARROW_VIEW_ATTACHED_TOP_TAG = "CAR_UI_ARROW_VIEW_TOP_TAG";
    private static final String ARROW_VIEW_ATTACHED_BOTTOM_TAG = "CAR_UI_ARROW_VIEW_BOTTOM_TAG";


    public CarUiArrowContainerView(@NonNull Context context) {
        super(context);
    }

    public CarUiArrowContainerView(@NonNull Context context,
            @Nullable AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.CarUiArrowContainerView, 0, 0);
        mHasArrow = a.getBoolean(R.styleable.CarUiArrowContainerView_carUiHasArrow, false);
        int arrowColor = a.getColor(R.styleable.CarUiArrowContainerView_carUiArrowColor,
                Color.GRAY);
        mArrowWidth = a.getDimension(R.styleable.CarUiArrowContainerView_carUiArrowWidth, 0);
        mArrowHeight = a.getDimension(R.styleable.CarUiArrowContainerView_carUiArrowHeight, 0);
        mArrowRadius = a.getDimension(R.styleable.CarUiArrowContainerView_carUiArrowRadius, 0);
        mArrowOffsetX = a.getDimension(R.styleable.CarUiArrowContainerView_carUiOffsetX, 0);
        mArrowOffsetY = a.getDimension(R.styleable.CarUiArrowContainerView_carUiOffsetY, 0);
        mArrowGravityLeft = (a.getInt(R.styleable.CarUiArrowContainerView_carUiArrowGravity,
                ARROW_DEFAULT_GRAVITY) & ARROW_GRAVITY_LEFT) == ARROW_GRAVITY_LEFT;
        mArrowGravityTop = (a.getInt(R.styleable.CarUiArrowContainerView_carUiArrowGravity,
                ARROW_DEFAULT_GRAVITY) & ARROW_GRAVITY_TOP) == ARROW_GRAVITY_TOP;
        mContentViewId = a.getResourceId(R.styleable.CarUiArrowContainerView_carUiContentView, 0);
        mContentDrawableId = a.getResourceId(
                R.styleable.CarUiArrowContainerView_carUiContentViewDrawable, 0);

        mPaint.setColor(arrowColor);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);

        //override the orientation of the parent Linear Layout to be always vertical
        setOrientation(VERTICAL);
        a.recycle();
        refreshArrowView(false);
        setWillNotDraw(false);
    }

    /**
     * Sets the height of the Arrow
     */
    public void setArrowHeight(int arrowHeight) {
        if (mArrowHeight == arrowHeight) {
            return;
        }
        mArrowHeight = arrowHeight;
        refreshArrowView(true);
        invalidate();
    }

    /**
     * Sets the width of the Arrow
     */
    public void setArrowWidth(int arrowWidth) {
        if (mArrowWidth == arrowWidth) {
            return;
        }
        mArrowWidth = arrowWidth;
        invalidate();
    }

    /**
     * arrowOffset in the horizontal direction to {@link CarUiArrowContainerView#mContentViewId}
     */
    public void setArrowOffsetX(int offsetX) {
        if (mArrowOffsetX == offsetX) {
            return;
        }
        mArrowOffsetX = offsetX;
        invalidate();
    }

    /**
     * arrowOffset in the vertical direction to {@link CarUiArrowContainerView#mContentViewId}
     */
    public void setArrowOffsetY(int offsetY) {
        if (mArrowOffsetY == offsetY) {
            return;
        }
        mArrowOffsetY = offsetY;
        invalidate();
    }

    /**
     * Sets the height of the Arrow
     */
    public void setArrowRadius(int arrowRadius) {
        if (mArrowRadius == arrowRadius) {
            return;
        }
        mArrowRadius = arrowRadius;
        invalidate();
    }

    /**
     * Sets the drawable to {@link CarUiArrowContainerView#mContentViewId}
     */
    public void setContentDrawableId(@DrawableRes int drawableId) {
        if (mContentDrawableId == drawableId) {
            return;
        }
        mContentDrawableId = drawableId;
        invalidate();
    }

    /**
     * Orientation is always {@link LinearLayout#VERTICAL}.
     * as the arrow is drawn either above or below the Content.
     */
    @Override
    public void setOrientation(int orientation) {
        super.setOrientation(VERTICAL);
    }


    /**
     * Sets the container View to add or remove the arrow from the container view
     */
    public void setHasArrow(boolean hasArrow) {
        if (mHasArrow == hasArrow) {
            return;
        }
        mHasArrow = hasArrow;
        refreshArrowView(false);
        invalidate();
    }

    /**
     * Sets the gravity of the arrow above the Content View {@link
     * CarUiArrowContainerView#mContentViewId}
     *
     * @param gravityTop - if true sets to top, otherwise below.
     */
    public void setArrowGravityTop(boolean gravityTop) {
        if (mArrowGravityTop == gravityTop) {
            return;
        }
        mArrowGravityTop = gravityTop;
        refreshArrowView(false);
        invalidate();
    }

    /**
     * Sets the gravity of the arrow left to the Content View {@link
     * CarUiArrowContainerView#mContentViewId}
     *
     * @param gravityLeft - if true sets to left, otherwise right.
     */
    public void setArrowGravityLeft(boolean gravityLeft) {
        if (mArrowGravityLeft == gravityLeft) {
            return;
        }
        mArrowGravityLeft = gravityLeft;
        refreshArrowView(false);
        invalidate();
    }

    private void refreshArrowView(boolean force) {
        String newArrowViewTag =
                mArrowGravityTop ? ARROW_VIEW_ATTACHED_TOP_TAG : ARROW_VIEW_ATTACHED_BOTTOM_TAG;
        if (mArrowViewSpace != null) {
            // check if the view is already attached at the expected arrowPositionY
            if (mHasArrow && mArrowViewSpace.getTag() == newArrowViewTag && !force) {
                return;
            }
            removeView(mArrowViewSpace);
        }
        if (!mHasArrow) {
            return;
        }
        ViewGroup.LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                (int) mArrowHeight);
        // need to accommodate more space to draw the arrow attached to the content view
        mArrowViewSpace = new View(getContext());
        mArrowViewSpace.setLayoutParams(params);
        // add the view before contentView if mArrowGravityTop is true,
        // otherwise add the view a after contentView
        mArrowViewSpace.setTag(newArrowViewTag);
        addView(mArrowViewSpace, mArrowGravityTop ? 0 : 1);
    }

    /**
     * Horizontal Offset for the Arrow from the edges.
     */
    public void setArrowHorizontalOffset(float offset) {
        if (mArrowOffsetX == offset) {
            return;
        }
        mArrowOffsetX = offset;
        invalidate();
    }

    /**
     * Vertical Offset for the Arrow from the edges.
     */
    public void setArrowVerticalOffset(float offset) {
        if (mArrowOffsetY == offset) {
            return;
        }
        mArrowOffsetY = offset;
        invalidate();
    }

    public int getArrowOffsetX() {
        return Math.round(mArrowOffsetX);
    }

    public int getArrowWidth() {
        return Math.round(mArrowWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPath.reset();
        drawContentBody();
        if (mHasArrow) {
            mPath.op(drawArrow(mArrowViewSpace, mArrowWidth, mArrowHeight, mArrowRadius,
                            mArrowGravityTop, mArrowGravityLeft, mArrowOffsetX, mArrowOffsetY
                    ),
                    Path.Op.UNION);
        }
        mPath.close();
        canvas.drawPath(mPath, mPaint);
        super.onDraw(canvas);
    }

    /**
     * sets the background for
     * {@link CarUiArrowContainerView#mContentViewId}
     */
    private void drawContentBody() {
        View contentView = findViewById(mContentViewId);
        contentView.setBackgroundResource(mContentDrawableId);
    }

    @Override
    public void setEnabled(boolean enabled) {
        View contentView = findViewById(mContentViewId);
        contentView.setEnabled(enabled);
    }

    /**
     * Calculated for the arrow pointing down
     * *p0--------p1
     * **\        /
     * ***p4  c  p2
     * ****\    /
     * ******p3
     * flip the Arrow if needed based on {@link CarUiArrowContainerView#mArrowGravityTop}
     *
     * @param arrowViewSpace   view where the arrow will be drawn
     * @param arrowWidth       width of the arrow
     * @param arrowHeight      height of the arrow
     * @param arrowRadius      radius of the arrow
     * @param arrowGravityTop  is arrow gravity top relative to
     *                         {@link CarUiArrowContainerView#mContentViewId}
     * @param arrowGravityLeft is arrow gravity left relative to
     *                         {@link CarUiArrowContainerView#mContentViewId}
     * @param arrowOffsetX     offsets arrow horizontally from the edges.
     * @param arrowOffsetY     offsets arrow vertically from the edges.
     *                         {@link CarUiArrowContainerView#mContentViewId}
     */
    private Path drawArrow(View arrowViewSpace, float arrowWidth, float arrowHeight,
            float arrowRadius, boolean arrowGravityTop, boolean arrowGravityLeft,
            float arrowOffsetX, float arrowOffsetY) {
        if (arrowViewSpace == null) {
            return new Path();
        }
        //the top of the arrow cab be below the content view
        float top = arrowViewSpace.getTop();

        //we need to calculate p2 and p3 and p4 based on the width, height and radius of the Arrow.
        //Theta is half of the angle inside the triangle tip
        float tanTheta = arrowWidth / (2.0f * arrowHeight);
        float theta = (float) atan(tanTheta);

        //fit the arc (Rounded point) at the tip of triangle.
        //centerX is mArrowWidth/2.
        float centerX = arrowWidth / 2.0f;
        float centerY = (float) (arrowHeight - (arrowRadius / sin(theta)));

        //hypotenuses,line p2-p3 in the triangle p3-c-p2
        float lineFromP2ToP3 = arrowRadius / tanTheta;
        float lineFromP2ToC = (float) (lineFromP2ToP3 * sin(theta));
        float lineFromP3ToC = (float) (arrowHeight - (lineFromP2ToP3 * cos(theta)));

        Path path = new Path();
        path.reset();
        //move to p0
        path.moveTo(0, top);
        //draw a line from p0-p1
        path.lineTo(arrowWidth, top);
        //draw line p1-p2
        path.lineTo(
                centerX + lineFromP2ToC,
                lineFromP3ToC + top);

        //draw the rounded point, going from point p2 to p4, centered at c
        float thetaDeg = (float) toDegrees(theta);
        path.arcTo(
                centerX - arrowRadius,
                centerY - arrowRadius + top,
                centerX + arrowRadius,
                centerY + arrowRadius + top,
                thetaDeg,
                180 - (2 * thetaDeg),
                false);

        // draw the line p4-p0
        path.lineTo(0, 0 + top);
        path.close();

        int blendInHeightOffset = ARROW_BLEND_IN_HEIGHT;
        //arrow is attached to the top, hence need to be reversed.
        if (arrowGravityTop) {
            blendInHeightOffset = blendInHeightOffset * -1;
            Matrix flipTransform = new Matrix();
            flipTransform.setRotate(180, arrowWidth * 0.5f, arrowHeight * 0.5f);
            path.transform(flipTransform);
        }
        //position of the arrow is on the right
        if (!arrowGravityLeft) {
            path.offset(getWidth() - arrowWidth - arrowOffsetX,
                    arrowOffsetY + blendInHeightOffset);
        } else {
            path.offset(arrowOffsetX, arrowOffsetY + blendInHeightOffset);
        }
        return path;
    }

}
