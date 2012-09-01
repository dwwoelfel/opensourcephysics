/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.Document;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.display.GUIUtils;
import org.opensourcephysics.display.OSPRuntime;

/**
 * This displays a digital library catalog.
 *
 * @author Douglas Brown
 */
public class LibraryTreePanel extends JPanel {
	
  // instance fields
	protected LibraryCatalog catalog;
	protected String pathToCatalog;
  protected JPanel displayPanel;
  protected JTextPane infoPane;
  protected JTree tree;
  protected JScrollPane treeScroller = new JScrollPane();
  protected JToolBar toolbar;
  protected JButton editButton, newButton;
  protected Box editorPanel, FileBox;
  protected JTextField nameField, infoField, basePathField, fileField;
  protected JLabel nameLabel, infoLabel, basePathLabel, filesLabel;
  protected ArrayList<JLabel> labels = new ArrayList<JLabel>();
  protected JSpinner indexSpinner;
  protected Color emptyColor = new Color(100, 200, 100);
  protected Icon unknownIcon;
  protected JPopupMenu popup;
  protected MouseAdapter treeMouseListener;
  protected TreeSelectionListener treeSelectionListener;
  protected HyperlinkListener hyperlinkListener;
  protected LibraryTreeNode rootNode;
  protected Action cutAction, copyAction, pasteAction;
  protected int toolbarTreeComponentCount;

  /**
   * Constructs an empty LibraryTreePanel.
   */
  public LibraryTreePanel() {
    super(new BorderLayout());
    createGUI();
  }

  /**
   * Sets the catalog displayed in the tree.
   */
  public void setCatalog(LibraryCatalog catalog, String path, boolean editable) {
    this.catalog = catalog;
    pathToCatalog = path;
    // clean up existing tree, if any
    if (tree!=null) {
      tree.removeTreeSelectionListener(treeSelectionListener);
      tree.removeMouseListener(treeMouseListener);    	
    }
    if (editButton.isSelected()) {
    	editButton.doClick(0);
    }
    if (catalog!=null) {
	    // create new tree
	    rootNode = new LibraryTreeNode(catalog);
	    rootNode.setEditable(editable);
	    createTree(rootNode);
	    tree.setSelectionRow(0);
    }
    refreshGUI();
  }

  /**
   * Gets the catalog displayed in the tree.
   */
  public LibraryCatalog getCatalog() {
    return catalog;
  }

  /**
   * Adds a component to the toolbar.
   */
  public void addToolbarComponent(Component c) {
  	if (toolbar.getComponentCount()==toolbarTreeComponentCount)
  		toolbar.addSeparator();
  	toolbar.add(c);
  }
  
  protected LibraryTreeNode getSelectedNode() {
  	return (LibraryTreeNode) tree.getLastSelectedPathComponent();
  }

  /**
   * Displays the info page for the specified node.
   *
   * @param node the LibraryTreeNode
   */
  protected void showInfo(LibraryTreeNode node) {
  	if (node==null) {
  		infoPane.setText(null);
    	nameField.setText(null);
    	basePathField.setText(null);
    	infoField.setText(null);
    	fileField.setText(null);
    	return;
  	}
  	String uri = node.getInfoURI();
  	if (uri!=null) {
  		try {
  			URL currentPage = infoPane.getPage();
  			if (currentPage!=null && uri.equals(currentPage.toString())) {
					Document doc = infoPane.getDocument();
					if (doc!=null)
						doc.putProperty(Document.StreamDescriptionProperty, null);
  			}
				infoPane.setPage(uri);
			} catch (IOException ex) {
			}
  	}
  	else {
  		infoPane.setText(node.getInfoString());
  	}
  	nameField.setText(node.toString());
  	basePathField.setText(node.getBasePath());
  	boolean recordHasBasePath = node.record.getBasePath()!=null;
  	basePathField.setForeground(recordHasBasePath? infoField.getForeground(): emptyColor);
  	infoField.setText(node.getInfoPath());
  	String[] files = node.getFileNames();
  	int index = (Integer)indexSpinner.getValue();
  	if (files!=null && files.length>index) {
  		fileField.setText(files[index]);
  	}
  	else
  		fileField.setText(null);
  	fileField.setEnabled(files!=null);
  	filesLabel.setEnabled(files!=null);
  	indexSpinner.setEnabled(files!=null);
  	tree.repaint();
  }
  
  /**
   * Creates the GUI and listeners.
   */
  protected void createGUI() {
    // create popup menu and icons
    String imageFile = "/org/opensourcephysics/resources/tools/images/redfile.gif";        //$NON-NLS-1$
    unknownIcon = new ImageIcon(LibraryTreePanel.class.getResource(imageFile));
    popup = new JPopupMenu();
    // create actions
	  cutAction = new AbstractAction() {
		  public void actionPerformed(ActionEvent e) {
	    	LibraryTreeNode node = getSelectedNode();
	    	if (node!=null) {
	    		copyAction.actionPerformed(null);
	    		removeNode(node);
	    	}
		  }
		};
		copyAction = new AbstractAction() {
		  public void actionPerformed(ActionEvent e) {
      	LibraryTreeNode node = getSelectedNode();
      	if (node!=null) {
	        XMLControl control = new XMLControlElement(node.record);
	        StringSelection data = new StringSelection(control.toXML());
	        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	        clipboard.setContents(data, data);
		    }
		  }		
		};
		pasteAction = new AbstractAction() {
		  public void actionPerformed(ActionEvent e) {
      	LibraryTreeNode parent = getSelectedNode();
      	if (parent==null || parent.record instanceof LibraryHolding)
      		return;
      	
		    try {
		      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		      Transferable data = clipboard.getContents(null);
		      String dataString = (String) data.getTransferData(DataFlavor.stringFlavor);
		      if(dataString!=null) {
		        XMLControlElement control = new XMLControlElement();
		        control.readXML(dataString);
		        if (LibraryRecord.class.isAssignableFrom(control.getObjectClass())) {
		        	LibraryRecord record =(LibraryRecord) control.loadObject(null);
		      		LibraryCatalog catalog = (LibraryCatalog)parent.record;
	          	catalog.addRecord(record);
	          	LibraryTreeNode newNode = new LibraryTreeNode(record);
	          	insertChildAt(newNode, parent, parent.getChildCount());
		        }
		      }
		    } catch(Exception ex) {
		      ex.printStackTrace();
		    }
		  }		
		};

    // create tree listeners
    treeSelectionListener = new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
      	showInfo(getSelectedNode());
      }
    };
    treeMouseListener = new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        // select node and show popup menu
        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
        if(path==null) {
          return;
        }
        tree.setSelectionPath(path);
        LibraryTreeNode node = (LibraryTreeNode) tree.getLastSelectedPathComponent();
        if (OSPRuntime.isPopupTrigger(e) && editButton.isSelected()) {
          getPopup(node).show(tree, e.getX(), e.getY()+8);
        }
        else if (e.getClickCount()==2) {
        	if (node.record instanceof LibraryHolding) {
        		LibraryHolding holding = (LibraryHolding)node.record;
        		holding.setBasePath(node.getBasePath());
        		firePropertyChange("library_holding_activated", null, holding); //$NON-NLS-1$
        	}
        }
      }
    };
    // create toolbar and buttons
    newButton = new JButton();
    newButton.setOpaque(false);
    Border border = newButton.getBorder();
    Border space = BorderFactory.createEmptyBorder(2, 1, 0, 1);
    newButton.setBorder(BorderFactory.createCompoundBorder(space, border));
    newButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	if (editButton.isSelected()) {  // user canceling    		
      		displayPanel.remove(editorPanel);
      		displayPanel.validate();
      		editButton.setSelected(false);
      		refreshGUI();
      	}
      	else {
      		File file = GUIUtils.showSaveDialog(LibraryTreePanel.this);
      		if (file !=null) {
      			String path = file.getAbsolutePath();
      			LibraryCatalog catalog = new LibraryCatalog(null);
      			catalog.setBasePath(XML.getDirectoryPath(path));
      			setCatalog(catalog, path, true);
      			editButton.doClick(0);
      		}
      	}
      }
    });
    editButton = new JButton();
    editButton.setOpaque(false);
    editButton.setBorder(BorderFactory.createCompoundBorder(space, border));
    editButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	editButton.setSelected(!editButton.isSelected());
      	if (editButton.isSelected()) {      		
      		displayPanel.add(editorPanel, BorderLayout.NORTH);
      	}
      	else {
      		displayPanel.remove(editorPanel);
        	// save changes to current catalog if a local file
      		if (catalog!=null && pathToCatalog!=null
      				&& !pathToCatalog.startsWith("http:")) { //$NON-NLS-1$
      			XMLControl control = new XMLControlElement(catalog);
      			control.write(pathToCatalog);
      		}
      	}
      	displayPanel.validate();
      	refreshGUI();
      }
    });
    toolbar = new JToolBar();
    toolbar.setFloatable(false);
    toolbar.add(editButton);
    toolbar.add(newButton);
    toolbarTreeComponentCount = toolbar.getComponentCount();
    // create info pane and scroller
    infoPane = new JTextPane() {
      public void paintComponent(Graphics g) {
        if(OSPRuntime.antiAliasText) {
          Graphics2D g2 = (Graphics2D) g;
          RenderingHints rh = g2.getRenderingHints();
          rh.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
          rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        super.paintComponent(g);
      }

    };
    infoPane.setPreferredSize(new Dimension(600, 400));
    infoPane.setEditable(false);
    infoPane.setContentType("text/html"); //$NON-NLS-1$
    hyperlinkListener = new HyperlinkListener() {
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if(e.getEventType()==HyperlinkEvent.EventType.ACTIVATED) {
          try {
            if(!org.opensourcephysics.desktop.OSPDesktop.browse(e.getURL().toURI())) {
              // try the old way
              org.opensourcephysics.desktop.ostermiller.Browser.init();
              org.opensourcephysics.desktop.ostermiller.Browser.displayURL(e.getURL().toString());
            }
          } catch(Exception ex) {}
        }
      }
    };
    infoPane.addHyperlinkListener(hyperlinkListener);  
    
    JScrollPane xmlScroller = new JScrollPane(infoPane);
    // create data panel for right side of split pane
    displayPanel = new JPanel(new BorderLayout());
    displayPanel.add(xmlScroller, BorderLayout.CENTER);
    // create split pane
    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroller, displayPanel);
    add(splitPane, BorderLayout.CENTER);    
    add(toolbar, BorderLayout.NORTH);
    treeScroller.setPreferredSize(new Dimension(200, 400));
    
    // create editorPanel and components
    editorPanel = Box.createVerticalBox();
    final KeyAdapter keyListener = new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if(e.getKeyCode()==KeyEvent.VK_ENTER) {
          setBackground(Color.white);
        } 
        else {
          setBackground(Color.yellow);
        }
      }
    };

    nameField = new JTextField();
    nameField.addKeyListener(keyListener);
    final ActionListener nameAction = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	LibraryTreeNode node = getSelectedNode();
      	if (node!=null) {
      		node.setName(nameField.getText());
      	}
      }
    };
    nameField.addActionListener(nameAction);
    nameField.addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) {
      	nameField.selectAll();
      }
      public void focusLost(FocusEvent e) {
      	nameAction.actionPerformed(null);
      }
    });
    
    infoField = new JTextField();
    infoField.addKeyListener(keyListener);
    final ActionListener infoAction = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	LibraryTreeNode node = getSelectedNode();
      	if (node!=null) {
      		node.setInfoPath(infoField.getText());
      	}
      }
    };
    infoField.addActionListener(infoAction);
    infoField.addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) {
      	infoField.selectAll();
      }
      public void focusLost(FocusEvent e) {
      	infoAction.actionPerformed(null);
      }
    });
    
    basePathField = new JTextField();
    final ActionListener basePathAction = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	LibraryTreeNode node = getSelectedNode();
      	if (node!=null) {
      		node.setBasePath(basePathField.getText());
      	}
      }
    };
    basePathField.addActionListener(basePathAction);
    basePathField.addKeyListener(keyListener);
    basePathField.addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) {
      	LibraryTreeNode node = getSelectedNode();
      	if (node.record.getBasePath()==null) {
      		basePathField.setText(null);
        	basePathField.setForeground(infoField.getForeground());
      	}
      	else
      		basePathField.selectAll();
      }
      public void focusLost(FocusEvent e) {
      	basePathAction.actionPerformed(null);
      }
    });

    fileField = new JTextField();
    fileField.addKeyListener(keyListener);
    final ActionListener fileAction = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
    		String s = fileField.getText().trim();
      	LibraryTreeNode node = getSelectedNode();
      	if (s!=null && node!=null && node.record instanceof LibraryHolding) {
      		s = s.trim();
      		int index = (Integer)indexSpinner.getValue();
      		LibraryHolding record = (LibraryHolding)node.record;
      		String[] files = record.getContents();
        	if (files.length>index && files[index]!=null && !s.equals(files[index])) {
        		record.removeContent(files[index]);
        	}
      		if (!s.equals("")) { //$NON-NLS-1$
      			record.addContent(s);
      		}
      		showInfo(getSelectedNode());
      	}
      }
    };
    fileField.addActionListener(fileAction);
    fileField.addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) {
      	fileField.selectAll();
      }
      public void focusLost(FocusEvent e) {
      	fileAction.actionPerformed(null);
      }
    });
    nameLabel = new JLabel();
    nameLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    nameLabel.setHorizontalAlignment(SwingConstants.TRAILING);
    infoLabel = new JLabel();
    infoLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    infoLabel.setHorizontalAlignment(SwingConstants.TRAILING);
    basePathLabel = new JLabel();
    basePathLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    basePathLabel.setHorizontalAlignment(SwingConstants.TRAILING);
    filesLabel = new JLabel();
    filesLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    filesLabel.setHorizontalAlignment(SwingConstants.TRAILING);
    labels.add(nameLabel);
    labels.add(infoLabel);
    labels.add(basePathLabel);
    labels.add(filesLabel);

    int maxFileCount = 4;
    SpinnerModel model = new SpinnerNumberModel(0, 0, maxFileCount-1, 1);
    indexSpinner = new JSpinner(model);
    JSpinner.NumberEditor editor = new JSpinner.NumberEditor(indexSpinner);
    indexSpinner.setEditor(editor);
    indexSpinner.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
      	showInfo(getSelectedNode());
      }
    });
    
    Box box = Box.createHorizontalBox();
    box.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 2));
    box.add(nameLabel);
    box.add(nameField);    
    editorPanel.add(box);
    
    box = Box.createHorizontalBox();
    box.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 2));
    box.add(infoLabel);
    box.add(infoField);    
    editorPanel.add(box);

    box = Box.createHorizontalBox();
    box.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 2));
    box.add(basePathLabel);
    box.add(basePathField);    
    editorPanel.add(box);

    FileBox = Box.createHorizontalBox();
    FileBox.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 1));
    FileBox.add(filesLabel);
    FileBox.add(indexSpinner);    
    FileBox.add(fileField);    
    editorPanel.add(FileBox);
  }
  
  /**
   * Refreshes the GUI.
   */
  protected void refreshGUI() {
    newButton.setText(editButton.isSelected()? 
    		ToolsRes.getString("LibraryTreePanel.Button.Cancel"): //$NON-NLS-1$
    		ToolsRes.getString("LibraryTreePanel.Button.New")); //$NON-NLS-1$
    editButton.setText(editButton.isSelected()? 
    		ToolsRes.getString("LibraryTreePanel.Button.Save"): //$NON-NLS-1$
    		ToolsRes.getString("LibraryTreePanel.Button.Edit")); //$NON-NLS-1$
  	editButton.setEnabled(rootNode!=null && rootNode.isEditable());
  	nameLabel.setText(ToolsRes.getString("LibraryTreePanel.Label.Name")); //$NON-NLS-1$
  	infoLabel.setText(ToolsRes.getString("LibraryTreePanel.Label.Info")); //$NON-NLS-1$
  	basePathLabel.setText(ToolsRes.getString("LibraryTreePanel.Label.BasePath")); //$NON-NLS-1$
  	filesLabel.setText(ToolsRes.getString("LibraryTreePanel.Label.Files")); //$NON-NLS-1$
    // adjust size of labels so they right-align
    int w = 0;
    Font font = nameLabel.getFont();
    FontRenderContext frc = new FontRenderContext(null, false, false); 
    for(JLabel next: labels) {
      Rectangle2D rect = font.getStringBounds(next.getText()+" ", frc); //$NON-NLS-1$
      w = Math.max(w, (int) rect.getWidth()+4);
    }
    Dimension labelSize = new Dimension(w, 20);
    for(JLabel next: labels) {
      next.setPreferredSize(labelSize);
    }
  }

  private void createTree(LibraryTreeNode root) {
    tree = new JTree(root);
    tree.setCellRenderer(new LibraryNodeRenderer());
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    ToolTipManager.sharedInstance().registerComponent(tree);
    // listen for tree selections and display the contents
    tree.addTreeSelectionListener(treeSelectionListener);
    // listen for mouse events to display node info and inform propertyChangeListeners
    tree.addMouseListener(treeMouseListener);
    // put tree in scroller
    treeScroller.setViewportView(tree);
  }
  
  /**
   * This is a DefaultMutableTreeNode for a library tree.
   *
   * @author Douglas Brown
   * @version 1.0
   */
  private class LibraryTreeNode extends DefaultMutableTreeNode {

    protected LibraryRecord record;
    protected boolean editable = true;

    /**
     * Constructs a node with a LibraryRecord
     *
     * @param libRecord the record
     */
    protected LibraryTreeNode(LibraryRecord record) {
    	this.record = record;
      if (record instanceof LibraryCatalog) {
      	LibraryCatalog catalog = (LibraryCatalog)record;
      	for (LibraryRecord next: catalog.getRecords()) {
      		add(new LibraryTreeNode(next));
      	}
      }
      setUserObject(this);
    }
    
    protected String getBasePath() {
    	String base = record.getBasePath();
    	if (base!=null && !base.equals("")) //$NON-NLS-1$
    		return base;
    	LibraryTreeNode parent = (LibraryTreeNode)getParent();
    	return parent.getBasePath();
    }

    protected String getBaseURI() {
    	return LibraryRecord.getURI(getBasePath());
    }

    protected String getInfoPath() {
    	String path = record.getInfoPath();
  		if (path!=null && !path.trim().equals("")) //$NON-NLS-1$
  			return path;
    	if (record instanceof LibraryHolding) {
    		LibraryHolding holding = (LibraryHolding)record;
    		for (String next: holding.getContents()) {
	    		if (next.endsWith(holding.getName()+".html")) { //$NON-NLS-1$
	    			return holding.getName()+".html"; //$NON-NLS-1$
	    		}
	    	}
	    }
    	return null;
    }
    
    protected String getInfoURI() {
    	String infoPath = getInfoPath();
    	if (infoPath!=null && XML.getExtension(infoPath)!=null) {
    		if (!infoPath.startsWith("http") && !infoPath.startsWith("file://")) {    			 //$NON-NLS-1$ //$NON-NLS-2$
    			infoPath = XML.getResolvedPath(infoPath, getBaseURI());
    		}
    		return infoPath;
    	}
    	return null;
    }
    
    protected String getInfoString() {
    	if (record instanceof LibraryCatalog) {
    		LibraryCatalog catalog = (LibraryCatalog)record;
    		if (catalog.getInfoPath()!=null) {
    			String s = ResourceLoader.getString(catalog.getInfoPath());
    			if (s!=null) return s;
    		}
	    }
    	StringBuffer buf = new StringBuffer();
    	if (record instanceof LibraryHolding) {
    		LibraryHolding holding = (LibraryHolding)record;    		
      	buf.append("<html>\n<head>\n<title>"); //$NON-NLS-1$
      	buf.append(holding.getName());
      	buf.append("</title>\n</head>\n<body>\n<blockquote><font size=\"5\" face=\"Arial, Helvetica, sans-serif\">"); //$NON-NLS-1$
      	buf.append(record.getDescription()+" \""+record.getName()+"\""); //$NON-NLS-1$ //$NON-NLS-2$
      	buf.append("</font>\n<p><font size=\"4\" face=\"Arial, Helvetica, sans-serif\">"); //$NON-NLS-1$
      	buf.append(ToolsRes.getString("LibraryTreePanel.Label.Files")+":"); //$NON-NLS-1$ //$NON-NLS-2$
      	buf.append("</font></p>\n<div>\n<ol>"); //$NON-NLS-1$
      	for (String next: holding.getContents()) {
      		buf.append("\n<li><font size=\"4\" face=\"Arial, Helvetica, sans-serif\">"+next+"</font></li>"); //$NON-NLS-1$ //$NON-NLS-2$
      	}
      	buf.append("\n</ol>\n</div>\n<blockquote>\n</body>\n</html>"); //$NON-NLS-1$
    	}
    	else {
    		LibraryCatalog catalog = (LibraryCatalog)record;
      	buf.append("<html>\n<head>\n<title>"); //$NON-NLS-1$
      	buf.append(catalog.getName());
      	buf.append("</title>\n</head>\n<body>\n<blockquote><font size=\"5\" face=\"Arial, Helvetica, sans-serif\">"); //$NON-NLS-1$
      	buf.append(record.getDescription()+" \""+record.getName()+"\""); //$NON-NLS-1$ //$NON-NLS-2$
      	buf.append("</font>\n<p><font size=\"4\" face=\"Arial, Helvetica, sans-serif\">"); //$NON-NLS-1$
      	buf.append(ToolsRes.getString("LibraryTreePanel.Holdings")+":"); //$NON-NLS-1$ //$NON-NLS-2$
      	buf.append("</font></p>\n<div>\n<ol>"); //$NON-NLS-1$
      	for (LibraryRecord next: catalog.getRecords()) {
      		String s = next.getDescription()+" \""+next.getName()+"\""; //$NON-NLS-1$ //$NON-NLS-2$
      		buf.append("\n<li><font size=\"4\" face=\"Arial, Helvetica, sans-serif\">"+s+"</font></li>"); //$NON-NLS-1$ //$NON-NLS-2$
      	}
      	buf.append("\n</ol>\n</div>\n<blockquote>\n</body>\n</html>"); //$NON-NLS-1$
    	}
    	return buf.toString();
    }
    
    protected String[] getFileNames() {
    	if (record instanceof LibraryHolding) {
    		LibraryHolding holding = (LibraryHolding)record;
    		return holding.getContents();
    	}
    	return null;
    }

    /**
     * Used by the tree node to get the display name.
     *
     * @return the display name of the node
     */
    public String toString() {
    	return record.getName();
    }
    
    protected boolean isEditable() {
    	if (isRoot()) return editable;
    	LibraryTreeNode parent = (LibraryTreeNode)getParent();
    	return editable && parent.isEditable();
    }
    
    protected void setEditable(boolean edit) {
    	editable = edit;
    }
    
    protected void setName(String name) {
    	record.setName(name);
    	tree.getModel().valueForPathChanged(new TreePath(getPath()), name);
    }
    
    protected void setInfoPath(String path) {
    	record.setInfoPath(path);
    	showInfo(this);
    }
    
    protected void setBasePath(String path) {
  		record.setBasePath(path);
    	showInfo(this);
    }
  }

  protected JPopupMenu getPopup(final LibraryTreeNode node) {
  	popup.removeAll();  	
   	if (node.record instanceof LibraryHolding) {
  		// copy or cut holding
   		JMenuItem item = new JMenuItem(ToolsRes.getString("MenuItem.Copy")); //$NON-NLS-1$
      popup.add(item);
      item.addActionListener(copyAction);
   		item = new JMenuItem(ToolsRes.getString("LibraryTreePanel.Popup.MenuItem.Cut")); //$NON-NLS-1$
      popup.add(item);
      item.addActionListener(cutAction);
  		// move up or down?
  	}
  	else {
  		final LibraryCatalog catalog = (LibraryCatalog)node.record;
  		// add new record to this catalog
      JMenuItem item = new JMenuItem(ToolsRes.getString("LibraryTreePanel.Popup.MenuItem.AddRecord")); //$NON-NLS-1$
      popup.add(item);
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
        	LibraryHolding record = new LibraryHolding(ToolsRes.getString("LibraryHolding.Name.Default")); //$NON-NLS-1$
        	catalog.addRecord(record);
        	LibraryTreeNode newNode = new LibraryTreeNode(record);
        	insertChildAt(newNode, node, node.getChildCount());
        }
      });
  		// add new catalog to this catalog
      item = new JMenuItem(ToolsRes.getString("LibraryTreePanel.Popup.MenuItem.AddCatalog")); //$NON-NLS-1$
      popup.add(item);
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
        	LibraryCatalog newCat = new LibraryCatalog(null);
        	catalog.addRecord(newCat);
        	LibraryTreeNode newNode = new LibraryTreeNode(newCat);
        	insertChildAt(newNode, node, node.getChildCount());
        }
      });
      popup.addSeparator();
  		// copy or cut catalog
   		item = new JMenuItem(ToolsRes.getString("MenuItem.Copy")); //$NON-NLS-1$
      popup.add(item);
      item.addActionListener(copyAction);
   		item = new JMenuItem(ToolsRes.getString("LibraryTreePanel.Popup.MenuItem.Cut")); //$NON-NLS-1$
      popup.add(item);
      item.addActionListener(cutAction);
   		item = new JMenuItem(ToolsRes.getString("MenuItem.Paste")); //$NON-NLS-1$
      popup.add(item);
      item.addActionListener(pasteAction);  		
  	}
    return popup;
  }

  
  /**
   * Inserts a child into a parent node at a specified index.
   *
   * @param parent the parent node
   * @param child the child node
   * @param index the index
   */
  private void insertChildAt(LibraryTreeNode child, LibraryTreeNode parent, int index) {
  	if (parent.getChildCount()<index) return;
  	DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
  	model.insertNodeInto(child, parent, index);
  	TreePath path = new TreePath(child.getPath());
  	tree.scrollPathToVisible(path);
    tree.setSelectionPath(path);
  }

  /**
   * Inserts a child into a parent node at a specified index.
   *
   * @param parent the parent node
   * @param child the child node
   * @param index the index
   */
  private void removeNode(LibraryTreeNode node) {
  	if (node==rootNode) return;
  	LibraryTreeNode parent = (LibraryTreeNode)node.getParent();
  	LibraryCatalog catalog = (LibraryCatalog)parent.record;
  	catalog.removeRecord(node.record);
  	DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
		model.removeNodeFromParent(node);
  	TreePath path = new TreePath(parent.getPath());
  	tree.scrollPathToVisible(path);
    tree.setSelectionPath(path);
  }

  /**
   * A cell renderer to show LibraryNodes.
   */
  private class LibraryNodeRenderer extends DefaultTreeCellRenderer {
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
      LibraryTreeNode node = (LibraryTreeNode) value;
      if (node.record instanceof LibraryCatalog)
      	setToolTipText(node.getBasePath());
      else {
      	LibraryHolding holding = (LibraryHolding)node.record;
      	String[] contents = holding.getContents();
      	String fileName = contents.length>0? contents[0]: null;
      	fileName = XML.getResolvedPath(fileName, node.getBasePath());
      	if (contents.length>1)
      		fileName +=", ..."; //$NON-NLS-1$
      	setToolTipText(fileName);
      }
      Icon icon = node.record.getIcon();
      setIcon(icon!=null? icon: unknownIcon);
      return this;
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
