/*
 * Unitex
 *
 * Copyright (C) 2001-2011 Université Paris-Est Marne-la-Vallée <unitex@univ-mlv.fr>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA.
 *
 */
package fr.umlv.unitex.graphrendering;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import javax.sound.midi.SysexMessage;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.undo.UndoableEdit;

import fr.umlv.unitex.MyCursors;
import fr.umlv.unitex.diff.GraphDecorator;
import fr.umlv.unitex.frames.GraphFrame;
import fr.umlv.unitex.frames.InternalFrameManager;
import fr.umlv.unitex.frames.UnitexFrame;
import fr.umlv.unitex.io.GraphIO;
import fr.umlv.unitex.undo.AddBoxEdit;
import fr.umlv.unitex.undo.BoxTextEdit;
import fr.umlv.unitex.undo.SelectEdit;
import fr.umlv.unitex.undo.SurroundEdit;
import fr.umlv.unitex.undo.TransitionEdit;
import fr.umlv.unitex.undo.TranslationGroupEdit;

/**
 * This class describes a component on which a graph can be drawn.
 *
 * @author Sébastien Paumier
 */
public class GraphicalZone extends GenericGraphicalZone implements Printable {
    boolean dragBegin = true;
    int dX;
    int dY;
    
    int popupX=-1,popupY=-1;

    JMenu submenu;

    /**
     * Constructs a new <code>GraphicalZone</code>.
     *
     * @param w width of the drawing area
     * @param h height of the drawing area
     * @param t text field to edit box contents
     * @param p frame that contains the component
     */
    public GraphicalZone(GraphIO gio, TextField t, GraphFrame p,GraphDecorator diff) {
        super(gio, t, p, diff);
        if (diff==null) {
        	/* No need to have mouse listeners on a read-only diff display */
        	addMouseListener(new MyMouseListener());
            addMouseMotionListener(new MyMouseMotionListener());
        }
        createPopup();
    }

    Action surroundWithInputVar;
    public Action getSurroundWithInputVarAction() {
    	return surroundWithInputVar;
    }
    Action surroundWithOutputVar;
    public Action getSurroundWithOutputVarAction() {
    	return surroundWithOutputVar;
    }
    Action surroundWithMorphologicalMode;
    public Action getSurroundWithMorphologicalModeAction() {
    	return surroundWithMorphologicalMode;
    }
    Action surroundWithLeftContext;
    public Action getSurroundWithLeftContextAction() {
    	return surroundWithLeftContext;
    }
    Action surroundWithRightContext;
    public Action getSurroundWithRightContextAction() {
    	return surroundWithRightContext;
    }
    Action surroundWithNegativeRightContext;
    public Action getSurroundWithNegativeRightContextAction() {
    	return surroundWithNegativeRightContext;
    }
    
    private void createPopup() {
		final JPopupMenu popup=new JPopupMenu();
		final Action newBox=new AbstractAction("Create box") {

			public void actionPerformed(ActionEvent e) {
				GraphBox b = (GraphBox) createBox((int) (popupX / scaleFactor),
                        (int) (popupY / scaleFactor));
                // if some boxes are selected, we rely them to the new one
                if (!selectedBoxes.isEmpty()) {
                    addTransitionsFromSelectedBoxes(b, false);
                }
                // then, the only selected box is the new one
                unSelectAllBoxes();
                b.selected = true;
                selectedBoxes.add(b);
                fireGraphTextChanged(b.content);
                fireGraphChanged(true);
                fireBoxSelectionChanged();
			}
		};
		newBox.setEnabled(true);
		newBox.putValue(Action.SHORT_DESCRIPTION,"Create a new box");
		popup.add(new JMenuItem(newBox));

		submenu=new JMenu("Surround with...");
		surroundWithInputVar=new AbstractAction("Input variable") {
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent e) {
				String name=InternalFrameManager.getManager(null).newVariableInsertionDialog(true);
				if (name==null || name.equals("")) return;
				surroundWithBoxes((ArrayList<GenericGraphBox>) selectedBoxes.clone(),
						"$"+name+"(","$"+name+")");
			}
		};
		surroundWithInputVar.setEnabled(false);
		surroundWithInputVar.putValue(Action.SHORT_DESCRIPTION,"Surround box selection with an input variable");
		submenu.add(new JMenuItem(surroundWithInputVar));
		
		surroundWithOutputVar=new AbstractAction("Output variable") {
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent e) {
				String name=InternalFrameManager.getManager(null).newVariableInsertionDialog(false);
				if (name==null || name.equals("")) return;
				surroundWithBoxes((ArrayList<GenericGraphBox>) selectedBoxes.clone(),
						"$|"+name+"(","$|"+name+")");
			}
		};
		surroundWithOutputVar.setEnabled(false);
		surroundWithOutputVar.putValue(Action.SHORT_DESCRIPTION,"Surround box selection with an output variable");
		submenu.add(new JMenuItem(surroundWithOutputVar));
		
		submenu.addSeparator();
		surroundWithMorphologicalMode=new AbstractAction("Morphological mode") {
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent e) {
				surroundWithBoxes((ArrayList<GenericGraphBox>) selectedBoxes.clone(),
						"$<","$>");
			}
		};
		surroundWithMorphologicalMode.setEnabled(false);
		surroundWithMorphologicalMode.putValue(Action.SHORT_DESCRIPTION,"Surround box selection with morphological mode tags");
		submenu.add(new JMenuItem(surroundWithMorphologicalMode));

		submenu.addSeparator();

		surroundWithLeftContext=new AbstractAction("Left context") {
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent e) {
				surroundWithBoxes((ArrayList<GenericGraphBox>) selectedBoxes.clone(),
						"$*",null);
			}
		};
		surroundWithLeftContext.setEnabled(false);
		surroundWithLeftContext.putValue(Action.SHORT_DESCRIPTION,"Inserts left context mark before box selection");
		submenu.add(new JMenuItem(surroundWithLeftContext));

		surroundWithRightContext=new AbstractAction("Right context") {
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent e) {
				surroundWithBoxes((ArrayList<GenericGraphBox>) selectedBoxes.clone(),
						"$[","$]");
			}
		};
		surroundWithRightContext.setEnabled(false);
		surroundWithRightContext.putValue(Action.SHORT_DESCRIPTION,"Surround box selection with right context tags");
		submenu.add(new JMenuItem(surroundWithRightContext));

		surroundWithNegativeRightContext=new AbstractAction("Negative right context") {
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent e) {
				surroundWithBoxes((ArrayList<GenericGraphBox>) selectedBoxes.clone(),
						"$![","$]");
			}
		};
		surroundWithNegativeRightContext.setEnabled(false);
		surroundWithNegativeRightContext.putValue(Action.SHORT_DESCRIPTION,"Surround box selection with negative right context tags");
		submenu.add(new JMenuItem(surroundWithNegativeRightContext));

		submenu.setEnabled(false);
		addBoxSelectionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean selected=selectedBoxes.size()!=0;
				newBox.setEnabled(!selected);
				submenu.setEnabled(selected);
				surroundWithInputVar.setEnabled(selected);
				surroundWithOutputVar.setEnabled(selected);
				surroundWithMorphologicalMode.setEnabled(selected);
				surroundWithLeftContext.setEnabled(selected);
				surroundWithRightContext.setEnabled(selected);
				surroundWithNegativeRightContext.setEnabled(selected);
			}
		});
		popup.add(submenu);
		
		addMouseListener(new MouseAdapter() {

			void show(MouseEvent e) {
				if (e.isPopupTrigger()) {
					popupX=e.getX();
					popupY=e.getY();
					popup.show(e.getComponent(),e.getX(),e.getY());
				}
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				show(e);
			}
			
			@Override
			public void mouseReleased(MouseEvent e) {
				show(e);
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				show(e);
			}
		});
	}

	protected void surroundWithBoxes(ArrayList<GenericGraphBox> selection,String box1,String box2) {
		ArrayList<GenericGraphBox> inputBoxes=new ArrayList<GenericGraphBox>();
		ArrayList<GenericGraphBox> outputBoxes=new ArrayList<GenericGraphBox>();
		computeInputOutputBoxes(selection,inputBoxes,outputBoxes);
		if (box1!=null && inputBoxes.isEmpty()) {
			return;
		}
		if (box2!=null && outputBoxes.isEmpty()) {
			return;
		}		
		SurroundEdit edit=new SurroundEdit();
		edit.addEdit(new SelectEdit(selection));
		if (box1!=null) {
			GraphBox inputBox=createInputBox(selection,inputBoxes,box1,edit);
			graphBoxes.add(inputBox);
		}
		if (box2!=null) {
			GraphBox outputBox=createOutputBox(selection,outputBoxes,box2,edit);
			graphBoxes.add(outputBox);
		}
		postEdit(edit);
		fireGraphChanged(true);
	}

	private GraphBox createInputBox(ArrayList<GenericGraphBox> selection,ArrayList<GenericGraphBox> inputBoxes,String content, SurroundEdit edit) {
		if (inputBoxes.isEmpty()) throw new IllegalArgumentException("Cannot compute the y average of no boxes");
		int y=getAverageY(inputBoxes);
		int x=inputBoxes.get(0).X_in;
		for (GenericGraphBox b:inputBoxes) {
			if (b.X_in<x) x=b.X_in; 
		}
		GraphBox newBox=new GraphBox(x-40,y,GenericGraphBox.NORMAL,this);
		edit.addEdit(new AddBoxEdit(newBox,graphBoxes,this));
		/* Finally, we set up all transitions */
		for (GenericGraphBox from:graphBoxes) {
			if (selection.contains(from)) continue;
			for (int i=from.transitions.size()-1;i>=0;i--) {
				GenericGraphBox dest=from.transitions.get(i);
				if (inputBoxes.contains(dest)) {
					from.removeTransitionTo(dest);
					edit.addEdit(new TransitionEdit(from,dest));
					if (!from.transitions.contains(newBox)) {
						from.addTransitionTo(newBox);
						edit.addEdit(new TransitionEdit(from,newBox));
					}
				}
			}
		}
		for (GenericGraphBox b:inputBoxes) {
			newBox.addTransitionTo(b);
			edit.addEdit(new TransitionEdit(newBox,b));
		}
		newBox.setContent(content);
		edit.addEdit(new BoxTextEdit(newBox,content,this));
		return newBox;
	}

	private GraphBox createOutputBox(ArrayList<GenericGraphBox> selection,ArrayList<GenericGraphBox> outputBoxes,String content, SurroundEdit edit) {
		if (outputBoxes.isEmpty()) throw new IllegalArgumentException("Cannot compute the y average of no boxes");
		int y=getAverageY(outputBoxes);
		int x=outputBoxes.get(0).X_out;
		for (GenericGraphBox b:outputBoxes) {
			if (b.X_out>x) x=b.X_out; 
		}
		GraphBox newBox=new GraphBox(x+30,y,GenericGraphBox.NORMAL,this);
		edit.addEdit(new AddBoxEdit(newBox,graphBoxes,this));
		/* Finally, we set up all transitions */
		for (GenericGraphBox from:outputBoxes) {
			for (int i=from.transitions.size()-1;i>=0;i--) {
				GenericGraphBox dest=from.transitions.get(i);
				if (selection.contains(dest)) continue;
				from.removeTransitionTo(dest);
				edit.addEdit(new TransitionEdit(from,dest));
				if (!newBox.transitions.contains(dest)) {
					newBox.addTransitionTo(dest);
					edit.addEdit(new TransitionEdit(newBox,dest));
				}
			}
		}
		for (GenericGraphBox b:outputBoxes) {
			b.addTransitionTo(newBox);
			edit.addEdit(new TransitionEdit(b,newBox));
		}
		newBox.setContent(content);
		edit.addEdit(new BoxTextEdit(newBox,content,this));
		return newBox;
	}

	/**
	 * We return the average Y coordinate computed from all the given boxes. 
	 */
	private int getAverageY(ArrayList<GenericGraphBox> boxes) {
		if (boxes.isEmpty()) throw new IllegalArgumentException("Cannot compute the y average of no boxes");
		int y=0;
		for (GenericGraphBox b:boxes) y=y+b.Y_in;
		return y/boxes.size();
	}

	/**
	 * This method considers a box group and computes which boxes within this group
	 * are to be considered as input and/or output ones. Those input and output
	 * boxes are useful when one wants to surround a box selection with, for instance,
	 * a variable declaration. In such a case, transitions to input boxes are turned
	 * into transitions to the $aaa( box that is then created, and the $aaa( box is then
	 * linked to all input boxes. The same for output boxes.  
	 */
	private void computeInputOutputBoxes(ArrayList<GenericGraphBox> selection,
			ArrayList<GenericGraphBox> inputBoxes,
			ArrayList<GenericGraphBox> outputBoxes) {
		ArrayList<GenericGraphBox> accessible=new ArrayList<GenericGraphBox>();
		for (GenericGraphBox ggb:graphBoxes) {
			if (selection.contains(ggb)) continue;
			for (GenericGraphBox dest:ggb.transitions) {
				if (selection.contains(dest)) {
					if (!inputBoxes.contains(dest) && dest.type==GenericGraphBox.NORMAL) inputBoxes.add(dest);
				}
			}
		}
		for (GenericGraphBox ggb:graphBoxes) {
			for (GenericGraphBox dest:ggb.transitions) {
				if (!accessible.contains(dest)) accessible.add(dest);
			}
		}
		for (GenericGraphBox ggb:selection) {
			if (!accessible.contains(ggb) && !inputBoxes.contains(ggb)
					&& ggb.type==GenericGraphBox.NORMAL) {
				/* By convention, we consider that boxes with no incoming transitions
				 * are input boxes */
				inputBoxes.add(ggb);
			}
			if (ggb.transitions.isEmpty()) {
				/* By convention, we consider that boxes with no outgoing transitions
				 * are output boxes */
				if (!outputBoxes.contains(ggb) && ggb.type==GenericGraphBox.NORMAL) outputBoxes.add(ggb);
			} else {
				for (GenericGraphBox dest:ggb.transitions) {
					if (selection.contains(dest)) continue;
					if (!outputBoxes.contains(ggb) && ggb.type==GenericGraphBox.NORMAL) outputBoxes.add(ggb);
				}
			}
		}
	}

	@Override
    protected void initializeEmptyGraph() {
        GraphBox g, g2;
        // creating the final state
        g = new GraphBox(300, 200, 1, this);
        g.setContent("<E>");
        // and the initial state
        g2 = new GraphBox(70, 200, 0, this);
        g2.n_lines = 0;
        g2.setContent("<E>");
        addBox(g2);
        addBox(g);
        Dimension d = new Dimension(1188, 840);
        setSize(d);
        setPreferredSize(new Dimension(d));
    }

    @Override
    protected GenericGraphBox createBox(int x, int y) {
        GraphBox g = new GraphBox(x, y, 2, this);
        g.setContent("<E>");
        addBox(g);
        return g;
    }

    @Override
    protected GenericGraphBox newBox(int x, int y, int type, GenericGraphicalZone p) {
        return new GraphBox(x, y, type, (GraphicalZone) p);
    }

    class MyMouseListener implements MouseListener {

    	
        // Shift+click
        // reverse transitions
    	boolean isReverseTransitionClick(MouseEvent e) {
    		return (EDITING_MODE == MyCursors.REVERSE_LINK_BOXES
                    || (EDITING_MODE == MyCursors.NORMAL && e.isShiftDown() && !e.isControlDown()));
    	}
    	
        // Control+click
        // creation of a new box
    	boolean isBoxCreationClick(MouseEvent e) {
    		return EDITING_MODE == MyCursors.CREATE_BOXES
            || (EDITING_MODE == MyCursors.NORMAL && ((e.isControlDown() && !e.isShiftDown())||e.getButton()==MouseEvent.BUTTON3));
    	}
    	
        // Alt+click
        // opening of a sub-graph
    	boolean isOpenGraphClick(MouseEvent e) {
    		return EDITING_MODE == MyCursors.OPEN_SUBGRAPH
            || (EDITING_MODE == MyCursors.NORMAL && e.isAltDown());
    	}
    	
    	// Ctrl+Shift+click
        // multiple box selection
    	boolean isMultipleSelectionClick(MouseEvent e) {
    		return (EDITING_MODE == MyCursors.NORMAL && e.isControlDown() && e.isShiftDown());
    	}
    	
        public void mouseClicked(MouseEvent e) {
            int boxSelected;
            GraphBox b;
            int x_tmp, y_tmp;
            if (isReverseTransitionClick(e)) {
            	System.err.println("A");
                boxSelected = getSelectedBox((int) (e.getX() / scaleFactor),
                        (int) (e.getY() / scaleFactor));
                if (boxSelected != -1) {
                    // if we click on a box
                    b = (GraphBox) graphBoxes.get(boxSelected);
                    fireBoxSelectionChanged();
                    if (!selectedBoxes.isEmpty()) {
                        // if there are selected boxes, we rely them to the
                        // current
                        addReverseTransitionsFromSelectedBoxes(b);
                        unSelectAllBoxes();
                    } else {
                        if (EDITING_MODE == MyCursors.REVERSE_LINK_BOXES) {
                            // if we click on a box while there is no box
                            // selected in REVERSE_LINK_BOXES mode,
                            // we select it
                            b.selected = true;
                            selectedBoxes.add(b);
                            fireBoxSelectionChanged();
                            fireGraphTextChanged(b.content);
                        }
                    }
                } else {
                    // simple click not on a box
                    unSelectAllBoxes();
                }
            } else if (isBoxCreationClick(e)) {
                b = (GraphBox) createBox((int) (e.getX() / scaleFactor),
                        (int) (e.getY() / scaleFactor));
                // if some boxes are selected, we rely them to the new one
                if (!selectedBoxes.isEmpty()) {
                    addTransitionsFromSelectedBoxes(b, false);
                }
                // then, the only selected box is the new one
                unSelectAllBoxes();
                b.selected = true;
                selectedBoxes.add(b);
                fireGraphTextChanged(b.content); /* Should be "<E>" */
                fireGraphChanged(true);
                fireBoxSelectionChanged();
            } else if (isOpenGraphClick(e)) {
                x_tmp = (int) (e.getX() / scaleFactor);
                y_tmp = (int) (e.getY() / scaleFactor);
                boxSelected = getSelectedBox(x_tmp, y_tmp);
                if (boxSelected != -1) {
                    // if we click on a box
                    b = (GraphBox) graphBoxes.get(boxSelected);
                    File file = b.getGraphClicked(y_tmp);
                    if (file != null) {
                    	InternalFrameManager.getManager(null).newGraphFrame(file);
                    }
                }
            } else if (EDITING_MODE == MyCursors.KILL_BOXES) {
                // killing a box
                if (!selectedBoxes.isEmpty()) {
                    // if boxes are selected, we remove them
                    removeSelected();
                } else {
                    // else, we check if we clicked on a box
                    x_tmp = (int) (e.getX() / scaleFactor);
                    y_tmp = (int) (e.getY() / scaleFactor);
                    boxSelected = getSelectedBox(x_tmp, y_tmp);
                    if (boxSelected != -1) {
                        b = (GraphBox) graphBoxes.get(boxSelected);
                        b.selected = true;
                        selectedBoxes.add(b);
                        removeSelected();
                    }
                }
            } else if (isMultipleSelectionClick(e)) {
                	boxSelected = getSelectedBox((int) (e.getX() / scaleFactor),
                        (int) (e.getY() / scaleFactor));
                	if (boxSelected != -1) {
                		// if we click on a box
                		b = (GraphBox) graphBoxes.get(boxSelected);
                		b.selected = true;
                		selectedBoxes.add(b);
                		fireGraphTextChanged(b.content);
                		fireBoxSelectionChanged();
                	}
            } else {
            	/* NORMAL BOX SELECTION */
                boxSelected = getSelectedBox((int) (e.getX() / scaleFactor),
                        (int) (e.getY() / scaleFactor));
                if (boxSelected != -1) {
                    // if we click on a box
                    b = (GraphBox) graphBoxes.get(boxSelected);
                    if (!selectedBoxes.isEmpty()) {
                        // if there are selected boxes, we rely them to the
                        // current one
                        addTransitionsFromSelectedBoxes(b, true);
                        unSelectAllBoxes();
                    } else {
                        if (!((EDITING_MODE == MyCursors.LINK_BOXES) && (b.type == 1))) {
                            // if not, we just select this one, but only if we
                            // are not clicking
                            // on final state in LINK_BOXES mode
                            b.selected = true;
                            selectedBoxes.add(b);
                            fireGraphTextChanged(b.content);
                            fireBoxSelectionChanged();
                        }
                    }
                    fireBoxSelectionChanged();
                } else {
                    // simple click not on a box
                    unSelectAllBoxes();
                }
            }
            fireGraphChanged(false);
        }

        public void mousePressed(MouseEvent e) {
            int selectedBox;
            if ((EDITING_MODE == MyCursors.NORMAL && (e.isShiftDown()
                    || e.isAltDown() || e.isControlDown()))
                    || (EDITING_MODE == MyCursors.OPEN_SUBGRAPH)
                    || (EDITING_MODE == MyCursors.KILL_BOXES)) {
                return;
            }
            validateContent();
            X_start_drag = (int) (e.getX() / scaleFactor);
            Y_start_drag = (int) (e.getY() / scaleFactor);
            X_end_drag = X_start_drag;
            Y_end_drag = Y_start_drag;
            X_drag = X_start_drag;
            Y_drag = Y_start_drag;
            dragWidth = 0;
            dragHeight = 0;
            selectedBox = getSelectedBox(X_start_drag, Y_start_drag);
            singleDragging = false;
            dragging = false;
            selecting = false;
            if (selectedBox != -1) {
                // if we start dragging a box
            	singleDraggedBox = graphBoxes.get(selectedBox);
                fireGraphTextChanged(singleDraggedBox.content);
                if (!singleDraggedBox.selected) {
                    /* Dragging a selected box is handled below with
                          * the general multiple box draggind case */
                    dragging = true;
                    singleDragging = true;
                    singleDraggedBox.singleDragging = true;
                    fireGraphChanged(true);
                    return;
                }
            }
            if (!selectedBoxes.isEmpty()) {
                dragging = true;
                fireGraphChanged(true);
                return;
            }
            if ((selectedBox == -1) && selectedBoxes.isEmpty()) {
                // being drawing a selection rectangle
                dragging = false;
                selecting = true;
                fireGraphChanged(false);
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isShiftDown() || e.isAltDown() || e.isControlDown())
                return;
            int dx = X_end_drag - X_start_drag;
            int dy = Y_end_drag - Y_start_drag;
            if (singleDragging) {
                // save position after the dragging
                selectedBoxes.add(singleDraggedBox);
                UndoableEdit edit = new TranslationGroupEdit(selectedBoxes, dx,
                        dy);
                postEdit(edit);
                selectedBoxes.remove(singleDraggedBox);
                dragging = false;
                singleDragging = false;
                singleDraggedBox.singleDragging = false;
                fireGraphChanged(true);
                return;
            }
            if (dragging && EDITING_MODE == MyCursors.NORMAL) {
                // save the position of all the translated boxes
                UndoableEdit edit = new TranslationGroupEdit(selectedBoxes,
                        dx, dy);
                postEdit(edit);
                fireGraphChanged(true);
            }
            dragging = false;
            if (selecting) {
                selectByRectangle(X_drag, Y_drag, dragWidth, dragHeight);
                selecting = false;
            }
            fireGraphChanged(false);
        }

        public void mouseEntered(MouseEvent e) {
            mouseInGraphicalZone = true;
            fireGraphChanged(false);
        }

        public void mouseExited(MouseEvent e) {
            mouseInGraphicalZone = false;
            fireGraphChanged(false);
        }
    }

    class MyMouseMotionListener implements MouseMotionListener {
        public void mouseDragged(MouseEvent e) {
            int Xtmp = X_end_drag;
            int Ytmp = Y_end_drag;
            X_end_drag = (int) (e.getX() / scaleFactor);
            Y_end_drag = (int) (e.getY() / scaleFactor);
            int dx = X_end_drag - Xtmp;
            int dy = Y_end_drag - Ytmp;
            dX += dx;
            dY += dy;
            if (singleDragging) {
                // translates the single dragged box
                singleDraggedBox.translate(dx, dy);
                fireGraphChanged(true);
                return;
            }
            if (dragging && EDITING_MODE == MyCursors.NORMAL) {
                // translates all the selected boxes
                translateAllSelectedBoxes(dx, dy);
                // if we were dragging, we have nothing else to do
                return;
            }
            /* If the user is setting the selection rectangle */
            if (X_start_drag < X_end_drag) {
                X_drag = X_start_drag;
                dragWidth = X_end_drag - X_start_drag;
            } else {
                X_drag = X_end_drag;
                dragWidth = X_start_drag - X_end_drag;
            }
            if (Y_start_drag < Y_end_drag) {
                Y_drag = Y_start_drag;
                dragHeight = Y_end_drag - Y_start_drag;
            } else {
                Y_drag = Y_end_drag;
                dragHeight = Y_start_drag - Y_end_drag;
            }
            fireGraphChanged(false);
        }

        public void mouseMoved(MouseEvent e) {
            Xmouse = (int) (e.getX() / scaleFactor);
            Ymouse = (int) (e.getY() / scaleFactor);
            if ((EDITING_MODE == MyCursors.REVERSE_LINK_BOXES || EDITING_MODE == MyCursors.LINK_BOXES)
                    && !selectedBoxes.isEmpty()) {
                fireGraphChanged(false);
            }
        }
    }

    /**
     * Draws the graph. This method should only be called by the virtual
     * machine.
     *
     * @param f_old the graphical context
     */
    @Override
    public void paintComponent(Graphics f_old) {
        setClipZone(f_old.getClipBounds());
        Graphics2D f = (Graphics2D) f_old;
        f.scale(scaleFactor, scaleFactor);
        if (info.antialiasing) {
            f.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            f.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        f.setColor(new Color(205, 205, 205));
        f.fillRect(0, 0, getWidth(), getHeight());
        f.setColor(info.backgroundColor);
        f.fillRect(0, 0, getWidth(), getHeight());
        if (info.frame) {
            f.setColor(info.foregroundColor);
            f.drawRect(10, 10, getWidth() - 20, getHeight() - 20);
            f.drawRect(9, 9, getWidth() - 18, getHeight() - 18);
        }
        f.setColor(info.foregroundColor);
        if (decorator==null) {
        File file = ((GraphFrame) parentFrame).getGraph();
        	if (info.filename) {
        		if (info.pathname)
        			f.drawString((file != null) ? file.getAbsolutePath() : "", 20,
                        getHeight() - 45);
        		else
        			f.drawString((file != null) ? file.getName() : "", 20,
                        getHeight() - 45);
        	}
        }
        if (info.date)
            f.drawString(new Date().toString(), 20, getHeight() - 25);
        drawGrid(f);
        if (mouseInGraphicalZone && !selectedBoxes.isEmpty()) {
            if (EDITING_MODE == MyCursors.REVERSE_LINK_BOXES) {
                drawTransitionsFromMousePointerToSelectedBoxes(f);
            } else if (EDITING_MODE == MyCursors.LINK_BOXES) {
                drawTransitionsFromSelectedBoxesToMousePointer(f);
            }
        }
        drawAllTransitions(f);
        drawAllBoxes(f);
        if (selecting) {
            // here we draw the selection rectangle
            f.setColor(info.foregroundColor);
            f.drawRect(X_drag, Y_drag, dragWidth, dragHeight);
        }
    }

    /**
     * Prints the graph.
     *
     * @param g         the graphical context
     * @param p         the page format
     * @param pageIndex the page index
     */
    public int print(Graphics g, PageFormat p, int pageIndex) {
        if (pageIndex != 0)
            return Printable.NO_SUCH_PAGE;
        Graphics2D f = (Graphics2D) g;
        double DPI = 96.0;
        double WidthInInches = p.getImageableWidth() / 72;
        double realWidthInInches = (getWidth() / DPI);
        double HeightInInches = p.getImageableHeight() / 72;
        double realHeightInInches = (getHeight() / DPI);
        double scale_x = WidthInInches / realWidthInInches;
        double scale_y = HeightInInches / realHeightInInches;
        f.translate(p.getImageableX(), p.getImageableY());
        if (scale_x < scale_y)
            f.scale(0.99 * 0.72 * scale_x, 0.99 * 0.72 * scale_x);
        else
            f.scale(0.99 * 0.72 * scale_y, 0.99 * 0.72 * scale_y);
        f.setColor(info.backgroundColor);
        f.fillRect(0, 0, getWidth(), getHeight());
        if (info.frame) {
            f.setColor(info.foregroundColor);
            Stroke oldStroke = f.getStroke();
            f.setStroke(GraphicalToolBox.frameStroke);
            f.drawRect(10, 10, getWidth() - 20, getHeight() - 20);
            f.setStroke(oldStroke);
        }
        f.setColor(info.foregroundColor);
        File file = ((GraphFrame) parentFrame).getGraph();
        if (info.filename) {
            if (info.pathname)
                f.drawString((file != null) ? file.getAbsolutePath() : "", 20,
                        getHeight() - 45);
            else
                f.drawString((file != null) ? file.getName() : "", 20,
                        getHeight() - 45);
        }
        if (info.date)
            f.drawString(new Date().toString(), 20, getHeight() - 25);
        drawGrid(f);
        drawAllTransitions(f);
        drawAllBoxes(f);
        if (selecting) {
            // here we draw the selection rectangle
            f.drawRect(X_drag, Y_drag, dragWidth, dragHeight);
        }
        return Printable.PAGE_EXISTS;
    }

}
