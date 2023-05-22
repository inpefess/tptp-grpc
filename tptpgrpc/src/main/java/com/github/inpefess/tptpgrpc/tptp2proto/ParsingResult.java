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

package com.github.inpefess.tptpgrpc.tptp2proto;

import java.util.HashSet;
import java.util.Set;
import com.github.inpefess.tptpgrpc.tptpproto.Node;

public class ParsingResult {
  public Node.Builder nodeBuilder;
  public Set<String> variableNames;
  public Set<String> functionAndPredicateNames;

  public ParsingResult(Node.Builder nodeBuilder, Set<String> variableNames,
      Set<String> functionAndPredicateNames) {
    this.nodeBuilder = nodeBuilder;
    this.variableNames = variableNames;
    this.functionAndPredicateNames = functionAndPredicateNames;
  }

  public static ParsingResult emptyParsingResult() {
    return new ParsingResult(Node.newBuilder(), new HashSet<>(), new HashSet<>());
  }

  public void addChild(ParsingResult parsingResult) {
    nodeBuilder.addChild(parsingResult.nodeBuilder.build());
    variableNames.addAll(parsingResult.variableNames);
    functionAndPredicateNames.addAll(parsingResult.functionAndPredicateNames);
  }

  public void addFunctionOrPredicate(String functionOrPredicateName) {
    nodeBuilder.setValue(functionOrPredicateName);
    functionAndPredicateNames.add(functionOrPredicateName);
  }

  public void addVariable(String variableName) {
    nodeBuilder.setValue(variableName);
    variableNames.add(variableName);
  }
}
