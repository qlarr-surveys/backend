function navigate(navigationInput) {
    frankieVariables = {};
    var valuesKeys = Object.keys(navigationInput.values)
    var codes = navigationInput.codes;

    codes.forEach(function(componentCode) {
        eval(componentCode + " = {};");
        frankieVariables[componentCode] = eval(componentCode);
    })
    // first we will set all the variables from DB
    valuesKeys.forEach(function(key){
        var value = navigationInput.values[key]
        var names = key.split('.')
        frankieVariables[names[0]][names[1]] = verifyValue(names[1], value.returnType.name, value.value)
    });

    navigationInput.sequence.forEach(function(systemInstruction, index) {
        var instruction = systemInstruction.instruction
        // Then we run active instructions
        // Or Defaults if they don't have value already
        if (instruction.isActive) {
            frankieVariables[systemInstruction.componentCode][instruction.code] = runInstruction(instruction.code, instruction.text, instruction.returnType.name)
        } else if (instruction.text != null && typeof frankieVariables[systemInstruction.componentCode][instruction.code] === 'undefined') {
            if (instruction.returnType.name == "String") {
                var text = "\"" + instruction.text + "\""
            } else {
                var text = instruction.text
            }
            try {
                frankieVariables[systemInstruction.componentCode][instruction.code] = JSON.parse(text)
            } catch (e) {
                //print(e)
                frankieVariables[systemInstruction.componentCode][instruction.code] = defaultValue(instruction.code, instruction.returnType.name);
            }
        }
    })
    navigationInput.formatInstructions.forEach(function(formatInstruction, index) {
        var instruction = formatInstruction.instruction
        frankieVariables[formatInstruction.componentCode][instruction.code] = runInstruction(instruction.code, instruction.text, "Map")
    })
    return JSON.stringify(frankieVariables);
}


function validate(validationInput) {
    frankieVariables = {};

    var codes = validationInput.codes;

    codes.forEach(function(componentCode) {
        eval(componentCode + " = {};");
        frankieVariables[componentCode] = eval(componentCode);
    })

    validationInput.systemInstructions.forEach(function(systemInstruction, index) {
        frankieVariables[systemInstruction.componentCode][systemInstruction.instruction.code] = defaultValue(systemInstruction.instruction.code, systemInstruction.instruction.returnType.name);
    })

    validationInput.systemInstructions.forEach(function(systemInstruction, index) {
        validationInput.systemInstructions[index] = validateSystemInstruction(systemInstruction)
    })

    var validationOutput = {
        "systemInstructions": validationInput.systemInstructions,
        "variables": frankieVariables
    }
    return JSON.stringify(validationOutput);
}

function verifyValue(code, returnTypeName, value) {
    if (isCorrectReturnType(returnTypeName, value)) {
        return value;
    } else {
        return defaultValue(code, returnTypeName);
    }
}

function validateSystemInstruction(componentInstruction) {
    var instruction = componentInstruction.instruction
    var code = instruction.code
    if (code == "value" && (instruction.text === "" )) {
        return componentInstruction
    }
    var returnTypeName = instruction.returnType.name
    var componentCode = componentInstruction.componentCode
    try {
        if (instruction.isActive && returnTypeName != "Map" && returnTypeName != "File") {
            var value = eval(instruction.text);
        } else if (instruction.isActive && (returnTypeName == "Map" || returnTypeName == "File")) {
            eval("var value = " + instruction.text + ";");
        } else if (returnTypeName == "Int" || returnTypeName == "Double" || returnTypeName == "Boolean" || returnTypeName == "Map" || returnTypeName == "File" || returnTypeName == "List") {
            try {
                var value = JSON.parse(instruction.text);
            } catch (e) {
                //print(e)
                var value = instruction.text;
            }
        } else {
            var value = instruction.text;
        }
    } catch (e) {
        //print(e)
        processException(instruction, "SYNTAX_ERROR", e.name + ": " + e.message)
        var value = defaultValue(code, returnTypeName);
    }
    if (code != "value" && typeof value === "undefined") {
        processException(instruction, "NULL_RETURN", "")
        var value = defaultValue(code, returnTypeName);
    } else if (code != "value" && typeof value !== "undefined" && !isCorrectReturnType(returnTypeName, value)) {
        processException(instruction, "INPUT_FORMAT_ERROR", "expected: " + returnTypeName + ", found: " + typeof value)
        var value = defaultValue(code, returnTypeName);
    }
    if (typeof value !== "undefined") {
        frankieVariables[componentCode][instruction.code] = value;
    }
    return componentInstruction;
}

function validateFormatInstruction(componentInstruction) {
    var instruction = componentInstruction.instruction
    var code = instruction.code
    var returnTypeName = "Map"
    var componentCode = componentInstruction.componentCode
    try {
//        print("var value = " + instruction.text + ";")
        eval("var value = " + instruction.text + ";");
    } catch (e) {
        //print(e)
        processException(instruction, "SYNTAX_ERROR", e.name + ": " + e.message)
        var value = defaultValue(code, returnTypeName);
    }
    if (typeof value === "undefined") {
        processException(instruction, "NULL_RETURN", "")
        var value = defaultValue(code, returnTypeName);
    } else if (!isCorrectReturnType(returnTypeName, value)) {
        processException(instruction, "INPUT_FORMAT_ERROR", "expected: " + returnTypeName + ", found: " + typeof value)
        var value = defaultValue(code, returnTypeName);
    }
    if (typeof value !== "undefined") {
        frankieVariables[componentCode][instruction.code] = value;
    }
    return componentInstruction;
}

function runInstruction(code, instructionText, returnTypeName) {
    try {
        if (returnTypeName != "Map" && returnTypeName != "File") {
            var value = eval(instructionText);
        } else {
            eval("var value = " + instructionText + ";");
        }
        if (isCorrectReturnType(returnTypeName, value)) {
            return value;
        } else {
            return defaultValue(code, returnTypeName);
        }
    } catch (e) {
        //print(e)
        return defaultValue(code, returnTypeName);
    }
}

function processException(componentInstruction, resultType, errorMessage) {
    var error = scriptError(resultType, errorMessage);
    if (typeof componentInstruction.errors === "undefined") {
        componentInstruction.errors = [];
    }
    componentInstruction.errors.push(error);
}

function isCorrectReturnType(returnTypeName, value) {
    switch (returnTypeName) {
        case "Boolean":
            return typeof value === "boolean";
            break;
        case "Date":
            return FrankieScripts.isValidSqlDateTime(value);
            break;
        case "Int":
            return typeof value === "number" && value % 1 == 0;
            break;
        case "Double":
            return typeof value === "number";
            break;
        case "List":
            return Array.isArray(value);
            break;
        case "String":
            return typeof value === "string";
            break;
        case "Map":
            return typeof value === "object";
            break;
        case "File":
            if (typeof value !== "object") {
                return false;
            } else {
                var keys = Object.keys(value);
                return keys.indexOf("filename") >= 0 && keys.indexOf("stored_filename") >= 0 && keys.indexOf("size") >= 0 && keys.indexOf("type") >= 0
            }
            break;
        default:
            return false;
    }
    return false;
}

function defaultValue(code, returnTypeName) {
    if (code == "value") {
        return undefined;
    } else if (code == "relevance" || code == "conditional_relevance" || code == "validity") {
        return true
    }
    switch (returnTypeName) {
        case "Boolean":
            return false;
            break;
        case "Date":
            return "1970-01-01 00:00:00";
            break;
        case "String":
            return "";
            break;
        case "Int":
        case "Double":
            return 0;
            break;
        case "List":
            return [];
            break;
        case "Map":
            return {};
            break;
        case "File":
            return {
                filename: "", stored_filename: "", size: 0, type: ""
            };
            break;
        default:
            return "";
    }
    return "";
}

function scriptError(resultType, errorMessage) {
    var error = {};
    error.name = "ScriptFailure";
    error.scriptFailure = {};
    error.scriptFailure.resultType = resultType;
    error.scriptFailure.valueOrError = errorMessage;
    return error;
}