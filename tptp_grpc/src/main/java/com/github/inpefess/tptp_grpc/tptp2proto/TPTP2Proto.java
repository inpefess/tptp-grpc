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

import java.io.StringReader;
import com.github.inpefess.tptp_grpc.tptp_proto.TPTPCNF;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.theoremsandstuff.tptp.ParserRuntimeModule;
import com.theoremsandstuff.tptp.ParserStandaloneSetup;
import com.theoremsandstuff.tptp.parser.Model;
import com.theoremsandstuff.tptp.parser.cnf_constant;
import com.theoremsandstuff.tptp.parser.cnf_equality;
import com.theoremsandstuff.tptp.parser.cnf_expression;
import com.theoremsandstuff.tptp.parser.cnf_not;
import com.theoremsandstuff.tptp.parser.cnf_or;
import com.theoremsandstuff.tptp.parser.cnf_var;
import org.eclipse.xtext.parser.IParser;

public class TPTP2Proto {
  @Inject
  private IParser parser;
  Injector injector = Guice.createInjector(new ParserRuntimeModule());

  public TPTP2Proto() {
    setupParser();
  }

  public TPTPCNF tptpCNF2Proto(String tptpCNFString) {
    Model result = (Model) parser.parse(new StringReader(tptpCNFString)).getRootASTElement();
    cnf_or clause = ((cnf_or) result.getTPTP_input().get(0).eContents().get(0).eContents().get(0));
    return transform_clause(clause);
  }

  private TPTPCNF.Term transform_term(cnf_expression term) {
    TPTPCNF.Term.Builder termProto = TPTPCNF.Term.newBuilder();
    if (term instanceof cnf_var) {
      TPTPCNF.Variable.Builder variableProto = TPTPCNF.Variable.newBuilder();
      variableProto.setName(((cnf_var) term).getName());
      termProto.setVariable(variableProto.build());
    } else {
      TPTPCNF.Function.Builder functionProto = TPTPCNF.Function.newBuilder();
      cnf_constant function = (cnf_constant) term;
      functionProto.setName(function.getName());
      for (cnf_expression argument : function.getParam()) {
        functionProto.addArgument(transform_term(argument));
      }
      termProto.setFunction(functionProto.build());
    }
    return termProto.build();
  }

  private TPTPCNF.Predicate transform_predicate(cnf_equality predicate) {
    TPTPCNF.Predicate.Builder predicateProto = TPTPCNF.Predicate.newBuilder();
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

  private TPTPCNF transform_clause(cnf_or clause) {
    TPTPCNF.Builder clauseProto = TPTPCNF.newBuilder();
    for (cnf_not literal : clause.getOr()) {
      TPTPCNF.Literal.Builder literalProto = TPTPCNF.Literal.newBuilder();
      literalProto.setNegated(literal.isNegated());
      literalProto.setPredicate(transform_predicate(literal.getLiteral()));
      clauseProto.addLiteral(literalProto.build());
    }
    return clauseProto.build();
  }

  private void setupParser() {
    Injector injector = new ParserStandaloneSetup().createInjectorAndDoEMFRegistration();
    injector.injectMembers(this);
  }
}
