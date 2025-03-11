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
package io.aleph0.lammy.core.serialization;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.CustomPojoSerializer;

/**
 * Dear Amazon, please add the {@link Context} to the {@link CustomPojoSerializer} interface
 * {@link CustomPojoSerializer#fromJson(InputStream, Type)} and
 * {@link CustomPojoSerializer#toJson(Object, OutputStream, Type)} methods so we can use it in our
 * serializers. Just in case there was some serializer that needed it. Like maybe the built-in
 * {@link PlatformCustomPojoSerializer}. Or something. Hypothetically.
 */
public interface ContextAwareCustomPojoSerializer {
  public static ContextAwareCustomPojoSerializer fromCustomPojoSerializer(
      CustomPojoSerializer serializer) {
    return new ContextAwareCustomPojoSerializer() {
      @Override
      public <T> T fromJson(InputStream src, Type type, Context context) {
        return serializer.fromJson(src, type);
      }

      @Override
      public <T> void toJson(T src, OutputStream dest, Type type, Context context) {
        serializer.toJson(src, dest, type);
      }
    };
  }

  public <T> T fromJson(InputStream src, Type type, Context context);

  public <T> void toJson(T src, OutputStream dest, Type type, Context context);
}
