/*
 *  Copyright 2023 Boris Shminke
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.inpefess.tptpgrpc.tptp2proto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import com.github.inpefess.tptpgrpc.tptpproto.Node;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.theoremsandstuff.tptp.ParserRuntimeModule;
import com.theoremsandstuff.tptp.ParserStandaloneSetup;
import com.theoremsandstuff.tptp.parser.cnf_constant;
import com.theoremsandstuff.tptp.parser.cnf_equality;
import com.theoremsandstuff.tptp.parser.cnf_expression;
import com.theoremsandstuff.tptp.parser.cnf_not;
import com.theoremsandstuff.tptp.parser.cnf_or;
import com.theoremsandstuff.tptp.parser.cnf_root;
import com.theoremsandstuff.tptp.parser.cnf_var;
import com.theoremsandstuff.tptp.parser.include;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.parser.IParser;

public class Tptp2Proto {
  @Inject
  private IParser parser;
  Injector injector = Guice.createInjector(new ParserRuntimeModule());
  private String tptpPath;

  public Tptp2Proto(String tptpPath) {
    this.tptpPath = tptpPath;
    setupParser();
  }

  public Node tptp2Proto(Reader reader) throws FileNotFoundException {
    Node.Builder parsedTptp = Node.newBuilder();
    Set<String> variableNames = new HashSet<>();
    Set<String> functionAndPredicateNames = new HashSet<>();
    parsedTptp.setValue("&");
    for (EObject entry : parser.parse(reader).getRootASTElement().eContents()) {
      if (entry instanceof cnf_root) {
        cnf_or clause = ((cnf_root) entry).getExp().getDisjunction();
        ParsingResult parsingResult = transform_clause(clause);
        parsedTptp.addChild(parsingResult.nodeBuilder.build());
        variableNames.addAll(parsingResult.variableNames);
        functionAndPredicateNames.addAll(parsingResult.functionAndPredicateNames);
      }
      if (entry instanceof include) {
        File includedFile = Paths.get(tptpPath, ((include) entry).getPath()).toFile();
        Node includedNode = tptp2Proto(new FileReader(includedFile));
        parsedTptp.addAllChild(includedNode.getChildList());
      }
    }
    Node.Builder universalQuantifier = Node.newBuilder();
    universalQuantifier.setValue("?");
    for (String functionOrPredicateName : functionAndPredicateNames) {
      Node.Builder functionOrPredicate = Node.newBuilder();
      functionOrPredicate.setValue(functionOrPredicateName);
      universalQuantifier.addChild(functionOrPredicate.build());
    }
    universalQuantifier.addChild(parsedTptp.build());
    return universalQuantifier.build();
  }

  private ParsingResult transform_term(cnf_expression term) {
    ParsingResult parsedTerm = ParsingResult.emptyParsingResult();
    if (term instanceof cnf_var) {
      String variableName = ((cnf_var) term).getName();
      parsedTerm.addVariable(variableName);
    } else {
      cnf_constant function = (cnf_constant) term;
      parsedTerm.addFunctionOrPredicate(function.getName());
      for (cnf_expression argument : function.getParam()) {
        parsedTerm.addChild(transform_term(argument));
      }
    }
    return parsedTerm;
  }

  private ParsingResult transform_predicate(cnf_equality predicate) {
    ParsingResult parsedPredicate = ParsingResult.emptyParsingResult();
    if (predicate.getExpR() != null) {
      parsedPredicate.nodeBuilder.setValue(predicate.getEq());
      parsedPredicate.addChild(transform_term(predicate.getExpL()));
      parsedPredicate.addChild(transform_term(predicate.getExpR()));
    } else {
      if (predicate.getExpL() instanceof cnf_constant) {
        parsedPredicate.addFunctionOrPredicate(predicate.getExpL().getName());
        for (cnf_expression argument : ((cnf_constant) predicate.getExpL()).getParam()) {
          parsedPredicate.addChild(transform_term(argument));
        }
      } else {
        parsedPredicate.nodeBuilder.setValue(predicate.getExpL().getCnf_exp());
      }
    }
    return parsedPredicate;
  }

  private ParsingResult transform_clause(cnf_or clause) {
    ParsingResult parsedClause = ParsingResult.emptyParsingResult();
    parsedClause.nodeBuilder.setValue("|");
    for (cnf_not literal : clause.getOr()) {
      ParsingResult parsedLiteral = transform_predicate(literal.getLiteral());
      if (literal.isNegated()) {
        Node.Builder negatedLiteral = Node.newBuilder();
        negatedLiteral.setValue("~");
        negatedLiteral.addChild(parsedLiteral.nodeBuilder.build());
        parsedClause.addChild(new ParsingResult(negatedLiteral, parsedLiteral.variableNames,
            parsedLiteral.functionAndPredicateNames));
      } else {
        parsedClause.addChild(parsedLiteral);
      }
    }
    return new ParsingResult(
        quantify(parsedClause.nodeBuilder.build(), "!", parsedClause.variableNames),
        new HashSet<>(), parsedClause.functionAndPredicateNames);
  }

  private Node.Builder quantify(Node node, String quantor, Set<String> symbolNames) {
    Node.Builder universalQuantifier = Node.newBuilder();
    universalQuantifier.setValue(quantor);
    for (String symbolName : symbolNames) {
      Node.Builder variable = Node.newBuilder();
      variable.setValue(symbolName);
      universalQuantifier.addChild(variable.build());
    }
    universalQuantifier.addChild(node);
    return universalQuantifier;
  }

  private void setupParser() {
    Injector injector = new ParserStandaloneSetup().createInjectorAndDoEMFRegistration();
    injector.injectMembers(this);
  }

  public static void main(String[] args) throws IOException {
    Tptp2Proto tptp2Proto = new Tptp2Proto(args[0]);
    Scanner problemList = new Scanner(new FileInputStream(args[1]));
    int fileIndex = 0;
    while (problemList.hasNextLine()) {
      String outputFilename = Paths.get(args[2], fileIndex++ + ".pb").toString();
      Node parsedTptp = tptp2Proto.tptp2Proto(new FileReader(problemList.nextLine()));
      parsedTptp.writeTo(new FileOutputStream(outputFilename));
    }
  }
}
