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
import com.github.inpefess.tptp_grpc.tptp_proto.Function;
import com.github.inpefess.tptp_grpc.tptp_proto.Term;
import com.github.inpefess.tptp_grpc.tptp_proto.Variable;
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

  public Function tptp2Proto(Reader reader) throws FileNotFoundException {
    Function.Builder parsedTPTP = Function.newBuilder();
    parsedTPTP.setName("$$$and");
    for (EObject entry : parser.parse(reader).getRootASTElement().eContents()) {
      if (entry instanceof cnf_root) {
        cnf_or clause = ((cnf_root) entry).getExp().getDisjunction();
        parsedTPTP.addArgument(Term.newBuilder().setFunction(transform_clause(clause)));
      }
      if (entry instanceof include) {
        File includedFile = Paths.get(tptpPath, ((include) entry).getPath()).toFile();
        Function includedFunction = tptp2Proto(new FileReader(includedFile));
        parsedTPTP.addAllArgument(includedFunction.getArgumentList());
      }
    }
    return parsedTPTP.build();
  }

  private Term transform_term(cnf_expression term) {
    Term.Builder termProto = Term.newBuilder();
    if (term instanceof cnf_var) {
      Variable.Builder variableProto = Variable.newBuilder();
      variableProto.setName(((cnf_var) term).getName());
      termProto.setVariable(variableProto.build());
    } else {
      Function.Builder functionProto = Function.newBuilder();
      cnf_constant function = (cnf_constant) term;
      functionProto.setName(function.getName());
      for (cnf_expression argument : function.getParam()) {
        functionProto.addArgument(transform_term(argument));
      }
      termProto.setFunction(functionProto.build());
    }
    return termProto.build();
  }

  private Function transform_predicate(cnf_equality predicate) {
    Function.Builder predicateProto = Function.newBuilder();
    cnf_expression rightHandSide = predicate.getExpR();
    cnf_expression leftHandSide = predicate.getExpL();
    if (rightHandSide != null) {
      predicateProto.setName(predicate.getEq());
      predicateProto.addArgument(transform_term(leftHandSide));
      predicateProto.addArgument(transform_term(rightHandSide));
    } else {
      if (leftHandSide instanceof cnf_constant) {
        predicateProto.setName(leftHandSide.getName());
        for (cnf_expression argument : ((cnf_constant) leftHandSide).getParam()) {
          predicateProto.addArgument(transform_term(argument));
        }
      } else {
        predicateProto.setName(leftHandSide.getCnf_exp());
      }
    }
    return predicateProto.build();
  }

  private Function transform_clause(cnf_or clause) {
    Function.Builder clauseProto = Function.newBuilder();
    clauseProto.setName("$$$or");
    for (cnf_not literal : clause.getOr()) {
      Function literalProto = transform_predicate(literal.getLiteral());
      if (literal.isNegated()) {
        Function.Builder negatedLiteral = Function.newBuilder();
        negatedLiteral.setName("$$$not");
        negatedLiteral.addArgument(Term.newBuilder().setFunction(literalProto));
        clauseProto.addArgument(Term.newBuilder().setFunction(negatedLiteral.build()));
      } else {
        clauseProto.addArgument(Term.newBuilder().setFunction(literalProto));
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
      Function parsedTPTP = tptp2Proto.tptp2Proto(new FileReader(problemList.nextLine()));
      parsedTPTP.writeTo(new FileOutputStream(outputFilename));
    }
  }
}
