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
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.util.IncorrectOperationException;
import de.halirutan.mathematica.intentions.IntentionBundle;
import de.halirutan.mathematica.parsing.psi.api.Symbol;
import de.halirutan.mathematica.parsing.psi.util.MathematicaPsiUtilities;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * @author patrick (07.06.17).
 */
public class CreateUsageMessage implements IntentionAction {
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
      final Symbol definition = ((Symbol) elementAtCaret).getResolveElement();
      if (definition != null) {
        if (MathematicaPsiUtilities.isSymbolUsageMessage(definition)) {
          return false;
        }
        final PsiElement[] allUsages = definition.getElementsReferencingToMe();
        for (PsiElement usage : allUsages) {
          if (MathematicaPsiUtilities.isSymbolUsageMessage(usage)) {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {

  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
