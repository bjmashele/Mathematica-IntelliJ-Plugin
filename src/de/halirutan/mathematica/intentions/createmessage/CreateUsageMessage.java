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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.util.IncorrectOperationException;
import de.halirutan.mathematica.intentions.IntentionBundle;
import de.halirutan.mathematica.parsing.psi.SymbolAssignmentType;
import de.halirutan.mathematica.parsing.psi.api.Symbol;
import de.halirutan.mathematica.parsing.psi.util.GlobalDefinitionCollector;
import de.halirutan.mathematica.parsing.psi.util.GlobalDefinitionCollector.AssignmentProperty;
import de.halirutan.mathematica.parsing.psi.util.MathematicaPsiUtilities;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Intention for automatically creating a symbol::usage message when over a function definition that does not already
 * have one.
 * @author patrick (07.06.17).
 */
public class CreateUsageMessage implements IntentionAction {

  private static final Set<SymbolAssignmentType> ourValidAssignments = new HashSet<>(5);

  static {
    ourValidAssignments.add(SymbolAssignmentType.SET_ASSIGNMENT);
    ourValidAssignments.add(SymbolAssignmentType.SET_DELAYED_ASSIGNMENT);
    ourValidAssignments.add(SymbolAssignmentType.TAG_SET_ASSIGNMENT);
    ourValidAssignments.add(SymbolAssignmentType.TAG_SET_DELAYED_ASSIGNMENT);
    ourValidAssignments.add(SymbolAssignmentType.UP_SET_ASSIGNMENT);
    ourValidAssignments.add(SymbolAssignmentType.UP_SET_DELAYED_ASSIGNMENT);
  }

  private AssignmentProperty myFoundAssignment = null;

  @Nls
  @NotNull
  @Override
  public String getText() {
    return IntentionBundle.message("usage.name");
  }

  @Nls
  @NotNull
  @Override
  public String getFamilyName() {
    return IntentionBundle.message("familyName");
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    PsiElement elementAtCaret = PsiUtilBase.getElementAtCaret(editor);
    if (elementAtCaret != null) {
      elementAtCaret = elementAtCaret.getParent();
    } else {
      return false;
    }
    if (elementAtCaret instanceof Symbol) {
      // we know that the element under the caret is a symbol
      // now we need to know if it is indeed a valid definition
      GlobalDefinitionCollector collector = new GlobalDefinitionCollector(file);
      final Map<String, HashSet<AssignmentProperty>> assignments = collector.getAssignments();
      if (!assignments.containsKey(((Symbol) elementAtCaret).getFullSymbolName())) {
        return false;
      }

      // we have collected all definitions of the file and we know that our symbol-name appears in this list
      // now we need to know if the symbol at the caret is one of those places where it gets a definition
      final HashSet<AssignmentProperty> assignment = assignments.get(((Symbol) elementAtCaret).getFullSymbolName());

      // check if there is already a usage definition
      for (AssignmentProperty current : assignment) {
        if (current.myAssignmentType.equals(SymbolAssignmentType.MESSAGE_ASSIGNMENT)) {
          if (MathematicaPsiUtilities.isSymbolUsageMessage(current.myAssignmentSymbol)) {
            return false;
          }
        }
      }

      for (AssignmentProperty current : assignment) {
        if (current.myAssignmentSymbol.equals(elementAtCaret)) {
          // finally, we know the symbol at caret is some form of definition
          // but we don't want to introduce usage messages based on e.g. SyntaxInformation[symbol] definitions
          // so we check if we have a valid Set, SetDelayed, ...
          if (ourValidAssignments.contains(current.myAssignmentType)) {
            myFoundAssignment = current;
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    final Document document = editor.getDocument();
    StripPatternVisitor lhsVisitor = new StripPatternVisitor();
    myFoundAssignment.myLhsOfAssignment.accept(lhsVisitor);
    final PsiElement myAssignmentSymbol = myFoundAssignment.myAssignmentSymbol;
    StringBuilder usage = new StringBuilder("\n" + myAssignmentSymbol.getText());
    usage.append("::usage = \"");
    usage.append(lhsVisitor.getCleanedDefinition());
    usage.append(" \";\n");
    if (document.isWritable()) {
      int offsetToInsert;
      FindPlaceForUsageVisitor placeVisitor = new FindPlaceForUsageVisitor();
      file.accept(placeVisitor);
      final PsiElement lastUsageElement = placeVisitor.getLastUsageElement();
      if (lastUsageElement != null) {
        final int lineNumber = document.getLineNumber(lastUsageElement.getTextOffset());
        offsetToInsert = document.getLineEndOffset(lineNumber);
      } else {
        offsetToInsert = myAssignmentSymbol.getTextOffset();
      }
      document.insertString(offsetToInsert, usage);
      final CaretModel caretModel = editor.getCaretModel();
      caretModel.moveToOffset(offsetToInsert + usage.length() - 3);
      final ScrollingModel scrollingModel = editor.getScrollingModel();
      scrollingModel.scrollToCaret(ScrollType.CENTER);
    }
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
