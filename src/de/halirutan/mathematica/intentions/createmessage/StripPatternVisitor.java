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

import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import de.halirutan.mathematica.parsing.psi.MathematicaRecursiveVisitor;
import de.halirutan.mathematica.parsing.psi.api.FunctionCall;
import de.halirutan.mathematica.parsing.psi.api.Symbol;
import de.halirutan.mathematica.parsing.psi.api.pattern.*;
import de.halirutan.mathematica.parsing.psi.api.string.MString;

import static de.halirutan.mathematica.parsing.MathematicaElementTypes.*;

/**
 * @author patrick (08.06.17).
 */
public class StripPatternVisitor extends MathematicaRecursiveVisitor {
  private static final TokenSet ourInsertIfEmpty;

  static {
    ourInsertIfEmpty = TokenSet.create(
        BLANK,
        BLANK_SEQUENCE,
        BLANK_NULL_SEQUENCE
    );
  }

  private final StringBuilder myCleanedDefinition = new StringBuilder();

  String getCleanedDefinition() {
    return myCleanedDefinition.toString();
  }

  @Override
  public void visitElement(PsiElement element) {
    ProgressIndicatorProvider.checkCanceled();
    insertAsText(element);
  }

  @Override
  public void visitFunctionCall(FunctionCall functionCall) {
    final PsiElement[] children = functionCall.getChildren();
    functionCall.getHead().accept(this);
    myCleanedDefinition.append("[");
    for (int i = 1; i < children.length; i++) {
      children[i].accept(this);
      if (i < children.length - 1) {
        myCleanedDefinition.append(", ");
      }
    }
    myCleanedDefinition.append("]");
  }

  @Override
  public void visitAlternative(Alternative alternative) {
    final PsiElement[] children = alternative.getChildren();
    for (int i = 0; i < children.length; i++) {
      children[i].accept(this);
      if (i < children.length - 1) {
        myCleanedDefinition.append(" | ");
      }
    }
  }

  @Override
  public void visitSymbol(Symbol symbol) {
    myCleanedDefinition.append(symbol.getFullSymbolName());
  }

  @Override
  public void visitPattern(Pattern pattern) {
    visitFirstChild(pattern);
  }

  @Override
  public void visitPatternTest(PatternTest patternTest) {
    visitFirstChild(patternTest);
  }

  @Override
  public void visitBlank(Blank blank) {
    visitFirstChild(blank);
  }

  @Override
  public void visitBlankSequence(BlankSequence blankSequence) {
    visitFirstChild(blankSequence);
  }

  @Override
  public void visitBlankNullSequence(BlankNullSequence blankNullSequence) {
    visitFirstChild(blankNullSequence);
  }

  @Override
  public void visitRepeated(Repeated repeated) {
    visitFirstChild(repeated);
  }

  @Override
  public void visitRepeatedNull(RepeatedNull repeatedNull) {
    visitFirstChild(repeatedNull);
  }

  @Override
  public void visitString(MString mString) {
    myCleanedDefinition.append(mString.getText().replace("\"", "\\\""));
  }


  private void visitFirstChild(PsiElement element) {
    final PsiElement firstChild = element.getFirstChild();
    if (firstChild != null) {
      if (ourInsertIfEmpty.contains(firstChild.getNode().getElementType())) {
        myCleanedDefinition.append(firstChild.getText());
      } else {
        firstChild.accept(this);
      }
    }
  }

  private void insertAsText(PsiElement element) {
    if (element != null) {
      myCleanedDefinition.append(element.getText());
    }
  }

}
