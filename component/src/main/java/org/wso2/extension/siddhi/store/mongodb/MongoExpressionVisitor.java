/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.extension.siddhi.store.mongodb;

import io.siddhi.core.table.record.BaseExpressionVisitor;
import io.siddhi.query.api.definition.Attribute;
import io.siddhi.query.api.expression.condition.Compare;
import org.wso2.extension.siddhi.store.mongodb.exception.MongoTableException;
import org.wso2.extension.siddhi.store.mongodb.util.Constant;
import org.wso2.extension.siddhi.store.mongodb.util.MongoTableConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which is used by the Siddhi runtime for instructions on converting the SiddhiQL condition to the condition
 * format understood by the MongoDB.
 */
public class MongoExpressionVisitor extends BaseExpressionVisitor {
    private Stack<String> conditionOperands;
    private Map<String, Object> placeholders;

    private int streamVarCount;
    private int constantCount;

    public MongoExpressionVisitor() {
        this.streamVarCount = 0;
        this.constantCount = 0;
        this.conditionOperands = new Stack<>();
        this.placeholders = new HashMap<>();
    }

    public String getCompiledCondition() {
        String compiledCondition = this.conditionOperands.pop();
        for (Map.Entry<String, Object> entry : this.placeholders.entrySet()) {
            if (entry.getValue() instanceof Constant) {
                Constant constant = (Constant) entry.getValue();
                if (constant.getType().equals(Attribute.Type.STRING)) {
                    compiledCondition = compiledCondition.replaceAll(entry.getKey(),
                            "'" + constant.getValue().toString() + "'");
                } else {
                    compiledCondition = compiledCondition.replaceAll(entry.getKey(),
                            constant.getValue().toString());
                }
                this.placeholders.remove(entry.getKey());
            }
        }
        return compiledCondition;
    }

    public Map<String, Object> getPlaceholders() {
        return placeholders;
    }

    @Override
    public void beginVisitAnd() {
    }

    @Override
    public void endVisitAnd() {
        String rightOperand = this.conditionOperands.pop();
        String leftOperand = this.conditionOperands.pop();
        if (rightOperand.matches(MongoTableConstants.REG_EXPRESSION) &&
                leftOperand.matches(MongoTableConstants.REG_EXPRESSION)) {
            String andFilter = MongoTableConstants.MONGO_AND_FILTER
                    .replace(MongoTableConstants.PLACEHOLDER_LEFT_OPERAND, leftOperand)
                    .replace(MongoTableConstants.PLACEHOLDER_RIGHT_OPERAND, rightOperand);
            this.conditionOperands.push(andFilter);
        } else {
            throw new MongoTableException("MongoDB Event Table found operands '" + leftOperand + "' and '" +
                    rightOperand + "' for AND operation. Mongo Event table only supports AND operation between " +
                    "two expressions. Please check your query and try again.");
        }
    }

    @Override
    public void beginVisitAndLeftOperand() {
    }

    @Override
    public void endVisitAndLeftOperand() {
    }

    @Override
    public void beginVisitAndRightOperand() {
    }

    @Override
    public void endVisitAndRightOperand() {
    }

    @Override
    public void beginVisitOr() {
    }

    @Override
    public void endVisitOr() {
        String rightOperand = this.conditionOperands.pop();
        String leftOperand = this.conditionOperands.pop();
        if (rightOperand.matches(MongoTableConstants.REG_EXPRESSION) &&
                leftOperand.matches(MongoTableConstants.REG_EXPRESSION)) {
            String orFilter = MongoTableConstants.MONGO_OR_FILTER
                    .replace(MongoTableConstants.PLACEHOLDER_LEFT_OPERAND, leftOperand)
                    .replace(MongoTableConstants.PLACEHOLDER_RIGHT_OPERAND, rightOperand);
            this.conditionOperands.push(orFilter);
        } else {
            throw new MongoTableException("MongoDB Event Table found operands '" + leftOperand + "' and '" +
                    rightOperand + "' for OR operation.The Mongo Event table only supports OR operation between " +
                    "two expressions. Please check your query and try again.");
        }
    }

    @Override
    public void beginVisitOrLeftOperand() {
    }

    @Override
    public void endVisitOrLeftOperand() {
    }

    @Override
    public void beginVisitOrRightOperand() {
    }

    @Override
    public void endVisitOrRightOperand() {
    }

    @Override
    public void beginVisitNot() {
    }

    @Override
    public void endVisitNot() {
        String operand = this.conditionOperands.pop();
        Pattern pattern = Pattern.compile(MongoTableConstants.REG_SIMPLE_EXPRESSION);
        Matcher matcher = pattern.matcher(operand);
        if (matcher.find()) {
            String fieldName = matcher.group(1);
            operand = operand.replace(fieldName, MongoTableConstants.MONGO_NOT);
            String notFilter = MongoTableConstants.MONGO_NOT_FILTER
                    .replace(MongoTableConstants.PLACEHOLDER_FIELD_NAME, fieldName)
                    .replace(MongoTableConstants.PLACEHOLDER_OPERAND, operand);
            this.conditionOperands.push(notFilter);
        } else {
            throw new MongoTableException("MongoDB Event Table found operand '" + operand + "' for NOT operation. " +
                    "The Mongo Event table only supports NOT operation on simple expression such as compare and null " +
                    "check. Please check your query and try again.");
        }
    }

    @Override
    public void beginVisitCompare(Compare.Operator operator) {
    }

    @Override
    public void endVisitCompare(Compare.Operator operator) {
        String compareFilter = MongoTableConstants.MONGO_COMPARE_FILTER;
        switch (operator) {
            case EQUAL:
                compareFilter = compareFilter.replace(
                        MongoTableConstants.PLACEHOLDER_COMPARE_OPERATOR,
                        MongoTableConstants.MONGO_COMPARE_EQUAL);
                break;
            case GREATER_THAN:
                compareFilter = compareFilter.replace(
                        MongoTableConstants.PLACEHOLDER_COMPARE_OPERATOR,
                        MongoTableConstants.MONGO_COMPARE_GREATER_THAN);
                break;
            case GREATER_THAN_EQUAL:
                compareFilter = compareFilter.replace(
                        MongoTableConstants.PLACEHOLDER_COMPARE_OPERATOR,
                        MongoTableConstants.MONGO_COMPARE_GREATER_THAN_EQUAL);
                break;
            case LESS_THAN:
                compareFilter = compareFilter.replace(
                        MongoTableConstants.PLACEHOLDER_COMPARE_OPERATOR,
                        MongoTableConstants.MONGO_COMPARE_LESS_THAN);
                break;
            case LESS_THAN_EQUAL:
                compareFilter = compareFilter
                        .replace(MongoTableConstants.PLACEHOLDER_COMPARE_OPERATOR,
                                MongoTableConstants.MONGO_COMPARE_LESS_THAN_EQUAL);
                break;
            case NOT_EQUAL:
                compareFilter = compareFilter
                        .replace(MongoTableConstants.PLACEHOLDER_COMPARE_OPERATOR,
                                MongoTableConstants.MONGO_COMPARE_NOT_EQUAL);
                break;
            default:
                throw new MongoTableException("MongoDB Event Table found unknown operator '" + operator + "' for " +
                        "COMPARE operation. Please check your query and try again.");
        }

        String rightOperand = this.conditionOperands.pop();
        String leftOperand = this.conditionOperands.pop();
        if (!rightOperand.matches(MongoTableConstants.REG_EXPRESSION) &&
                !leftOperand.matches(MongoTableConstants.REG_EXPRESSION)) {
            if (leftOperand.matches(MongoTableConstants.REG_STREAMVAR_OR_CONST) !=
                    rightOperand.matches(MongoTableConstants.REG_STREAMVAR_OR_CONST)) {
                if (!rightOperand.matches(MongoTableConstants.REG_STREAMVAR_OR_CONST)) {
                    compareFilter = compareFilter
                            .replace(MongoTableConstants.PLACEHOLDER_LEFT_OPERAND, rightOperand)
                            .replace(MongoTableConstants.PLACEHOLDER_RIGHT_OPERAND, leftOperand);
                } else {
                    compareFilter = compareFilter
                            .replace(MongoTableConstants.PLACEHOLDER_LEFT_OPERAND, leftOperand)
                            .replace(MongoTableConstants.PLACEHOLDER_RIGHT_OPERAND, rightOperand);
                }
                this.conditionOperands.push(compareFilter);
            } else {
                throw new MongoTableException("MongoDB Event Table found operands '" + leftOperand + "' and '" +
                        rightOperand + "' for COMPARE operation. The Mongo Event table only supports COMPARE " +
                        "operation between table attribute and Stream variable/ Constant. Please check your query " +
                        "and try again.");
            }
        } else {
            throw new MongoTableException("MongoDB Event Table found operands '" + leftOperand + "' and '" +
                    rightOperand + "' for COMPARE operation. The Mongo Event table does not supports COMPARE " +
                    "operation between expressions. Please check your query and try again.");
        }
    }

    @Override
    public void beginVisitCompareLeftOperand(Compare.Operator operator) {
    }

    @Override
    public void endVisitCompareLeftOperand(Compare.Operator operator) {
    }

    @Override
    public void beginVisitCompareRightOperand(Compare.Operator operator) {
    }

    @Override
    public void endVisitCompareRightOperand(Compare.Operator operator) {
    }


    @Override
    public void beginVisitIsNull(String streamId) {


    }

    @Override
    public void endVisitIsNull(String streamId) {
        String operand = this.conditionOperands.pop();
        if (!operand.matches(MongoTableConstants.REG_EXPRESSION) &&
                !operand.matches(MongoTableConstants.REG_STREAMVAR_OR_CONST)) {
            String isNullFilter = MongoTableConstants.MONGO_IS_NULL_FILTER
                    .replace(MongoTableConstants.PLACEHOLDER_OPERAND, operand);
            this.conditionOperands.push(isNullFilter);
        } else {
            throw new MongoTableException("MongoDB Event Table found operand '" + operand + "' for is NULL operation." +
                    " The Mongo Event table only supports is NULL operation on a table attribute. Please check your" +
                    " query and try again.");
        }
    }

    @Override
    public void beginVisitIn(String storeId) {
        throw new MongoTableException("MongoDB Event Table does not support IN operations. Please check your" +
                " query and try again.");
    }

    @Override
    public void endVisitIn(String storeId) {
    }

    @Override
    public void beginVisitMath(MathOperator mathOperator) {
        throw new MongoTableException("The Mongo Event table do not support MATH operations. " +
                " Please check your query and try again.");
    }

    @Override
    public void endVisitMath(MathOperator mathOperator) {
    }

    @Override
    public void beginVisitMathLeftOperand(MathOperator mathOperator) {
    }

    @Override
    public void endVisitMathLeftOperand(MathOperator mathOperator) {
    }

    @Override
    public void beginVisitMathRightOperand(MathOperator mathOperator) {
    }

    @Override
    public void endVisitMathRightOperand(MathOperator mathOperator) {
    }

    @Override
    public void beginVisitAttributeFunction(String namespace, String functionName) {
        throw new MongoTableException("The Mongo Event table do not support Attribute functions. " +
                " Please check your query and try again.");
    }


    @Override
    public void endVisitAttributeFunction(String namespace, String functionName) {
    }

    @Override
    public void beginVisitParameterAttributeFunction(int index) {
    }

    @Override
    public void endVisitParameterAttributeFunction(int index) {
    }

    @Override
    public void beginVisitStreamVariable(String id, String streamId, String attributeName, Attribute.Type type) {
        String name = this.generateStreamVarName();
        this.placeholders.put(name, new Attribute(id, type));
        conditionOperands.push(name);
    }

    @Override
    public void endVisitStreamVariable(String id, String streamId, String attributeName, Attribute.Type type) {
    }

    @Override
    public void beginVisitConstant(Object value, Attribute.Type type) {
        String name = this.generateConstantName();
        this.placeholders.put(name, new Constant(value, type));
        conditionOperands.push(name);
    }

    @Override
    public void endVisitConstant(Object value, Attribute.Type type) {
    }

    @Override
    public void beginVisitStoreVariable(String storeId, String attributeName, Attribute.Type type) {
        this.conditionOperands.push(attributeName);
    }

    @Override
    public void endVisitStoreVariable(String storeId, String attributeName, Attribute.Type type) {
    }

    /**
     * Method for generating a temporary placeholder for stream variables.
     *
     * @return a placeholder string of known format.
     */
    private String generateStreamVarName() {
        String name = "strVar" + this.streamVarCount;
        this.streamVarCount++;
        return name;
    }

    /**
     * Method for generating a temporary placeholder for constants.
     *
     * @return a placeholder string of known format.
     */
    private String generateConstantName() {
        String name = "const" + this.constantCount;
        this.constantCount++;
        return name;
    }
}
