/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (C) 2011 Iranian Supreme Council of ICT, The FarsiTel Project
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
package android.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.CharacterStyle;
import android.text.style.MetricAffectingSpan;
import android.text.style.ReplacementSpan;

/**
 * This class provides static methods for drawing and measuring styled text,
 * like {@link android.text.Spanned} object with
 * {@link android.text.style.ReplacementSpan}.
 *
 * @hide
 */
public class Styled
{
    /**
     * Draws and/or measures a uniform run of text on a single line. No span of
     * interest should start or end in the middle of this run (if not
     * drawing, character spans that don't affect metrics can be ignored).
     * Neither should the run direction change in the middle of the run.
     *
     * <p>The x position is the leading edge of the text. In a right-to-left
     * paragraph, this will be to the right of the text to be drawn. Paint
     * should not have an Align value other than LEFT or positioning will get
     * confused.
     *
     * <p>On return, workPaint will reflect the original paint plus any
     * modifications made by character styles on the run.
     *
     * <p>The returned width is signed and will be < 0 if the paragraph
     * direction is right-to-left.
     */
    private static float drawUniformRun(Canvas canvas,
                              Spanned text, int start, int end,
                              float x, int top, int y, int bottom,
                              Paint.FontMetricsInt fmi,
                              TextPaint paint,
                              TextPaint workPaint,
                              boolean needWidth) {

        boolean haveWidth = false;
        float ret = 0;
        CharacterStyle[] spans = text.getSpans(start, end, CharacterStyle.class);

        ReplacementSpan replacement = null;

        // XXX: This shouldn't be modifying paint, only workPaint.
        // However, the members belonging to TextPaint should have default
        // values anyway.  Better to ensure this in the Layout constructor.
        paint.bgColor = 0;
        paint.baselineShift = 0;
        workPaint.set(paint);

		if (spans.length > 0) {
			for (int i = 0; i < spans.length; i++) {
				CharacterStyle span = spans[i];

				if (span instanceof ReplacementSpan) {
					replacement = (ReplacementSpan)span;
				}
				else {
					span.updateDrawState(workPaint);
				}
			}
		}

        if (replacement == null) {
            String tmp;
            int tmpstart, tmpend;

            tmp = text.toString();
            tmpstart = start;
            tmpend = end;

            if (fmi != null) {
                workPaint.getFontMetricsInt(fmi);
            }

            if (canvas != null) {
                if (workPaint.bgColor != 0) {
                    int c = workPaint.getColor();
                    Paint.Style s = workPaint.getStyle();
                    workPaint.setColor(workPaint.bgColor);
                    workPaint.setStyle(Paint.Style.FILL);

                    if (!haveWidth) {
                        ret = workPaint._measureText(tmp, tmpstart, tmpend);
                        haveWidth = true;
                    }

                    canvas.drawRect(x, top, x + ret, bottom, workPaint);

                    workPaint.setStyle(s);
                    workPaint.setColor(c);
                }

                if (needWidth) {
                    if (!haveWidth) {
                        ret = workPaint._measureText(tmp, tmpstart, tmpend);
                        haveWidth = true;
                    }
                }

                canvas._drawText(tmp, tmpstart, tmpend,
                                x, y + workPaint.baselineShift, workPaint);
            } else {
                if (needWidth && !haveWidth) {
                    ret = workPaint._measureText(tmp, tmpstart, tmpend);
                    haveWidth = true;
                }
            }
        } else {
            ret = replacement.getSize(workPaint, text, start, end, fmi);

            if (canvas != null) {
                replacement.draw(canvas, text, start, end,
                                 x, top, y, bottom, workPaint);
            }
        }

        return ret;
    }

    /**
     * Returns the advance widths for a uniform left-to-right run of text with
     * no style changes in the middle of the run. If any style is replacement
     * text, the first character will get the width of the replacement and the
     * remaining characters will get a width of 0.
     * 
     * @param paint the paint, will not be modified
     * @param workPaint a paint to modify; on return will reflect the original
     *        paint plus the effect of all spans on the run
     * @param text the text
     * @param start the start of the run
     * @param end the limit of the run
     * @param widths array to receive the advance widths of the characters. Must
     *        be at least a large as (end - start).
     * @param fmi FontMetrics information; can be null
     * @return the actual number of widths returned
     */
    public static int getTextWidths(TextPaint paint,
                                    TextPaint workPaint,
                                    Spanned text, FriBidi fribidi,
                                    int start, int end,
                                    float[] widths, Paint.FontMetricsInt fmi) {
        MetricAffectingSpan[] spans =
            text.getSpans(start, end, MetricAffectingSpan.class);

		ReplacementSpan replacement = null;
        workPaint.set(paint);
		
		for (int i = 0; i < spans.length; i++) {
			MetricAffectingSpan span = spans[i];
			if (span instanceof ReplacementSpan) {
				replacement = (ReplacementSpan)span;
			}
			else {
				span.updateMeasureState(workPaint);
			}
		}
	
        int result;
        if (replacement == null) {
            workPaint.getFontMetricsInt(fmi);
            if (fribidi != null)
                result = workPaint._getTextWidths(fribidi.before_reorder, start, end, widths);
            else
                result = workPaint.getTextWidths(text, start, end, widths);
        } else {
            int wid = replacement.getSize(workPaint, text, start, end, fmi);

            if (end > start) {
                widths[0] = wid;
                for (int i = start + 1; i < end; i++)
                    widths[i - start] = 0;
            }
            result = end - start;
        }
        return result;
    }

    /**
     * Renders and/or measures a directional run of text on a single line.
     * Unlike {@link #drawUniformRun}, this can render runs that cross style
     * boundaries.  Returns the signed advance width, if requested.
     *
     * <p>The x position is the leading edge of the text. In a right-to-left
     * paragraph, this will be to the right of the text to be drawn. Paint
     * should not have an Align value other than LEFT or positioning will get
     * confused.
     *
     * <p>This optimizes for unstyled text and so workPaint might not be
     * modified by this call.
     *
     * <p>The returned advance width will be < 0 if the paragraph
     * direction is right-to-left.
     */
    private static float drawDirectionalRun(Canvas canvas,
                                 CharSequence text, FriBidi fribidi,
                                 int start, int end,
                                 float x, int top, int y, int bottom,
                                 Paint.FontMetricsInt fmi,
                                 TextPaint paint,
                                 TextPaint workPaint,
                                 boolean needWidth) {

        // XXX: It looks like all calls to this API match dir and runIsRtl, so
        // having both parameters is redundant and confusing.

        // fast path for unstyled text
        if (!(text instanceof Spanned)) {
            float ret = 0;

            String cs;
            if (fribidi != null)
                cs = fribidi.before_reorder;
            else
                cs = text.toString();

            if (needWidth)
                ret = paint._measureText(cs, start, end);

            if (canvas != null)
                canvas._drawText(cs, start, end, x, y, paint);

            if (fmi != null) {
                paint.getFontMetricsInt(fmi);
            }

            return ret; // DIR_RIGHT_TO_LEFT -> -ret ?
        }
        
        float ox = x;
        int minAscent = 0, maxDescent = 0, minTop = 0, maxBottom = 0;

        Spanned sp = (Spanned) text;
        if (fribidi != null)
            sp = fribidi.getSpanWrapper(sp);

        Class<?> division;

        if (canvas == null)
            division = MetricAffectingSpan.class;
        else
            division = CharacterStyle.class;

        int next;
        for (int i = start; i < end; i = next) {
            next = sp.nextSpanTransition(i, end, division);

            // XXX: if dir and runIsRtl were not the same, this would draw
            // spans in the wrong order, but no one appears to call it this
            // way.
            x += drawUniformRun(canvas, sp, i, next,
                  x, top, y, bottom, fmi, paint, workPaint,
                  needWidth || next != end);

            if (fmi != null) {
                if (fmi.ascent < minAscent)
                    minAscent = fmi.ascent;
                if (fmi.descent > maxDescent)
                    maxDescent = fmi.descent;

                if (fmi.top < minTop)
                    minTop = fmi.top;
                if (fmi.bottom > maxBottom)
                    maxBottom = fmi.bottom;
            }
        }

        if (fmi != null) {
            if (start == end) {
                paint.getFontMetricsInt(fmi);
            } else {
                fmi.ascent = minAscent;
                fmi.descent = maxDescent;
                fmi.top = minTop;
                fmi.bottom = maxBottom;
            }
        }

        return x - ox;
    }

    /**
     * Draws a run of text on a single line, with its
     * origin at (x,y), in the specified Paint. The origin is interpreted based
     * on the Align setting in the Paint.
     *
     * This method considers style information in the text (e.g. even when text
     * is an instance of {@link android.text.Spanned}, this method correctly
     * draws the text). See also
     * {@link android.graphics.Canvas#drawText(CharSequence, int, int, float,
     * float, Paint)} and
     * {@link android.graphics.Canvas#drawRect(float, float, float, float,
     * Paint)}.
     * 
     * @param canvas The target canvas
     * @param text The text to be drawn
     * @param start The index of the first character in text to draw
     * @param end (end - 1) is the index of the last character in text to draw
     * @param direction The direction of the text. This must be
     *        {@link android.text.Layout#DIR_LEFT_TO_RIGHT} or
     *        {@link android.text.Layout#DIR_RIGHT_TO_LEFT}.
     * @param x The x-coordinate of origin for where to draw the text
     * @param top The top side of the rectangle to be drawn
     * @param y The y-coordinate of origin for where to draw the text
     * @param bottom The bottom side of the rectangle to be drawn
     * @param paint The main {@link TextPaint} object.
     * @param workPaint The {@link TextPaint} object used for temporal
     *        workspace.
     * @param needWidth If true, this method returns the width of drawn text
     * @return Width of the drawn text if needWidth is true
     */
    public static float drawText(Canvas canvas,
                                 CharSequence text, FriBidi fribidi,
                                 int start, int end,
                                 float x, int top, int y, int bottom,
                                 TextPaint paint,
                                 TextPaint workPaint,
                                 boolean needWidth) {
    	return drawDirectionalRun(canvas, text, fribidi, start, end,
                       x, top, y, bottom, null, paint, workPaint,
                       needWidth);
    }
    
    /**
     * Returns the width of a run of left-to-right text on a single line,
     * considering style information in the text (e.g. even when text is an
     * instance of {@link android.text.Spanned}, this method correctly measures
     * the width of the text).
     * 
     * @param paint the main {@link TextPaint} object; will not be modified
     * @param workPaint the {@link TextPaint} object available for modification;
     *        will not necessarily be used
     * @param text the text to measure
     * @param start the index of the first character to start measuring
     * @param end 1 beyond the index of the last character to measure
     * @param fmi FontMetrics information; can be null
     * @return The width of the text
     */
    public static float measureText(TextPaint paint,
                                    TextPaint workPaint,
                                    CharSequence text, FriBidi fribidi,
                                    int start, int end,
                                    Paint.FontMetricsInt fmi) {
        return drawDirectionalRun(null, text, fribidi, start, end,
                       0, 0, 0, 0, fmi, paint, workPaint, true);
    }
}
