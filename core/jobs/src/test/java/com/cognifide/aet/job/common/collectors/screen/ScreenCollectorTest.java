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
package com.cognifide.aet.job.common.collectors.screen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import com.cognifide.aet.communication.api.metadata.exclude.LayoutExclude;
import com.cognifide.aet.job.api.collector.CollectorProperties;
import com.cognifide.aet.job.api.exceptions.ParametersException;
import com.cognifide.aet.vs.ArtifactsDAO;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

@RunWith(MockitoJUnitRunner.class)
public class ScreenCollectorTest {

  private static final String SELECTOR_1 = ".dynamic1";

  private static final String SELECTOR_2 = ".dynamic2";

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private WebElement webElement1;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private WebElement webElement2;

  @Mock
  private RemoteWebDriver webDriver;

  @Mock
  private CollectorProperties collectorProperties;

  @Mock
  private ArtifactsDAO artifactsDAO;

  @InjectMocks
  private ScreenCollector screenCollector;

  private byte[] screenshot = {1, 2};

  @Before
  public void setup() throws Exception {
    when(collectorProperties.getPatternId()).thenReturn(null);
    when(webDriver.getScreenshotAs(OutputType.BYTES)).thenReturn(screenshot);

    when(webElement1.getLocation().moveBy(anyInt(), anyInt())).thenReturn(new Point(0, 0));
    when(webElement2.getLocation().moveBy(anyInt(), anyInt())).thenReturn(new Point(0, 0));

    setExcludeElementsParam(String.format("%s,%s", SELECTOR_1, SELECTOR_2));
  }

  @Test
  public void collectScreenExcludeElements_findAll() throws Exception {
    when(webDriver.findElements(By.cssSelector(SELECTOR_1)))
        .thenReturn(Collections.singletonList(webElement1));
    when(webDriver.findElements(By.cssSelector(SELECTOR_2)))
        .thenReturn(Collections.singletonList(webElement2));

    LayoutExclude exclude = screenCollector.collect().getPayload().getLayoutExclude();

    assertThat(exclude.getExcludedElements()).hasSize(2);
    assertThat(exclude.getNotFoundElements()).isNullOrEmpty();
  }

  @Test
  public void collectScreenExcludeElements_findNone() throws Exception {
    when(webDriver.findElements(By.cssSelector(SELECTOR_1)))
        .thenReturn(Collections.emptyList());
    when(webDriver.findElements(By.cssSelector(SELECTOR_2)))
        .thenReturn(Collections.emptyList());

    LayoutExclude exclude = screenCollector.collect().getPayload().getLayoutExclude();

    assertThat(exclude.getExcludedElements()).isEmpty();
    assertThat(exclude.getNotFoundElements()).hasSize(2);
  }

  @Test
  public void collectScreenExcludeElements_findSome() throws Exception {
    when(webDriver.findElements(By.cssSelector(SELECTOR_1)))
        .thenReturn(Collections.singletonList(webElement1));
    when(webDriver.findElements(By.cssSelector(SELECTOR_2)))
        .thenReturn(Collections.emptyList());

    LayoutExclude exclude = screenCollector.collect().getPayload().getLayoutExclude();

    assertThat(exclude.getExcludedElements()).hasSize(1);
    assertThat(exclude.getNotFoundElements()).hasSize(1);
  }

  private void setExcludeElementsParam(String excludeCssElements) throws ParametersException {
    Map<String, String> params = new HashMap<>();
    params.put("exclude-elements", excludeCssElements);
    screenCollector.setParameters(params);
  }
}
