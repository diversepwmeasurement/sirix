package org.sirix.axis.visitor;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.sirix.access.AbstractVisitor;
import org.sirix.api.NodeWriteTrx;
import org.sirix.api.visitor.VisitResultType;
import org.sirix.api.visitor.VisitResult;
import org.sirix.axis.DescendantAxis;
import org.sirix.exception.SirixException;
import org.sirix.node.Kind;
import org.sirix.node.immutable.ImmutableElement;
import org.sirix.node.immutable.ImmutableText;
import org.sirix.node.interfaces.StructNode;
import org.sirix.utils.LogWrapper;
import org.slf4j.LoggerFactory;

/**
 * Visitor implementation for use with the {@link VisitorDescendantAxis} to
 * modify nodes.
 * 
 * @author Johannes Lichtenberger, University of Konstanz
 * 
 */
public final class ModificationVisitor extends AbstractVisitor {

	/** {@link LogWrapper} reference. */
	private static final LogWrapper LOGWRAPPER = new LogWrapper(
			LoggerFactory.getLogger(ModificationVisitor.class));

	/** Determines the modify rate. */
	private static final int MODIFY_EVERY = 1111;

	/** Sirix {@link NodeWriteTrx}. */
	private final NodeWriteTrx mWtx;

	/** Random number generator. */
	private final Random mRandom = new Random();

	/** Start key. */
	private final long mStartKey;

	/** Current node index, that is every node is indexed starting at 1. */
	private long mNodeIndex;

	/**
	 * Constructor.
	 * 
	 * @param pWtx
	 *          sirix {@link NodeWriteTrx}
	 * @param pStartKey
	 *          start key
	 */
	public ModificationVisitor(final NodeWriteTrx pWtx, final long pStartKey) {
		mWtx = checkNotNull(pWtx);
		checkArgument(pStartKey >= 0, "start key must be >= 0!");
		mStartKey = pStartKey;
		mNodeIndex = 1;
	}

	@Override
	public VisitResult visit(final @Nonnull ImmutableElement pNode) {
		return processNode(pNode);
	}

	/**
	 * Process a node, that is decide if it has to be deleted and move
	 * accordingly.
	 * 
	 * @param pNode
	 *          the node to check
	 * @return the appropriate {@link VisitResultType} value
	 */
	private VisitResult processNode(final StructNode pNode) {
		assert pNode != null;
		final VisitResult result = modify(pNode);
		if (pNode.getNodeKey() == mStartKey) {
			return VisitResultType.TERMINATE;
		}
		return result;
	}

	@Override
	public VisitResult visit(final @Nonnull ImmutableText pNode) {
		return processNode(pNode);
	}

	/**
	 * Determines if a node must be modified. If yes, it is deleted and
	 * {@code true} is returned. If it must not be deleted {@code false} is
	 * returned. The transaction is moved accordingly in case of a
	 * remove-operation such that the {@link DescendantAxis} can move to the next
	 * node after a delete occurred.
	 * 
	 * @param pNode
	 *          the node to check and possibly delete
	 * @return {@code true} if node has been deleted, {@code false} otherwise
	 */
	private VisitResult modify(final StructNode pNode) {
		assert pNode != null;
		if (mNodeIndex % MODIFY_EVERY == 0) {
			mNodeIndex = 1;

			try {
				switch (mRandom.nextInt(4)) {
				case 0:
					final QName insert = new QName("testInsert");
					final long key = mWtx.getNodeKey();
					mWtx.insertElementAsLeftSibling(insert);
					boolean moved = mWtx.moveTo(key).hasMoved();
					assert moved;
					return VisitResultType.CONTINUE;
				case 1:
					if (mWtx.getKind() == Kind.TEXT) {
						mWtx.setValue("testUpdate");
					} else if (mWtx.getKind() == Kind.ELEMENT) {
						mWtx.setQName(new QName("testUpdate"));
					}
					return VisitResultType.CONTINUE;
				case 2:
					return delete();
				case 3:
					mWtx.replaceNode("<foo/>");
					return VisitResultType.CONTINUE;
				}
			} catch (final SirixException | IOException | XMLStreamException e) {
				LOGWRAPPER.error(e.getMessage(), e);
				return VisitResultType.TERMINATE;
			}
		} else {
			mNodeIndex++;
			return VisitResultType.CONTINUE;
		}
		return VisitResultType.CONTINUE;
	}

	/** Delete a subtree and determine movement. */
	private VisitResult delete() throws SirixException {
		try {
			final long nodeKey = mWtx.getNodeKey();
			boolean removeTextNode = false;
			if (mWtx.getLeftSiblingKind() == Kind.TEXT
					&& mWtx.getRightSiblingKind() == Kind.TEXT) {
				removeTextNode = true;
			}
			mWtx.moveTo(nodeKey);

			// Case: Has no right and no left sibl. but the parent has a right sibl.
			if (!removeTextNode) {
				final boolean movedToParent = mWtx.moveToParent().hasMoved();
				assert movedToParent;
				final long parentNodeKey = mWtx.getNodeKey();
				if (mWtx.getChildCount() == 1 && mWtx.hasRightSibling()) {
					mWtx.moveTo(nodeKey);
					mWtx.remove();
					assert mWtx.getNodeKey() == parentNodeKey;
					return LocalVisitResult.SKIPSUBTREEPOPSTACK;
				}
			}
			mWtx.moveTo(nodeKey);

			// Case: Has left sibl. but no right sibl.
			if (!mWtx.hasRightSibling() && mWtx.hasLeftSibling()) {
				final long leftSiblKey = mWtx.getLeftSiblingKey();
				mWtx.remove();
				assert mWtx.getNodeKey() == leftSiblKey;
				return VisitResultType.SKIPSUBTREE;
			}

			// Case: Has right sibl. and left sibl.
			if (mWtx.hasRightSibling() && mWtx.hasLeftSibling()) {
				final long rightSiblKey = mWtx.getRightSiblingKey();
				final long rightRightSiblKey = mWtx.moveToRightSibling().get()
						.getRightSiblingKey();
				mWtx.moveTo(nodeKey);
				mWtx.remove();
				if (removeTextNode) {
					assert mWtx.getKind() == Kind.TEXT;
					assert mWtx.getRightSiblingKey() == rightRightSiblKey;
					return VisitResultType.CONTINUE;
				} else {
					final boolean moved = mWtx.moveToLeftSibling().hasMoved();
					assert moved;
					assert mWtx.getRightSiblingKey() == rightSiblKey;
					return VisitResultType.SKIPSUBTREE;
				}
			}

			// Case: Has right sibl. but no left sibl.
			if (mWtx.hasRightSibling() && !mWtx.hasLeftSibling()) {
				final long rightSiblKey = mWtx.getRightSiblingKey();
				mWtx.remove();
				mWtx.moveToParent();
				assert mWtx.getFirstChildKey() == rightSiblKey;
				return VisitResultType.CONTINUE;
			}

			// Case: Has no right and no left sibl.
			final long parentKey = mWtx.getParentKey();
			mWtx.remove();
			assert mWtx.getNodeKey() == parentKey;
		} catch (final SirixException e) {
			LOGWRAPPER.error(e.getMessage(), e);
		}
		return VisitResultType.CONTINUE;
	}
}
