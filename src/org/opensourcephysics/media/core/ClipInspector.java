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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;

/**
 * This displays and sets VideoClip and ClipControl properties.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class ClipInspector extends JDialog {
  // instance fields
  protected VideoClip clip;
  protected ClipControl clipControl;
  protected JPanel dataPanel;
  protected JLabel startLabel;
  protected JLabel stepSizeLabel;
  protected JLabel t0Label;
  protected JLabel endLabel;
  protected JLabel dtLabel;
  protected JLabel fpsLabel;
  protected IntegerField startField;
  protected IntegerField stepSizeField;
  protected NumberField t0Field;
  protected IntegerField endField;
  protected NumberField dtField;
  protected NumberField fpsField;
  protected JButton okButton;
  protected JButton cancelButton;
  protected int prevFrame;
  protected int prevStart;
  protected int prevEnd;
  protected int prevSize;
  protected int prevCount;
  protected double prevDt;
  protected double prevRate;
  protected double prevStartTime;
  protected boolean prevDefault;

  /**
   * Constructs a non-modal ClipInspector with access to the clip control.
   *
   * @param videoClip the video clip
   * @param control the clip control
   * @param frame the owner
   */
  public ClipInspector(VideoClip videoClip, ClipControl control, Frame frame) {
    super(frame, false); // non-modal dialog
    setTitle(MediaRes.getString("ClipInspector.Title")); //$NON-NLS-1$
    setResizable(false);
    clip = videoClip;
    clipControl = control;
    createGUI();
    initialize();
    pack();
  }

  /**
   * Enables the startField. When enabled, the startField sets the clip
   * start frame number.
   *
   * @param enabled <code>true</code> to enable the startField
   */
  public void setStartFrameEnabled(final boolean enabled) {
    Runnable enable = new Runnable() {
      public void run() {
        startField.setEnabled(enabled);
      }

    };
    try {
      EventQueue.invokeAndWait(enable);
    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Enables the stepSizeField. When enabled, the stepSizeField sets the
   * clip step size.
   *
   * @param enabled <code>true</code> to enable the stepSizeField
   */
  public void setStepSizeEnabled(final boolean enabled) {
    Runnable enable = new Runnable() {
      public void run() {
        stepSizeField.setEnabled(enabled);
      }

    };
    try {
      EventQueue.invokeAndWait(enable);
    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Enables the countField. When enabled, the countField sets the
   * clip step count. Changed to apply to endField 11/06 DB
   *
   * @param enabled <code>true</code> to enable the countField
   */
  public void setStepCountEnabled(final boolean enabled) {
    Runnable enable = new Runnable() {
      public void run() {
        endField.setEnabled(enabled);
      }

    };
    try {
      EventQueue.invokeAndWait(enable);
    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Initializes this clip inspector.
   */
  public void initialize() {
    updateDisplay();
    prevStart = clip.getStartFrameNumber();
    prevEnd = clip.getEndFrameNumber();
    prevSize = clip.getStepSize();
    prevCount = clip.getStepCount();
    prevStartTime = clip.getStartTime();
    prevDefault = clip.isDefaultStartTime;
    prevDt = clipControl.getMeanFrameDuration();
    prevRate = clipControl.getRate();
    prevFrame = clipControl.getFrameNumber();
  }

  /**
   * Refreshes the GUI.
   */
  public void refresh() {
    setTitle(MediaRes.getString("ClipInspector.Title"));                       //$NON-NLS-1$
    startLabel.setText(MediaRes.getString("ClipInspector.Label.StartFrame"));  //$NON-NLS-1$
    stepSizeLabel.setText(MediaRes.getString("ClipInspector.Label.StepSize")); //$NON-NLS-1$
    t0Label.setText(MediaRes.getString("ClipInspector.Label.StartTime"));      //$NON-NLS-1$
    endLabel.setText(MediaRes.getString("ClipInspector.Label.EndFrame"));      //$NON-NLS-1$
    cancelButton.setText(MediaRes.getString("Dialog.Button.Cancel"));          //$NON-NLS-1$
    okButton.setText(MediaRes.getString("Dialog.Button.OK"));                  //$NON-NLS-1$
    dtLabel.setText(MediaRes.getString("ClipInspector.Label.FrameDT"));     //$NON-NLS-1$
    fpsLabel.setText(MediaRes.getString("ClipInspector.Label.FPS")); //$NON-NLS-1$
    pack();
  }

  //_____________________________ private methods ____________________________

  /**
   * Creates the visible components for the clip.
   */
  private void createGUI() {
    JPanel inspectorPanel = new JPanel(new BorderLayout());
    setContentPane(inspectorPanel);
    JPanel controlPanel = new JPanel(new BorderLayout());
    inspectorPanel.add(controlPanel, BorderLayout.SOUTH);
    // create start label and field
    startLabel = new JLabel(MediaRes.getString("ClipInspector.Label.StartFrame")); //$NON-NLS-1$
    startLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
    startLabel.setForeground(new Color(0, 0, 102));
    startLabel.setFont(new Font("Dialog", Font.PLAIN, 12)); //$NON-NLS-1$
    startField = new IntegerField(5);
    startField.setMaximumSize(startField.getPreferredSize());
    final ActionListener startListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
  			int prevStart = clip.getStartFrameNumber();
        clip.setStartFrameNumber(startField.getIntValue(), prevEnd);
    		int newStart = clip.getStartFrameNumber();
        // reset start time if needed
        if (!clip.isDefaultStartTime) {
    			double startTime = clip.getStartTime();
        	startTime += (newStart-prevStart)*clipControl.getMeanFrameDuration();
        	clip.setStartTime(startTime);
        }        		
  			clip.trimFrameCount();
        updateDisplay();
        startField.selectAll();
  			clipControl.setStepNumber(0);
      }

    };
    startField.addActionListener(startListener);
    startField.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        startField.selectAll();
      }
      public void focusLost(FocusEvent e) {
        startListener.actionPerformed(null);
      }

    });
    // create stepSize label and field
    stepSizeLabel = new JLabel(MediaRes.getString("ClipInspector.Label.StepSize")); //$NON-NLS-1$
    stepSizeLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
    stepSizeLabel.setForeground(new Color(0, 0, 102));
    stepSizeLabel.setFont(new Font("Dialog", Font.PLAIN, 12)); //$NON-NLS-1$
    stepSizeField = new IntegerField(5);
    stepSizeField.setMaximumSize(stepSizeField.getPreferredSize());
    stepSizeField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	int frameNumber = clipControl.getFrameNumber();
        clip.setStepSize(stepSizeField.getIntValue());
        updateDisplay();
        stepSizeField.selectAll();
        clipControl.setStepNumber(clip.frameToStep(frameNumber));
      }

    });
    stepSizeField.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        stepSizeField.selectAll();
      }
      public void focusLost(FocusEvent e) {
      	int frameNumber = clipControl.getFrameNumber();
        clip.setStepSize(stepSizeField.getIntValue());
        updateDisplay();
        clipControl.setStepNumber(clip.frameToStep(frameNumber));
      }

    });
    // create end frame label and field
    endLabel = new JLabel(MediaRes.getString("ClipInspector.Label.EndFrame")); //$NON-NLS-1$
    endLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
    endLabel.setForeground(new Color(0, 0, 102));
    endLabel.setFont(new Font("Dialog", Font.PLAIN, 12)); //$NON-NLS-1$
    endField = new IntegerField(5);
    endField.setMaximumSize(endField.getPreferredSize());
    endField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clip.setEndFrameNumber(endField.getIntValue());
        updateDisplay();
        endField.selectAll();
        Video video = clip.getVideo();
        if (video!=null && video.getFrameCount()>1)
        	clipControl.setStepNumber(clip.getStepCount()-1);
      }

    });
    endField.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        endField.selectAll();
      }
      public void focusLost(FocusEvent e) {
        clip.setEndFrameNumber(endField.getIntValue());
        updateDisplay();
        Video video = clip.getVideo();
        if (video!=null && video.getFrameCount()>1)
        	clipControl.setStepNumber(clip.getStepCount()-1);
      }

    });
    // create start time label and field
    t0Label = new JLabel(MediaRes.getString("ClipInspector.Label.StartTime")); //$NON-NLS-1$
    t0Label.setAlignmentX(Component.RIGHT_ALIGNMENT);
    t0Label.setForeground(new Color(0, 0, 102));
    t0Label.setFont(new Font("Dialog", Font.PLAIN, 12)); //$NON-NLS-1$
    t0Field = new NumberField(5);
    String exp = "0.00E0"; //$NON-NLS-1$
    String fixed = "0.000"; //$NON-NLS-1$
    double[] limits = new double[] {.01, .1, 1, 10};
    t0Field.setPatterns(new String[] {fixed, fixed, fixed, fixed, exp}, limits);
    t0Field.setUnits(" s"); //$NON-NLS-1$
    t0Field.setMaximumSize(t0Field.getPreferredSize());
    t0Field.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	if (t0Field.getText().equals("")) //$NON-NLS-1$
          clip.setStartTime(Double.NaN);
      	else
      		clip.setStartTime(t0Field.getValue()*1000);
        updateDisplay();
        t0Field.selectAll();
      }

    });
    t0Field.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        t0Field.selectAll();
      }
      public void focusLost(FocusEvent e) {
      	if (t0Field.getText().equals("")) //$NON-NLS-1$
          clip.setStartTime(Double.NaN);
      	else
      		clip.setStartTime(t0Field.getValue()*1000);
        updateDisplay();
      }

    });
    // create dt label and field
    dtLabel = new JLabel(MediaRes.getString("ClipInspector.Label.FrameDt")); //$NON-NLS-1$
    dtLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
    dtLabel.setForeground(new Color(0, 0, 102));
    dtLabel.setFont(new Font("Dialog", Font.PLAIN, 12)); //$NON-NLS-1$
    dtLabel.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        // inner popup menu listener class
        ActionListener listener = new ActionListener() {
          public void actionPerformed(ActionEvent e) {
          	dtLabel.setText(e.getActionCommand());
          }
        };
        // create popup menu and add menu items
        JPopupMenu popup = new JPopupMenu();
        JMenuItem item;
        for(int i = 0; i<3; i++) {
          if(i==0) {
            item = new JMenuItem(MediaRes.getString("ClipInspector.Label.FPS"));  //$NON-NLS-1$ 
          } else if(i==1) {
            item = new JMenuItem(MediaRes.getString("ClipInspector.Label.FrameDt"));  //$NON-NLS-1$ 
          } else {
            item = new JMenuItem(MediaRes.getString("ClipInspector.Label.StepDt")); //$NON-NLS-1$
          }
          item.setFont(new Font("Dialog", Font.PLAIN, 12));                                 //$NON-NLS-1$
          item.setActionCommand(item.getText());
          item.addActionListener(listener);
          popup.add(item);
        }
        // show popup menu
        popup.show(dtLabel, 0, dtLabel.getHeight());
      }

    });
    dtField = new NumberField(5);
    dtField.setPatterns(new String[] {exp, fixed, fixed, fixed, exp}, limits);
    dtField.setUnits(" s"); //$NON-NLS-1$
    dtField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clipControl.setFrameDuration(dtField.getValue()*1000);
        updateDisplay();
        dtField.selectAll();
      }

    });
    dtField.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        dtField.selectAll();
      }
      public void focusLost(FocusEvent e) {
      	clipControl.setFrameDuration(dtField.getValue()*1000);
        updateDisplay();
      }

    });
    // create fps label and field
    fpsLabel = new JLabel(MediaRes.getString("ClipInspector.Label.FPS")); //$NON-NLS-1$
    fpsLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
    fpsLabel.setForeground(new Color(0, 0, 102));
    fpsLabel.setFont(new Font("Dialog", Font.PLAIN, 12)); //$NON-NLS-1$
    fpsField = new NumberField(5);
    exp = "0.0E0"; //$NON-NLS-1$
    fixed = "0"; //$NON-NLS-1$
    fpsField.setPatterns(new String[] {exp, "0.0", fixed, fixed, exp}); //$NON-NLS-1$
    fpsField.setMaxValue(100000);
    fpsField.setUnits(" /s"); //$NON-NLS-1$
    fpsField.setMinValue(.000001);
    fpsField.setMaximumSize(fpsField.getPreferredSize());
    fpsField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clipControl.setFrameDuration(1000/fpsField.getValue());
        updateDisplay();
        fpsField.selectAll();
      }

    });
    fpsField.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        fpsField.selectAll();
      }
      public void focusLost(FocusEvent e) {
        clipControl.setFrameDuration(1000/fpsField.getValue());
        updateDisplay();
      }

    });
    // create data panel and add labels and fields
    dataPanel = new JPanel(new GridLayout(2, 3));
    Border lined = BorderFactory.createLineBorder(Color.GRAY);
    Border empty = BorderFactory.createEmptyBorder(2, 6, 2, 6);
    dataPanel.setBorder(BorderFactory.createCompoundBorder(lined, empty));
    controlPanel.add(dataPanel, BorderLayout.CENTER);
    GridBagConstraints c = new GridBagConstraints();
    // create startframe pane
    JPanel startPane = new JPanel(new GridBagLayout());
    c.gridwidth = 2; // 2 columns wide
    c.weightx = 0.5;
    c.anchor = GridBagConstraints.LINE_END;
    c.gridx = 0;
    c.gridy = 0;
    startPane.add(startLabel, c);
    c.weightx = 0;
    c.anchor = GridBagConstraints.LINE_START;
    c.gridwidth = 1; // 1 column wide
    c.gridx = 2;
    startPane.add(startField, c);
    dataPanel.add(startPane);
    // create stepsize pane
    JPanel sizePane = new JPanel(new GridBagLayout());
    c.weightx = 0.5;
    c.anchor = GridBagConstraints.LINE_END;
    c.gridwidth = 2; // 2 columns wide
    c.gridx = 0;
    sizePane.add(stepSizeLabel, c);
    c.weightx = 0;
    c.anchor = GridBagConstraints.LINE_START;
    c.gridwidth = 1; // 1 column wide
    c.gridx = 2;
    sizePane.add(stepSizeField, c);
    dataPanel.add(sizePane);
    // create endframe pane
    JPanel endPane = new JPanel(new GridBagLayout());
    c.weightx = 0.5;
    c.anchor = GridBagConstraints.LINE_END;
    c.gridwidth = 2; // 2 columns wide
    c.gridx = 0;
    endPane.add(endLabel, c);
    c.weightx = 0;
    c.anchor = GridBagConstraints.LINE_START;
    c.gridwidth = 1; // 1 column wide
    c.gridx = 2;
    endPane.add(endField, c);
    dataPanel.add(endPane);
    // create t0 pane
    JPanel t0Pane = new JPanel(new GridBagLayout());
    c.weightx = 0.5;
    c.anchor = GridBagConstraints.LINE_END;
    c.gridwidth = 2; // 2 columns wide
    c.gridx = 0;
    t0Pane.add(t0Label, c);
    c.weightx = 0;
    c.anchor = GridBagConstraints.LINE_START;
    c.gridwidth = 1; // 1 column wide
    c.gridx = 2;
    t0Pane.add(t0Field, c);
    dataPanel.add(t0Pane);
    // create fps pane
    JPanel fpsPane = new JPanel(new GridBagLayout());
    c.weightx = 0.5;
    c.anchor = GridBagConstraints.LINE_END;
    c.gridwidth = 2; // 2 columns wide
    c.gridx = 0;
    fpsPane.add(fpsLabel, c);
    c.weightx = 0;
    c.anchor = GridBagConstraints.LINE_START;
    c.gridwidth = 1; // 1 column wide
    c.gridx = 2;
    fpsPane.add(fpsField, c);
    dataPanel.add(fpsPane);
    // create dt pane
    JPanel dtPane = new JPanel(new GridBagLayout());
    c.weightx = 0.5;
    c.anchor = GridBagConstraints.LINE_END;
    c.gridwidth = 2; // 2 columns wide
    c.gridx = 0;
    c.gridy = 0;
    dtPane.add(dtLabel, c);
    c.weightx = 0;
    c.anchor = GridBagConstraints.LINE_START;
    c.gridwidth = 1; // 1 column wide
    c.gridx = 2;
    dtPane.add(dtField, c);
    dataPanel.add(dtPane);
    // create cancel button
    cancelButton = new JButton(MediaRes.getString("Dialog.Button.Cancel")); //$NON-NLS-1$
    cancelButton.setForeground(new Color(0, 0, 102));
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        revert();
        setVisible(false);
      }

    });
    // create ok button
    okButton = new JButton(MediaRes.getString("Dialog.Button.OK")); //$NON-NLS-1$
    okButton.setForeground(new Color(0, 0, 102));
    okButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setVisible(false);
      }

    });
    // create buttonbar and add buttons
    JPanel buttonbar = new JPanel(new GridLayout(1, 4));
    buttonbar.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
    controlPanel.add(buttonbar, BorderLayout.SOUTH);
    buttonbar.add(Box.createHorizontalBox());
    buttonbar.add(okButton);
    buttonbar.add(cancelButton);
    buttonbar.add(Box.createHorizontalBox());
  }

  /**
   * Updates this clip inspector to reflect the current clip settings.
   */
  public void updateDisplay() {
    startField.setIntValue(clip.getStartFrameNumber());
    stepSizeField.setIntValue(clip.getStepSize());
    t0Field.setValue(clip.getStartTime()/1000);
    endField.setIntValue(clip.getEndFrameNumber());
    double duration = clipControl.getMeanFrameDuration();
    if (duration>0) {
	    dtField.setValue(duration/1000);
	    fpsField.setValue(1000/duration);
    }
    else {
	    dtField.setText(null);
	    fpsField.setText(null);
    }
    repaint();
  }

  /**
   * Reverts to the previous clip settings.
   */
  private void revert() {
    clip.setStartFrameNumber(prevStart);
    clip.setStepSize(prevSize);
    clip.setStepCount(prevCount);
    if (prevDefault)
      clip.setStartTime(Double.NaN);
    else
    	clip.setStartTime(prevStartTime);
    clipControl.setRate(prevRate);
    clipControl.setFrameDuration(prevDt);
    clipControl.setStepNumber(clip.frameToStep(prevFrame));
    clip.trimFrameCount();
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
