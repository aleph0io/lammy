/*-
 * =================================LICENSE_START==================================
 * lammy-core
 * ====================================SECTION=====================================
 * Copyright (C) 2023 - 2025 Andy Boothe
 * ====================================SECTION=====================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==================================LICENSE_END===================================
 */
package io.aleph0.lammy.core.base.bean;


import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.collect.Lists;
import io.aleph0.lammy.core.model.bean.ExceptionMapper;
import io.aleph0.lammy.core.model.bean.RequestContext;
import io.aleph0.lammy.core.model.bean.RequestFilter;
import io.aleph0.lammy.core.model.bean.ResponseContext;
import io.aleph0.lammy.core.model.bean.ResponseFilter;

public class BeanLambdaProcessorBaseTest {
  public static class TestRequestFilter implements RequestFilter<Map<String, Object>> {
    public final String id;

    public TestRequestFilter(String id) {
      this.id = id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void filterRequest(RequestContext<Map<String, Object>> requestContext,
        Context lambdaContext) {
      final Map<String, Object> originalValue = requestContext.getInputValue();

      final Map<String, Object> replacementValue = new HashMap<>(originalValue);
      List<String> ids =
          (List<String>) replacementValue.computeIfAbsent("requestFilters", k -> new ArrayList<>());
      ids.add(id);

      requestContext.setInputValue(replacementValue);
    }
  }

  public static class TestResponseFilter
      implements ResponseFilter<Map<String, Object>, Map<String, Object>> {
    public final String id;

    public TestResponseFilter(String id) {
      this.id = id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void filterResponse(RequestContext<Map<String, Object>> requestContext,
        ResponseContext<Map<String, Object>> responseContext, Context lambdaContext) {
      final Map<String, Object> originalValue = responseContext.getOutputValue();

      final Map<String, Object> replacementValue = new HashMap<>(originalValue);
      List<String> ids = (List<String>) replacementValue.computeIfAbsent("responseFilters",
          k -> new ArrayList<>());
      ids.add(id);

      responseContext.setOutputValue(replacementValue);
    }
  }

  public static class FilterTestBeanLambdaProcessor
      extends BeanLambdaProcessorBase<Map<String, Object>, Map<String, Object>> {
    public FilterTestBeanLambdaProcessor() {
      registerRequestFilter(new TestRequestFilter("A"));
      registerRequestFilter(new TestRequestFilter("B"));
      registerResponseFilter(new TestResponseFilter("X"));
      registerResponseFilter(new TestResponseFilter("Y"));
    }

    @Override
    public Map<String, Object> handleBeanRequest(Map<String, Object> input, Context context) {
      final String name = (String) input.get("name");

      final String greeting = "Hello, " + name + "!";

      final Map<String, Object> output = new HashMap<>(input);
      output.put("greeting", greeting);

      return output;
    }
  }

  @Test
  public void givenProcessorWithFilters_whenInvoke_thenFiltersRunAsExpected() {
    final FilterTestBeanLambdaProcessor unit = new FilterTestBeanLambdaProcessor();

    final Map<String, Object> input = new HashMap<>();
    input.put("name", "Gandalf");

    final Map<String, Object> output = unit.handleRequest(input, null);

    final Map<String, Object> expected = new HashMap<>();
    expected.put("name", "Gandalf");
    expected.put("greeting", "Hello, Gandalf!");
    expected.put("requestFilters", Lists.newArrayList("A", "B"));
    expected.put("responseFilters", Lists.newArrayList("X", "Y"));

    assertThat(output).isEqualTo(expected);
  }

  public static class TestExceptionMapper
      implements ExceptionMapper<IllegalArgumentException, Map<String, Object>> {
    @Override
    public Map<String, Object> mapExceptionTo(IllegalArgumentException e, Type responseType,
        Context context) {
      final Map<String, Object> output = new HashMap<>();
      output.put("error", e.getMessage());
      return output;
    }
  }

  public static class ExceptionTestBeanLambdaProcessor
      extends BeanLambdaProcessorBase<Map<String, Object>, Map<String, Object>> {
    private final Supplier<? extends RuntimeException> exceptionFactory;

    public ExceptionTestBeanLambdaProcessor(Supplier<? extends RuntimeException> exceptionFactory) {
      registerExceptionMapper(new TestExceptionMapper());
      this.exceptionFactory = requireNonNull(exceptionFactory);
    }

    @Override
    public Map<String, Object> handleBeanRequest(Map<String, Object> input, Context context) {
      throw exceptionFactory.get();
    }
  }

  @Test
  public void givenProcessorWithExceptionMapper_whenThrowSubclassOfHandledExceptionType_thenExceptionMapped() {
    // We use NumberFormatException here because it's a convenient child of IllegalArgumentException
    final ExceptionTestBeanLambdaProcessor unit =
        new ExceptionTestBeanLambdaProcessor(() -> new NumberFormatException("message"));

    final Map<String, Object> output = unit.handleRequest(new HashMap<>(), null);

    final Map<String, Object> expected = new HashMap<>();
    expected.put("error", "message");

    assertThat(output).isEqualTo(expected);
  }

  @Test
  public void givenProcessorWithExceptionMapper_whenThrowExactHandledExceptionType_thenExceptionMapped() {
    final ExceptionTestBeanLambdaProcessor unit =
        new ExceptionTestBeanLambdaProcessor(() -> new IllegalArgumentException("message"));

    final Map<String, Object> output = unit.handleRequest(new HashMap<>(), null);

    final Map<String, Object> expected = new HashMap<>();
    expected.put("error", "message");

    assertThat(output).isEqualTo(expected);
  }

  @Test
  public void givenProcessorWithExceptionMapper_whenThrowUnhandledExceptionType_thenExceptionMapped() {
    final ExceptionTestBeanLambdaProcessor unit =
        new ExceptionTestBeanLambdaProcessor(() -> new RuntimeException("message"));
    assertThatThrownBy(() -> unit.handleRequest(new HashMap<>(), null))
        .isExactlyInstanceOf(RuntimeException.class);
  }
}
