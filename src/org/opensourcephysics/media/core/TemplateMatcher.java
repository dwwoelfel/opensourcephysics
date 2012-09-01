/*
 * The org.opensourcephysics.media.core package defines the Open Source Physics
 * media framework for working with video and other media.
 *
 * Copyright (c) 2004  Douglas Brown and Wolfgang Christian.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * For additional information and documentation on Open Source Physics,
 * please see <http://www.opensourcephysics.org/>.
 */
package org.opensourcephysics.media.core;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import org.opensourcephysics.display.*;
import org.opensourcephysics.tools.DatasetCurveFitter;
import org.opensourcephysics.tools.UserFunction;

/**
 * A class to find the best match of a template image in a target image.
 * The match location is estimated to sub-pixel accuracy by assuming the 
 * distribution of match scores near a peak is Gaussian. 
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class TemplateMatcher {
	
	// static constants
	private static final double LARGE_NUMBER = 1.0E10;
	
  // instance fields
	private BufferedImage original, template, working;
	private Shape mask;
  private int[] pixels, templateR, templateG, templateB;
  private boolean[] isPixelTransparent;
  private int[] targetPixels;
  private int wTemplate, hTemplate; // width and height of the template image
  private int wTarget, hTarget; // width and height of the target image
  private int wTest, hTest; // width and height of the tested image (in search rect)
  private TPoint p = new TPoint(); // for general use in methods
  private double largeNumber = 1.0E20; // bigger than any expected difference
  private DatasetCurveFitter fitter; // used for Gaussian fit
  private Dataset dataset; // used for Gaussian fit
  private UserFunction f; // used for Gaussian fit
  private double[] pixelOffsets = {-1, 0, 1}; // used for Gaussian fit
  private double[] xValues = new double[3]; // used for Gaussian fit
  private double[] yValues = new double[3]; // used for Gaussian fit
  private double peakHeight, peakWidth; // peak height and width of most recent match
  private int trimLeft, trimTop;

  /**
   * Constructs a TemplateMatcher object. If a mask shape is specified, then
   * only pixels that are entirely inside the mask are included in the template.
   * 
   * @param image the image to match
   * @param maskShape a shape to define inside pixels (may be null)
   */
  public TemplateMatcher(BufferedImage image, Shape maskShape) {
		original = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
  	original.createGraphics().drawImage(image, 0, 0, null);
  	mask = maskShape;
    getTemplate();
    // set up the Gaussian curve fitter
		dataset = new Dataset();
		fitter = new DatasetCurveFitter(dataset);
    fitter.setAutofit(true);
    f = new UserFunction("gaussian"); //$NON-NLS-1$
    f.setParameters(new String[] {"a", "b", "c"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    		new double[] {1, 0, 1});
    f.setExpression("a*exp(-(x-b)^2/c)", new String[] {"x"}); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /**
   * Gets the template. Includes only pixels inside the mask, if any.
   *
   * @return the template
   */
  public BufferedImage getTemplate() {
  	if (template == null) {
  		rebuildTemplate(original, 255, 0); // builds from scratch
  	}
  	return template;
  }
  	
  /**
   * Rebuilds the template from an input image. 
   * The input image dimensions must match the original. 
   * The input and original are overlaid onto the existing template, if any. 
   * Pixels that fall outside the mask are ignored in the final template.		
   * 
   * @param image the input image
   * @param alphaInput the opacity with which the input image is overlaid
   * @param alphaOriginal the opacity with which the original image is overlaid
   */
  public void rebuildTemplate(BufferedImage image, int alphaInput, int alphaOriginal) {
  	int w = image.getWidth();
		int h = image.getHeight();
  	// return if image dimensions do not match original image
		if (original.getWidth()!=w || original.getHeight()!=h)
			return;
		// return if both alphas are zero
		if (alphaInput==0 && alphaOriginal==0)
			return;
  	// draw image onto argb input
		BufferedImage input = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
  	input.createGraphics().drawImage(image, 0, 0, null);
		// create working image if needed
  	if (working==null) {
  		working = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
  	}
		// reset template dimensions and create new template if needed
		if (template==null || w!=wTemplate || h!=hTemplate) {			
			wTemplate = w;
			hTemplate = h;
			int len = w*h;
			template = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	    pixels = new int[len];
	    templateR = new int[len];
	    templateG = new int[len];
	    templateB = new int[len];
	    isPixelTransparent = new boolean[len];
		}
  	// set alpha of input and draw onto working
  	Graphics2D gWorking = working.createGraphics();
  	alphaInput = Math.max(0, Math.min(255, alphaInput));
		if (alphaInput>0) { // overlay only if not transparent
			gWorking.setComposite(getComposite(alphaInput));
		  input.getRaster().getDataElements(0, 0, w, h, pixels);
	    gWorking.drawImage(input, 0, 0, null);
		}
  	// set alpha of original and draw onto working
  	alphaOriginal = Math.max(0, Math.min(255, alphaOriginal));
		if (alphaOriginal>0) { // overlay only if not transparent
			gWorking.setComposite(getComposite(alphaOriginal));
	    original.getRaster().getDataElements(0, 0, w, h, pixels);
	    gWorking.drawImage(original, 0, 0, null);
		}
		// read pixels from working raster
		working.getRaster().getDataElements(0, 0, wTemplate, hTemplate, pixels);
  	if (mask != null) {
	    // set pixels outside mask to transparent
    	for (int i = 0; i < pixels.length; i++) {
      	boolean inside = true;
    		// pixel is inside only if all corners are inside
    		int x = i%wTemplate, y = i/wTemplate;
    		for (int j = 0; j < 2; j++) {
    			for (int k = 0; k < 2; k++) {
    	    	p.setLocation(x+j, y+k);
    	    	inside = inside && mask.contains(p);
    			}
    		}
      	if (!inside)
      		pixels[i] = pixels[i] & (0 << 24); // set alpha to zero (transparent)
    	}
    }
  	// write pixels to template raster
  	template.getRaster().setDataElements(0, 0, wTemplate, hTemplate, pixels);
  	// trim transparent edges from template
  	int trimRight=0, trimBottom=0;
  	trimLeft = trimTop = 0;
  	// left edge
  	boolean transparentEdge = true;
  	while (transparentEdge && trimLeft < wTemplate) {
  		for (int line = 0; line < hTemplate; line++) {
  			int i = line*wTemplate+trimLeft;
  			transparentEdge = transparentEdge && getAlpha(pixels[i])==0;	
  		}
  		if (transparentEdge) trimLeft++;
  	}
  	// right edge
  	transparentEdge = true;
  	while (transparentEdge && (trimLeft+trimRight) < wTemplate) {
  		for (int line = 0; line < hTemplate; line++) {
  			int i = (line+1)*wTemplate-1-trimRight;
  			transparentEdge = transparentEdge && getAlpha(pixels[i])==0;	
  		}
  		if (transparentEdge) trimRight++;
  	}
  	// top edge
  	transparentEdge = true;
  	while (transparentEdge && trimTop < hTemplate) {
  		for (int col = 0; col < wTemplate; col++) {
  			int i = trimTop*wTemplate+col;
  			transparentEdge = transparentEdge && getAlpha(pixels[i])==0;	
  		}
  		if (transparentEdge) trimTop++;
  	}
  	// bottom edge
  	transparentEdge = true;
  	while (transparentEdge && (trimTop+trimBottom) < hTemplate) {
  		for (int col = 0; col < wTemplate; col++) {
  			int i = (hTemplate-1-trimBottom)*wTemplate+col;
  			transparentEdge = transparentEdge && getAlpha(pixels[i])==0;	
  		}
  		if (transparentEdge) trimBottom++;
  	}
  	// reduce size of template if needed
  	if (trimLeft+trimRight+trimTop+trimBottom > 0) {
  		wTemplate -= (trimLeft+trimRight);
  		hTemplate -= (trimTop+trimBottom);
	    pixels = new int[wTemplate * hTemplate];
	    templateR = new int[wTemplate * hTemplate];
	    templateG = new int[wTemplate * hTemplate];
	    templateB = new int[wTemplate * hTemplate];
	    isPixelTransparent = new boolean[wTemplate * hTemplate];
	    BufferedImage bi = new BufferedImage(wTemplate, hTemplate, BufferedImage.TYPE_INT_ARGB);
	    bi.createGraphics().drawImage(template, -trimLeft, -trimTop, null);
  		template = bi;
	    template.getRaster().getDataElements(0, 0, wTemplate, hTemplate, pixels);
  	}
  	// set up rgb and transparency arrays for faster matching
    for (int i = 0; i < pixels.length; i++) {
      int val = pixels[i];
      templateR[i] = getRed(val);		// red
      templateG[i] = getGreen(val);		// green
      templateB[i] = getBlue(val);		// blue
      isPixelTransparent[i] = getAlpha(val)==0;		// alpha
    }
  }
  	
  /**
   * Gets the template location at which the best match occurs in a rectangle. 
   * May return null.
   *
   * @param target the image to search
   * @param searchRect the rectangle to search within the target image
   * @return the optimized template location at which the best match, if any, is found
   */
  public TPoint getMatchLocation(BufferedImage target, Rectangle searchRect) {
    wTarget = target.getWidth();
    hTarget = target.getHeight();
    // determine insets needed to accommodate template
    int left = wTemplate/2, right = left;
    if (wTemplate%2>0) right++;
    int top = hTemplate/2, bottom = top;
    if (hTemplate%2>0) bottom++;
    // trim search rectangle if necessary
  	searchRect.x = Math.max(left, Math.min(wTarget-right, searchRect.x));
  	searchRect.y = Math.max(top, Math.min(hTarget-bottom, searchRect.y));
  	searchRect.width = Math.min(wTarget-searchRect.x-right, searchRect.width);
  	searchRect.height = Math.min(hTarget-searchRect.y-bottom, searchRect.height);
  	if (searchRect.width <= 0 || searchRect.height <= 0) {
  		peakHeight = Double.NaN;
  		peakWidth = Double.NaN;
  		return null;
  	}
  	// set up test pixels to search (rectangle plus template)
  	int xMin = Math.max(0, searchRect.x-left);
  	int xMax = Math.min(wTarget, searchRect.x+searchRect.width+right);
  	int yMin = Math.max(0, searchRect.y-top);
  	int yMax = Math.min(hTarget, searchRect.y+searchRect.height+bottom);
  	wTest = xMax-xMin;
  	hTest = yMax-yMin;
    if (target.getType() != BufferedImage.TYPE_INT_RGB) {
    	BufferedImage image = new BufferedImage(wTarget, hTarget, BufferedImage.TYPE_INT_RGB);
      image.createGraphics().drawImage(target, 0, 0, null);
      target = image;
    }
    targetPixels = new int[wTest * hTest];
    target.getRaster().getDataElements(xMin, yMin, wTest, hTest, targetPixels);
    // find the rectangle point with the minimum difference
    double matchDiff = largeNumber; // larger than typical differences
    int xMatch=0, yMatch=0;
    double avgDiff = 0;
  	for (int x = 0; x <= searchRect.width; x++) {
  		for (int y = 0; y <= searchRect.height; y++) {
    		double diff = getDifferenceAtTestPoint(x, y);
    		avgDiff += diff;
    		if (diff < matchDiff) {
    			matchDiff = diff;
    			xMatch = x;
    			yMatch = y;
    		}
    	}
    }
  	avgDiff /= (searchRect.width*searchRect.height);
		peakHeight = avgDiff/matchDiff-1;
		peakWidth = Double.NaN;
		double dx = 0, dy = 0;
		// if match is not exact, fit a Gaussian and find peak
		if (!Double.isInfinite(peakHeight)) {
			// fill data arrays
	  	xValues[1] = yValues[1] = peakHeight;
  		for (int i = -1; i < 2; i++) {
  			if (i == 0) continue;
				double diff = getDifferenceAtTestPoint(xMatch+i, yMatch); 
  			xValues[i+1] = avgDiff/diff-1; 
				diff = getDifferenceAtTestPoint(xMatch, yMatch+i); 
  			yValues[i+1] = avgDiff/diff-1; 
  		}
  		// estimate peakHeight = peak of gaussian
  		// estimate offset dx of gaussian
  		double pull = 1/(xValues[1]-xValues[0]);
  		double push = 1/(xValues[1]-xValues[2]);
  		if (Double.isNaN(pull)) pull=LARGE_NUMBER;
  		if (Double.isNaN(push)) push=LARGE_NUMBER;
  		dx = 0.6*(push-pull)/(push+pull);
  		// estimate width wx of gaussian
			double ratio = dx>0? peakHeight/xValues[0]: peakHeight/xValues[2];
			double wx = dx>0? dx+1: dx-1;
			wx = wx*wx/Math.log(ratio);
  		// estimate offset dy of gaussian
  		pull = 1/(yValues[1]-yValues[0]);
  		push = 1/(yValues[1]-yValues[2]);
  		if (Double.isNaN(pull)) pull=LARGE_NUMBER;
  		if (Double.isNaN(push)) push=LARGE_NUMBER;
  		dy = 0.6*(push-pull)/(push+pull);
  		// estimate width wy of gaussian
			ratio = dy>0? peakHeight/yValues[0]: peakHeight/yValues[2];
			double wy = dy>0? dy+1: dy-1;
			wy = wy*wy/Math.log(ratio);

  		// set x parameters and fit to x data
  		dataset.clear();
  		dataset.append(pixelOffsets, xValues);
			double rmsDev = 1;			
  		for (int k = 0; k < 3; k++) {
  			double c = k==0? wx: k==1? wx/3: wx*3;
	  		f.setParameterValue(0, peakHeight);
	  		f.setParameterValue(1, dx);
	  		f.setParameterValue(2, c);
    		rmsDev = fitter.fit(f);
	      if (rmsDev < 0.01) { // fitter succeeded (3-point fit should be exact)	      	
	      	dx = f.getParameterValue(1);
	    		peakWidth = f.getParameterValue(2);
	    		break;
	      }
  		}      
  		if (!Double.isNaN(peakWidth)) {
	      // set y parameters and fit to y data
	  		dataset.clear();
	  		dataset.append(pixelOffsets, yValues);
	  		for (int k = 0; k < 3; k++) {
	  			double c = k==0? wy: k==1? wy/3: wy*3;
		  		f.setParameterValue(0, peakHeight);
		  		f.setParameterValue(1, dx);
		  		f.setParameterValue(2, c);
	    		rmsDev = fitter.fit(f);
		      if (rmsDev < 0.01) { // fitter succeeded (3-point fit should be exact)	      	
		      	dy = f.getParameterValue(1);
		      	peakWidth = (peakWidth+f.getParameterValue(2))/2;
		    		break;
		      }
	  		}
    		if (rmsDev > 0.01)
    			peakWidth = Double.NaN;
  		}
		}
		double xImage = xMatch+searchRect.x-left-trimLeft+dx;
		double yImage = yMatch+searchRect.y-top-trimTop+dy;
		return new TPoint(xImage, yImage);
  }
  
  /**
   * Gets the template location at which the best match occurs in a
   * rectangle and along a line. May return null.
   *
   * @param target the image to search
   * @param searchRect the rectangle to search within the target image
   * @param x0 the x-component of a point on the line
   * @param y0 the y-component of a point on the line
   * @param slope the slope of the line
   * @param spread the spread of the line (line width = 1+2*spread)
   * @return the optimized template location of the best match, if any
   */
  public TPoint getMatchLocation(BufferedImage target, Rectangle searchRect,
  		double x0, double y0, double theta, int spread) {  	
    wTarget = target.getWidth();
    hTarget = target.getHeight();
    // determine insets needed to accommodate template
    int left = wTemplate/2, right = left;
    if (wTemplate%2>0) right++;
    int top = hTemplate/2, bottom = top;
    if (hTemplate%2>0) bottom++;
    
    // trim search rectangle if necessary
  	searchRect.x = Math.max(left, Math.min(wTarget-right, searchRect.x));
  	searchRect.y = Math.max(top, Math.min(hTarget-bottom, searchRect.y));
  	searchRect.width = Math.min(wTarget-searchRect.x-right, searchRect.width);
  	searchRect.height = Math.min(hTarget-searchRect.y-bottom, searchRect.height);
  	if (searchRect.width <= 0 || searchRect.height <= 0) {
  		peakHeight = Double.NaN;
  		peakWidth = Double.NaN;
  		return null;
  	}
  	// set up test pixels to search (rectangle plus template)
  	int xMin = Math.max(0, searchRect.x-left);
  	int xMax = Math.min(wTarget, searchRect.x+searchRect.width+right);
  	int yMin = Math.max(0, searchRect.y-top);
  	int yMax = Math.min(hTarget, searchRect.y+searchRect.height+bottom);
  	wTest = xMax-xMin;
  	hTest = yMax-yMin;
    if (target.getType() != BufferedImage.TYPE_INT_RGB) {
    	BufferedImage image = new BufferedImage(wTarget, hTarget, BufferedImage.TYPE_INT_RGB);
      image.createGraphics().drawImage(target, 0, 0, null);
      target = image;
    }
    targetPixels = new int[wTest * hTest];
    target.getRaster().getDataElements(xMin, yMin, wTest, hTest, targetPixels);
    // get the points to search along the line
    ArrayList<Point2D> searchPts = getSearchPoints(searchRect, x0, y0, theta);
    if (searchPts==null) {
  		peakHeight = Double.NaN;
  		peakWidth = Double.NaN;
  		return null;
    }
    // collect differences in a map as they are measured
    HashMap<Point2D, Double> diffs = new HashMap<Point2D, Double>();
    // find the point with the minimum difference from template
    double matchDiff = largeNumber; // larger than typical differences
    int xMatch=0, yMatch=0;
    double avgDiff = 0;
    Point2D matchPt = null;
  	for (Point2D pt: searchPts) {
  		int x = (int)pt.getX();
  		int y = (int)pt.getY();
  		double diff = getDifferenceAtTestPoint(x, y);
  		diffs.put(pt, diff);
  		avgDiff += diff;
  		if (diff < matchDiff) {
  			matchDiff = diff;
  			xMatch = x;
  			yMatch = y;
  			matchPt = pt;
  		}
  	}
  	avgDiff /= searchPts.size();
		peakHeight = avgDiff/matchDiff-1;
		peakWidth = Double.NaN;
		double dl = 0;
		int matchIndex = searchPts.indexOf(matchPt);
		
		// if match is not exact, fit a Gaussian and find peak
		if (!Double.isInfinite(peakHeight) && matchIndex>0 && matchIndex<searchPts.size()-1) {
			// fill data arrays
			Point2D pt = searchPts.get(matchIndex-1);
			double diff = diffs.get(pt);
	  	xValues[0] = -pt.distance(matchPt);
	  	yValues[0] = avgDiff/diff-1;
	  	xValues[1] = 0;
	  	yValues[1] = peakHeight;
			pt = searchPts.get(matchIndex+1);
			diff = diffs.get(pt);
	  	xValues[2] = pt.distance(matchPt);
	  	yValues[2] = avgDiff/diff-1;
	  	
  		// determine approximate offset (dl) and width (w) values 
  		double pull = -xValues[0]/(yValues[1]-yValues[0]);
  		double push = xValues[2]/(yValues[1]-yValues[2]);
  		if (Double.isNaN(pull)) pull=LARGE_NUMBER;
  		if (Double.isNaN(push)) push=LARGE_NUMBER;
  		dl = 0.3*(xValues[2]-xValues[0])*(push-pull)/(push+pull);
			double ratio = dl>0? peakHeight/yValues[0]: peakHeight/yValues[2];
			double w = dl>0? dl-xValues[0]: dl-xValues[2];
			w = w*w/Math.log(ratio);

  		// set parameters and fit to x data
  		dataset.clear();
  		dataset.append(xValues, yValues);
			double rmsDev = 1;
  		for (int k = 0; k < 3; k++) {
  			double c = k==0? w: k==1? w/3: w*3;
	  		f.setParameterValue(0, peakHeight);
	  		f.setParameterValue(1, dl);
	  		f.setParameterValue(2, c);
    		rmsDev = fitter.fit(f);
	      if (rmsDev < 0.01) { // fitter succeeded (3-point fit should be exact)	      	
	      	dl = f.getParameterValue(1);
	    		peakWidth = f.getParameterValue(2);
	    		break;
	      }
  		}
		}
		double dx = dl*Math.cos(theta);
		double dy = dl*Math.sin(theta);
		double xImage = xMatch+searchRect.x-left-trimLeft+dx;
		double yImage = yMatch+searchRect.y-top-trimTop+dy;
		return new TPoint(xImage, yImage);
  }
  
  /**
   * Returns the width and height of the peak for the most recent match.
   * The peak height is the ratio meanSqPixelDiff/matchSqPixelDiff.
   * The peak width is the mean of the vertical and horizontal Gaussian fit widths. 
   * This data can be used to determine whether a match is acceptable.
   * A peak height greater than 5 is a reasonable standard for acceptability.
   *  
   * Special cases:
   * 1. If the match is perfect, then the height is infinite and the width NaN.
   * 2. If the searchRect fell outside the target image, then no match was
   *    possible and the height is NaN. 
   * 3. If the Gaussian fit optimization was not successful (either horizontally 
   *    or vertically) then the width is NaN.
   *
   * @return double[2] {mean Gaussian width, height}
   */
  public double[] getMatchWidthAndHeight() {
  	return new double[] {peakWidth, peakHeight};
  }
  
  /**
   * Method to get the color value
   * 
   * @param value the color value
   * @return 0-255 red component
   */
  public static int getValue(int a, int r, int g, int b) {
  	 int value = (a << 24) + (r << 16) + (g << 8) + b;
     return value;
  }
  
  /**
   * Method to get the alpha component from a color value
   * 
   * @param value the color value
   * @return 0-255 alpha component
   */
  public static int getAlpha(int value) {
  	int alpha = (value >> 24) & 0xff;   
    return alpha;
  }
  
  /**
   * Method to get the red component from a color value
   * 
   * @param value the color value
   * @return 0-255 red component
   */
  public static int getRed(int value) {
    int red = (value >> 16) & 0xff;
    return red;
  }
  
  /**
   * Method to get the green component from a color value
   * 
   * @param value the color value
   * @return 0-255 green component
   */
  public static int getGreen(int value) {
    int green = (value >> 8) & 0xff;
    return green;
  }
  
  /**
   * Method to get the blue component from a color value
   * 
   * @param value the color value
   * @return 0-255 blue component
   */
  public static int getBlue(int value) {
    int blue = value & 0xff;
    return blue;
  }
  
//_____________________________ private methods _______________________

  /**
   * Gets a list of Point2D objects that lie within pixels in a rectangle
   * and along a line.
   * 
   * @param searchRect the rectangle
   * @param x0 the x-component of a point on the line
   * @param y0 the y-component of a point on the line
   * @param slope the slope of the line
   * @return a list of Point2D
   */
  public ArrayList<Point2D> getSearchPoints(Rectangle searchRect,
  		double x0, double y0, double theta) {
  	double slope = -Math.tan(theta);
  	// create line to search along
  	Line2D line = new Line2D.Double();
  	if (slope>LARGE_NUMBER) {
  		line.setLine(x0, y0, x0, y0+1);
  	}
  	else if (slope<1/LARGE_NUMBER) {
  		line.setLine(x0, y0, x0+1, y0);
  	}
  	else {
    	line.setLine(x0, y0, x0+1, y0+slope);
  	}
  	// create intersection points (to set line ends)
  	Point2D p1 = new Point2D.Double();
  	Point2D p2 = new Point2D.Double(Double.NaN, Double.NaN);
  	Point2D p = p1;
  	boolean foundBoth = false;
  	double d = searchRect.x;
  	Object[] data = getDistanceAndPointAtX(line, d);
  	if (data!=null) {
	  	p.setLocation((Point2D)data[1]);
			if (p.getY()>=searchRect.y && p.getY()<=searchRect.y+searchRect.height) {
				// line end is left edge
				p = p2;
			}
  	}
		d += searchRect.width;
  	data = getDistanceAndPointAtX(line, d);
  	if (data!=null) {
	  	p.setLocation((Point2D)data[1]);
			if (p.getY()>=searchRect.y && p.getY()<=searchRect.y+searchRect.height) {
				// line end is right edge
				if (p==p1) p = p2;
				else foundBoth = true;
			}
  	}
		if (!foundBoth) {
	  	d = searchRect.y;
	  	data = getDistanceAndPointAtY(line, d);
	  	if (data!=null) {
		  	p.setLocation((Point2D)data[1]);
				if (p.getX()>=searchRect.x && p.getX()<=searchRect.x+searchRect.width) {
					// line end is top edge
					if (p==p1) p = p2;
					else if (!p1.equals(p2)) foundBoth = true;
				}
	  	}
		}
		if (!foundBoth) {
	  	d += searchRect.height;
	  	data = getDistanceAndPointAtY(line, d);
	  	if (data!=null) {
		  	p.setLocation((Point2D)data[1]);
				if (p.getX()>=searchRect.x && p.getX()<=searchRect.x+searchRect.width) {
					// line end is bottom edge
					if (p==p2 && !p1.equals(p2)) foundBoth = true;
				}
	  	}
		}
		// if both line ends have been found, use line to find pixels to search
  	if (foundBoth) {
  		// set line ends to intersections
  		line.setLine(p1, p2);
	  	if (p1.getX()>p2.getX()) {
	  		line.setLine(p2, p1);
	  	}
	  	// find pixel intersections that fall along the line
	  	int xMin = (int)Math.ceil(Math.min(p1.getX(), p2.getX()));
	  	int xMax = (int)Math.floor(Math.max(p1.getX(), p2.getX()));
	  	int yMin = (int)Math.ceil(Math.min(p1.getY(), p2.getY()));
	  	int yMax = (int)Math.floor(Math.max(p1.getY(), p2.getY()));
	  	// collect intersections in TreeMap sorted by position along line
	  	TreeMap<Double, Point2D> intersections = new TreeMap<Double, Point2D>();
	  	for (int x = xMin; x <= xMax; x++) {
				Object[] next = getDistanceAndPointAtX(line, x);
				intersections.put((Double)next[0], (Point2D)next[1]);
	  	}
	  	for (int y = yMin; y <= yMax; y++) {
				Object[] next = getDistanceAndPointAtY(line, y);
				intersections.put((Double)next[0], (Point2D)next[1]);
	  	}
	  	p = null;
	  	// create array of search points that are midway between intersections
	  	ArrayList<Point2D> searchPts = new ArrayList<Point2D>();
	  	for (Double key: intersections.keySet()) {
	  		Point2D next = intersections.get(key);
	  		if (p!=null) {
	  			double x = (p.getX()+next.getX())/2 - searchRect.x;
	  			double y = (p.getY()+next.getY())/2 - searchRect.y;
		  		p.setLocation(x, y);
		  		searchPts.add(p);
	  		}
	  		p = next;
	  	}
	  	return searchPts;
  	}
  	return null;
  }

  /**
   * Gets the distance and point along a Line2D at a specified x.
   * If the Line2D is vertical this returns null.
   * 
   * Based on a simplification of algorithm described by Paul Burke
   * at http://local.wasp.uwa.edu.au/~pbourke/geometry/lineline2d/ (April 1986)
   * 
   * @param line the line
   * @param x the value of x
   * @return Object[] {fractional distance from line end, Point2D}
   */
  private Object[] getDistanceAndPointAtX(Line2D line, double x) {
  	double dx = line.getX2()-line.getX1();
  	// if line is vertical, return null
  	if (dx==0) return null;
  	// parametric eqn of line: P = P1 + u(P2 - P1)
  	double u = (x-line.getX1())/dx;
		double y = line.getY1() + u*(line.getY2()-line.getY1());
		return new Object[] {u, new Point2D.Double(x, y)};
  }
  
  /**
   * Gets the distance and point along a Line2D at a specified y.
   * If the Line2D is horizontal this returns null.
   * 
   * Based on a simplification of algorithm described by Paul Burke
   * at http://local.wasp.uwa.edu.au/~pbourke/geometry/lineline2d/ (April 1986)
   * 
   * @param line the line
   * @param y the value of y
   * @return Object[] {fractional distance from line end, Point2D}
   */
  private Object[] getDistanceAndPointAtY(Line2D line, double y) {
  	double dy = line.getY2()-line.getY1();
  	// if line is horizontal, return null
  	if (dy==0) return null;
  	// parametric eqn of line: P = P1 + u(P2 - P1)
  	double u = (y-line.getY1())/dy;
		double x = line.getX1() + u*(line.getX2()-line.getX1());
		return new Object[] {u, new Point2D.Double(x, y)};
  }

  /**
   * Gets the total difference between the template and test pixels 
   * at a specified test point. The test point is the point on the test
   * image where the top left corner of the template is located.
   * 
   * @param x the test point x-component
   * @param y the test point y-component
   */
  private double getDifferenceAtTestPoint(int x, int y) {
  	// for each pixel in template, get difference from corresponding test pixel
  	// return sum of these differences
    double diff = 0;
    for (int i = 0; i < wTemplate; i++) {
    	for (int j = 0; j < hTemplate; j++) {
    		int templateIndex = j*wTemplate+i;
    		int testIndex = (y+j)*wTest+x+i;
    		if (testIndex < 0 || testIndex >= targetPixels.length)
    			return Double.NaN; // may occur when doing Gaussian fit
      	if (!isPixelTransparent[templateIndex]) { // include only non-transparent pixels
      		int pixel = targetPixels[testIndex];
      		diff += getRGBDifference(pixel, templateR[templateIndex], templateG[templateIndex], templateB[templateIndex]);
      	}
    	}
    }
    return diff;
  }

  /**
   * Gets the difference between a pixel and a comparison set of rgb components.
   */
  private double getRGBDifference(int pixel, int r, int g, int b) {
    int rPix = (pixel >> 16) & 0xff;		// red
    int gPix = (pixel >>  8) & 0xff;		// green
    int bPix = (pixel      ) & 0xff;		// blue
    int dr = r-rPix;
	  int dg = g-gPix;
	  int db = b-bPix;
	  return dr*dr + dg*dg + db*db; // sum of squares of rgb differences
  }

  /**
   * Gets an AlphaComposite object with a specified alpha value.
   */
  private AlphaComposite getComposite(int alpha) {
  	float a = 1.0f*alpha/255;
    return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a);
  }

}
