package com.ruoyi.web.kernel;

import org.opentcs.access.KernelServicePortal;
import org.opentcs.access.rmi.KernelServicePortalBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KernelServiceConfig {
  private static KernelServicePortal kernelServicePortal = null;

  @Bean
  public static KernelServicePortal getKernelServicePortal() {
    if (kernelServicePortal == null) {
      // 在此处配置连接参数，例如用户名、密码等
      kernelServicePortal = new KernelServicePortalBuilder("Alice", "xyz").build();
      kernelServicePortal.login("127.0.0.1", 1099);
    }
    return kernelServicePortal;
  }
}
