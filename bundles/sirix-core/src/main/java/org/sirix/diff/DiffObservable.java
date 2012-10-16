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

package org.sirix.diff;

import javax.annotation.Nonnull;

import org.sirix.diff.DiffFactory.EDiff;
import org.sirix.exception.SirixException;
import org.sirix.node.interfaces.StructNode;

/**
 * Observable class to fire diffs for interested observers, which implement the {@link DiffObserver}
 * interface.
 * 
 * @author Johannes Lichtenberger, University of Konstanz
 * 
 */
interface DiffObservable {
  /**
   * Fire a diff for exactly one node comparsion. Must call the diffListener(EDiff) method defined in the
   * {@link DiffObserver} interface.
   * 
   * @param pDiff
   *          the encountered diff
   * @param pNewNodeKey
   *          node key of current node in new revision
   * @param pOldNodeKey
   *          node key of current node in old revision
   * @param pDepth
   *          current {@link DiffDepth} instance
   */
  void fireDiff(@Nonnull final EDiff pDiff,
    @Nonnull final long pNewNodeKey, @Nonnull final long pOldNodeKey,
    @Nonnull final DiffDepth pDepth);

  /**
   * Diff computation done, thus inform listeners.
   * 
   * @throws SirixException
   *           if closing transactions failes
   */
  void done() throws SirixException;

  /**
   * Add an observer. This means add an instance of a class which implements the {@link DiffObserver}
   * interface.
   * 
   * @param pObserver
   *          instance of the class which implements {@link DiffObserver}.
   */
  void addObserver(@Nonnull final DiffObserver pObserver);

  /**
   * Remove an observer.
   * 
   * @param pObserver
   *          instance of the class which implements {@link DiffObserver}.
   */
  void removeObserver(@Nonnull final DiffObserver pObserver);
}