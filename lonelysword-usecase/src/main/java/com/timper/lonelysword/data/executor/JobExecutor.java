/**
 * Copyright (C) 2015 Fernando Cejas Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.timper.lonelysword.data.executor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

/**
 * User: tangpeng.yang
 * Date: 13/03/2018
 * Description:{@link ThreadPoolExecutor} 工作线程池
 * FIXME
 */
public class JobExecutor implements ThreadExecutor {
  private final ThreadPoolExecutor threadPoolExecutor;

  @Inject
  JobExecutor() {
    this.threadPoolExecutor =
        new ThreadPoolExecutor(3, 5, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new JobThreadFactory());
  }

  @Override public void execute( Runnable runnable) {
    this.threadPoolExecutor.execute(runnable);
  }

  private static class JobThreadFactory implements ThreadFactory {
    private int counter = 0;

    @Override public Thread newThread( Runnable runnable) {
      return new Thread(runnable, "android_" + counter++);
    }
  }
}
