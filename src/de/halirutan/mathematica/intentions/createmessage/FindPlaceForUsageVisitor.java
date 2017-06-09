/*
 * Copyright (c) 2017 Patrick Scheibe
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package de.halirutan.mathematica.intentions.createmessage;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import de.halirutan.mathematica.parsing.psi.MathematicaVisitor;
import de.halirutan.mathematica.parsing.psi.api.CompoundExpression;
import de.halirutan.mathematica.parsing.psi.api.FunctionCall;
import de.halirutan.mathematica.parsing.psi.api.MessageName;
import de.halirutan.mathematica.parsing.psi.api.assignment.Set;
import de.halirutan.mathematica.parsing.psi.util.MathematicaPsiUtilities;

import javax.annotation.Nullable;

/**
 * @author patrick (09.06.17).
 */
public class FindPlaceForUsageVisitor extends MathematicaVisitor {

  private int myDepth = 0;
  private boolean myFoundBeginPackage = false, myFoundBegin = false, myStopped = false;

  private PsiElement myBeginPackage = null;
  private PsiElement myBegin = null;
  private PsiElement myLastUsage = null;

  @Nullable
  public PsiElement getLastUsageElement() {
    if (myBeginPackage != null) {
      return myLastUsage;
    }
    return null;
  }

  @Override
  public void visitFile(PsiFile file) {
    super.visitFile(file);
    file.acceptChildren(this);
  }

  @Override
  public void visitCompoundExpression(CompoundExpression compoundExpression) {
    if (myDepth == 0) {
      myDepth++;
      compoundExpression.acceptChildren(this);
    }
  }

  @Override
  public void visitFunctionCall(FunctionCall functionCall) {
    if (!myStopped && functionCall.matchesHead("BeginPackage") && !myFoundBegin) {
      myFoundBeginPackage = true;
      myBeginPackage = functionCall;
    } else if (myFoundBeginPackage && !myFoundBegin && functionCall.matchesHead("Begin")) {
      myBegin = functionCall;
      myFoundBegin = true;
      myStopped = true;
    }
  }

  @Override
  public void visitSet(Set set) {
    final PsiElement firstChild = set.getFirstChild();
    if (firstChild instanceof MessageName) {
      firstChild.accept(this);
    }
  }

  @Override
  public void visitMessageName(MessageName messageName) {
    if (!myStopped && myFoundBeginPackage) {
      if (MathematicaPsiUtilities.isUsageMessage(messageName)) {
        myLastUsage = messageName;
      }
    }
  }
}
