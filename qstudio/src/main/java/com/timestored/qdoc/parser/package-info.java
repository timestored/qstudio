/**
 * Comprehensive kdb+/q parser designed to power advanced linting, static analysis,
 * and editor tooling. The parser is O(n) with no backtracking (Pratt parser) and
 * produces a full AST from q source code.
 *
 * Key classes: QLexer (tokenizer), QParser (AST), QLinter (33 rules),
 * QCodeMetrics (quality metrics), QProjectIndex (cross-file symbols).
 */
package com.timestored.qdoc.parser;
