

package ac.soton.eventb.atomicitydecomposition.design.layout;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gmf.runtime.common.core.service.IOperation;
import org.eclipse.gmf.runtime.diagram.ui.providers.TopDownProvider;
import org.eclipse.gmf.runtime.diagram.ui.services.layout.ILayoutNode;
import org.eclipse.gmf.runtime.diagram.ui.services.layout.ILayoutNodeOperation;
import org.eclipse.gmf.runtime.notation.Bounds;
import org.eclipse.gmf.runtime.notation.Node;
import org.eclipse.sirius.diagram.DNode;
import org.eventb.emf.core.machine.Machine;

import ac.soton.eventb.atomicitydecomposition.FlowDiagram;
import ac.soton.eventb.atomicitydecomposition.Leaf;
import ac.soton.eventb.atomicitydecomposition.TypedParameterExpression;


/**
 * Provides the layout algorithm for the Sirius based ERS Diagram Editor.
 * This Class thus describes the way that graphical element are organized in an ERS Diagram.
 * Its implementation is heavily inspired by the layout algorithm that existed for the GMF-based diagram editor.
 * @see ac.soton.eventb.atomicitydecomposition.diagram.layout.SquareLayoutProvider
 */
public class ERSDiagramLayoutProvider extends TopDownProvider{

	/**
	 * Vertical spacing (in levels) left free to display the root of the tree
	 */
	protected static final Float INITIAL_ROOT_SPACING = (float) 1.5;
	 
	@Override
	public boolean provides(IOperation operation) {
		
		if (operation instanceof ILayoutNodeOperation) {
			Iterator nodes = ((ILayoutNodeOperation)operation).getLayoutNodes().listIterator();
			
			if (nodes.hasNext()) { 
				//we access any node of the diagram to check that its linked semantic element is contained in a FlowDiagram
				Node node = ((ILayoutNode)nodes.next()).getNode();
				if(node.getElement() instanceof DNode) {
					EList<EObject> linkedSemanticElements = ((DNode) node.getElement()).getSemanticElements();
					if(linkedSemanticElements != null && linkedSemanticElements.size() > 0) {
						EObject semanticElement = linkedSemanticElements.get(0);
						//we check that the container of the semantic element is a FlowDiagram
						return semanticElement.eContainer() instanceof FlowDiagram;
					}
				}
			} 
		} //else
		
		return false;
	}
	
	@Override
	public Runnable layoutLayoutNodes(List layoutNodes,
			boolean offsetFromBoundingBox, IAdaptable layoutHint) {
		
		final List lnodes = layoutNodes;
		
		return new Runnable() {
			
			private HashMap<EObject, ILayoutNode> object2node;
			//private HashMap<EObject, Float> object2offset;
			private EObject topLevelElement;
			private int spaceX = 20;
			private int spaceY = 80;
			
			public void run() {
				object2node = new HashMap<EObject, ILayoutNode>();
				//object2offset = new HashMap<EObject, Float>();
				int topLevel = -1;
				ListIterator li = lnodes.listIterator();
				while (li.hasNext()) {
					ILayoutNode lnode = (ILayoutNode)li.next();
					EObject semanticElement = null;
					if(lnode.getNode().getElement() instanceof DNode) {
						EList<EObject> linkedSemanticElements = ((DNode) lnode.getNode().getElement()).getSemanticElements();
						if(linkedSemanticElements != null && linkedSemanticElements.size() > 0) {
							//we get the first (and only) semantic element linked to this DNode
							semanticElement = linkedSemanticElements.get(0);
							object2node.put(semanticElement, lnode);
							
							int level = findLevel(semanticElement);
							if(level < topLevel || topLevel == -1){
								topLevel = level;
								topLevelElement = semanticElement;
							}
						}
					}//else we ignore the node				
				}
				positionTree(topLevelElement);
			}
			
			/**
			 * Returns the level of a semantic element in the hierarchy of the tree. <br>
			 * The root element (the root FlowDiagram in the case of an ERS Diagram) 
			 * is considered to be of level 1, its direct children of level 2, and so on. <br>
			 * This level is then used to display elements as a tree.
			 * @param eobj semantic element
			 * @return level of a semantic element in the hierarchy of the tree.
			 */
			private int findLevel(EObject eobj){
				if(eobj.eContainer() == null || eobj.eContainer() instanceof Machine) {
					return 1;
				} else {
					int level = findLevel(eobj.eContainer()) + 1;
					return level;
				}
			}
						
			private void positionTree(EObject obj){
				Float offset = (float)30;
				for(EObject eobj : obj.eContents()){
					if(object2node.get(eobj) == null) {
						continue;
					}
					System.out.println("eobj : "+eobj);
					offset = positionSubtree(eobj, offset, INITIAL_ROOT_SPACING);
					System.out.println("new offset : "+offset);
				}
			}
			
			
			private Float positionSubtree(EObject obj, Float Xoffset, Float level){
				System.out.println("positionning : "+obj);
				ILayoutNode ln = object2node.get(obj);
				
				Float initialXOffset = Xoffset;
				
				if(obj instanceof Leaf && obj.eContainer() instanceof FlowDiagram &&
						!(obj.eContainer().equals(topLevelElement))) {
					level += 1;
				}
				
				for(EObject eobj : obj.eContents()){
					if(eobj instanceof TypedParameterExpression || object2node.get(eobj) == null) {
						continue;
					}
					Xoffset = positionSubtree(eobj, Xoffset, level + 1);
				}
				
				Bounds bounds = (Bounds)ln.getNode().getLayoutConstraint();
				if(obj.eContents().size() == 0 || (obj.eContents().size() == 1 && obj.eContents().size() == 0)) {
					bounds.setX( (int) Math.round(initialXOffset)) ;
				} else {
					bounds.setX(  (int) (Math.round(initialXOffset + (Xoffset - initialXOffset)/2 - (object2node.get(obj).getWidth()/2)) > initialXOffset ? (int) Math.round(initialXOffset + (Xoffset - initialXOffset)/2 - (object2node.get(obj).getWidth()/2)) : initialXOffset) ) ;
				}

				bounds.setY( (int) Math.round(level * spaceY));
				ln.getNode().setLayoutConstraint(bounds);

				if(obj.eContents().size() == 0  || Math.round(initialXOffset + (Xoffset - initialXOffset)/2 - (object2node.get(obj).getWidth()/2)) < initialXOffset ) {
					return initialXOffset + object2node.get(obj).getWidth() + spaceX;
				}
				
				Float abstractEnd = initialXOffset + (Xoffset - initialXOffset)/2 - (object2node.get(obj).getWidth()/2) 
						+ object2node.get(obj).getWidth() + spaceX;
				Float subtreeEnd = Xoffset;
				
				return abstractEnd > subtreeEnd ? abstractEnd : subtreeEnd;
				
			}
					
		};
	}
}

	