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
package io.aleph0.lammy.core.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CountingOutputStream extends FilterOutputStream {
  private long count;

  public CountingOutputStream(OutputStream delegate) {
    super(delegate);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    count = count + len;
    super.write(b, off, len);
  }

  @Override
  public void write(byte[] b) throws IOException {
    count = count + b.length;
    super.write(b);
  }

  @Override
  public void write(int b) throws IOException {
    count = count + 1L;
    super.write(b);
  }

  public long getCount() {
    return count;
  }
}
