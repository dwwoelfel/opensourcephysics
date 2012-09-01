/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

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
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.display.Interactive;

/**
 * This is a Filter that corrects perspective in the source image.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class PerspectiveFilter extends Filter {
	
  // static fields
  protected static FontRenderContext frc
		  = new FontRenderContext(null,   // no AffineTransform
		                          false,  // no antialiasing
		                          false); // no fractional metrics

  // instance fields
  private BufferedImage source, input, output;
  private int[] pixelsIn, pixelsOut; // pixel color values
  private int w, h;
  private Graphics2D gIn;
  private double[][] matrix = new double[3][3]; // perspective transform matrix
  private double[][] temp1 = new double[3][3]; // intermediate matrix
  private double[][] temp2 = new double[3][3]; // intermediate matrix
  private double[] xOut, yOut, xIn, yIn; // pixel positions on input and output images
  private int interpolation = 2; // neighborhood size for color interpolation
  private Quadrilateral quad;
  private QuadEditor inputEditor, outputEditor;
  
  // inspector fields
  private Inspector inspector;


  /**
   * Constructs a RotateFilter object.
   */
  public PerspectiveFilter() {
    quad = new Quadrilateral();
    refresh();
    hasInspector = true;
  }
  
  /**
   * Applies the filter to a source image and returns the result.
   *
   * @param sourceImage the source image
   * @return the filtered image
   */
  public BufferedImage getFilteredImage(BufferedImage sourceImage) {
    if(!isEnabled()) {
      return sourceImage;
    }
    if(sourceImage!=source) {
      initialize(sourceImage);
    }
    if(sourceImage!=input) {
      gIn.drawImage(source, 0, 0, null);
    }
    setOutputToTransformed(input);
    return output;
  }

  /**
   * Gets whether this filter is enabled.
   *
   * @return <code>true</code> if this is enabled.
   */
  @Override
  public boolean isEnabled() {
    boolean disabled = !super.isEnabled();
    boolean editingInput = inspector!=null && inspector.isVisible()
    		&& inspector.tabbedPane.getSelectedComponent()==inputEditor;
    if (disabled || editingInput)
    	return false;
    return true;
  }

  /**
   * Gets the inspector for this filter.
   *
   * @return the inspector
   */
  public JDialog getInspector() {
    if(inspector==null) {
      inspector = new Inspector();
    }
    if(inspector.isModal()&&(vidPanel!=null)) {
      Frame f = JOptionPane.getFrameForComponent(vidPanel);
      if(frame!=f) {
        frame = f;
        if(inspector!=null) {
          inspector.setVisible(false);
        }
        inspector = new Inspector();
      }
    }
    inspector.initialize();
    return inspector;
  }

  /**
   * Refreshes this filter's GUI
   */
  public void refresh() {
    super.refresh();
    if(inspector!=null) {
      inspector.setTitle(MediaRes.getString("Filter.Perspective.Title")); //$NON-NLS-1$
      inspector.tabbedPane.setTitleAt(0, MediaRes.getString("PerspectiveFilter.Tab.Input")); //$NON-NLS-1$
      inspector.tabbedPane.setTitleAt(1, MediaRes.getString("PerspectiveFilter.Tab.Output")); //$NON-NLS-1$

      inspector.helpButton.setText(MediaRes.getString("PerspectiveFilter.Button.Help")); //$NON-NLS-1$
      inspector.colorButton.setText(MediaRes.getString("PerspectiveFilter.Button.Color")); //$NON-NLS-1$
      ableButton.setText(super.isEnabled() ? MediaRes.getString("Filter.Button.Disable") : //$NON-NLS-1$
        MediaRes.getString("Filter.Button.Enable"));                                 //$NON-NLS-1$
      inputEditor.refreshGUI();
      outputEditor.refreshGUI();
    }
  }

  //_____________________________ private methods _______________________

  /**
   * Creates the input and output images and ColorConvertOp.
   *
   * @param image a new input image
   */
  private void initialize(BufferedImage image) {
    source = image;
    w = source.getWidth();
    h = source.getHeight();
    output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    pixelsIn = new int[w*h];
    pixelsOut = new int[w*h];    
    xIn = new double[w*h];
    yIn = new double[w*h];
    // output positions are integer pixels
    xOut = new double[w*h];
    yOut = new double[w*h];    
    for (int i=0; i<w; i++) {
    	for (int j=0; j<h; j++) {
    		xOut[j*w+i] = i;
    		yOut[j*w+i] = j;
    	}
    }
    if(source.getType()==BufferedImage.TYPE_INT_RGB) {
      input = source;
    } else {
      input = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      gIn = input.createGraphics();
    }
    // initialize corner positions
    quad.inCorners[0].setLocation(w/4, h/4);
    quad.inCorners[1].setLocation(3*w/4, h/4);
    quad.inCorners[2].setLocation(3*w/4, 3*h/4);
    quad.inCorners[3].setLocation(w/4, 3*h/4);
    quad.outCorners[0].setLocation(w/4, h/4);
    quad.outCorners[1].setLocation(3*w/4, h/4);
    quad.outCorners[2].setLocation(3*w/4, 3*h/4);
    quad.outCorners[3].setLocation(w/4, 3*h/4);
  }

  /**
   * Sets the output image pixels to a rotated version of the input pixels.
   *
   * @param image the input image
   */
  private void setOutputToTransformed(BufferedImage image) {
    image.getRaster().getDataElements(0, 0, w, h, pixelsIn);
    // set up transform matrix based on quad input/output corners
    
    // set temp1 to transform output to square
    getQuadToSquare(temp1, 
    		quad.outCorners[0].getX(), quad.outCorners[0].getY(), 
    		quad.outCorners[1].getX(), quad.outCorners[1].getY(), 
    		quad.outCorners[2].getX(), quad.outCorners[2].getY(), 
    		quad.outCorners[3].getX(), quad.outCorners[3].getY());

    // set temp2 to transform square to input
    getSquareToQuad(temp2,
    		quad.inCorners[0].getX(), quad.inCorners[0].getY(), 
    		quad.inCorners[1].getX(), quad.inCorners[1].getY(), 
    		quad.inCorners[2].getX(), quad.inCorners[2].getY(), 
    		quad.inCorners[3].getX(), quad.inCorners[3].getY());
    // concatenate temp2 to temp1 to obtain transform matrix output->input
    concatenate(temp1, temp2);
    
    // transform the output (pixel) positions to input positions
    transform(xOut, yOut, xIn, yIn);
    
    // find output pixel values by interpolating input pixels
    for (int i=0; i<pixelsOut.length; i++) {
    	pixelsOut[i] = getColor(xIn[i], yIn[i], w, h, pixelsIn);
    }
  	output.getRaster().setDataElements(0, 0, w, h, pixelsOut);
  }
  
  /**
   * Transforms arrays of position coordinates using the current transform matrix.
   * 
   * @param xSource array of source x-coordinates
   * @param ySource array of source y-coordinates
   * @param xTrans array of transformed x-coordinates
   * @param yTrans array of transformed y-coordinates
   */
  private void transform(double[] xSource, double[] ySource, 
  		double[] xTrans, double[] yTrans) {
  	int n = xSource.length;
  	for (int i=0; i<n; i++) {
	    double w = matrix[2][0]*xSource[i] + matrix[2][1]*ySource[i] + matrix[2][2];
	    if (w==0) {
	    	xTrans[i] = xSource[i];
	    	yTrans[i] = ySource[i];
	    }
	    else {
	    	xTrans[i] = (matrix[0][0]*xSource[i] + matrix[0][1]*ySource[i] + matrix[0][2])/w;
	    	yTrans[i] = (matrix[1][0]*xSource[i] + matrix[1][1]*ySource[i] + matrix[1][2])/w;
	    }
  		
  	}
  }
  
  /**
   * Get the interpolated color at a non-integer position (between pixels points).
   * 
   * @param x the x-coordinate of the position
   * @param y the y-coordinate of the position
   * @param w the width of the image
   * @param h the height of the image
   * @param pixelValues the color values of the pixels in the image
   */
  private int getColor(double x, double y, int w, int h, int[] pixelValues) {
  	// get central pixel position
  	int col = (int)Math.floor(x);
  	int row = (int)Math.floor(y);
  	if (col<0 || col>=w || row<0 || row>=h) {
  		return 0;  // black if not in image
  	}
  	if (col+1==w || row+1==h) {
  		interpolation = 1;
  	}
  	if (interpolation<=1) {
  		return pixelValues[row*w+col];
  	}
  	if (interpolation>=2) {
    	// get 2x2 neighborhood pixel values
  		int p00 = pixelValues[row*w+col];
  		int p01 = pixelValues[row*w+col+1];
  		int p10 = pixelValues[(row+1)*w+col];
  		int p11 = pixelValues[(row+1)*w+col+1];
  		double u = x%col;
  		double v = y%row;
  		double outValue = (1-v)*((1-u)*p00 + u*p10) + v*((1-u)*p01 + u*p11);
  		return (int)outValue;
  	}
  	return 0;
  }

  /**
   * Creates a transform matrix to map a unit square onto a quadrilateral.
   *
   * (0, 0) -> (x0, y0)
   * (1, 0) -> (x1, y1)
   * (1, 1) -> (x2, y2)
   * (0, 1) -> (x3, y3)
   */
  private void getSquareToQuad(double[][] matrix, double x0, double y0, double x1, double y1,
      double x2, double y2, double x3, double y3) {
  	
		double dx3 = x0 - x1 + x2 - x3;
		double dy3 = y0 - y1 + y2 - y3;
		
		matrix[2][2] = 1.0F;			
		if ((dx3 == 0.0F) && (dy3 == 0.0F)) {
			matrix[0][0] = x1 - x0;
			matrix[0][1] = x2 - x1;
			matrix[0][2] = x0;
			matrix[1][0] = y1 - y0;
			matrix[1][1] = y2 - y1;
			matrix[1][2] = y0;
			matrix[2][0] = 0.0F;
			matrix[2][1] = 0.0F;
		} 
		else {
			double dx1 = x1 - x2;
			double dy1 = y1 - y2;
			double dx2 = x3 - x2;
			double dy2 = y3 - y2;			
			double invdet = 1.0F/(dx1*dy2 - dx2*dy1);
			
			matrix[2][0] = (dx3*dy2 - dx2*dy3)*invdet;
			matrix[2][1] = (dx1*dy3 - dx3*dy1)*invdet;
			matrix[0][0] = x1 - x0 + matrix[2][0]*x1;
			matrix[0][1] = x3 - x0 + matrix[2][1]*x3;
			matrix[0][2] = x0;
			matrix[1][0] = y1 - y0 + matrix[2][0]*y1;
			matrix[1][1] = y3 - y0 + matrix[2][1]*y3;
			matrix[1][2] = y0;
		}
	}
  
  /**
   * Creates a transform matrix to map a quadrilateral onto a unit square.
   *
   * (x0, y0) -> (0, 0)
   * (x1, y1) -> (1, 0)
   * (x2, y2) -> (1, 1)
   * (x3, y3) -> (0, 1)
   */
  private void getQuadToSquare(double[][] matrix, double x0, double y0, double x1, double y1,
      double x2, double y2, double x3, double y3) {
  	// get square to quad and convert to adjoint
    getSquareToQuad(matrix, x0, y0, x1, y1, x2, y2, x3, y3);
    // get sub-determinants
	  double m00 = matrix[1][1]*matrix[2][2] - matrix[1][2]*matrix[2][1];
	  double m01 = matrix[1][2]*matrix[2][0] - matrix[1][0]*matrix[2][2];
	  double m02 = matrix[1][0]*matrix[2][1] - matrix[1][1]*matrix[2][0];
	  double m10 = matrix[0][2]*matrix[2][1] - matrix[0][1]*matrix[2][2];
	  double m11 = matrix[0][0]*matrix[2][2] - matrix[0][2]*matrix[2][0];
	  double m12 = matrix[0][1]*matrix[2][0] - matrix[0][0]*matrix[2][1];
	  double m20 = matrix[0][1]*matrix[1][2] - matrix[0][2]*matrix[1][1];
	  double m21 = matrix[0][2]*matrix[1][0] - matrix[0][0]*matrix[1][2];
	  double m22 = matrix[0][0]*matrix[1][1] - matrix[0][1]*matrix[1][0];
	  // transpose
	  matrix[0][0] = m00;
	  matrix[0][1] = m10;
	  matrix[0][2] = m20;
	  matrix[1][0] = m01;
	  matrix[1][1] = m11;
	  matrix[1][2] = m21;
	  matrix[2][0] = m02;
	  matrix[2][1] = m12;
	  matrix[2][2] = m22;
  }

  /**
   * Concatenates two transform matrices and puts the result into the transform matrix.
   */
  private void concatenate(double[][] m1, double[][] m2) {
  	matrix[0][0] = m1[0][0]*m2[0][0] + m1[1][0]*m2[0][1] + m1[2][0]*m2[0][2]; 
  	matrix[1][0] = m1[0][0]*m2[1][0] + m1[1][0]*m2[1][1] + m1[2][0]*m2[1][2]; 
  	matrix[2][0] = m1[0][0]*m2[2][0] + m1[1][0]*m2[2][1] + m1[2][0]*m2[2][2]; 
  	matrix[0][1] = m1[0][1]*m2[0][0] + m1[1][1]*m2[0][1] + m1[2][1]*m2[0][2]; 
  	matrix[1][1] = m1[0][1]*m2[1][0] + m1[1][1]*m2[1][1] + m1[2][1]*m2[1][2]; 
  	matrix[2][1] = m1[0][1]*m2[2][0] + m1[1][1]*m2[2][1] + m1[2][1]*m2[2][2]; 
  	matrix[0][2] = m1[0][2]*m2[0][0] + m1[1][2]*m2[0][1] + m1[2][2]*m2[0][2]; 
  	matrix[1][2] = m1[0][2]*m2[1][0] + m1[1][2]*m2[1][1] + m1[2][2]*m2[1][2]; 
  	matrix[2][2] = m1[0][2]*m2[2][0] + m1[1][2]*m2[2][1] + m1[2][2]*m2[2][2]; 
  }

  /**
   * Inner Inspector class to control filter parameters
   */
  private class Inspector extends JDialog {
  	
    JButton helpButton, colorButton;
    JTabbedPane tabbedPane;
  	JPanel contentPane;
  	  	
    /**
     * Constructs the Inspector.
     */
    public Inspector() {
      super(frame, !(frame instanceof org.opensourcephysics.display.OSPFrame));
      setResizable(false);
      createGUI();
      refresh();
      pack();
      // center on screen
      Rectangle rect = getBounds();
      Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
      int x = (dim.width-rect.width)/2;
      int y = (dim.height-rect.height)/2;
      setLocation(x, y);
    }
    
    /**
     * Creates the visible components.
     */
    void createGUI() {
    	tabbedPane = new JTabbedPane();
    	tabbedPane.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
        	refresh();
          PerspectiveFilter.this.support.firePropertyChange("image", null, null); //$NON-NLS-1$
        }        	
      });
    	inputEditor = new QuadEditor(true);
    	outputEditor = new QuadEditor(false);
    	outputEditor.selectedShapeIndex = 1;
    	tabbedPane.addTab("", inputEditor); //$NON-NLS-1$
    	tabbedPane.addTab("", outputEditor); //$NON-NLS-1$
    	tabbedPane.setSelectedComponent(inputEditor);
    	helpButton = new JButton();
    	helpButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
        	String s = MediaRes.getString("PerspectiveFilter.Help.Message1") //$NON-NLS-1$
    			+"\n"+MediaRes.getString("PerspectiveFilter.Help.Message2") //$NON-NLS-1$ //$NON-NLS-2$
    			+"\n"+MediaRes.getString("PerspectiveFilter.Help.Message3") //$NON-NLS-1$ //$NON-NLS-2$
    			+"\n"+MediaRes.getString("PerspectiveFilter.Help.Message4") //$NON-NLS-1$ //$NON-NLS-2$
    			+"\n\n"+MediaRes.getString("PerspectiveFilter.Help.Message5") //$NON-NLS-1$ //$NON-NLS-2$
    			+"\n  "+MediaRes.getString("PerspectiveFilter.Help.Message6") //$NON-NLS-1$ //$NON-NLS-2$
    			+"\n  "+MediaRes.getString("PerspectiveFilter.Help.Message7") //$NON-NLS-1$ //$NON-NLS-2$
    			+"\n  "+MediaRes.getString("PerspectiveFilter.Help.Message8") //$NON-NLS-1$ //$NON-NLS-2$
    			+"\n      "+MediaRes.getString("PerspectiveFilter.Help.Message9"); //$NON-NLS-1$ //$NON-NLS-2$
        	JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(vidPanel), s,
		    			MediaRes.getString("PerspectiveFilter.Help.Title"),  //$NON-NLS-1$
		    			JOptionPane.INFORMATION_MESSAGE);
		    }
      });
    	colorButton = new JButton();
    	colorButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // show color chooser dialog with color of this track
          Color newColor = JColorChooser.showDialog(null, 
          		MediaRes.getString("PerspectiveFilter.Dialog.Color.Title"),  //$NON-NLS-1$
          		quad.color);
          if (newColor != null) {
            quad.color = newColor;
            vidPanel.repaint();
          }
        }
      });
    	ableButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
        	boolean enable = !PerspectiveFilter.super.isEnabled();
        	colorButton.setEnabled(enable);
        	tabbedPane.setEnabled(enable); 
        	inputEditor.setEnabled(enable);
        	outputEditor.setEnabled(enable);
        }
      });

      // add components to content pane
      contentPane = new JPanel(new BorderLayout());
      setContentPane(contentPane);
      JPanel buttonbar = new JPanel(new FlowLayout());
      buttonbar.add(helpButton);
      buttonbar.add(colorButton);
      buttonbar.add(ableButton);
      buttonbar.add(closeButton);
      contentPane.add(buttonbar, BorderLayout.SOUTH);
			contentPane.add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Initializes this inspector
     */
    void initialize() {
      refresh();
      pack();
    }
    
    public void setVisible(boolean vis) {
    	super.setVisible(vis);
    	if (vis) {
    		vidPanel.addDrawable(quad);
      	vidPanel.addPropertyChangeListener("selectedpoint", quad); //$NON-NLS-1$
      	PerspectiveFilter.this.addPropertyChangeListener("visible", vidPanel); //$NON-NLS-1$
    	}
    	else {
    		vidPanel.removeDrawable(quad);
      	PerspectiveFilter.this.support.firePropertyChange("visible", null, null); //$NON-NLS-1$
      	PerspectiveFilter.this.removePropertyChangeListener("visible", vidPanel); //$NON-NLS-1$
      	vidPanel.removePropertyChangeListener("selectedpoint", quad); //$NON-NLS-1$
    	}
    	boolean enable = PerspectiveFilter.super.isEnabled();
    	colorButton.setEnabled(enable);
    	tabbedPane.setEnabled(enable); 
    	inputEditor.setEnabled(enable);
    	outputEditor.setEnabled(enable);
    	PerspectiveFilter.this.support.firePropertyChange("image", null, null); //$NON-NLS-1$
    }

  }
  
  private class Corner extends TPoint {
  	
    /**
     * Overrides TPoint setXY method.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public void setXY(double x, double y) {
    	super.setXY(x, y);
    	boolean output = PerspectiveFilter.this.isEnabled();
    	Corner[] corners = output? quad.outCorners: quad.inCorners;
    	QuadEditor editor = output? outputEditor: inputEditor;
    	if (editor.shapes[editor.selectedShapeIndex].equals("Rectangle")) { //$NON-NLS-1$
      	if (this==corners[0]) {
      		corners[3].x = x;
      		corners[1].y = y;
      	}
      	else if (this==corners[1]) {
      		corners[2].x = x;
      		corners[0].y = y;
      	}
      	else if (this==corners[2]) {
      		corners[1].x = x;
      		corners[3].y = y;
      	}
      	else if (this==corners[3]) {
      		corners[0].x = x;
      		corners[2].y = y;
      	}
    	}
    	editor.refreshFields();
      if (editor==outputEditor)
      	PerspectiveFilter.this.support.firePropertyChange("image", null, null); //$NON-NLS-1$
    }
  	
  }


  /**
   * Inner Quadrilateral class draws and provides interactive control 
   * of input and output quadrilateral shapes.
   */
  private class Quadrilateral implements Trackable, Interactive,
  		PropertyChangeListener {
  	
    private Corner[] inCorners = new Corner[4]; // input image corner positions (image units)
    private Corner[] outCorners = new Corner[4]; // output image corner positions
		private Point2D[] screenPts = new Point2D[4];
		private GeneralPath path = new GeneralPath();
		private Stroke stroke = new BasicStroke(2);
		private Stroke cornerStroke = new BasicStroke();
		private Shape selectionShape = new Rectangle(-4, -4, 8, 8); 
		private Shape cornerShape = new Ellipse2D.Double(-5, -5, 10, 10); 
		private Shape[] hitShapes = new Shape[4];
		private Shape[] drawShapes = new Shape[5];
		private Corner selectedCorner;
		private AffineTransform transform = new AffineTransform();
	  private TextLayout[] textLayouts = new TextLayout[4];
	  private Font font = new JTextField().getFont();
	  private Point p = new Point();
	  private Color color = Color.RED;
   
  	public Quadrilateral() {
    	for (int i = 0; i< inCorners.length; i++) {
    		inCorners[i] = new Corner();
    		outCorners[i] = new Corner();
        textLayouts[i] = new TextLayout(String.valueOf(i), font, frc);
    	}
  	}
    
    /**
     * Responds to property change events.
     *
     * @param e the property change event
     */
    public void propertyChange(PropertyChangeEvent e) {
    	Corner prev = selectedCorner;
    	selectedCorner = null;
      for (int i=0; i<4; i++) {
      	if (e.getNewValue()==inCorners[i])
      		selectedCorner=inCorners[i];
      	else if (e.getNewValue()==outCorners[i])
      		selectedCorner=outCorners[i];
      }
      if (selectedCorner!=prev)
      	vidPanel.repaint();
    }
    
    public void draw(DrawingPanel panel, Graphics g) {
    	if (!PerspectiveFilter.super.isEnabled()) return;
    	VideoPanel vidPanel = (VideoPanel)panel;
    	Corner[] corners = PerspectiveFilter.this.isEnabled()? outCorners: inCorners;
  		for (int i=0; i<4; i++) {
  			screenPts[i] = corners[i].getScreenPosition(vidPanel);
        transform.setToTranslation(screenPts[i].getX(), screenPts[i].getY());
        Shape s = corners[i]==selectedCorner? selectionShape: cornerShape;
        Stroke sk = corners[i]==selectedCorner? stroke: cornerStroke;
        hitShapes[i] = transform.createTransformedShape(s);
        drawShapes[i] = sk.createStrokedShape(hitShapes[i]);
  		}
			path.reset();
			path.moveTo((float)screenPts[0].getX(), (float)screenPts[0].getY());
			path.lineTo((float)screenPts[1].getX(), (float)screenPts[1].getY());
			path.lineTo((float)screenPts[2].getX(), (float)screenPts[2].getY());
			path.lineTo((float)screenPts[3].getX(), (float)screenPts[3].getY());
			path.closePath();
			drawShapes[4] = stroke.createStrokedShape(path);
			Graphics2D g2 = (Graphics2D)g;
      Color gcolor = g2.getColor();
      g2.setColor(color);
      Font gfont = g.getFont();
      g2.setFont(font);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                         RenderingHints.VALUE_ANTIALIAS_ON);
      for (int i=0; i< drawShapes.length; i++) {
      	g2.fill(drawShapes[i]);
      }
      for (int i=0; i<textLayouts.length; i++) {
        p.setLocation(screenPts[i].getX()-4-font.getSize(), screenPts[i].getY()-6);
      	textLayouts[i].draw(g2, p.x, p.y);
      }
      g2.setFont(gfont);
      g2.setColor(gcolor);
  	}
  	
    /**
     * Finds the interactive drawable object located at the specified
     * pixel position.
     *
     * @param panel the drawing panel
     * @param xpix the x pixel position on the panel
     * @param ypix the y pixel position on the panel
     * @return the first corner that is hit
     */
    public Interactive findInteractive(
           DrawingPanel panel, int xpix, int ypix) {
    	if (!PerspectiveFilter.super.isEnabled()) return null;
      if (!(panel instanceof VideoPanel) || !isEnabled()) return null;
      for (int i = 0; i < hitShapes.length; i++) {
        if (hitShapes[i].contains(xpix, ypix)) {
          if (!PerspectiveFilter.this.isEnabled())
          	return inCorners[i];
          return outCorners[i];
        }
      }
      return null;
    }
    
    /**
     * Return value is ignored since not measured
     *
     * @return 0
     */
    public double getX () {
      return 0;
    }

    /**
     * Return value is ignored since not measured
     *
     * @return 0
     */
    public double getY () {
      return 0;
    }

    /**
     * Empty setX method.
     *
     * @param x the x position
     */
    public void setX(double x) {}

    /**
     * Empty setY method.
     *
     * @param y the y position
     */
    public void setY(double y) {}

    /**
     * Empty setXY method.
     *
     * @param x the x position
     * @param y the y position
     */
    public void setXY(double x, double y) {}

    /**
     * Sets whether this responds to mouse hits.
     *
     * @param enabled <code>true</code> if this responds to mouse hits.
     */
    public void setEnabled(boolean enabled) {
    }

    /**
     * Gets whether this responds to mouse hits.
     *
     * @return <code>true</code> if this responds to mouse hits.
     */
    public boolean isEnabled() {
      return true;
    }

    /**
     * Reports whether information is available to set min/max values.
     *
     * @return <code>false</code> since Quadrilateral knows only image coordinates
     */
    public boolean isMeasured() {
      return false;
    }

    /**
     * Gets the minimum x needed to draw this object.
     *
     * @return 0
     */
    public double getXMin() {
      return getX();
    }

    /**
     * Gets the maximum x needed to draw this object.
     *
     * @return 0
     */
    public double getXMax() {
      return getX();
    }

    /**
     * Gets the minimum y needed to draw this object.
     *
     * @return 0
     */
    public double getYMin() {
      return getY();
    }

    /**
     * Gets the maximum y needed to draw this object.
     *
     * @return 0
     */
    public double getYMax() {
      return getY();
    }
    

  }
  
  private class QuadEditor extends JPanel {
  	  	
  	DecimalField[][] fields = new DecimalField[4][2];
  	boolean isInput;
  	String[] shapes = {"Any", "Rectangle"}; //$NON-NLS-1$ //$NON-NLS-2$
  	int selectedShapeIndex;
  	JComboBox shapeDropdown = new JComboBox();
  	JLabel shapeLabel = new JLabel();
  	boolean refreshing;
  	Box[] boxes = new Box[4];
  	TitledBorder cornersBorder = BorderFactory.createTitledBorder(""); //$NON-NLS-1$
  	
  	QuadEditor(boolean input) {
  		super(new BorderLayout());
  		isInput = input;
  		ActionListener cornerSetter = new AbstractAction() {
  			public void actionPerformed(ActionEvent e) {
  				TPoint[] corners = isInput? quad.inCorners: quad.outCorners;
  	  		for (int i=0; i< fields.length; i++) {
  	  			if (e.getSource()==fields[i][0]
  	  			    || e.getSource()==fields[i][1]) {
  	  				corners[i].setXY(fields[i][0].getValue(), fields[i][1].getValue());
  	  			}
  	  		}
  	  		refreshFields();
          PerspectiveFilter.this.support.firePropertyChange("image", null, null); //$NON-NLS-1$
  			}
  		};
  		JPanel fieldPanel = new JPanel(new GridLayout(2, 2));
  		fieldPanel.setBorder(cornersBorder);  		
  		for (int i=0; i< fields.length; i++) {
  			fields[i][0] = new DecimalField(4, 1);
  			fields[i][0].addActionListener(cornerSetter);
  			fields[i][1] = new DecimalField(4, 1);
  			fields[i][1].addActionListener(cornerSetter);
  			boxes[i] = Box.createHorizontalBox();
  			JLabel label = new JLabel(i+":  x"); //$NON-NLS-1$
  			label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));	
  			boxes[i].add(label);
  			boxes[i].add(fields[i][0]);
  			label = new JLabel("y"); //$NON-NLS-1$
  			label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));	
  			boxes[i].add(label);
  			boxes[i].add(fields[i][1]);
  			boxes[i].setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
  		}
			fieldPanel.add(boxes[0]);
			fieldPanel.add(boxes[1]);
			fieldPanel.add(boxes[3]);
			fieldPanel.add(boxes[2]);
			
			shapeLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));			
			shapeDropdown.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 8));			
  		shapeDropdown.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				if (refreshing) return;
  				selectedShapeIndex = shapeDropdown.getSelectedIndex();
  	    	if (shapes[selectedShapeIndex].equals("Rectangle")) { //$NON-NLS-1$
    				TPoint[] corners = isInput? quad.inCorners: quad.outCorners;
  	    		for (int i=0; i<4; i++) {
  	    			corners[i].setXY(corners[i].x, corners[i].y);
  	    		}
  	    		vidPanel.repaint();
  	    	}
  			}
  		});
  		// assemble editor
  		Box box = Box.createHorizontalBox();
  		box.add(shapeLabel);
  		box.add(shapeDropdown);
  		add(box, BorderLayout.NORTH);
  		add(fieldPanel, BorderLayout.CENTER);
  		refreshGUI();
  		refreshFields();
  	}
  	
  	void refreshGUI() {
  		refreshing = true;
  		shapeLabel.setText(MediaRes.getString("PerspectiveFilter.Label.Shape")); //$NON-NLS-1$
  		cornersBorder.setTitle(MediaRes.getString("PerspectiveFilter.Corners.Title")); //$NON-NLS-1$
  		shapeDropdown.removeAllItems();
  		if (this==inputEditor) {
  			shapeDropdown.addItem(MediaRes.getString("PerspectiveFilter.Shape.Any")); //$NON-NLS-1$
  		}
  		else {
	    	for (int i=0; i<shapes.length; i++) {
	      	shapeDropdown.addItem(MediaRes.getString("PerspectiveFilter.Shape."+shapes[i])); //$NON-NLS-1$
	    	}
  		}
    	shapeDropdown.setSelectedIndex(selectedShapeIndex);
    	refreshing = false;
  	}
  	
  	void refreshFields() {
    	Corner[] corners = this==outputEditor? quad.outCorners: quad.inCorners;
    	for (int i=0; i<4; i++) {
    		fields[i][0].setValue(corners[i].x);
    		fields[i][1].setValue(corners[i].y);
    	}
  	}
  	
  	public void setEnabled(boolean b) {
  		shapeDropdown.setEnabled(b);
  		shapeLabel.setEnabled(b);
  		for (int i=0; i<boxes.length; i++) {
  			for (Component c: boxes[i].getComponents()) {
  				c.setEnabled(b);
  			}
  		}
  		cornersBorder.setTitleColor(b? shapeLabel.getForeground(): new Color(153,153,153));
  	}
  }
  
  /**
   * Returns an XML.ObjectLoader to save and load filter data.
   *
   * @return the object loader
   */
  public static XML.ObjectLoader getLoader() {
    return new Loader();
  }

  /**
   * A class to save and load filter data.
   */
  static class Loader implements XML.ObjectLoader {
    /**
     * Saves data to an XMLControl.
     *
     * @param control the control to save to
     * @param obj the filter to save
     */
    public void saveObject(XMLControl control, Object obj) {
    	PerspectiveFilter filter = (PerspectiveFilter) obj;
      if((filter.frame!=null)&&(filter.inspector!=null)&&filter.inspector.isVisible()) {
        int x = filter.inspector.getLocation().x-filter.frame.getLocation().x;
        int y = filter.inspector.getLocation().y-filter.frame.getLocation().y;
        control.setValue("inspector_x", x); //$NON-NLS-1$
        control.setValue("inspector_y", y); //$NON-NLS-1$
      }
    }

    /**
     * Creates a new filter.
     *
     * @param control the control
     * @return the new filter
     */
    public Object createObject(XMLControl control) {
      return new RotateFilter();
    }

    /**
     * Loads a filter with data from an XMLControl.
     *
     * @param control the control
     * @param obj the filter
     * @return the loaded object
     */
    public Object loadObject(XMLControl control, Object obj) {
      final PerspectiveFilter filter = (PerspectiveFilter) obj;
      filter.inspectorX = control.getInt("inspector_x"); //$NON-NLS-1$
      filter.inspectorY = control.getInt("inspector_y"); //$NON-NLS-1$
      return obj;
    }

  }

}

/*
 * Open Source Physics software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.

 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be released
 * under the GNU GPL license.
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
 * Copyright (c) 2007  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
