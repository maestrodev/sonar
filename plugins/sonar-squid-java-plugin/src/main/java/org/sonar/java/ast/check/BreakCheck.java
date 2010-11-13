package org.sonar.java.ast.check;

import java.util.Arrays;
import java.util.List;

import org.sonar.check.IsoCategory;
import org.sonar.check.Rule;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

@Rule(key = "BreakCheck", isoCategory = IsoCategory.Maintainability)
public class BreakCheck extends JavaAstCheck {

  private boolean insideSwitch = false;

  @Override
  public List<Integer> getWantedTokens() {
    return wantedTokens;
  }

  @Override
  public void visitToken(DetailAST ast) {
    if (ast.getType() == TokenTypes.LITERAL_SWITCH) {
      insideSwitch = true;
    }
    if ((ast.getType() == TokenTypes.LITERAL_BREAK) && !insideSwitch) {
      CheckMessage message = new CheckMessage(this, "Avoid usage of break outside switch statement");
      message.setLine(ast.getLineNo());
      SourceFile sourceFile = peekSourceCode().getParent(SourceFile.class);
      sourceFile.log(message);
    }
  }

  @Override
  public void leaveToken(DetailAST ast) {
    if (ast.getType() == TokenTypes.LITERAL_SWITCH) {
      insideSwitch = false;
    }
  }

  private static final List<Integer> wantedTokens = Arrays.asList(TokenTypes.LITERAL_BREAK, TokenTypes.LITERAL_SWITCH);

}