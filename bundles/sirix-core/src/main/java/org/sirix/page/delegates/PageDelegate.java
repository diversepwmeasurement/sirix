/**
 * Copyright (c) 2011, University of Konstanz, Distributed Systems Group
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the University of Konstanz nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.sirix.page.delegates;

import static com.google.common.base.Preconditions.checkArgument;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import org.sirix.api.PageWriteTrx;
import org.sirix.exception.SirixException;
import org.sirix.page.PageReference;
import org.sirix.page.interfaces.Page;

import com.google.common.base.Objects;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

/**
 * <h1>PageDelegate</h1>
 * 
 * <p>
 * Class to provide basic reference handling functionality.
 * </p>
 */
public class PageDelegate implements Page {

	/** Page references. */
	private PageReference[] mReferences;

	/** Revision of this page. */
	private final int mRevision;

	/** Determines if page is new or changed. */
	private boolean mIsDirty;

	/**
	 * Constructor to initialize instance.
	 * 
	 * @param referenceCount
	 *          number of references of page
	 * @param revision
	 *          revision number
	 */
	public PageDelegate(final @Nonnegative int referenceCount,
			final @Nonnegative int revision) {
		checkArgument(referenceCount >= 0);
		checkArgument(revision >= 0);
		mReferences = new PageReference[referenceCount];
		mRevision = revision;
		mIsDirty = true;
		for (int i = 0; i < referenceCount; i++) {
			mReferences[i] = new PageReference();
		}
	}

	/**
	 * Constructor to initialize instance.
	 * 
	 * @param referenceCount
	 *          number of references of page
	 * @param in
	 *          input stream to read from
	 */
	public PageDelegate(final @Nonnegative int referenceCount,
			final @Nonnull ByteArrayDataInput in) {
		checkArgument(referenceCount >= 0);
		mReferences = new PageReference[referenceCount];
		mRevision = in.readInt();
		mIsDirty = false;
		for (int offset = 0; offset < mReferences.length; offset++) {
			mReferences[offset] = new PageReference();
			mReferences[offset].setKey(in.readLong());
		}
	}

	/**
	 * Constructor to initialize instance.
	 * 
	 * @param commitedPage
	 *          commited page
	 * @param revision
	 *          revision number
	 */
	public PageDelegate(final @Nonnull Page commitedPage,
			final @Nonnegative int revision) {
		checkArgument(revision >= 0);
		mReferences = commitedPage.getReferences();
		mIsDirty = true;
		mRevision = revision;
	}

	/**
	 * Get page reference of given offset.
	 * 
	 * @param offset
	 *          offset of page reference
	 * @return {@link PageReference} at given offset
	 */
	public final PageReference getReference(final @Nonnegative int offset) {
		if (mReferences[offset] == null) {
			mReferences[offset] = new PageReference();
		}
		return mReferences[offset];
	}

	/**
	 * Recursively call commit on all referenced pages.
	 * 
	 * @param pState
	 *          IWriteTransaction state
	 * @throws SirixException
	 *           if a write-error occured
	 */
	@Override
	public final void commit(final @Nonnull PageWriteTrx pageWriteTrx)
			throws SirixException {
		for (final PageReference reference : mReferences) {
			pageWriteTrx.commit(reference);
		}
	}

	/**
	 * Serialize page references into output.
	 * 
	 * @param out
	 *          output stream
	 */
	@Override
	public void serialize(final @Nonnull ByteArrayDataOutput out) {
		out.writeInt(mRevision);
		for (final PageReference reference : mReferences) {
			out.writeLong(reference.getKey());
		}
	}

	/**
	 * Get all references.
	 * 
	 * @return copied references
	 */
	@Override
	public final PageReference[] getReferences() {
		// final PageReference[] copiedRefs = new PageReference[mReferences.length];
		// System.arraycopy(mReferences, 0, copiedRefs, 0, mReferences.length);
		// return copiedRefs;
		return mReferences;
	}

	/**
	 * Get the revision.
	 * 
	 * @return the revision
	 */
	@Override
	public final int getRevision() {
		return mRevision;
	}

	@Override
	public String toString() {
		final Objects.ToStringHelper helper = Objects.toStringHelper(this);
		for (final PageReference ref : mReferences) {
			helper.add("reference", ref);
		}
		return helper.toString();
	}

	@Override
	public boolean isDirty() {
		return mIsDirty;
	}

	@Override
	public Page setDirty(final boolean pDirty) {
		mIsDirty = pDirty;
		return this;
	}

}
