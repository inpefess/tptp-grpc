/*
 *  Copyright 2023 Boris Shminke
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/

package io.github.inpefess.tptpgrpc.tptp2proto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
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
import io.github.inpefess.tptpgrpc.tptpproto.Node;


/**
 * Parses TPTP problems into protobuf objects.
 */
public final class Tptp2Proto {
  @Inject
  private IParser parser;
  Injector injector = Guice.createInjector(new ParserRuntimeModule());
  private final String tptpPath;

  /**
   * Constructor.
   *
   * @param tptpPath the absolute path to the TPTP root folder
   */
  public Tptp2Proto(final String tptpPath) {
    this.tptpPath = tptpPath;
    setupParser();
  }

  /**
   * Parse a TPTP problem into protobuf.
   *
   * @param reader a reader of a TPTP problem
   * @return a protobuf object representing the parsed TPTP problem
   * @throws IOException if encounters errors when reading the problem
   */
  public final Node tptp2Proto(final Reader reader) throws IOException {
    final ParsingResult parsedTptp = ParsingResult.emptyParsingResult();
    parsedTptp.nodeBuilder.setValue("&");
    for (final EObject entry : parser.parse(reader).getRootASTElement().eContents()) {
      if (entry instanceof cnf_root) {
        final cnf_or clause = ((cnf_root) entry).getExp().getDisjunction();
        parsedTptp.addChild(transform_clause(clause));
      }
      if (entry instanceof include) {
        parseInclude(parsedTptp, (include) entry);
      }
    }
    return quantify(parsedTptp.nodeBuilder.build(), "?", parsedTptp.functionAndPredicateNames)
        .build();
  }

  private final void parseInclude(final ParsingResult parsedTptp, final include entry)
      throws IOException {
    final File includedFile = Paths.get(tptpPath, entry.getPath()).toFile();
    try (final FileReader includedFileReader = new FileReader(includedFile)) {
      final List<Node> includedEntries = tptp2Proto(includedFileReader).getChildList();
      final int symbolCount = includedEntries.size() - 1;
      for (int i = 0; i < symbolCount; i++) {
        parsedTptp.functionAndPredicateNames.add(includedEntries.get(i).getValue());
      }
      parsedTptp.nodeBuilder.addAllChild(includedEntries.get(symbolCount).getChildList());
    }
  }

  private final ParsingResult transform_term(final cnf_expression term) {
    final ParsingResult parsedTerm = ParsingResult.emptyParsingResult();
    if (term instanceof cnf_var) {
      final String variableName = ((cnf_var) term).getName();
      parsedTerm.addVariable(variableName);
    } else {
      final cnf_constant function = (cnf_constant) term;
      parsedTerm.addFunctionOrPredicate(function.getName());
      for (final cnf_expression argument : function.getParam()) {
        parsedTerm.addChild(transform_term(argument));
      }
    }
    return parsedTerm;
  }

  private final ParsingResult transform_predicate(final cnf_equality predicate) {
    final ParsingResult parsedPredicate = ParsingResult.emptyParsingResult();
    if (predicate.getExpR() != null) {
      parsedPredicate.nodeBuilder.setValue(predicate.getEq());
      parsedPredicate.addChild(transform_term(predicate.getExpL()));
      parsedPredicate.addChild(transform_term(predicate.getExpR()));
    } else {
      if (predicate.getExpL() instanceof cnf_constant) {
        parsedPredicate.addFunctionOrPredicate(predicate.getExpL().getName());
        for (final cnf_expression argument : ((cnf_constant) predicate.getExpL()).getParam()) {
          parsedPredicate.addChild(transform_term(argument));
        }
      } else {
        parsedPredicate.nodeBuilder.setValue(predicate.getExpL().getCnf_exp());
      }
    }
    return parsedPredicate;
  }

  private final ParsingResult transform_clause(final cnf_or clause) {
    final ParsingResult parsedClause = ParsingResult.emptyParsingResult();
    parsedClause.nodeBuilder.setValue("|");
    for (final cnf_not literal : clause.getOr()) {
      final ParsingResult parsedLiteral = transform_predicate(literal.getLiteral());
      if (literal.isNegated()) {
        final Node.Builder negatedLiteral = Node.newBuilder();
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

  private final Node.Builder quantify(final Node node, final String quantor,
      final Set<String> symbolNames) {
    final Node.Builder universalQuantifier = Node.newBuilder();
    universalQuantifier.setValue(quantor);
    for (final String symbolName : symbolNames) {
      final Node.Builder variable = Node.newBuilder();
      variable.setValue(symbolName);
      universalQuantifier.addChild(variable.build());
    }
    universalQuantifier.addChild(node);
    return universalQuantifier;
  }

  private final void setupParser() {
    final Injector injector = new ParserStandaloneSetup().createInjectorAndDoEMFRegistration();
    injector.injectMembers(this);
  }

  public static final void main(final String[] args) throws IOException {
    final Tptp2Proto tptp2Proto = new Tptp2Proto(args[0]);
    try (final FileInputStream fileInputStream = new FileInputStream(args[1]);
        final Scanner problemList = new Scanner(fileInputStream)) {
      int fileIndex = 0;
      while (problemList.hasNextLine()) {
        try (FileReader problemReader = new FileReader(problemList.nextLine())) {
          final Node parsedTptp = tptp2Proto.tptp2Proto(problemReader);
          final String outputFilename = Paths.get(args[2], fileIndex++ + ".pb").toString();
          try (FileOutputStream outputProtobufFile = new FileOutputStream(outputFilename)) {
            parsedTptp.writeTo(outputProtobufFile);
          }
        }
      }
    }
  }
}
