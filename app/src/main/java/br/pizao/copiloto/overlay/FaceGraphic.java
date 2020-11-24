/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.pizao.copiloto.overlay;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;

import java.util.Arrays;
import java.util.Locale;

/**
 * Graphic instance for rendering face position, contour, and landmarks within the associated
 * graphic overlay view.
 */
public class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float FACE_POSITION_RADIUS = 8.0f;
    private static final float ID_TEXT_SIZE = 30.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;
    private static final int NUM_COLORS = 10;
    private static final int[][] COLORS =
            new int[][]{
                    {Color.BLACK, Color.WHITE},
                    {Color.WHITE, Color.MAGENTA},
                    {Color.BLACK, Color.LTGRAY},
                    {Color.WHITE, Color.RED},
                    {Color.WHITE, Color.BLUE},
                    {Color.WHITE, Color.DKGRAY},
                    {Color.BLACK, Color.CYAN},
                    {Color.BLACK, Color.YELLOW},
                    {Color.WHITE, Color.BLACK},
                    {Color.BLACK, Color.GREEN}
            };

    private final Paint facePositionPaint;
    private final Paint[] idPaints;
    private final Paint[] boxPaints;
    private final Paint[] labelPaints;

    private volatile Face face;

    public FaceGraphic(GraphicOverlay overlay, Face face) {
        super(overlay);

        this.face = face;
        final int selectedColor = Color.WHITE;

        facePositionPaint = new Paint();
        facePositionPaint.setColor(selectedColor);

        int numColors = COLORS.length;
        idPaints = new Paint[numColors];
        boxPaints = new Paint[numColors];
        labelPaints = new Paint[numColors];
        for (int i = 0; i < numColors; i++) {
            idPaints[i] = new Paint();
            idPaints[i].setColor(COLORS[i][0] /* text color */);
            idPaints[i].setTextSize(ID_TEXT_SIZE);

            boxPaints[i] = new Paint();
            boxPaints[i].setColor(COLORS[i][1] /* background color */);
            boxPaints[i].setStyle(Paint.Style.STROKE);
            boxPaints[i].setStrokeWidth(BOX_STROKE_WIDTH);

            labelPaints[i] = new Paint();
            labelPaints[i].setColor(COLORS[i][1] /* background color */);
            labelPaints[i].setStyle(Paint.Style.FILL);
        }
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Face face = this.face;
        if (face == null) {
            return;
        }

        float x = translateX(face.getBoundingBox().centerX());
        float y = translateY(face.getBoundingBox().centerY());

        // Calculate positions.
        float left = x - scale(face.getBoundingBox().width() / 2.0f);
        float top = y - scale(face.getBoundingBox().height() / 2.0f);
        float right = x + scale(face.getBoundingBox().width() / 2.0f);
        float bottom = y + scale(face.getBoundingBox().height() / 2.0f);
        float lineHeight = ID_TEXT_SIZE + BOX_STROKE_WIDTH;
        float yLabelOffset = (face.getTrackingId() == null) ? 0 : -lineHeight;

        // Decide color based on face ID
        int colorID = (face.getTrackingId() == null) ? 0 : Math.abs(face.getTrackingId() % NUM_COLORS);

        // Calculate width and height of label box
        float textWidth = 0;
        if (face.getLeftEyeOpenProbability() != null) {
            yLabelOffset -= lineHeight;
            textWidth =
                    Math.max(
                            textWidth,
                            idPaints[colorID].measureText(
                                    String.format(
                                            Locale.US, "Left eye open: %.2f", face.getLeftEyeOpenProbability())));
        }
        if (face.getRightEyeOpenProbability() != null) {
            yLabelOffset -= lineHeight;
            textWidth =
                    Math.max(
                            textWidth,
                            idPaints[colorID].measureText(
                                    String.format(
                                            Locale.US, "Right eye open: %.2f", face.getRightEyeOpenProbability())));
        }

        // Draw labels
        canvas.drawRect(
                left - BOX_STROKE_WIDTH,
                top + yLabelOffset,
                left + textWidth + (2 * BOX_STROKE_WIDTH),
                top,
                labelPaints[colorID]);
        yLabelOffset += ID_TEXT_SIZE;
        canvas.drawRect(left, top, right, bottom, boxPaints[colorID]);

        FaceContour leftEye = face.getContour(FaceContour.LEFT_EYE);
        FaceContour rightEye = face.getContour(FaceContour.RIGHT_EYE);
        for (FaceContour contour : Arrays.asList(leftEye, rightEye)) {
            if (contour != null) {
                for (PointF point : contour.getPoints()) {
                    canvas.drawCircle(
                            translateX(point.x), translateY(point.y), FACE_POSITION_RADIUS, facePositionPaint);
                }
            }
        }

        if (face.getLeftEyeOpenProbability() != null) {
            canvas.drawText(
                    "Left eye open: " + String.format(Locale.US, "%.2f", face.getLeftEyeOpenProbability()),
                    left,
                    top + yLabelOffset,
                    idPaints[colorID]);
            yLabelOffset += lineHeight;
        }

        if (face.getRightEyeOpenProbability() != null) {
            canvas.drawText(
                    "Right eye open: " + String.format(Locale.US, "%.2f", face.getRightEyeOpenProbability()),
                    left,
                    top + yLabelOffset,
                    idPaints[colorID]);
        }
    }
}
