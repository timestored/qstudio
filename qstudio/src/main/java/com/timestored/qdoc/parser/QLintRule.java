package com.timestored.qdoc.parser;

/**
 * Registry of lint rules with severity and description.
 * Rule IDs are compatible with kx qlint naming conventions.
 */
public enum QLintRule {
    ASSIGN_RESERVED_WORD(Severity.ERROR, "Assignment to a reserved word"),
    COND_EVENARGS(Severity.ERROR, "Conditional $ should not have even number of arguments"),
    INVALID_ESCAPE(Severity.ERROR, "Invalid escape sequence in string"),
    DEPRECATED_DATETIME(Severity.WARNING, "Datetime type has been deprecated"),
    EMPTY_IF(Severity.WARNING, "If statement lacks code to execute"),
    CAST_TYPE_NUMERICAL(Severity.WARNING, "Casting using short type indicator is unnecessarily unclear"),
    STATEMENT_IN_EXPR(Severity.ERROR, "If, while, or do statement used in expression"),
    INVALID_ASSIGN(Severity.ERROR, "Attempt to assign to a string, symbol, or number"),
    UNUSED_PARAM(Severity.WARNING, "Parameter declared but never used"),
    UNUSED_VAR(Severity.WARNING, "Variable declared but never used"),
    UNDECLARED_VAR(Severity.WARNING, "Undeclared variable will be treated as global"),
    DECLARED_AFTER_USE(Severity.ERROR, "Variable was declared after being used"),
    UNREACHABLE_CODE(Severity.WARNING, "Code after return statement is unreachable"),
    NEED_EXPLICIT_RETURN(Severity.WARNING, "Function ends with assignment - returns generic null"),
    TOO_MANY_LOCALS(Severity.ERROR, "Too many local variables in function"),
    DEBUG_FUNCTION(Severity.WARNING, "Debug function call should not be in release code"),
    LINE_TOO_LONG(Severity.WARNING, "Line exceeds maximum length"),
    FUNCTION_TOO_LONG(Severity.WARNING, "Function exceeds maximum line count"),
    TOO_MANY_PARAMS(Severity.WARNING, "Function has too many parameters"),
    UNDERSCORE_IN_NAME(Severity.WARNING, "Underscore in name conflicts with drop operator"),
    IMPLICIT_PARAMS(Severity.INFO, "Function uses implicit parameters - consider naming them"),
    XYZ_AS_LOCAL(Severity.WARNING, "x/y/z used as local variable - reserved for implicit parameters"),
    TRAILING_SEMICOLON(Severity.WARNING, "Trailing semicolon after function - returns generic null"),
    MULTIPLE_STATEMENTS_PER_LINE(Severity.INFO, "Multiple statements on one line"),
    TOO_MANY_GLOBALS(Severity.ERROR, "Too many global assignments in function"),
    TOO_MANY_CONSTANTS(Severity.ERROR, "Too many constants in function"),
    GLOBAL_PEACH(Severity.ERROR, "Modifying globals inside peach is not allowed"),
    CONDITIONALLY_DECLARED(Severity.WARNING, "Variable may be undefined - declared only in one branch"),
    UNINDENTED_CODE(Severity.WARNING, "Multiline expression must be indented after first line"),
    NAME_COLLISION(Severity.WARNING, "Assignment could overwrite built-in function"),
    BACKWARD_COMPATIBILITY(Severity.WARNING, "Feature may not be available in older kdb versions"),
    UNDEFINED_REFERENCE(Severity.ERROR, "Reference to undefined symbol in known namespace"),
    RESERVED_NAME(Severity.ERROR, "File has reserved name"),
    WRONG_ARG_COUNT(Severity.ERROR, "Wrong number of arguments in function call"),
    PARAM_TYPE_MISMATCH(Severity.ERROR, "Literal argument type conflicts with parameter name hint"),
    DEPRECATED_INTERNAL(Severity.ERROR, "Internal function has a q replacement"),
    UNARY_FORM(Severity.ERROR, "Unary operator form should use q keyword"),
    TODO_MARKER(Severity.INFO, "TODO/FIXME marker in comment");

    public enum Severity { ERROR, WARNING, INFO }

    public final Severity severity;
    public final String description;

    QLintRule(Severity severity, String description) {
        this.severity = severity;
        this.description = description;
    }
}
