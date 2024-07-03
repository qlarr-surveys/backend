package com.frankie.expressionmanager.ext

import java.util.regex.Pattern


fun isValidVariableName(varName: String): Boolean = Pattern.compile(VAR_NAME_PATTERN).matcher(varName).matches()

private const val VAR_NAME_PATTERN = "[a-zA-Z][a-zA-Z0-9_]*"