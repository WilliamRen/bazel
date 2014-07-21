// Copyright 2014 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.syntax;

import java.util.ArrayList;
import java.util.List;

/**
 * Syntax node for list and tuple literals.
 *
 * (Note that during evaluation, both list and tuple values are represented by
 * java.util.List objects, the only difference between them being whether or not
 * they are mutable.)
 */
public final class ListLiteral extends Expression {

  /**
   * Types of the ListLiteral.
   */
  public static enum Kind {LIST, TUPLE}

  private final Kind kind;

  private final List<Expression> exprs;

  private ListLiteral(Kind kind, List<Expression> exprs) {
    this.kind = kind;
    this.exprs = exprs;
  }

  public static ListLiteral makeList(List<Expression> exprs) {
    return new ListLiteral(Kind.LIST, exprs);
  }

  public static ListLiteral makeTuple(List<Expression> exprs) {
    return new ListLiteral(Kind.TUPLE, exprs);
  }

  /**
   * Returns the list of expressions for each element of the tuple.
   */
  public List<Expression> getElements() {
    return exprs;
  }

  /**
   * Returns true if this list is a tuple (a hash table, immutable list).
   */
  public boolean isTuple() {
    return kind == Kind.TUPLE;
  }

  private static char startChar(Kind kind) {
    switch(kind) {
    case LIST:  return '[';
    case TUPLE: return '(';
    }
    return '[';
  }

  private static char endChar(Kind kind) {
    switch(kind) {
    case LIST:  return ']';
    case TUPLE: return ')';
    }
    return ']';
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(startChar(kind));
    String sep = "";
    for (Expression e : exprs) {
      sb.append(sep);
      sb.append(e);
      sep = ", ";
    }
    sb.append(endChar(kind));
    return sb.toString();
  }

  @Override
  Object eval(Environment env) throws EvalException, InterruptedException {
    List<Object> result = new ArrayList<>();
    for (Expression expr : exprs) {
      // Convert NPEs to EvalExceptions.
      if (expr == null) {
        throw new EvalException(getLocation(), "null expression in " + this);
      }
      result.add(expr.eval(env));
    }
    return EvalUtils.makeSequence(result, isTuple());
  }

  @Override
  public void accept(SyntaxTreeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  SkylarkType validate(ValidationEnvironment env) throws EvalException {
    SkylarkType type = SkylarkType.UNKNOWN;
    for (Expression expr : exprs) {
      SkylarkType nextType = expr.validate(env);
      if (!nextType.isSimple()) {
        throw new EvalException(getLocation(),
            String.format("List cannot contain composite type '%s'", nextType));
      }
      type = type.infer(nextType, "list literal", expr.getLocation(), getLocation());
    }
    return SkylarkType.of(List.class, type.getType());
  }
}
