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
package com.github.inpefess.tptp_grpc.tptp2proto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.Scanner;
import com.github.inpefess.tptp_grpc.tptp_proto.Node;
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

public class TPTP2Proto {
  @Inject
  private IParser parser;
  Injector injector = Guice.createInjector(new ParserRuntimeModule());
  private String tptpPath;

  public TPTP2Proto(String tptpPath) {
    this.tptpPath = tptpPath;
    setupParser();
  }

  public Node tptp2Proto(Reader reader) throws FileNotFoundException {
    Node.Builder parsedTPTP = Node.newBuilder();
    parsedTPTP.setValue("&");
    for (EObject entry : parser.parse(reader).getRootASTElement().eContents()) {
      if (entry instanceof cnf_root) {
        cnf_or clause = ((cnf_root) entry).getExp().getDisjunction();
        parsedTPTP.addChild(transform_clause(clause));
      }
      if (entry instanceof include) {
        File includedFile = Paths.get(tptpPath, ((include) entry).getPath()).toFile();
        Node includedNode = tptp2Proto(new FileReader(includedFile));
        parsedTPTP.addAllChild(includedNode.getChildList());
      }
    }
    return parsedTPTP.build();
  }

  private Node transform_term(cnf_expression term) {
    Node.Builder termProto = Node.newBuilder();
    if (term instanceof cnf_var) {
      termProto.setValue(((cnf_var) term).getName());
    } else {
      cnf_constant function = (cnf_constant) term;
      termProto.setValue(function.getName());
      for (cnf_expression argument : function.getParam()) {
        termProto.addChild(transform_term(argument));
      }
    }
    return termProto.build();
  }

  private Node transform_predicate(cnf_equality predicate) {
    Node.Builder predicateProto = Node.newBuilder();
    cnf_expression rightHandSide = predicate.getExpR();
    cnf_expression leftHandSide = predicate.getExpL();
    if (rightHandSide != null) {
      predicateProto.setValue(predicate.getEq());
      predicateProto.addChild(transform_term(leftHandSide));
      predicateProto.addChild(transform_term(rightHandSide));
    } else {
      if (leftHandSide instanceof cnf_constant) {
        predicateProto.setValue(leftHandSide.getName());
        for (cnf_expression argument : ((cnf_constant) leftHandSide).getParam()) {
          predicateProto.addChild(transform_term(argument));
        }
      } else {
        predicateProto.setValue(leftHandSide.getCnf_exp());
      }
    }
    return predicateProto.build();
  }

  private Node transform_clause(cnf_or clause) {
    Node.Builder clauseProto = Node.newBuilder();
    clauseProto.setValue("|");
    for (cnf_not literal : clause.getOr()) {
      Node literalProto = transform_predicate(literal.getLiteral());
      if (literal.isNegated()) {
        Node.Builder negatedLiteral = Node.newBuilder();
        negatedLiteral.setValue("~");
        negatedLiteral.addChild(literalProto);
        clauseProto.addChild(negatedLiteral.build());
      } else {
        clauseProto.addChild(literalProto);
      }
    }
    return clauseProto.build();
  }

  private void setupParser() {
    Injector injector = new ParserStandaloneSetup().createInjectorAndDoEMFRegistration();
    injector.injectMembers(this);
  }

  public static void main(String[] args) throws IOException {
    TPTP2Proto tptp2Proto = new TPTP2Proto(args[0]);
    Scanner problemList = new Scanner(new FileInputStream(args[1]));
    int fileIndex = 0;
    while (problemList.hasNextLine()) {
      String outputFilename = Paths.get(args[2], fileIndex++ + ".pb").toString();
      Node parsedTPTP = tptp2Proto.tptp2Proto(new FileReader(problemList.nextLine()));
      parsedTPTP.writeTo(new FileOutputStream(outputFilename));
    }
  }
}
