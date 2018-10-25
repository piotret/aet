/**
 * AET
 *
 * Copyright (C) 2013 Cognifide Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.cognifide.aet.job.common.comparators.apicontract;

import com.cognifide.aet.communication.api.metadata.ComparatorStepResult;
import com.cognifide.aet.job.api.comparator.ComparatorJob;
import com.cognifide.aet.job.api.comparator.ComparatorProperties;
import com.cognifide.aet.job.api.datafilter.DataFilterJob;
import com.cognifide.aet.job.api.exceptions.ParametersException;
import com.cognifide.aet.job.api.exceptions.ProcessingException;
import com.cognifide.aet.job.common.comparators.apicontract.diff.ResultDelta;
import com.cognifide.aet.job.common.comparators.apicontract.diff.DiffParser;
import com.cognifide.aet.vs.ArtifactsDAO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ApiContractComparator implements ComparatorJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiContractComparator.class);

    public static final String COMPARATOR_TYPE = "apicontract";

    public static final String COMPARATOR_NAME = "apicontract";

    private static final String SOURCE_COMPARE_TYPE = "compareType";

    private static final String DEFAULT_VAL_REPLACEMENT = "";

    private final ComparatorProperties properties;

    private final DiffParser diffParser;

    private final ArtifactsDAO artifactsDAO;

    private ApiContractCompareType sourceCompareType = ApiContractCompareType.ALL;

    private final List<DataFilterJob> dataFilterJobs;

    public ApiContractComparator(ArtifactsDAO artifactsDAO, ComparatorProperties properties,
                                 DiffParser diffParser,
                                 List<DataFilterJob> dataFilterJobs) {
        this.artifactsDAO = artifactsDAO;
        this.properties = properties;
        this.diffParser = diffParser;
        this.dataFilterJobs = dataFilterJobs;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final ComparatorStepResult compare() throws ProcessingException {
        final ComparatorStepResult result;

        // TODO: prepare output from JSON comparison

        try {
            String patternJson = formatCode(
                    artifactsDAO.getArtifactAsString(properties, properties.getPatternId()));
            String dataSource = formatCode(
                    artifactsDAO.getArtifactAsString(properties, properties.getCollectedId()));

            for (DataFilterJob<String> dataFilterJob : dataFilterJobs) {
                LOGGER.info("Starting {}. Company: {} Project: {}",
                        dataFilterJob.getInfo(), properties.getCompany(), properties.getProject());

                dataSource = dataFilterJob.modifyData(dataSource);

                LOGGER.info("Successfully ended data modifications using  {}. Company: {} Project: {}",
                        dataFilterJob.getInfo(), properties.getCompany(), properties.getProject());

                patternJson = dataFilterJob.modifyPattern(patternJson);

                LOGGER.info("Successfully ended pattern modifications using {}. Company: {} Project: {}",
                        dataFilterJob.getInfo(), properties.getCompany(), properties.getProject());
            }

            if (StringUtils.isNotBlank(patternJson)) {
                boolean compareTrimmedLines = shouldCompareTrimmedLines(sourceCompareType);
                final List<ResultDelta> deltas = diffParser
                        .generateDiffs(patternJson, dataSource, compareTrimmedLines);
                if (deltas.isEmpty()) {
                    result = new ComparatorStepResult(null, ComparatorStepResult.Status.PASSED, false);
                } else {
                    result = new ComparatorStepResult(artifactsDAO.saveArtifactInJsonFormat(properties,
                            Collections.singletonMap("differences", deltas)),
                            ComparatorStepResult.Status.FAILED, true);
                    result.addData("formattedPattern", artifactsDAO.saveArtifact(properties, patternJson));
                    result.addData("formattedSource", artifactsDAO.saveArtifact(properties, dataSource));
                    result.addData("sourceCompareType", sourceCompareType.name());
                }
            } else {
                result = new ComparatorStepResult(null, ComparatorStepResult.Status.PASSED);
            }
        } catch (Exception e) {
            throw new ProcessingException(e.getMessage(), e);
        }

            return result;
    }


    private boolean shouldCompareTrimmedLines(ApiContractCompareType sourceCompareType) {
        return ApiContractCompareType.ALL.equals(sourceCompareType)
                || ApiContractCompareType.JSONMARKUP.equals(sourceCompareType);
    }

    @Override
    public void setParameters(final Map<String, String> params) throws ParametersException {
        if (params.containsKey(SOURCE_COMPARE_TYPE)) {
            this.sourceCompareType = ApiContractCompareType
                    .valueOf(params.get(SOURCE_COMPARE_TYPE).toUpperCase());
        }
    }

    private String formatCode(String code) {
        String result;
        switch (sourceCompareType) {
            case JSONMARKUP:
                result = formatJsonMarkup(code);
                break;
            case JSONSCHEMA:
                result = formatJsonSchema(code);
                break;
            case ALL:
                result = formatCodeAllFormatted(code);
                break;
            default:
                result = code;
                break;
        }
        return result;
    }

    // package scoped for unit test
    String removeEmptyLines(String source) {
        String result = source;
        if (StringUtils.isNotBlank(source)) {
            result = result.replaceAll("(?m)^[ \\t]*[\\r\\n]+", "");
        }
        return result;
    }

    private String replaceValues(String json, String replacement) {
        String result = json;
        if (StringUtils.isNotBlank(json)) {
            result = result.replaceAll(":[ \\t]*\"[ \\S]+\"", ": \"" + replacement + "\"");
            result = result.replaceAll("\"([ \\S]+)\"[ \\t]*:[ \\t]*[.\\d]+", "$1 : 0");
        }
        // Debug - remove it
        Random rnd = new Random();
        result = String.valueOf(rnd.nextInt()) + result;

        return result;
    }

    private String formatJsonMarkup(String code) {
        String out = replaceValues(removeEmptyLines(code), DEFAULT_VAL_REPLACEMENT);

        // TODO: add formatting here
        return out;
    }

    private String formatJsonSchema(String code) {
        String out = code;

        // TODO: format it here
        return out;
    }

    private String formatCodeAllFormatted(String code) {
        return removeEmptyLines(code);
    }

}
